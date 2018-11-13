package kohgylw.kiftd.mc;

import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.ui.module.*;
import kohgylw.kiftd.server.ctl.*;
import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.pojo.ServerSetting;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.ui.callback.*;

/**
 * 
 * <h2>UI界面模式启动器</h2>
 * <p>该启动器将以界面模式启动kiftd，请执行静态build()方法开启界面并初始化kiftd服务器引擎。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class UIRunner {
	
	private static UIRunner ui;
	
	private UIRunner() {
		Printer.init(true);
		final ServerUIModule ui = ServerUIModule.getInsatnce();
		ui.show();
		final Thread t = new Thread(() -> {
			KiftdCtl ctl = new KiftdCtl();//服务器控制层，用于连接UI与服务器内核
			ServerUIModule.setStartServer(() -> ctl.start());
			ServerUIModule.setOnCloseServer(() -> ctl.stop());
			ServerUIModule.setGetServerTime(() -> ServerTimeUtil.getServerTime());
			ServerUIModule.setGetServerStatus(new GetServerStatus() {

				@Override
				public boolean getServerStatus() {
					// TODO 自动生成的方法存根
					return ctl.started();
				}

				@Override
				public boolean getPropertiesStatus() {
					// TODO 自动生成的方法存根
					return ConfigureReader.instance().getPropertiesStatus()==ConfigureReader.LEGAL_PROPERTIES;
				}

				@Override
				public int getPort() {
					// TODO 自动生成的方法存根
					return ConfigureReader.instance().getPort();
				}

				@Override
				public boolean getMustLogin() {
					// TODO 自动生成的方法存根
					return ConfigureReader.instance().mustLogin();
				}

				@Override
				public LogLevel getLogLevel() {
					// TODO 自动生成的方法存根
					return ConfigureReader.instance().getLogLevel();
				}

				@Override
				public String getFileSystemPath() {
					// TODO 自动生成的方法存根
					return ConfigureReader.instance().getFileSystemPath();
				}

				@Override
				public int getBufferSize() {
					// TODO 自动生成的方法存根
					return ConfigureReader.instance().getBuffSize();
				}
			});
			ServerUIModule.setUpdateSetting(new UpdateSetting() {
				
				@Override
				public boolean update(ServerSetting s) {
					// TODO 自动生成的方法存根
					return ConfigureReader.instance().doUpdate(s);
				}
			});
			if(ConfigureReader.instance().getPropertiesStatus()==ConfigureReader.LEGAL_PROPERTIES) {
				ui.updateServerStatus();
			}else {
				ConfigureReader.instance().createDefaultServerPropertiesFile();
				Printer.instance.print("配置文件存在错误，已还原为初始状态，请重新启动kiftd。");
			}
		});
		t.start();
	}
	
	/**
	 * 
	 * <h2>以UI模式运行kiftd</h2>
	 * <p>启动UI模式操作并初始化服务器引擎，该方法将返回本启动器的唯一实例。</p>
	 * @author 青阳龙野(kohgylw)
	 * @return kohgylw.kiftd.mc.UIRunner 本启动器唯一实例
	 */
	public static UIRunner build() {
		if (UIRunner.ui == null) {
			UIRunner.ui = new UIRunner();
		}
		return UIRunner.ui;
	}
}
