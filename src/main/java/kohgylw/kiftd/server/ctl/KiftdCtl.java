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
 * <h2>服务器引擎控制器</h2>
 * <p>该类为服务器引擎的控制层，负责连接服务器内核与用户操作界面，用于控制服务器行为。包括启动、关闭、重启等。同时，该类也为SpringBoot框架
 * 应用入口，负责初始化SpringBoot容器。详见内置公有方法。
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
		Printer.instance.print("正在启动服务器...");
		final String[] args = new String[0];
		if (!KiftdCtl.run) {
			if (ConfigureReader.instance().getPropertiesStatus() == 0) {
				try {
					Printer.instance.print("正在开启服务器引擎...");
					KiftdCtl.context = (ApplicationContext) SpringApplication.run(KiftdCtl.class, args);
					KiftdCtl.run = (KiftdCtl.context != null);
					Printer.instance.print("服务器引擎已启动。");
					return KiftdCtl.run;
				} catch (Exception e) {
					return false;
				}
			}
			Printer.instance.print(
					"服务器设置检查失败，无法开启服务器。");
			return false;
		}
		Printer.instance.print("服务器正在运行中。");
		return true;
	}

	public boolean stop() {
		Printer.instance.print("正在关闭服务器...");
		if (KiftdCtl.context != null) {
			Printer.instance.print("正在终止服务器引擎...");
			try {
				KiftdCtl.run = (SpringApplication.exit(KiftdCtl.context, new ExitCodeGenerator[0]) != 0);
				Printer.instance.print("服务器引擎已终止。");
				return !KiftdCtl.run;
			} catch (Exception e) {
				return false;
			}
		}
		Printer.instance.print("服务器未启动。");
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
