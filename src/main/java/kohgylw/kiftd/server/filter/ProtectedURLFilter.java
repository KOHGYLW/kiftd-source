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

import org.springframework.core.annotation.Order;

import kohgylw.kiftd.server.util.ConfigureReader;

/**
 * 
 * <h2>受保护URL禁止直接访问过滤器</h2>
 * <p>该过滤器主要用于避免访问者直接访问某些资源，这些URL仅支持转发进入而不能直接访问。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebFilter({ "/prv/*" })
@Order(4)
public class ProtectedURLFilter implements Filter{

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final HttpServletRequest hsq = (HttpServletRequest)request;
        final HttpServletResponse hsr = (HttpServletResponse)response;
        final String url = hsq.getServletPath();
        switch (url) {
		case "/prv/forbidden.html":
		case "/prv/error.html":
			hsr.sendRedirect("/home.html");
			break;
		case "/prv/login.html":
			final String account = (String) hsq.getSession().getAttribute("ACCOUNT");
			if(ConfigureReader.instance().foundAccount(account)) {
				hsr.sendRedirect("/home.html");
			}else {
				chain.doFilter(request, response);
			}
			break;
		default:
			chain.doFilter(request, response);
			break;
		}
	}

	@Override
	public void destroy() {
		
	}

}
