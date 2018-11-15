package kohgylw.kiftd.server.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * <h2>断点式文件输出流写出工具</h2>
 * <p>
 * 该工具负责处理断点下载请求并以相应规则写出文件流。需要提供断点续传服务，请继承该类并调用writeRangeFileStream方法。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class RangeFileStreamWriter {

	/**
	 * 
	 * <h2>使用断点续传技术提供输出流</h2>
	 * <p>
	 * 处理带有断点续传参数的下载请求，并提供输出流写出。请传入相应的参数并执行该方法以开始断点传输。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @param response
	 *            javax.servlet.http.HttpServletResponse 响应对象
	 * @param fo
	 *            java.io.File 需要写出的文件
	 * @param fname
	 *            java.lang.String 文件名
	 * @param contentType
	 *            java.lang.String HTTP Content-Type类型（用于控制客户端行为）
	 * @return void
	 */
	protected void writeRangeFileStream(HttpServletRequest request, HttpServletResponse response, File fo, String fname,
			String contentType) {
		long fileLength = fo.length(); // 记录文件大小
		long pastLength = 0; // 记录已下载文件大小
		int rangeSwitch = 0; // 0：从头开始的全文下载；1：从某字节开始的下载（bytes=27000-）；2：从某字节开始到某字节结束的下载（bytes=27000-39000）
		long toLength = 0; // 记录客户端需要下载的字节段的最后一个字节偏移量（比如bytes=27000-39000，则这个值是为39000）
		long contentLength = 0; // 客户端请求的字节总量
		String rangeBytes = ""; // 记录客户端传来的形如“bytes=27000-”或者“bytes=27000-39000”的内容
		OutputStream os = null; // 写出数据
		OutputStream out = null; // 缓冲
		byte b[] = new byte[ConfigureReader.instance().getBuffSize()]; // 暂存容器
		if (request.getHeader("Range") != null) { // 客户端请求的下载的文件块的开始字节
			response.setStatus(javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT);
			rangeBytes = request.getHeader("Range").replaceAll("bytes=", "");
			if (rangeBytes.indexOf('-') == rangeBytes.length() - 1) {// bytes=969998336-
				rangeSwitch = 1;
				rangeBytes = rangeBytes.substring(0, rangeBytes.indexOf('-'));
				pastLength = Long.parseLong(rangeBytes.trim());
				contentLength = fileLength - pastLength; // 客户端请求的是 969998336 之后的字节
			} else { // bytes=1275856879-1275877358
				rangeSwitch = 2;
				String temp0 = rangeBytes.substring(0, rangeBytes.indexOf('-'));
				String temp2 = rangeBytes.substring(rangeBytes.indexOf('-') + 1, rangeBytes.length());
				pastLength = Long.parseLong(temp0.trim()); // bytes=1275856879-1275877358，从第 1275856879 个字节开始下载
				toLength = Long.parseLong(temp2); // bytes=1275856879-1275877358，到第 1275877358 个字节结束
				contentLength = toLength - pastLength; // 客户端请求的是 1275856879-1275877358 之间的字节
			}
		} else { // 从开始进行下载
			contentLength = fileLength; // 客户端要求全文下载
		}

		/**
		 * 如果设设置了Content -Length，则客户端会自动进行多线程下载。如果不希望支持多线程，则不要设置这个参数。 响应的格式是: Content -
		 * Length: [文件的总大小] - [客户端请求的下载的文件块的开始字节]
		 * ServletActionContext.getResponse().setHeader("Content- Length", new
		 * Long(file.length() - p).toString());
		 */
		response.setHeader("Accept-Ranges", "bytes");// 如果是第一次下,还没有断点续传,状态是默认的 200,无需显式设置;响应的格式是:HTTP/1.1 200 OK
		if (pastLength != 0) {
			// 不是从最开始下载,
			// 响应的格式是:
			// Content-Range: bytes [文件块的开始字节]-[文件的总大小 - 1]/[文件的总大小]
			switch (rangeSwitch) {
				case 1: { // 针对 bytes=27000- 的请求
					String contentRange = new StringBuffer("bytes ").append(new Long(pastLength).toString()).append("-")
							.append(new Long(fileLength - 1).toString()).append("/").append(new Long(fileLength).toString())
							.toString();
					response.setHeader("Content-Range", contentRange);
					break;
				}
				case 2: { // 针对 bytes=27000-39000 的请求
					String contentRange = new StringBuffer("bytes ").append(rangeBytes).append("/").append(new Long(fileLength).toString()).toString();
					response.setHeader("Content-Range", contentRange);
					break;
				}
				default: {
					break;
				}
			}
		} else {
			// 是从开始下载
		}
		response.addHeader("Content-Disposition", "attachment; filename=\"" + fname + "\"");
		response.setContentType(contentType); // set the MIME type.
		response.addHeader("Content-Length", String.valueOf(contentLength));
		try (RandomAccessFile raf = new RandomAccessFile(fo, "r")) {
			os = response.getOutputStream();
			out = new BufferedOutputStream(os);
			System.out.println("--Range Download--");
			System.out.println("Content-Type:"+response.getContentType());
			System.out.println("Content-Length:"+response.getHeader("Content-Length"));
			System.out.println("Content-Range:"+response.getHeader("Content-Range"));
			System.out.println("--Range Download--");
			switch (rangeSwitch) {
				case 0: { // 普通下载，或者从头开始的下载
					// 同1
				}
				case 1: { // 针对 bytes=27000- 的请求
					raf.seek(pastLength); // 形如 bytes=969998336- 的客户端请求，跳过 969998336 个字节
					int n = 0;
					while ((n = raf.read(b)) != -1) {
						out.write(b, 0, n);
					}
					break;
				}
				case 2: { // 针对 bytes=27000-39000 的请求
					raf.seek(pastLength); // 形如 bytes=1275856879-1275877358 的客户端请求，找到第 1275856879 个字节
					int n = 0;
					long readLength = 0; // 记录已读字节数
					while (readLength <= contentLength) {// 大部分字节在这里读取
						n = raf.read(b);
						readLength += n;
						out.write(b, 0, n);
					}
					break;
				}
				default: {
					break;
				}
			}
			out.flush();

		} catch (IOException ex) {

		} finally {

		}
	}
}
