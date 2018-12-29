package kohgylw.kiftd.server.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

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

}
