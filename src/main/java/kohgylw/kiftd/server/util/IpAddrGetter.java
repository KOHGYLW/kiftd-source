package kohgylw.kiftd.server.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

/**
 * 
 * <h2>请求IP地址解析工具</h2>
 * <p>该工具包含了public String getIpAddr(HttpServletRequest request)方法用于解析某次请求的来源IP地址。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class IpAddrGetter {
	
	// 可能的转发标识请求头名称
	private String[] ipAddrHeaders = { "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP",
			"HTTP_X_FORWARDED_FOR" };

	/**
	 * 
	 * <h2>获得请求来源的IP地址（公网）</h2>
	 * <p>
	 * 该方法用于从请求对象中获得此请求的来源IP地址，支持反向代理。该地址将以字符串形式返回，例如“127.0.0.1”。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String 请求来源IP地址 
	 */
	public String getIpAddr(HttpServletRequest request) {
		if(ConfigureReader.instance().isIpXFFAnalysis()) {
			for (String ipAddrHeader : ipAddrHeaders) {
				String ipAddress = request.getHeader(ipAddrHeader);
				if (ipAddress != null && ipAddress.length() > 0 && !"unknown".equalsIgnoreCase(ipAddress)) {
					int indexOfIpSeparator = ipAddress.indexOf(",");
					if (indexOfIpSeparator >= 0) {
						return ipAddress.substring(0, indexOfIpSeparator).trim();
					} else {
						return ipAddress.trim();
					}
				}
			}
		}
		String remoteAddr = request.getRemoteAddr();
		if(remoteAddr != null) {
			return request.getRemoteAddr().trim();
		}else {
			return "获取失败";
		}
	}

}
