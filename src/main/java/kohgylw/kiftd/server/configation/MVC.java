package kohgylw.kiftd.server.configation;

import org.springframework.web.servlet.resource.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.web.servlet.config.annotation.*;

import kohgylw.kiftd.server.util.*;
import kohgylw.kiftd.server.webdav.KiftdWebDAVServlet;

import java.io.*;

import javax.servlet.*;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.*;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.*;
import org.springframework.util.unit.DataSize;

/**
 * 
 * <h2>Web功能-MVC相关配置类</h2>
 * <p>
 * 该Spring配置类主要负责配置kiftd网页服务器的基本处理行为，并在IOC容器中生成所需的工具实例。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@AutoConfiguration
@ComponentScan({ "kohgylw.kiftd.server.controller", "kohgylw.kiftd.server.service.impl", "kohgylw.kiftd.server.util",
		"kohgylw.kiftd.server.webdav.util" })
@ServletComponentScan({ "kohgylw.kiftd.server.listener", "kohgylw.kiftd.server.filter" })
@Import({ DataAccess.class })
public class MVC extends ResourceHttpRequestHandler implements WebMvcConfigurer {
	
	// 注册DefaultServlet
	@Bean
	WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> enableDefaultServlet() {
	    return (factory) -> factory.setRegisterDefaultServlet(true);
	}

	// 启用DefaultServlet用以处理可直接请求的静态资源
	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}

	// 设置Web静态资源映射路径
	public void addResourceHandlers(final ResourceHandlerRegistry registry) {
		// 将静态页面资源所在文件夹加入至资源路径中
		registry.addResourceHandler(new String[] { "/**" }).addResourceLocations(new String[] {
				"file:" + ConfigureReader.instance().getPath() + File.separator + "webContext" + File.separator });
	}

	// 生成上传管理器，用于接收/缓存上传文件
	@Bean
	public MultipartConfigElement multipartConfigElement() {
		final MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofBytes(-1L));
		factory.setLocation(ConfigureReader.instance().getTemporaryfilePath());
		return factory.createMultipartConfig();
	}

	// 生成Gson实例，用于服务Json序列化和反序列化
	@Bean
	public Gson gson() {
		return new GsonBuilder().create();
	}

	// 注册WebDAV处理Servlet
	@Bean
	public ServletRegistrationBean<Servlet> WebDAVServlet() {
		return new ServletRegistrationBean<Servlet>(new KiftdWebDAVServlet(), "/dav/*");
	}
}
