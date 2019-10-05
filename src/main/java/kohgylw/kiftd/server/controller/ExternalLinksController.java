package kohgylw.kiftd.server.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kohgylw.kiftd.server.service.ExternalDownloadService;
import kohgylw.kiftd.server.service.FileChainService;

/**
 * 
 * <h2>外部链接控制器</h2>
 * <p>该控制器主要处理来自外部链接的请求，例如用于分享的下载链接。该控制器内的所有请求均允许跨域。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Controller
@CrossOrigin
@RequestMapping({"/externalLinksController"})
public class ExternalLinksController {
	
	@Resource
	private ExternalDownloadService eds;//分享下载链接的相关处理
	@Resource
	private FileChainService fcs;
	
	@RequestMapping("/getDownloadKey.ajax")
	public @ResponseBody String getDownloadKey(HttpServletRequest request) {
		return eds.getDownloadKey(request);
	}
	
	@RequestMapping("/downloadFileByKey/{fileName}")
	public void downloadFileByKey(HttpServletRequest request,HttpServletResponse response) {
		eds.downloadFileByKey(request, response);
	}
	
	@RequestMapping("/chain/{fileName}")
	public void chain(HttpServletRequest request,HttpServletResponse response) {
		fcs.getResourceByChainKey(request, response);
	}

}
