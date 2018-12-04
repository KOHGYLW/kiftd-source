package kohgylw.kiftd.server.filter;

import javax.servlet.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.springframework.core.annotation.Order;

import kohgylw.kiftd.server.util.*;
import kohgylw.kiftd.server.enumeration.*;

import java.io.*;

/**
 * 
 * <h2>文件块资源权限检查器</h2>
 * <p>该过滤器用于检查所有发往资源文件夹（文件块、临时文件夹）的请求，判断该请求权限是否合法，并指定请求MIME类型为输出流。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebFilter({ "/fileblocks/*"})
@Order(2)
public class VCFilter implements Filter {
	
	public void init(final FilterConfig filterConfig) throws ServletException {
	}

	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		final HttpServletRequest hsr = (HttpServletRequest) request;
		final String account = (String) hsr.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			response.setContentType("application/octet-stream");
			chain.doFilter(request, response);
		} else {
			hsr.getRequestDispatcher("/errorController/pageNotFound.do").forward(request, response);
		}
	}

	public void destroy() {
	}
}
