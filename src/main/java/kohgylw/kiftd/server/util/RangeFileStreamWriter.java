package kohgylw.kiftd.server.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * <h2>断点式文件输出流写出工具</h2>
 * <p>
 * 该工具负责处理断点下载请求并以相应规则写出文件流。需要提供断点续传服务，请继承该类并调用writeRangeFileStream方法，
 * 该操作已换回较为简单的RandomAccessFile实现（效率与NIO相近，更节省内存）。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class RangeFileStreamWriter {

	private static final long DOWNLOAD_CACHE_MAX_AGE = 1800L;

	/**
	 * 
	 * <h2>使用断点续传技术提供输出流</h2>
	 * <p>
	 * 处理普通的或带有断点续传参数的下载请求，并按照请求方式提供相应的输出流写出。请传入相应的参数并执行该方法以开始传输。
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
	 * @param maxRate
	 *            long 最大输出速率，以KB/s为单位，若为负数则不限制输出速率（用于限制客户端的下载速度）
	 * @param eTag
	 *            java.lang.String 资源的唯一性标识，例如"aabbcc"
	 * @param isAttachment
	 *            boolean 是否作为附件回传，若希望用户下载则应设置为true
	 * @return int 操作结束时返回的状态码
	 */
	protected int writeRangeFileStream(HttpServletRequest request, HttpServletResponse response, File fo, String fname,
			String contentType, long maxRate, String eTag, boolean isAttachment) {
		long fileLength = fo.length();// 文件总大小
		long startOffset = 0; // 起始偏移量
		boolean hasEnd = false;// 请求区间是否存在结束标识
		long endOffset = 0; // 结束偏移量
		long contentLength = 0; // 响应体长度
		String rangeBytes = "";// 请求中的Range参数
		int status = HttpServletResponse.SC_OK;// 初始响应码为200
		// 检查是否有可用的缓存
		String lastModified = ServerTimeUtil.getLastModifiedFormBlock(fo);
		String ifModifiedSince = request.getHeader("If-Modified-Since");
		String ifNoneMatch = request.getHeader("If-None-Match");
		// 是否提供了两个判断参数之一？
		if (ifModifiedSince != null || ifNoneMatch != null) {
			// 是，那么是否提供了Etag？
			if (ifNoneMatch != null) {
				// 是，则只检查Etag，理论上其比Last-Modified更准确
				if (ifNoneMatch.trim().equals(eTag)) {
					status = HttpServletResponse.SC_NOT_MODIFIED;
					response.setStatus(status);// 304
					return status;
				}
			} else {
				// 不是，则再检查Last-Modified
				if (ifModifiedSince.trim().equals(lastModified)) {
					status = HttpServletResponse.SC_NOT_MODIFIED;
					response.setStatus(status);// 304
					return status;
				}
			}
		}
		// 检查断点续传请求是否过期，两个条件，有就要满足，没有就算了
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
		// 设置请求头，基于kiftd文件系统推荐使用application/octet-stream
		response.setContentType(contentType);
		// 设置文件信息
		response.setCharacterEncoding("UTF-8");
		// 设置Content-Disposition信息
		if (isAttachment) {
			response.setHeader("Content-Disposition", "attachment; filename=\"" + EncodeUtil.getFileNameByUTF8(fname)
					+ "\"; filename*=utf-8''" + EncodeUtil.getFileNameByUTF8(fname));
		} else {
			response.setHeader("Content-Disposition", "inline");
		}
		// 设置支持断点续传功能
		response.setHeader("Accept-Ranges", "bytes");
		// 设置缓存控制信息
		response.setHeader("ETag", eTag);
		response.setHeader("Last-Modified", ServerTimeUtil.getLastModifiedFormBlock(fo));
		response.setHeader("Cache-Control", "max-age=" + DOWNLOAD_CACHE_MAX_AGE);
		// 针对具备断点续传性质的请求进行解析
		final String rangeTag = request.getHeader("Range");
		final String ifRange = request.getHeader("If-Range");
		if (rangeTag != null && rangeTag.startsWith("bytes=")
				&& (ifRange == null || ifRange.trim().equals(eTag) || ifRange.trim().equals(lastModified))) {
			status = HttpServletResponse.SC_PARTIAL_CONTENT;
			response.setStatus(status);
			rangeBytes = rangeTag.replaceAll("bytes=", "");
			if (rangeBytes.endsWith("-")) {
				// 解析请求参数范围为仅有起始偏移量而无结束偏移量的情况
				startOffset = Long.parseLong(rangeBytes.substring(0, rangeBytes.indexOf('-')).trim());
				// 仅具备起始偏移量时，例如文件长为13，请求为5-，则响应体长度为8
				contentLength = fileLength - startOffset;
			} else {
				hasEnd = true;
				startOffset = Long.parseLong(rangeBytes.substring(0, rangeBytes.indexOf('-')).trim());
				endOffset = Long.parseLong(rangeBytes.substring(rangeBytes.indexOf('-') + 1).trim());
				// 具备起始偏移量与结束偏移量时，例如0-9，则响应体长度为10个字节
				contentLength = endOffset - startOffset + 1;
			}
			// 设置Content-Range，格式为“bytes 起始偏移-结束偏移/文件的总大小”
			String contentRange;
			if (!hasEnd) {
				contentRange = new StringBuffer("bytes ").append("" + startOffset).append("-")
						.append("" + (fileLength - 1)).append("/").append("" + fileLength).toString();
			} else {
				contentRange = new StringBuffer("bytes ").append(rangeBytes).append("/").append("" + fileLength)
						.toString();
			}
			response.setHeader("Content-Range", contentRange);
		} else { // 从开始进行下载
			contentLength = fileLength; // 客户端要求全文下载
		}
		response.setHeader("Content-Length", "" + contentLength);// 设置请求体长度
		// 写出缓冲
		byte[] buf = new byte[ConfigureReader.instance().getBuffSize()];
		// 读取文件并写处至输出流
		try (RandomAccessFile raf = new RandomAccessFile(fo, "r")) {
			BufferedOutputStream out = maxRate >= 0
					? new VariableSpeedBufferedOutputStream(response.getOutputStream(), maxRate, request.getSession())
					: new BufferedOutputStream(response.getOutputStream());
			raf.seek(startOffset);
			if (!hasEnd) {
				// 无结束偏移量时，将其从起始偏移量开始写到文件整体结束，如果从头开始下载，起始偏移量为0
				int n = 0;
				while ((n = raf.read(buf)) != -1) {
					out.write(buf, 0, n);
				}
			} else {
				// 有结束偏移量时，将其从起始偏移量开始写至指定偏移量结束。
				int n = 0;
				long readLength = 0;// 写出量，用于确定结束位置
				while (readLength < contentLength) {
					n = raf.read(buf);
					readLength += n;
					out.write(buf, 0, n);
				}
			}
			out.flush();
			out.close();
			return status;
		} catch (IOException ex) {
			// 针对任何IO异常忽略，传输失败不处理
			status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			return status;
		} catch (IllegalArgumentException e) {
			status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			try {
				response.sendError(status);
			} catch (IOException e1) {
			}
			return status;
		}
	}
}
