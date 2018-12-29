package kohgylw.kiftd.server.service.impl;

import java.io.File;
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
import kohgylw.kiftd.server.service.ResourceService;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.LogUtil;

@Service
public class ResourceServiceImpl implements ResourceService {

	@Resource
	private NodeMapper nm;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private LogUtil lu;
	
	//提供资源的输出流，原理与下载相同，但是个别细节有区别
	@Override
	public void getResource(HttpServletRequest request, HttpServletResponse response) {
		// TODO 自动生成的方法存根
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			String fid=request.getParameter("fid");
			if (fid != null) {
				Node n = nm.queryById(fid);
				if (n != null) {
					File file = fbu.getFileFromBlocks(n);
					String suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".")).trim().toLowerCase();
					String contentType = "application/octet-stream";
					switch (suffix) {
					case ".mp4":
						contentType = "video/mp4";
						break;
					case ".webm":
						contentType = "video/webm";
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
	
	//使用各个浏览器（主要是Safari）兼容的通用格式发送资源至请求来源，类似于断点续传下载功能
	private void sendResource(File resource, String fname, String contentType, HttpServletRequest request,
			HttpServletResponse response) {
		try (RandomAccessFile randomFile = new RandomAccessFile(resource, "r")) {
			long contentLength = randomFile.length();
			String range = request.getHeader("Range");
			int start = 0, end = 0;
			if (range != null && range.startsWith("bytes=")) {
				String[] values = range.split("=")[1].split("-");
				start = Integer.parseInt(values[0]);
				if (values.length > 1) {
					end = Integer.parseInt(values[1]);
				}
			}
			int requestSize = 0;
			if (end != 0 && end > start) {
				requestSize = end - start + 1;
			} else {
				requestSize = Integer.MAX_VALUE;
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
					requestStart = Integer.parseInt(rangeDatas[0]);
					if (rangeDatas.length > 1) {
						requestEnd = Integer.parseInt(rangeDatas[1]);
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
			int needSize = requestSize;
			randomFile.seek(start);
			while (needSize > 0) {
				int len = randomFile.read(buffer);
				if (needSize < buffer.length) {
					out.write(buffer, 0, needSize);
				} else {
					out.write(buffer, 0, len);
					if (len < buffer.length) {
						break;
					}
				}
				needSize -= buffer.length;
			}
			out.close();
		} catch (IOException e) {

		}
	}

}
