package kohgylw.kiftd.server.listener;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.LogUtil;

/**
 * 
 * <h2>失效额外权限清理器</h2>
 * <p>该监听器用于在服务器运行的过程中自动清除被删除文件夹的额外权限设置，保持账户配置文件的整洁性。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@WebListener
public class CleanInvalidAddedAuthListener implements ServletContextListener{
	
	private static final int CYVLE_TIME=30000;//检查周期，毫秒
	private FolderMapper nm;
	private LogUtil lu;
	public static boolean needCheck;
	private static boolean continueCheck;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		needCheck=true;
		continueCheck=true;
		ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
		nm = context.getBean(FolderMapper.class);
		lu = context.getBean(LogUtil.class);
		new Thread(()->{
			while(continueCheck) {
				if(needCheck) {
					List<String> invalidIdList = new ArrayList<>();
					List<String> idList = ConfigureReader.instance().getAllAddedAuthFoldersId();
					for(String id:idList) {
						if(nm.queryById(id) == null) {
							invalidIdList.add(id);
							Printer.instance.print("文件夹ID："+id+"对应的文件夹不存在或已被删除，相关的额外权限设置将被清理。");
						}
					}
					if(ConfigureReader.instance().removeAddedAuthByFolderId(invalidIdList)) {
						Printer.instance.print("失效的额外权限设置已经清理完成。");
					}
					needCheck=false;
				}
				try {
					Thread.sleep(CYVLE_TIME);
				} catch (InterruptedException e) {
					continueCheck =false;
					lu.writeException(e);
				}
			}
		}).start();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		continueCheck=false;
	}

}
