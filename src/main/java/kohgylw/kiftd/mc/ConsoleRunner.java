package kohgylw.kiftd.mc;

import kohgylw.kiftd.server.ctl.*;

import java.util.Scanner;

import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.util.*;

public class ConsoleRunner {
	private static ConsoleRunner cs;
	private static KiftdCtl ctl;
	private static String commandTips;

	private ConsoleRunner() {
		Printer.init(false);
		ConsoleRunner.ctl=new KiftdCtl();
		ConsoleRunner.commandTips = "您可以输入以下指令以控制服务器：\r\n-start 启动服务器\r\n-stop 停止服务器\r\n-exit 停止服务器并退出应用\r\n-restart 重启服务器\r\n-status 查看服务器状态";
	}

	public static ConsoleRunner build(final String[] args) {
		if (ConsoleRunner.cs == null) {
			ConsoleRunner.cs = new ConsoleRunner();
		}
		ConsoleRunner.cs.execute(args);
		return ConsoleRunner.cs;
	}

	private void execute(final String[] args) {
		if (args.length > 0) {
			final String command = args[0];
			switch (command) {
			case "-console": {
				this.startKiftdByConsole();
				break;
			}
			case "-start":{
				ConsoleRunner.ctl.start();
				break;
			}
			default: {
				Printer.instance.print("kiftd:无效的指令，使用控制台模式启动请输入参数 -console，使用UI模式启动请不传入任何参数。");
				break;
			}
			}
		}
	}

	private void startKiftdByConsole() {
		Printer.instance.print(" 青阳网络文件系统-kiftd 控制台模式[Console model]");
		Printer.instance.print("kiftd:Character encoding with UTF-8");
		final Thread t = new Thread(() -> {
			Printer.instance.print("正在初始化服务器...");
			if (ConfigureReader.instance().getPropertiesStatus() == 0) {
				this.awaiting();
			} else {
				Printer.instance.print("服务器参数配置错误，无法启动kiftd，请检查。");
			}
			return;
		});
		t.start();
	}

	private void startServer() {
		Printer.instance.print("执行命令：启动服务器...");
		if (ConsoleRunner.ctl.started()) {
			Printer.instance.print("错误：服务器已经启动了。");
		} else if (ConsoleRunner.ctl.start()) {
			Printer.instance.print("kiftd服务器已启动，可以正常访问了，您可以使用 -status 指令查看运行状态。");
		} else {
			Printer.instance.print("错误：kiftd服务器未能启动，请重试或检查设置。");
		}
	}

	private void exit() {
		Printer.instance.print("执行命令：停止服务器并退出kiftd...");
		if (ConsoleRunner.ctl.started() && ConsoleRunner.ctl.stop()) {
			Printer.instance.print("服务器已关闭，停止所有访问。");
		}
		Printer.instance.print("退出应用。");
		System.exit(0);
	}

	private void restartServer() {
		Printer.instance.print("执行命令：重启服务器...");
		if (ConsoleRunner.ctl.started()) {
			if (ConsoleRunner.ctl.stop()) {
				if (ConsoleRunner.ctl.start()) {
					Printer.instance.print("服务器重启成功，可以正常访问了。");
				} else {
					Printer.instance.print("错误：无法重新启动服务器，请尝试手动启动。");
				}
			} else {
				Printer.instance.print("错误：无法关闭服务器，请尝试手动关闭。");
			}
		}
	}

	private void stopServer() {
		Printer.instance.print("执行命令：停止服务器...");
		if (ConsoleRunner.ctl.started()) {
			if (ConsoleRunner.ctl.stop()) {
				Printer.instance.print("服务器已关闭，停止所有访问。");
			} else {
				Printer.instance.print("错误：无法关闭服务器，您可以尝试强制关闭。");
			}
		} else {
			Printer.instance.print("错误：服务器尚未启动。");
		}
	}

	private void awaiting() {
		Thread t = new Thread(() -> {
			Scanner reader = new Scanner(System.in);
			try {
				while (true) {
					String command = reader.nextLine();
					switch (command) {
					case "-start":
						startServer();
						break;
					case "-stop":
						stopServer();
						break;
					case "-restart":
						restartServer();
						break;
					case "-status":
						printServerStatus();
						break;
					case "-exit":
						reader.close();
						exit();
						return;
					default:
						Printer.instance.print("错误：未能识别输入的内容，" + commandTips);
						break;
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
				Printer.instance.print("错误：读取命令时出现意外导致程序退出，请重启kiftd。");
			}
		});
		t.start();
	}

	private void printServerStatus() {
		Printer.instance.print("服务器状态：\r\n<Port>端口号:" + ConfigureReader.instance().getPort() + "\r\n<LogLevel>日志等级:"
				+ ConfigureReader.instance().getLogLevel() + "\r\n<BufferSize>缓冲区大小:"
				+ ConfigureReader.instance().getBuffSize() + " B\r\n<FileSystemPath>文件系统存储路径："
				+ ConfigureReader.instance().getFileSystemPath() + "\r\n<MustLogin>是否必须登录："
				+ ConfigureReader.instance().mustLogin() + "\r\n<Running>运行状态：" + ConsoleRunner.ctl.started());
	}
}
