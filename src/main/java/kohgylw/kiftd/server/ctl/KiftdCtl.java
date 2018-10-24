package kohgylw.kiftd.server.ctl;

import org.springframework.boot.web.servlet.server.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import kohgylw.kiftd.server.configation.*;
import org.springframework.context.*;
import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.util.*;
import org.springframework.boot.*;
import org.springframework.http.*;
import org.springframework.boot.web.server.*;


/**
 * 
 * <h2>服务器控制器</h2>
 * <p>该层连接服务器内核与用户操作界面，用于控制服务器行为。包括启动、关闭、重启等。同时，该类也为SpringBoot框架
 * 应用入口，负责初始化SpringBoot容器。
 * </p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@SpringBootApplication
@Import({ MVC.class })
public class KiftdCtl implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
	private static ApplicationContext context;
	private static boolean run;

	public boolean start() {
		Printer.instance.print("\u6b63\u5728\u542f\u52a8\u670d\u52a1\u5668...");
		final String[] args = new String[0];
		if (!KiftdCtl.run) {
			if (ConfigureReader.instance().getPropertiesStatus() == 0) {
				try {
					Printer.instance.print("\u6b63\u5728\u5f00\u542f\u670d\u52a1\u5668\u5f15\u64ce...");
					KiftdCtl.context = (ApplicationContext) SpringApplication.run(KiftdCtl.class, args);
					KiftdCtl.run = (KiftdCtl.context != null);
					Printer.instance.print("\u670d\u52a1\u5668\u5f15\u64ce\u5df2\u542f\u52a8\u3002");
					return KiftdCtl.run;
				} catch (Exception e) {
					return false;
				}
			}
			Printer.instance.print(
					"\u670d\u52a1\u5668\u8bbe\u7f6e\u68c0\u67e5\u5931\u8d25\uff0c\u65e0\u6cd5\u5f00\u542f\u670d\u52a1\u5668\u3002");
			return false;
		}
		Printer.instance.print("\u670d\u52a1\u5668\u6b63\u5728\u8fd0\u884c\u4e2d\u3002");
		return true;
	}

	public boolean stop() {
		Printer.instance.print("\u6b63\u5728\u5173\u95ed\u670d\u52a1\u5668...");
		if (KiftdCtl.context != null) {
			Printer.instance.print("\u6b63\u5728\u7ec8\u6b62\u670d\u52a1\u5668\u5f15\u64ce...");
			try {
				KiftdCtl.run = (SpringApplication.exit(KiftdCtl.context, new ExitCodeGenerator[0]) != 0);
				Printer.instance.print("\u670d\u52a1\u5668\u5f15\u64ce\u5df2\u7ec8\u6b62\u3002");
				return !KiftdCtl.run;
			} catch (Exception e) {
				return false;
			}
		}
		Printer.instance.print("\u670d\u52a1\u5668\u672a\u542f\u52a8\u3002");
		return true;
	}

	public void customize(final ConfigurableServletWebServerFactory factory) {
		factory.setPort(ConfigureReader.instance().getPort());
		factory.addErrorPages(
				new ErrorPage[] { new ErrorPage(HttpStatus.NOT_FOUND, "/errorController/pageNotFound.do") });
		factory.addErrorPages(new ErrorPage[] {
				new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/errorController/pageNotFound.do") });
	}

	public boolean started() {
		return KiftdCtl.run;
	}

	static {
		KiftdCtl.run = false;
	}
}
