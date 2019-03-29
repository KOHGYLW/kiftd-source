package kohgylw.kiftd.server.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import kohgylw.kiftd.server.service.ResourceService;

//转为在线资源打造的控制器，针对在线播放等功能的适配
@Controller
@RequestMapping("/resourceController")
public class ResourceController {

	@Resource
	private ResourceService rs;
	
	//以严格的HTTP响应格式返回在线资源流，适用于多数浏览器
	@RequestMapping("/getResource.do")
	public void getResource(HttpServletRequest request, HttpServletResponse response) {
		rs.getResource(request, response);
	}
	
	//以PDF格式获取word预览视图
	@RequestMapping("/getWordView/{fileId}")
	public void getWordView(@PathVariable("fileId") String fileId,HttpServletRequest request,HttpServletResponse response) {
		rs.getWordView(fileId,request, response);
	}
	
	//以PDF格式获取TXT预览视图
	@RequestMapping("/getTxtView/{fileId}")
	public void getTxtView(@PathVariable("fileId") String fileId,HttpServletRequest request,HttpServletResponse response) {
		rs.getTxtView(fileId, request, response);
	}
	
	//获取视频转码状态，如指定视频未开始转码则开始，如已经开始则返回进度，如已经完成则返回FIN
	@RequestMapping("/getVideoTranscodeStatus.ajax")
	public @ResponseBody String getVideoTranscodeStatus(HttpServletRequest request) {
		return rs.getVideoTranscodeStatus(request);
	}

}
