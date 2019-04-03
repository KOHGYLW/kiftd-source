package kohgylw.kiftd.server.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * <h2>外部下载链接处理服务</h2>
 * <p>该服务主要包含了生成、获取外部下载链接以及使用它们进行下载的相关操作。详见各个方法。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface ExternalDownloadService {
	
	/**
	 * 
	 * <h2>获取一个下载凭证</h2>
	 * <p>针对指定资源获取一个下载凭证，要求该凭证的签收者必须具备下载权限。该凭证在服务器关闭前将一直有效。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String 下载凭证
	 */
	String getDownloadKey(HttpServletRequest request);
	
	/**
	 * 
	 * <h2>使用凭证下载指定文件</h2>
	 * <p>根据请求所持凭证，下载指定文件资源。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @param response javax.servlet.http.HttpServletResponse 响应对象
	 */
	void downloadFileByKey(HttpServletRequest request,HttpServletResponse response);

}
