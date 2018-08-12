package kohgylw.kiftd.mc;

import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.ui.module.*;
import kohgylw.kiftd.server.ctl.*;
import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.pojo.ServerSetting;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.ui.callback.*;

public class UIRunner {
	
	private static UIRunner ui;

	private UIRunner() {
		Printer.init(true);
		final ServerUIModule ui = ServerUIModule.getInsatnce();
		ui.show();
		final Thread t = new Thread(() -> {
			KiftdCtl ctl = new KiftdCtl();
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

	public static UIRunner build() {
		if (UIRunner.ui == null) {
			UIRunner.ui = new UIRunner();
		}
		return UIRunner.ui;
	}
}
