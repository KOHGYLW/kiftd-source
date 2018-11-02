package kohgylw.kiftd.server.util;

import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RangeFileStreamWriter {

	// 使用断点续传技术提供文件下载服务
	protected void writeRangeFileStream(HttpServletRequest request, HttpServletResponse response, File fo, String fname,
			String contentType) {
		long skipLength = 0;// 下载时跳过的字节数
		long downLength = 0;// 需要继续下载的字节数
		boolean hasEnd = false;// 是否具备结束字节声明
		try {
			response.setHeader("Accept-Ranges", "bytes");// 支持断点续传声明
			// 获取已下载字节数和需下载字节数
			String rangeLabel = request.getHeader("Range");// 获取下载长度声明
			if (null != rangeLabel) {
				// 当进行断点续传时，返回响应码206
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				// 解析下载跳过长度和继续长度
				rangeLabel = request.getHeader("Range").replaceAll("bytes=", "");
				if (rangeLabel.indexOf('-') == rangeLabel.length() - 1) {
					hasEnd = false;
					rangeLabel = rangeLabel.substring(0, rangeLabel.indexOf('-'));
					skipLength = Long.parseLong(rangeLabel.trim());
				} else {
					hasEnd = true;
					String startBytes = rangeLabel.substring(0, rangeLabel.indexOf('-'));
					String endBytes = rangeLabel.substring(rangeLabel.indexOf('-') + 1, rangeLabel.length());
					skipLength = Long.parseLong(startBytes.trim());
					downLength = Long.parseLong(endBytes);
				}
			}
			// 设置响应中文件块声明
			long fileLength = fo.length();// 文件长度
			if (0 != skipLength) {
				String contentRange = "";
				if (hasEnd) {
					contentRange = new StringBuffer(rangeLabel).append("/").append(new Long(fileLength).toString())
							.toString();
				} else {
					contentRange = new StringBuffer("bytes").append(new Long(skipLength).toString()).append("-")
							.append(new Long(fileLength - 1).toString()).append("/")
							.append(new Long(fileLength).toString()).toString();
				}
				response.setHeader("Content-Range", contentRange);
			}
			// 开始执行下载
			response.setContentType(contentType);
			response.setHeader("Content-Length", "" + fileLength);
			response.addHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(fname, "UTF-8"));
			final int buffersize = ConfigureReader.instance().getBuffSize();
			final byte[] buffer = new byte[buffersize];
			final RandomAccessFile raf = new RandomAccessFile(fo, "r");
			final OutputStream os = (OutputStream) response.getOutputStream();
			raf.seek(skipLength);// 跳过已经下载的字节数
			if (hasEnd) {
				while (raf.getFilePointer() < downLength) {
					os.write(raf.read());
				}
			} else {
				int index = 0;
				while ((index = raf.read(buffer)) != -1) {
					os.write(buffer, 0, index);
				}
			}
			raf.close();
			os.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
