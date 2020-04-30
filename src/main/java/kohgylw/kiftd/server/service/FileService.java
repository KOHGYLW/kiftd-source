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
	
	/**
	 * 
	 * <h2>移动文件的前置判断操作</h2>
	 * <p>该方法用于验证将要执行的移动（或复制）操作是否合法，应在正式执行移动（或复制）操作前调用，并根据返回值判断是否继续执行。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象，应包含：
	 * <ul>
	 * <li>strIdList 涉及的文件ID数组，JSON格式</li>
	 * <li>strFidList 涉及的文件夹ID数组，JSON格式</li>
	 * <li>locationpath 目标文件夹ID</li>
	 * <li>method 决定是移动还是复制，仅当传入“COPY”时为复制模式</li>
	 * </ul>
	 * @return java.lang.String 判断结果，可能为：
	 * <ul>
	 * <li>confirmMoveFiles 允许进行移动操作</li>
	 * <li>
	 * duplicationFileName:{JSON对象} 允许进行移动操作，但必须为冲突的文件或文件夹指定处理方式，例如覆盖或保留两者。
	 * JSON对象中记录了repeFolders冲突文件夹数组和repeNodes冲突文件数组
	 * </li>
	 * <li>noAuthorized 无操作权限</li>
	 * <li>errorParameter 传入参数有误，例如目标文件夹不存在</li>
	 * <li>filesTotalOutOfLimit或foldersTotalOutOfLimit 目标文件夹已经不能添加更多文件或文件夹了</li>
	 * <li>CANT_MOVE_TO_INSIDE:{文件夹名} 不能将一个文件夹移动到自己内部</li>
	 * </ul>
	 */
	String confirmMoveFiles(final HttpServletRequest request);
	
	/**
	 * 
	 * <h2>执行移动文件操作</h2>
	 * <p>该方法用于执行移动（或复制）操作，在调用该方法前应先进行前置检查。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象，应包含：
	 * <ul>
	 * <li>strIdList 涉及的文件ID数组，JSON格式</li>
	 * <li>strFidList 涉及的文件夹ID数组，JSON格式</li>
	 * <li>strOptMap 对冲突文件的处理方式列表（若存在），JSON格式</li>
	 * <li>locationpath 目标文件夹ID</li>
	 * <li>method 决定是移动还是复制，仅当传入“COPY”时为复制模式</li>
	 * </ul>
	 * @return java.lang.String 执行结果，可能为：
	 * <ul>
	 * <li>moveFilesSuccess 执行成功</li>
	 * <li>noAuthorized 无操作权限</li>
	 * <li>errorParameter 传入参数有误，例如目标文件夹不存在</li>
	 * <li>filesTotalOutOfLimit或foldersTotalOutOfLimit 目标文件夹已经不能添加更多文件或文件夹了</li>
	 * <li>cannotMoveFiles 各种原因引起的操作失败</li>
	 * </ul>
	 */
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
