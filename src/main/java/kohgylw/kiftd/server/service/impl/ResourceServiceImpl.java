package kohgylw.kiftd.server.service.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.enumeration.PowerPointType;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.VideoTranscodeThread;
import kohgylw.kiftd.server.service.ResourceService;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.ContentTypeMap;
import kohgylw.kiftd.server.util.Docx2PDFUtil;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.KiftdFFMPEGLocator;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.NoticeUtil;
import kohgylw.kiftd.server.util.PowerPoint2PDFUtil;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.server.util.Txt2PDFUtil;
import kohgylw.kiftd.server.util.TxtCharsetGetter;
import kohgylw.kiftd.server.util.VideoTranscodeUtil;

//资源服务类，所有处理非下载流请求的工作均在此完成
@Service
public class ResourceServiceImpl implements ResourceService {

	@Resource
	private NodeMapper nm;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private LogUtil lu;
	@Resource
	private Docx2PDFUtil d2pu;
	@Resource
	private Txt2PDFUtil t2pu;
	@Resource
	private VideoTranscodeUtil vtu;
	@Resource
	private PowerPoint2PDFUtil p2pu;
	@Resource
	private FolderUtil fu;
	@Resource
	private FolderMapper fm;
	@Resource
	private TxtCharsetGetter tcg;
	@Resource
	private NoticeUtil nu;
	@Resource
	private ContentTypeMap ctm;
	@Resource
	private KiftdFFMPEGLocator kfl;
	@Resource
	private IpAddrGetter idg;

	private static final long RESOURCE_CACHE_MAX_AGE = 1800L;// 资源缓存的寿命30分钟，正好对应账户的自动注销时间

	// 提供资源的输出流，原理与下载相同，但是个别细节有区别
	@Override
	public void getResource(String fid, HttpServletRequest request, HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (fid != null) {
			Node n = nm.queryById(fid);
			if (n != null) {
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(n.getFileParentFolder()))
						&& ConfigureReader.instance().accessFolder(fm.queryById(n.getFileParentFolder()), account)) {
					File file = fbu.getFileFromBlocks(n);
					if (file != null && file.isFile()) {
						String suffix = "";
						if (n.getFileName().indexOf(".") >= 0) {
							suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".")).trim().toLowerCase();
						}
						String contentType = ctm.getContentType(suffix);
						switch (suffix) {
						case ".mp4":
						case ".webm":
						case ".mov":
						case ".avi":
						case ".wmv":
						case ".mkv":
						case ".flv":
							if (kfl.getExecutablePath() != null) {
								contentType = "video/mp4";
								synchronized (VideoTranscodeUtil.videoTranscodeThreads) {
									VideoTranscodeThread vtt = VideoTranscodeUtil.videoTranscodeThreads.get(fid);
									if (vtt != null) {// 针对需要转码的视频（在转码列表中存在）
										File f = new File(ConfigureReader.instance().getTemporaryfilePath(),
												vtt.getOutputFileName());
										if (f.isFile() && vtt.getProgress().equals("FIN")) {// 判断是否转码成功
											file = f;// 成功，则播放它
										} else {
											try {
												response.sendError(500);// 否则，返回处理失败
											} catch (IOException e) {
											}
											return;
										}
									}
								}
							}
							break;
						default:
							break;
						}
						String ip = idg.getIpAddr(request);
						String range = request.getHeader("Range");
						int status = sendResource(file, n.getFileName(), contentType, request, response);
						if (status == HttpServletResponse.SC_OK || (range != null && range.startsWith("bytes=0-"))) {
							this.lu.writeDownloadFileEvent(account, ip, n);
						}
						return;
					}
				} else {// 处理资源未被授权的问题
					try {
						response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					} catch (IOException e) {
					}
				}
			}
		}
		try {
			//  处理无法下载的资源
			response.sendError(404);
		} catch (IOException e) {
		}
	}

	/**
	 * 
	 * <h2>返回资源</h2>
	 * <p>
	 * 该方法用于回传某个文件资源，支持断点续传。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param resource
	 *            java.io.File 要发送的文件资源
	 * @param fname
	 *            java.lang.String 要传递给客户端的文件名，会加入到响应头中
	 * @param contentType
	 *            java.lang.String 返回资源的CONTENT_TYPE标识名，例如“text/html”
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @param response
	 *            javax.servlet.http.HttpServletResponse 响应对象
	 * @return int 操作完毕后返回的状态码，例如“200”
	 */
	private int sendResource(File resource, String fname, String contentType, HttpServletRequest request,
			HttpServletResponse response) {
		// 状态码，初始为200
		int status = HttpServletResponse.SC_OK;
		// 开始资源传输
		try (RandomAccessFile randomFile = new RandomAccessFile(resource, "r")) {
			long contentLength = randomFile.length();
			final String lastModified = ServerTimeUtil.getLastModifiedFormBlock(resource);
			// 如果请求中包含了对本地缓存的过期判定参数，则执行过期判定
			final String eTag = this.fbu.getETag(resource);
			final String ifModifiedSince = request.getHeader("If-Modified-Since");
			final String ifNoneMatch = request.getHeader("If-None-Match");
			if (ifModifiedSince != null || ifNoneMatch != null) {
				if (ifNoneMatch != null) {
					if (ifNoneMatch.trim().equals(eTag)) {
						status = HttpServletResponse.SC_NOT_MODIFIED;
						response.setStatus(status);// 304
						return status;
					}
				} else {
					if (ifModifiedSince.trim().equals(lastModified)) {
						status = HttpServletResponse.SC_NOT_MODIFIED;
						response.setStatus(status);// 304
						return status;
					}
				}
			}
			// 检查断点续传请求是否过期
			String ifUnmodifiedSince = request.getHeader("If-Unmodified-Since");
			if (ifUnmodifiedSince != null && !(ifUnmodifiedSince.trim().equals(lastModified))) {
				status = HttpServletResponse.SC_PRECONDITION_FAILED;
				response.setStatus(status);// 412
				return status;
			}
			String ifMatch = request.getHeader("If-Match");
			if (ifMatch != null && !(ifMatch.trim().equals(eTag))) {
				status = HttpServletResponse.SC_PRECONDITION_FAILED;
				response.setStatus(status);// 412
				return status;
			}
			// 如果缓存过期或无缓存，则按请求参数返回数据
			String range = request.getHeader("Range");
			long start = 0, end = 0;
			if (range != null && range.startsWith("bytes=")) {
				String[] values = range.split("=")[1].split("-");
				start = Long.parseLong(values[0]);
				if (values.length > 1) {
					end = Long.parseLong(values[1]);
				}
			}
			long requestSize = 0;
			if (end != 0 && end > start) {
				requestSize = end - start + 1;
			} else {
				requestSize = Long.MAX_VALUE;
			}
			byte[] buffer = new byte[ConfigureReader.instance().getBuffSize()];
			response.setContentType(contentType);
			response.setHeader("Accept-Ranges", "bytes");
			response.setHeader("ETag", this.fbu.getETag(resource));
			response.setHeader("Last-Modified", ServerTimeUtil.getLastModifiedFormBlock(resource));
			response.setHeader("Cache-Control", "max-age=" + RESOURCE_CACHE_MAX_AGE);
			// 第一次请求只返回content length来让客户端请求多次实际数据
			final String ifRange = request.getHeader("If-Range");
			if (range != null && range.startsWith("bytes=")
					&& (ifRange == null || ifRange.trim().equals(eTag) || ifRange.trim().equals(lastModified))) {
				// 以后的多次以断点续传的方式来返回视频数据
				status = HttpServletResponse.SC_PARTIAL_CONTENT;
				response.setStatus(status);// 206
				long requestStart = 0, requestEnd = 0;
				String[] ranges = range.split("=");
				if (ranges.length > 1) {
					String[] rangeDatas = ranges[1].split("-");
					requestStart = Long.parseLong(rangeDatas[0]);
					if (rangeDatas.length > 1) {
						requestEnd = Long.parseLong(rangeDatas[1]);
					}
				}
				long length = 0;
				if (requestEnd > 0) {
					length = requestEnd - requestStart + 1;
					response.setHeader("Content-length", "" + length);
					response.setHeader("Content-Range",
							"bytes " + requestStart + "-" + requestEnd + "/" + contentLength);
				} else {
					length = contentLength - requestStart;
					response.setHeader("Content-length", "" + length);
					response.setHeader("Content-Range",
							"bytes " + requestStart + "-" + (contentLength - 1) + "/" + contentLength);
				}
			} else {
				response.setHeader("Content-length", contentLength + "");
			}
			ServletOutputStream out = response.getOutputStream();
			long needSize = requestSize;
			randomFile.seek(start);
			while (needSize > 0) {
				int len = randomFile.read(buffer);
				if (needSize < buffer.length) {
					out.write(buffer, 0, (int) needSize);
				} else {
					out.write(buffer, 0, len);
					if (len < buffer.length) {
						break;
					}
				}
				needSize -= buffer.length;
			}
			out.close();
			return status;
		} catch (Exception e) {
			status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			return status;
		}
	}

	// 对word的预览实现
	@Override
	public void getWordView(String fileId, HttpServletRequest request, HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 权限检查
		if (fileId != null) {
			Node n = nm.queryById(fileId);
			if (n != null) {
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(n.getFileParentFolder()))
						&& ConfigureReader.instance().accessFolder(fm.queryById(n.getFileParentFolder()), account)) {
					File file = fbu.getFileFromBlocks(n);
					if (file != null && file.isFile()) {
						// 后缀检查
						String suffix = "";
						if (n.getFileName().indexOf(".") >= 0) {
							suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".")).trim().toLowerCase();
						}
						if (".docx".equals(suffix)) {
							String contentType = ctm.getContentType(".pdf");
							response.setContentType(contentType);
							String ip = idg.getIpAddr(request);
							// 执行转换并写出输出流
							try {
								d2pu.convertPdf(new FileInputStream(file), response.getOutputStream());
								lu.writeDownloadFileEvent(account, ip, n);
								return;
							} catch (IOException e) {
							} catch (Exception e) {
								Printer.instance.print(e.getMessage());
								lu.writeException(e);
							}
						}
					}
				}
			}
		}
		try {
			response.sendError(500);
		} catch (Exception e1) {
		}
	}

	// 对TXT预览的实现
	@Override
	public void getTxtView(String fileId, HttpServletRequest request, HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 权限检查
		if (fileId != null) {
			Node n = nm.queryById(fileId);
			if (n != null) {
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(n.getFileParentFolder()))
						&& ConfigureReader.instance().accessFolder(fm.queryById(n.getFileParentFolder()), account)) {
					File file = fbu.getFileFromBlocks(n);
					if (file != null && file.isFile()) {
						// 后缀检查
						String suffix = "";
						if (n.getFileName().indexOf(".") >= 0) {
							suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".")).trim().toLowerCase();
						}
						if (".txt".equals(suffix)) {
							String contentType = ctm.getContentType(".pdf");
							response.setContentType(contentType);
							String ip = idg.getIpAddr(request);
							// 执行转换并写出输出流
							try {
								t2pu.convertPdf(file, response.getOutputStream());
								lu.writeDownloadFileEvent(account, ip, n);
								return;
							} catch (Exception e) {
								Printer.instance.print(e.getMessage());
								lu.writeException(e);
							}
						}
					}
				}
			}
		}
		try {
			response.sendError(500);
		} catch (Exception e1) {
		}
	}

	@Override
	public String getVideoTranscodeStatus(HttpServletRequest request) {
		if (kfl.getExecutablePath() != null) {
			String fId = request.getParameter("fileId");
			if (fId != null) {
				try {
					return vtu.getTranscodeProcess(fId);
				} catch (Exception e) {
					Printer.instance.print("错误：视频转码功能出现意外错误。详细信息：" + e.getMessage());
					lu.writeException(e);
				}
			}
		}
		return "ERROR";
	}

	// 对PPT预览的实现
	@Override
	public void getPPTView(String fileId, HttpServletRequest request, HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 权限检查
		if (fileId != null) {
			Node n = nm.queryById(fileId);
			if (n != null) {
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(n.getFileParentFolder()))
						&& ConfigureReader.instance().accessFolder(fm.queryById(n.getFileParentFolder()), account)) {
					File file = fbu.getFileFromBlocks(n);
					if (file != null && file.isFile()) {
						// 后缀检查
						String suffix = "";
						if (n.getFileName().indexOf(".") >= 0) {
							suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".")).trim().toLowerCase();
						}
						switch (suffix) {
						case ".ppt":
						case ".pptx":
							String contentType = ctm.getContentType(".pdf");
							response.setContentType(contentType);
							String ip = idg.getIpAddr(request);
							// 执行转换并写出输出流
							try {
								p2pu.convertPdf(new FileInputStream(file), response.getOutputStream(),
										".ppt".equals(suffix) ? PowerPointType.PPT : PowerPointType.PPTX);
								lu.writeDownloadFileEvent(account, ip, n);
								return;
							} catch (IOException e) {
							} catch (Exception e) {
								Printer.instance.print(e.getMessage());
								lu.writeException(e);
							}
							break;
						default:
							break;
						}
					}
				}
			}
		}
		try {
			response.sendError(500);
		} catch (Exception e1) {
		}
	}

	@Override
	public void getLRContextByUTF8(String fileId, HttpServletRequest request, HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String ip = idg.getIpAddr(request);
		// 权限检查
		if (fileId != null) {
			Node n = nm.queryById(fileId);
			if (n != null) {
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(n.getFileParentFolder()))
						&& ConfigureReader.instance().accessFolder(fm.queryById(n.getFileParentFolder()), account)) {
					File file = fbu.getFileFromBlocks(n);
					if (file != null && file.isFile()) {
						// 检查是否有可用缓存
						String ifModifiedSince = request.getHeader("If-Modified-Since");
						if (ifModifiedSince != null
								&& ifModifiedSince.trim().equals(ServerTimeUtil.getLastModifiedFormBlock(file))) {
							response.setStatus(304);
							return;
						}
						String ifNoneMatch = request.getHeader("If-None-Match");
						if (ifNoneMatch != null && ifNoneMatch.trim().equals(this.fbu.getETag(file))) {
							response.setStatus(304);
							return;
						}
						// 如无，则返回新数据
						// 后缀检查
						String suffix = "";
						if (n.getFileName().indexOf(".") >= 0) {
							suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".")).trim().toLowerCase();
						}
						if (".lrc".equals(suffix)) {
							String contentType = "text/plain";
							response.setContentType(contentType);
							response.setCharacterEncoding("UTF-8");
							response.setHeader("ETag", this.fbu.getETag(file));
							response.setHeader("Last-Modified", ServerTimeUtil.getLastModifiedFormBlock(file));
							response.setHeader("Cache-Control", "max-age=" + RESOURCE_CACHE_MAX_AGE);
							// 执行转换并写出输出流
							try {
								String inputFileEncode = tcg.getTxtCharset(new FileInputStream(file));
								BufferedReader bufferedReader = new BufferedReader(
										new InputStreamReader(new FileInputStream(file), inputFileEncode));
								BufferedWriter bufferedWriter = new BufferedWriter(
										new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
								String line;
								while ((line = bufferedReader.readLine()) != null) {
									bufferedWriter.write(line);
									bufferedWriter.newLine();
								}
								bufferedWriter.close();
								bufferedReader.close();
								this.lu.writeDownloadFileEvent(account, ip, n);
								return;
							} catch (IOException e) {
							} catch (Exception e) {
								Printer.instance.print(e.getMessage());
								lu.writeException(e);
							}

						}
					}
				}
			}
		}
		try {
			response.sendError(500);
		} catch (Exception e1) {
		}
	}

	@Override
	public void getNoticeContext(HttpServletRequest request, HttpServletResponse response) {
		File noticeHTML = new File(ConfigureReader.instance().getTemporaryfilePath(), NoticeUtil.NOTICE_OUTPUT_NAME);
		String contentType = "text/html";
		if (noticeHTML.isFile() && noticeHTML.canRead()) {
			sendResource(noticeHTML, NoticeUtil.NOTICE_FILE_NAME, contentType, request, response);
		} else {
			try {
				response.setContentType(contentType);
				response.setCharacterEncoding("UTF-8");
				PrintWriter writer = response.getWriter();
				writer.write("<p class=\"lead\">暂无新公告。</p>");
				writer.flush();
				writer.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public String getNoticeMD5() {
		return nu.getMd5();
	}

}
