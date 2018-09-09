package kohgylw.kiftd.mc;

public class MC {
	public static void main(final String[] args) {
		if (args == null || args.length == 0) {
			try {
				UIRunner.build();
			} catch (Exception e) {
				System.out.println("错误！无法以图形界面模式启动kiftd，您的操作系统可能不支持图形界面。您可以尝试使用命令模式“-console”来启动kiftd并开始使用。");
			}
		} else {
			ConsoleRunner.build(args);
		}
	}
}
