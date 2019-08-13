package kohgylw.kiftd.server.service;

import org.springframework.web.multipart.*;
import javax.servlet.http.*;

public interface FileService {
	String checkUploadFile(final HttpServletRequest request, final HttpServletResponse response);

	String doUploadFile(final HttpServletRequest request, final HttpServletResponse response, final MultipartFile file);

	String deleteFile(final HttpServletRequest request);

	void doDownloadFile(final HttpServletRequest request, final HttpServletResponse response);

	String doRenameFile(final HttpServletRequest request);

	String deleteCheckedFiles(final HttpServletRequest request);

	String getPackTime(final HttpServletRequest request);

	String downloadCheckedFiles(final HttpServletRequest request);

	void downloadCheckedFilesZip(final HttpServletRequest request, final HttpServletResponse response) throws Exception;

	String confirmMoveFiles(final HttpServletRequest request);

	String doMoveFiles(final HttpServletRequest request);

	/**
	 * 
	 * <h2>上传文件夹前置检查</h2>
	 * <p>
	 * 用于验证上传文件夹的合法性，包括权限、是否重名、文件是否超限等。并使用不同的返回结果告知前端应进行的下一步操作。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String
	 *         检查结果的json对象，其中的maxSize属性标明能够上传的最大文件体积，而result属性标明前端应进行的操作，对应如下：
	 *         <ul>
	 *         <li>noAuthorized 权限不合法，无法上传</li>
	 *         <li>errorParameter 参数不合法，无法上传</li>
	 *         <li>fileOverSize 文件体积超限，无法上传</li>
	 *         <li>coverOrBoth 允许上传，但存在同名文件夹，可选择覆盖与保留二者</li>
	 *         <li>onlyBoth 允许上传，但存在同名文件夹，仅能保留二者</li>
	 *         <li>permitUpload 允许直接上传</li>
	 *         </ul>
	 */
	String checkImportFolder(final HttpServletRequest request);

	/**
	 * 
	 * <h2>执行上传文件夹的操作</h2>
	 * <p>
	 * 处理上传文件夹请求并生成对应的文件结构来存储上传的文件，再返回信息告知前端是否上传成功。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @param file
	 *            org.springframework.web.multipart.MultipartFile 上传文件的封装对象
	 * @return java.lang.String 处理结果
	 */
	String doImportFolder(final HttpServletRequest request, final MultipartFile file);
}
