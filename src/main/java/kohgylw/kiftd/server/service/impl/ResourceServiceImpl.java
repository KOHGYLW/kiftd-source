package kohgylw.kiftd.server.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.VideoTranscodeThread;
import kohgylw.kiftd.server.service.ResourceService;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.Docx2PDFUtil;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.Txt2PDFUtil;
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

	// 提供资源的输出流，原理与下载相同，但是个别细节有区别
	@Override
	public void getResource(HttpServletRequest request, HttpServletResponse response) {
		// TODO 自动生成的方法存根
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			String fid = request.getParameter("fid");
			if (fid != null) {
				Node n = nm.queryById(fid);
				if (n != null) {
					File file = fbu.getFileFromBlocks(n);
					String suffix = "";
					if (n.getFileName().indexOf(".") >= 0) {
						suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".")).trim().toLowerCase();
					}
					String contentType = "application/octet-stream";
					switch (suffix) {
					case ".mp4":
					case ".webm":
					case ".mov":
					case ".avi":
					case ".wmv":
					case ".mkv":
					case ".flv":
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
						break;
					case ".mp3":
						contentType = "audio/mpeg";
						break;
					case ".ogg":
						contentType = "audio/ogg";
						break;
					default:
						break;
					}
					sendResource(file, n.getFileName(), contentType, request, response);
					if (request.getHeader("Range") == null) {
						this.lu.writeDownloadFileEvent(request, n);
					}
					return;
				}
			}
		}
		try {
			//  处理无法下载的资源
			response.sendError(404);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块
		}
	}

	// 使用各个浏览器（主要是Safari）兼容的通用格式发送资源至请求来源，类似于断点续传下载功能
	private void sendResource(File resource, String fname, String contentType, HttpServletRequest request,
			HttpServletResponse response) {
		try (RandomAccessFile randomFile = new RandomAccessFile(resource, "r")) {
			long contentLength = randomFile.length();
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
			response.setHeader("ETag", fname);
			response.setHeader("Last-Modified", new Date().toString());
			// 第一次请求只返回content length来让客户端请求多次实际数据
			if (range == null) {
				response.setHeader("Content-length", contentLength + "");
			} else {
				// 以后的多次以断点续传的方式来返回视频数据
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);// 206
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
		} catch (Exception e) {

		}
	}

	// 对word的预览实现
	@Override
	public void getWordView(String fileId, HttpServletRequest request, HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 权限检查
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			if (fileId != null) {
				Node n = nm.queryById(fileId);
				if (n != null) {
					File file = fbu.getFileFromBlocks(n);
					// 后缀检查
					String suffix = "";
					if (n.getFileName().indexOf(".") >= 0) {
						suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".")).trim().toLowerCase();
					}
					if (".docx".equals(suffix)) {
						String contentType = "application/octet-stream";
						response.setContentType(contentType);
						// 执行转换并写出输出流
						try {
							d2pu.convertPdf(new FileInputStream(file), response.getOutputStream());
							return;
						} catch (Exception e) {
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
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			if (fileId != null) {
				Node n = nm.queryById(fileId);
				if (n != null) {
					File file = fbu.getFileFromBlocks(n);
					// 后缀检查
					String suffix = "";
					if (n.getFileName().indexOf(".") >= 0) {
						suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".")).trim().toLowerCase();
					}
					if (".txt".equals(suffix)) {
						String contentType = "application/octet-stream";
						response.setContentType(contentType);
						// 执行转换并写出输出流
						try {
							t2pu.convertPdf(file, response.getOutputStream());
							return;
						} catch (Exception e) {
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
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			String fId = request.getParameter("fileId");
			if (fId != null) {
				try {
					return vtu.getTranscodeProcess(fId);
				} catch (Exception e) {
					e.printStackTrace();
					lu.writeException(e);
				}
			}
		}
		return "ERROR";
	}

}
