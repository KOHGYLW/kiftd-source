package kohgylw.kiftd.server.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.IpAddrGetter;

/**
 * 
 * <h2>阻止特定IP访问过滤器</h2>
 * <p>该过滤器用于阻止特定IP进行访问，从而保护用户资源。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebFilter
@Order(1)
public class IPFilter implements Filter {

	private IpAddrGetter idg;
	private boolean enable;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		ApplicationContext context = WebApplicationContextUtils
				.getWebApplicationContext(filterConfig.getServletContext());
		idg = context.getBean(IpAddrGetter.class);
		enable = ConfigureReader.instance().hasBannedIP();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if(enable) {
			HttpServletRequest hsr = (HttpServletRequest) request;
			if(ConfigureReader.instance().isBannedIP(idg.getIpAddr(hsr))) {
				((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN);
			}else {
				chain.doFilter(request, response);
			}
		}else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
	}

}
