package kohgylw.kiftd.server.filter;

import javax.servlet.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kohgylw.kiftd.server.util.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;

import java.io.*;

/**
 * 
 * <h2>文件块资源权限检查器</h2>
 * <p>
 * 该过滤器用于检查所有发往资源文件夹（文件块、临时文件夹）的请求，判断该请求权限是否合法，并指定请求MIME类型为输出流。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebFilter({ "/fileblocks/*" })
@Order(3)
public class VCFilter implements Filter {

	private static FolderUtil fu;
	private static NodeMapper nm;
	private static FolderMapper fm;
	private static LogUtil lu;

	public void init(final FilterConfig filterConfig) throws ServletException {
		ApplicationContext context = WebApplicationContextUtils
				.getWebApplicationContext(filterConfig.getServletContext());
		fu = context.getBean(FolderUtil.class);
		nm = context.getBean(NodeMapper.class);
		fm = context.getBean(FolderMapper.class);
		lu = context.getBean(LogUtil.class);
	}

	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		final HttpServletRequest hsr = (HttpServletRequest) request;
		final String account = (String) hsr.getSession().getAttribute("ACCOUNT");
		try {
			String blockPath = hsr.getServletPath().substring(12);
			Node targetNode = nm.queryByPath(blockPath);
			if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
					fu.getAllFoldersId(targetNode.getFileParentFolder()))
					&& ConfigureReader.instance().accessFolder(fm.queryById(targetNode.getFileParentFolder()),
							account)) {
				response.setContentType("application/octet-stream");
				lu.writeDownloadFileEvent(hsr, targetNode);// 直接访问文件块也算一种下载操作
				chain.doFilter(request, response);
			} else {
				hsr.getRequestDispatcher("/errorController/pageNotFound.do").forward(request, response);
			}
		} catch (Exception e) {

		}
	}

	public void destroy() {
	}
}
