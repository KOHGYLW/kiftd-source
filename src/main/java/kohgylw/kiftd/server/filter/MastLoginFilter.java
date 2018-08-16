package kohgylw.kiftd.server.filter;

import javax.servlet.annotation.*;
import javax.servlet.*;
import kohgylw.kiftd.server.util.*;
import javax.servlet.http.*;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.*;

@WebFilter
public class MastLoginFilter implements Filter
{
    public void init(final FilterConfig filterConfig) throws ServletException {
    }
    
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final ConfigureReader cr = ConfigureReader.instance();
        final boolean s = cr.mustLogin();
        final HttpServletRequest hsq = (HttpServletRequest)request;
        final HttpServletResponse hsr = (HttpServletResponse)response;
        final String url = hsq.getServletPath();
        final HttpSession session = hsq.getSession();
        //检测跨域访问
        checkCross();
        if (url.equals("//prv/login.html") || url.equals("/prv/login.html")) {
            if (s) {
                final String account = (String)session.getAttribute("ACCOUNT");
                if (cr.foundAccount(account)) {
                    hsr.sendRedirect("/home.html");
                }
                else {
                    chain.doFilter(request, response);
                }
            }
            else {
                hsr.sendRedirect("/home.html");
            }
        }
        else if (s) {
            if (url.equals("/") || url.endsWith(".html") || url.endsWith(".do")) {
                if (session.getAttribute("ACCOUNT") != null) {
                    final String account = (String)session.getAttribute("ACCOUNT");
                    if (cr.foundAccount(account)) {
                        chain.doFilter(request, response);
                    }
                    else {
                        hsr.sendRedirect("/prv/login.html");
                    }
                }
                else {
                    hsr.sendRedirect("/prv/login.html");
                }
            }
            else if (url.endsWith(".ajax")) {
                if (url.equals("/homeController/doLogin.ajax") || url.equals("/homeController/getPublicKey.ajax")) {
                    chain.doFilter(request, response);
                }
                else if (session.getAttribute("ACCOUNT") != null) {
                    final String account = (String)session.getAttribute("ACCOUNT");
                    if (cr.foundAccount(account)) {
                        chain.doFilter(request, response);
                    }
                    else {
                        hsr.setCharacterEncoding("UTF-8");
                        final PrintWriter pw = hsr.getWriter();
                        pw.print("mustLogin");
                        pw.flush();
                    }
                }
                else {
                    hsr.setCharacterEncoding("UTF-8");
                    final PrintWriter pw2 = hsr.getWriter();
                    pw2.print("mustLogin");
                    pw2.flush();
                }
            }
            else {
                chain.doFilter(request, response);
            }
        }
        else {
            chain.doFilter(request, response);
        }
    }
    /**
     * @author pengzhonghui
     * 检测当前是否支持将跨域访问开放
     */
    public void checkCross() {
    	HttpServletResponse resp = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse();
		HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
    	final String cross = request.getParameter("cross");
    	if(cross.equals("accept")){
			resp.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
			resp.setHeader("Access-Control-Allow-Credentials", "true");
			resp.setHeader("P3P", "CP=CAO PSA OUR");
	        if (request.getHeader("Access-Control-Request-Method") != null && "OPTIONS".equals(request.getMethod())) {
	        	resp.addHeader("Access-Control-Allow-Methods", "POST,GET,TRACE,OPTIONS");
	        	resp.addHeader("Access-Control-Allow-Headers", "Content-Type,Origin,Accept");
	        	resp.addHeader("Access-Control-Max-Age", "120");
	        }
		}
    }
    public void destroy() {
    }
}
