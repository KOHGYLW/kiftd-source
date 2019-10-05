package kohgylw.kiftd.server.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * <h2>外部链接处理器</h2>
 * <p>该服务用于处理来自永久资源链接的请求。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface FileChainService {
	
	/**
	 * 
	 * <h2>根据文件ID获取其永久资源链接的ckey</h2>
	 * <p>该方法用于获取加密的ckey，以便在使用资源链接时声明其指向的文件。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String 获取的ckey，若获取失败则返回ERROR
	 */
	public String getChainKeyByFid(HttpServletRequest request);
	
	/**
	 * 
	 * <h2>根据链接中的ckey返回对应的资源数据</h2>
	 * <p>该方法将通过永久资源链接返回其ckey指向的文件数据，并声明其可能的ContentType类型。若未开启永久资源链接功能则返回403。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @param response javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getResourceByChainKey(HttpServletRequest request,HttpServletResponse response);

}
