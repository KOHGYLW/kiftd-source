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
	 * <h2>根据链接返回资源数据</h2>
	 * <p>该方法将通过永久资源链接返回其指向的数据，并声明其可能的ContentType类型。若未开启永久资源链接功能则返回403。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @param response javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getResourceBychain(HttpServletRequest request,HttpServletResponse response);

}
