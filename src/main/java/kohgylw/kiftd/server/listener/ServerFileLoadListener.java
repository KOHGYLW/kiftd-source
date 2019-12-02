package kohgylw.kiftd.server.listener;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.NoticeUtil;

/**
 * 
 * <h2>服务器相关外部文件加载监听器</h2>
 * <p>
 * 该监听器实时监听服务器主文件夹，并在指定文件发生改动时将数据实时加载到系统中。理论上，所有需要在服务器运行时动态加载以保持最新的
 * 文件均应放置到程序主目录下并使用该监听器进行监听。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebListener
public class ServerFileLoadListener implements ServletContextListener {

	private Thread pathWatchServiceThread;
	private boolean run;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// 获取IOC容器，以方便调用各种工具
		final ApplicationContext context = (ApplicationContext) WebApplicationContextUtils
				.getWebApplicationContext(sce.getServletContext());
		final NoticeUtil nu = context.getBean(NoticeUtil.class);
		// 服务器启动后的初始加载部分
		nu.loadNotice();// 先加载公告文件
		// 后期的动态监听部分
		run = true;
		// 之后当监听到改动操作时再重载内容
		if (pathWatchServiceThread == null) {
			// 对服务器主目录进行监听，主要监听文件改动事件
			Path confPath = Paths.get(ConfigureReader.instance().getPath());
			pathWatchServiceThread = new Thread(() -> {
				try {
					while (run) {
						WatchService ws = confPath.getFileSystem().newWatchService();
						confPath.register(ws, StandardWatchEventKinds.ENTRY_MODIFY,
								StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_CREATE);
						WatchKey wk = ws.take();
						List<WatchEvent<?>> es = wk.pollEvents();
						for (WatchEvent<?> we : es) {
							// 根据改动文件的不同调用不同的处理方法
							switch (we.context().toString()) {
							case NoticeUtil.NOTICE_FILE_NAME:
								nu.loadNotice();// 更新公告文件
								break;

							default:
								break;
							}
						}
					}
				} catch (Exception e) {
					Printer.instance.print("错误：公告文件自动更新失败，该功能已失效，kiftd将无法实时更新公告信息（请尝试重启程序以恢复该功能）。");
				}
			});
			pathWatchServiceThread.start();
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		run = false;
	}

}
