package kohgylw.kiftd.server.filter;

import javax.servlet.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;
import kohgylw.kiftd.server.util.*;
import kohgylw.kiftd.server.enumeration.*;
import java.io.*;

@WebFilter
public class VCFilter implements Filter
{
    public void init(final FilterConfig filterConfig) throws ServletException {
    }
    
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest hsr = (HttpServletRequest)request;
        final String url = hsr.getServletPath();
        if (url.startsWith("/fileblocks/") || url.startsWith("//fileblocks/")) {
            final String account = (String)hsr.getSession().getAttribute("ACCOUNT");
            if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
                chain.doFilter(request, response);
            }
            else {
                hsr.getRequestDispatcher("/errorController/pageNotFound.do").forward(request, response);
            }
        }
        else {
            chain.doFilter(request, response);
        }
    }
    
    public void destroy() {
    }
}
