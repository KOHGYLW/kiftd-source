package kohgylw.kiftd.server.listener;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.springframework.web.context.support.WebApplicationContextUtils;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.util.ConfigureReader;

/**
 * 
 * <h2>无效额外权限设置清理监听器</h2>
 * <p>该监听器用于在服务器启动后每隔一定时间定期清理账户文件中已经失效的文件夹额外权限设置（目标文件夹已经被删除），避免这些无效配置随时间堆积。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebListener
public class InvalidAddedFolderAuthCleanListener implements ServletContextListener{
	
	private static boolean continueExec;
	public static boolean doCheck;
	private FolderMapper nm;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		continueExec  = true;
		doCheck = true;
		nm = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext()).getBean(FolderMapper.class);
		new Thread(()->{
			while(continueExec) {
				if(doCheck) {
					List<String> idList = ConfigureReader.instance().getAllAddedAuthFoldersId();
					List<String> invalidId = new ArrayList<>();
					for(String id:idList) {
						if(nm.queryById(id) == null) {
							invalidId.add(id);
						}
					}
					ConfigureReader.instance().removeAddedAuthsAndUpdate(invalidId);
					doCheck = false;//检查完毕，将需要检查状态至于false
				}
				try {
					Thread.sleep(60000);//检查周期
				} catch (InterruptedException e) {
					Printer.instance.print("错误：清理无效额外权限设置功能出现错误，该功能已失效，您可以尝试重启程序以恢复该功能。");
				}
			}
		}).start();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		continueExec = false;
	}

}
