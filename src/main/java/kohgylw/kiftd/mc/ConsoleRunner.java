package kohgylw.kiftd.mc;

import kohgylw.kiftd.server.ctl.*;
import kohgylw.kiftd.server.exception.FilesTotalOutOfLimitException;
import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.server.model.Node;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.util.*;
import kohgylw.kiftd.util.file_system_manager.FileSystemManager;
import kohgylw.kiftd.util.file_system_manager.pojo.Folder;
import kohgylw.kiftd.util.file_system_manager.pojo.FolderView;

/**
 * 
 * <h2>命令模式启动器</h2>
 * <p>
 * 该启动器将以命令模式启动kiftd，请执行静态build()方法开启界面并初始化kiftd服务器引擎。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class ConsoleRunner {
	private static ConsoleRunner cs;
	private static KiftdCtl ctl;
	private static String commandTips;
	private static String fsCommandTips;
	private static FolderView currentFolder;
	private Scanner reader;

	private ExecutorService worker;

	private ConsoleRunner() {
		Printer.init(false);
		ConsoleRunner.ctl = new KiftdCtl();
		worker = Executors.newSingleThreadExecutor();
		ConsoleRunner.commandTips = "kiftd:您可以输入以下指令以控制服务器：\r\n-start 启动服务器\r\n-stop 停止服务器\r\n-exit 停止服务器并退出应用\r\n-restart 重启服务器\r\n-files 文件管理\r\n-status 查看服务器状态\r\n-help 显示帮助文本";
		ConsoleRunner.fsCommandTips = "kiftd files:您可以输入以下指令进行文件管理：\r\nls 显示当前文件夹内容\r\ncd {“文件夹名称” 或 “--文件夹序号”} 进入指定文件夹（示例：“cd foo” 或 “cd --1”，如需返回上一级请输入“cd ../”）\r\nimport {要导入的本地文件（必须使用完整路径）} 将本地文件或文件夹导入至此\r\nexport {“目标名称” 或 “--目标序号”（省略该项则导出当前文件夹的全部内容）} {要导出至本地的路径（必须使用完整路径）} 将指定文件或文件夹导出本地\r\nrm {“文件夹名称” 或 “--文件夹序号”} 删除指定文件或文件夹\r\nexit 退出文件管理并返回kiftd控制台\r\nhelp 显示帮助文本";
	}

	/**
	 * 
	 * <h2>以命令模式运行kiftd</h2>
	 * <p>
	 * 启动命令模式操作并初始化服务器引擎，该方法将返回本启动器的唯一实例。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param args
	 *            java.lang.String[] 启动参数
	 * @return kohgylw.kiftd.mc.ConsoleRunner 本启动器唯一实例
	 */
	public static ConsoleRunner build(final String[] args) {
		if (ConsoleRunner.cs == null) {
			ConsoleRunner.cs = new ConsoleRunner();
		}
		ConsoleRunner.cs.execute(args);
		return ConsoleRunner.cs;
	}

	// 执行相应的指令并进行后续处理，该方法为整个kiftd命令模式的起点。
	private void execute(final String[] args) {
		if (args.length > 0) {
			final String command = args[0];
			switch (command) {
			case "-export": {
				doExport(args);
				break;
			}
			case "-import": {
				doImport(args);
				break;
			}
			case "-console": {
				this.startKiftdByConsole();
				break;
			}
			case "-start": {
				ConsoleRunner.ctl.start();
				break;
			}
			default: {
				Printer.instance.print("kiftd:无效的指令，使用控制台模式启动请输入参数 -console，直接启动服务器引擎请输入参数 -start，使用UI模式启动请不要传入任何参数。");
				break;
			}
			}
		}
	}

	private void startKiftdByConsole() {
		Printer.instance.print(" 青阳网络文件系统-kiftd 控制台模式[Console model]");
		Printer.instance.print("Character encoding with UTF-8");
		final Thread t = new Thread(() -> {
			Printer.instance.print("正在初始化服务器...");
			if (ConfigureReader.instance().getPropertiesStatus() == 0) {
				this.awaiting();
			}
			return;
		});
		t.start();
	}

	private void startServer() {
		Printer.instance.print("执行命令：启动服务器...");
		if (ConsoleRunner.ctl.started()) {
			Printer.instance.print("错误：服务器已经启动了。您可以使用 -status 命令查看服务器运行状态或使用 -stop 命令停止服务器。");
		} else if (ConsoleRunner.ctl.start()) {
			Printer.instance.print("kiftd服务器已启动，可以正常访问了，您可以使用 -status 指令查看运行状态。");
		} else {
			if (ConfigureReader.instance().getPropertiesStatus() != 0) {
				switch (ConfigureReader.instance().getPropertiesStatus()) {
				case ConfigureReader.INVALID_PORT:
					Printer.instance.print("错误：kiftd服务器未能启动，端口设置无效。");
					break;
				case ConfigureReader.INVALID_BUFFER_SIZE:
					Printer.instance.print("错误：kiftd服务器未能启动，缓存设置无效。");
					break;
				case ConfigureReader.INVALID_FILE_SYSTEM_PATH:
					Printer.instance.print("错误：kiftd服务器未能启动，文件系统路径或某一扩展存储区设置无效。");
					break;
				case ConfigureReader.INVALID_LOG:
					Printer.instance.print("错误：kiftd服务器未能启动，日志设置无效。");
					break;
				case ConfigureReader.INVALID_VC:
					Printer.instance.print("错误：kiftd服务器未能启动，登录验证码设置无效。");
					break;
				default:
					Printer.instance.print("错误：kiftd服务器未能启动，请重试或检查设置。");
					break;
				}
			} else {
				Printer.instance.print("错误：kiftd服务器未能启动，请重试或检查设置。");
			}
		}
	}

	private void exit() {
		Printer.instance.print("执行命令：停止服务器并退出kiftd...");
		if (ConsoleRunner.ctl.started() && ConsoleRunner.ctl.stop()) {
			Printer.instance.print("服务器已关闭，停止所有访问。");
		}
		worker.shutdown();
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
		} else {
			Printer.instance.print("错误：服务器尚未启动。您可以使用 -start 命令启动服务器或使用 -status 命令查看服务器运行状态。");
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
			Printer.instance.print("错误：服务器尚未启动。您可以使用 -start 命令启动服务器或使用 -exit 命令退出应用。");
		}
	}

	private void awaiting() {
		Thread t = new Thread(() -> {
			reader = new Scanner(System.in);
			Printer.instance.print("命令帮助：\r\n" + commandTips + "\r\n");
			try {
				while (true) {
					Printer.instance.print("kiftd: console$ ");
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
					case "-files":
						fileSystemManagerModel();
						break;
					case "-exit":
						reader.close();
						exit();
						return;
					case "help":
					case "--help":
					case "-help":
						Printer.instance.print("命令帮助：\r\n" + commandTips);
						break;
					default:
						Printer.instance.print("错误：无法识别的指令。\r\n" + commandTips);
						break;
					}
				}
			} catch (Exception e) {
				Printer.instance.print("错误：读取命令时出现意外导致程序退出，请重启kiftd。");
			}
		});
		t.start();
	}

	// 打印服务器状态
	private void printServerStatus() {
		Printer.instance.print("服务器状态：\r\n<Port>端口号:" + ConfigureReader.instance().getPort() + "\r\n<LogLevel>日志等级:"
				+ ConfigureReader.instance().getLogLevel() + "\r\n<BufferSize>缓冲区大小:"
				+ ConfigureReader.instance().getBuffSize() + " B\r\n<FileSystemPath>文件系统存储路径："
				+ ConfigureReader.instance().getFileSystemPath() + "\r\n<MustLogin>是否必须登录："
				+ ConfigureReader.instance().mustLogin() + "\r\n<Running>运行状态：" + ConsoleRunner.ctl.started());
	}

	// 进入文件管理模式（航天飞机模式）
	private void fileSystemManagerModel() {
		Printer.instance.print("已进入文件管理功能。");
		try {
			FileNodeUtil.initNodeTableToDataBase();
			if (currentFolder == null || currentFolder.getCurrent() == null || FileSystemManager.getInstance()
					.selectFolderById(currentFolder.getCurrent().getFolderId()) == null) {
				getFolderView("root");
			}
		} catch (Exception e) {
			Printer.instance.print("错误：无法打开文件系统，该文件系统可能正在被另一个kiftd占用。");
			return;
		}
		System.out.println("命令帮助：\r\n" + fsCommandTips + "\r\n");
		try {
			while (true) {
				System.out.println("kiftd: " + currentFolder.getCurrent().getFolderName() + "$ ");
				String command = reader.nextLine();
				// 针对一些带参数指令的操作
				if (command.startsWith("cd ")) {
					gotoFolder(command.substring(3));
					continue;
				}
				if (command.startsWith("import ")) {
					doImport(command.substring(7));
					continue;
				}
				if (command.startsWith("rm ")) {
					doDelete(command.substring(3));
					continue;
				}
				if (command.startsWith("export ")) {
					doExport(command.substring(7));
					continue;
				}
				// 针对一些不带参数指令的操作
				switch (command) {
				case "ls":
					showCurrentFolder();
					break;
				case "exit":
					Printer.instance.print("退出文件管理。");
					return;
				case "help":
				case "--help":
				case "-help":
					Printer.instance.print("命令帮助：\r\n" + fsCommandTips);
					break;
				default:
					Printer.instance.print("错误：无法识别的指令。\r\n" + fsCommandTips);
					break;
				}
			}
		} catch (Exception e) {
			Printer.instance.print("错误：读取命令时出现意外，已退出文件管理功能。");
		}
	}

	// 得到指定ID的文件夹视图
	private void getFolderView(String fid) throws SQLException {
		currentFolder = FileSystemManager.getInstance().getFolderView(fid);
	}

	// 打印当前文件夹内容（ls）
	private void showCurrentFolder() {
		try {
			String folderId = currentFolder.getCurrent().getFolderId();
			if (Math.max(FileSystemManager.getInstance().getFilesTotalNumByFoldersId(folderId),
					FileSystemManager.getInstance().getFoldersTotalNumByFoldersId(folderId)) > Integer.MAX_VALUE) {
				System.out.println("警告：文件夹列表长度超过最大限值，只能显示前" + Integer.MAX_VALUE + "行。");
			}
			currentFolder = FileSystemManager.getInstance().getFolderView(folderId);
		} catch (SQLException e) {
			openFolderError();
			return;
		}
		List<Folder> fls = currentFolder.getFolders();
		int index = 1;
		for (Folder f : fls) {
			System.out.println("--" + index + " [文件夹] " + f);
			index++;
		}
		List<Node> fs = currentFolder.getFiles();
		for (Node f : fs) {
			System.out.println("--" + index + " [文件] " + f.getFileName());
			index++;
		}
		System.out.println();
	}

	// 进入某一文件夹，可以输入文件夹名或是前方编号（例如cd foo或是cd --1）
	private void gotoFolder(String fname) {
		try {
			currentFolder = FileSystemManager.getInstance().getFolderView(currentFolder.getCurrent().getFolderId());
			String fid = getSelectFolderId(fname);
			if (fid != null) {
				getFolderView(fid);
				return;
			}
			Printer.instance.print("错误：该文件夹不存在或其不是一个文件夹（" + fname + "）。");
		} catch (SQLException e) {
			openFolderError();
		}
	}

	// 根据路径获取文件夹ID，例如“/ROOT/foo/bar”这样的，如果不能被解析则返回null，否则返回文件夹ID
	public Object getPath(String path) {
		if (path.startsWith("/ROOT")) {
			String[] paths = path.split("/");
			try {
				String parent = "null";
				for (int i = 1; i < paths.length - 1; i++) {
					String folderName = paths[i];
					parent = FileSystemManager.getInstance().getFoldersByParentId(parent).parallelStream()
							.filter((e) -> e.getFolderName().equals(folderName)).findFirst().get().getFolderId();
				}
				String fname = paths[paths.length - 1];
				List<Folder> folders = FileSystemManager.getInstance().getFoldersByParentId(parent);
				if (path.endsWith("/") || folders.parallelStream().anyMatch((e) -> e.getFolderName().equals(fname))) {
					return folders.parallelStream().filter((e) -> e.getFolderName().equals(fname)).findFirst().get();
				} else {
					return FileSystemManager.getInstance().selectNodesByFolderId(parent).parallelStream()
							.filter((e) -> e.getFileName().equals(fname)).findFirst().get();
				}
			} catch (Exception e) {
			}
		}
		return null;
	}

	// 根据用户输入的序号或者名称得到相应的文件夹ID
	private String getSelectFolderId(String fname) {
		if ("../".equals(fname) || "..".equals(fname)) {
			if (currentFolder.getCurrent().getFolderId().equals("root")) {
				return "root";
			} else {
				return currentFolder.getCurrent().getFolderParent();
			}
		}
		if ("./".equals(fname) || ".".equals(fname)) {
			return currentFolder.getCurrent().getFolderId();
		}
		if (fname.startsWith("--")) {
			int index = Integer.parseInt(fname.substring(2));
			try {
				if (index >= 1 && index <= currentFolder.getFolders().size()) {
					return currentFolder.getFolders().get(index - 1).getFolderId();
				}
			} catch (Exception e) {

			}
			return null;
		}
		try {
			return currentFolder.getFolders().parallelStream().filter((e) -> e.getFolderName().equals(fname))
					.findFirst().get().getFolderId();
		} catch (NoSuchElementException e) {

		}
		return null;
	}

	// 根据用户输入的序号或者名称得到相应的文件或文件夹ID（不区分文件夹或文件）
	private String getSelectFolderOrFileId(String fname) {
		if ("../".equals(fname) || "..".equals(fname)) {
			if (currentFolder.getCurrent().getFolderId().equals("root")) {
				return "root";
			} else {
				return currentFolder.getCurrent().getFolderParent();
			}
		}
		if ("./".equals(fname) || ".".equals(fname)) {
			return currentFolder.getCurrent().getFolderId();
		}
		if (fname.startsWith("--")) {
			int index = Integer.parseInt(fname.substring(2));
			try {
				if (index >= 1 && index <= currentFolder.getFolders().size()) {
					return currentFolder.getFolders().get(index - 1).getFolderId();
				} else {
					return currentFolder.getFiles().get(index - currentFolder.getFolders().size() - 1).getFileId();
				}
			} catch (Exception e) {

			}
			return null;
		}
		try {
			return currentFolder.getFolders().parallelStream().filter((e) -> e.getFolderName().equals(fname))
					.findFirst().get().getFolderId();
		} catch (NoSuchElementException e) {
			try {
				return currentFolder.getFiles().parallelStream().filter((m) -> m.getFileName().equals(fname))
						.findFirst().get().getFileId();
			} catch (NoSuchElementException e2) {
			}
		}
		return null;
	}

	// 导入一个文件或文件夹（直接应用的简化版）
	private void doImport(String[] args) {
		// 针对简化命令（只有1个参数），认为要将整个ROOT导出至某位置
		try {
			FileNodeUtil.initNodeTableToDataBase();
			String importTarget;
			String importPath;
			Object path;
			File target;
			if (args.length == 2) {
				importTarget = args[1];
				importPath = "/ROOT";
				path = FileSystemManager.getInstance().selectFolderById("root");
				target = new File(importTarget);
			} else if (args.length == 3 || args.length == 4) {
				importPath = args[1];
				importTarget = args[2];
				target = new File(importTarget);
				path = getPath(importPath);
			} else {
				Printer.instance.print("错误：导入失败，必须指定导入目标（示例：“-import /ROOT/ /home/your/import/file.txt”）。");
				return;
			}
			if (!(path instanceof Folder)) {
				Printer.instance.print("错误：导入位置（" + importPath + "）必须是一个文件夹（示例：“/ROOT”）。");
				return;
			}
			String folderId = ((Folder) path).getFolderId();
			if (!target.exists()) {
				Printer.instance.print("错误：导入失败，要导入的文件或文件夹不存在（" + importTarget + "）。");
				return;
			}
			File[] files = new File[] { target };
			String type;
			if (FileSystemManager.getInstance().hasExistsFilesOrFolders(files, folderId) > 0) {
				if (args.length == 4) {
					switch (args[3]) {
					case "-C":
						type = FileSystemManager.COVER;
						break;
					case "-B":
						type = FileSystemManager.BOTH;
						break;
					default:
						Printer.instance.print("错误：导入失败，导入路径下存在相同的文件或文件夹（请使用以下参数：[-C]覆盖 [-B]保留两者）。");
						return;
					}
				} else if (args.length == 2) {
					type = FileSystemManager.COVER;
				} else {
					Printer.instance.print("错误：导入失败，导入路径下存在相同的文件或文件夹（请增加以下参数：[-C]覆盖 [-B]保留两者）。");
					return;
				}
			} else {
				type = "cancel";
			}
			if (FileSystemManager.getInstance().importFrom(files, folderId, type)) {
				return;
			} else {
				Printer.instance.print("错误：导入失败，可能导入全部文件。");
			}
		} catch (Exception e1) {
			Printer.instance.print("错误：导入失败，出现意外错误。");
		}
	}

	// 导入一个文件或文件夹
	private void doImport(String fpath) {
		File f = new File(fpath);
		if (!f.exists()) {
			Printer.instance.print("错误：无法导入文件或文件夹，该目标不存在（" + fpath + "），请重新检查。");
			return;
		}
		String targetFolder = currentFolder.getCurrent().getFolderId();
		String type = "";
		File[] importFiles = new File[] { f };
		ProgressListener pl = null;
		try {
			if (FileSystemManager.getInstance().hasExistsFilesOrFolders(importFiles, targetFolder) > 0) {
				System.out.println("提示：该路径下已经存在同名文件或文件夹（" + f.getName() + "），您希望？[C]取消 [V]覆盖 [B]保留两者");
				q: while (true) {
					String command = reader.nextLine();
					switch (command) {
					case "C":
						Printer.instance.print("导入被取消。");
						return;
					case "V":
						type = FileSystemManager.COVER;
						break q;
					case "B":
						type = FileSystemManager.BOTH;
						break q;
					default:
						System.out.println("请输入C、V或B：");
						break;
					}
				}
			}
			Printer.instance.print("正在导入，请稍候...");
			pl = new ProgressListener();
			worker.execute(pl);
			FileSystemManager.getInstance().importFrom(importFiles, targetFolder, type);
			pl.c = false;
			Printer.instance.print("导入完成。");
		} catch (FilesTotalOutOfLimitException e1) {
			if (pl != null) {
				pl.c = false;
			}
			Printer.instance.print("错误：导入失败，该文件夹内的文件数目已达上限，无法导入更多文件。");
		} catch (FoldersTotalOutOfLimitException e2) {
			if (pl != null) {
				pl.c = false;
			}
			Printer.instance.print("错误：导入失败，该文件夹内的文件夹数目已达上限，无法导入更多文件夹。");
		} catch (Exception e3) {
			if (pl != null) {
				pl.c = false;
			}
			Printer.instance.print("错误：无法导入该文件（或文件夹），请重试。");
		}
	}

	// 导出一个文件或文件夹（直接应用的简化版）
	private void doExport(String[] args) {
		// 针对简化命令（只有1个参数），认为要将整个ROOT导出至某位置
		try {
			FileNodeUtil.initNodeTableToDataBase();
			String exportTarget;
			String exportPath;
			Object target;
			File path;
			if (args.length == 2) {
				exportPath = args[1];
				exportTarget = "/ROOT";
				path = new File(exportPath);
				target = FileSystemManager.getInstance().selectFolderById("root");
			} else if (args.length == 3 || args.length == 4) {
				exportTarget = args[1];
				exportPath = args[2];
				target = getPath(exportTarget);
				path = new File(exportPath);
			} else {
				Printer.instance.print("错误：导出失败，必须指定导出路径（示例：“-export /ROOT/ /home/your/export/folder”）。");
				return;
			}
			if (!path.isDirectory()) {
				Printer.instance.print("错误：导出路径（" + exportPath + "）必须指向一个已经存在的文件夹。");
				return;
			}
			if (target == null) {
				Printer.instance.print("错误：导出失败，要导出的文件或文件夹不存在（" + exportTarget + "）。");
				return;
			}
			String[] foldersId;
			String[] filesId;
			String type;
			if (target instanceof Node) {
				foldersId = new String[] {};
				filesId = new String[] { ((Node) target).getFileId() };
			} else if (target instanceof Folder) {
				foldersId = new String[] { ((Folder) target).getFolderId() };
				filesId = new String[] {};
			} else {
				Printer.instance.print("错误：导出失败，出现意外错误。");
				return;
			}
			if (FileSystemManager.getInstance().hasExistsFilesOrFolders(foldersId, filesId, path) > 0) {
				if (args.length == 4) {
					switch (args[3]) {
					case "-C":
						type = FileSystemManager.COVER;
						break;
					case "-B":
						type = FileSystemManager.BOTH;
						break;
					default:
						Printer.instance.print("错误：导出失败，导出路径下存在相同的文件或文件夹（请使用以下参数：[-C]覆盖 [-B]保留两者）。");
						return;
					}
				} else if (args.length == 2) {
					type = FileSystemManager.COVER;
				} else {
					Printer.instance.print("错误：导出失败，导出路径下存在相同的文件或文件夹（请增加以下参数：[-C]覆盖 [-B]保留两者）。");
					return;
				}
			} else {
				type = "cancel";
			}
			if (FileSystemManager.getInstance().exportTo(foldersId, filesId, path, type)) {
				return;
			} else {
				Printer.instance.print("错误：导出失败，可能导出全部文件。");
			}
		} catch (Exception e1) {
			Printer.instance.print("错误：导出失败，出现意外错误。");
		}
	}

	// 导出一个文件或文件夹
	private void doExport(String command) {
		String[] args = command.split(" ");
		String id;
		String path;
		ProgressListener pl = null;
		if (args.length == 1) {
			path = args[0];
			id = currentFolder.getCurrent().getFolderId();
		} else if (args.length == 2) {
			id = getSelectFolderOrFileId(args[0]);
			path = args[1];
		} else {
			Printer.instance.print("错误：导出失败，输入参数不正确。");
			return;
		}
		File targetPath = new File(path);
		if (targetPath.isDirectory()) {
			if (id == null) {
				Printer.instance.print("错误：导出失败，该文件或文件夹不存在（" + args[0] + "）。");
				return;
			}
			try {
				String[] foldersId = null;
				String[] filesId = null;
				String type = "";
				if (id.equals(currentFolder.getCurrent().getFolderId())
						|| currentFolder.getFolders().parallelStream().anyMatch((e) -> e.getFolderId().equals(id))) {
					foldersId = new String[] { id };
					filesId = new String[] {};
				} else if (currentFolder.getFiles().parallelStream().anyMatch((e) -> e.getFileId().equals(id))) {
					foldersId = new String[] {};
					filesId = new String[] { id };
				} else {
					Printer.instance.print("错误：要导出的文件（或文件夹）不合法，只允许在当前文件夹内的选择（" + path + "）。");
					return;
				}
				if (FileSystemManager.getInstance().hasExistsFilesOrFolders(foldersId, filesId, targetPath) > 0) {
					System.out.println("提示：该路径下已经存在同名文件或文件夹（" + targetPath.getName() + "），您希望？[C]取消 [V]覆盖 [B]保留两者");
					q: while (true) {
						String command2 = reader.nextLine();
						switch (command2) {
						case "C":
							Printer.instance.print("导出被取消。");
							return;
						case "V":
							type = FileSystemManager.COVER;
							break q;
						case "B":
							type = FileSystemManager.BOTH;
							break q;
						default:
							System.out.println("请输入C、V或B：");
							break;
						}
					}
				}
				Printer.instance.print("正在导出，请稍候...");
				pl = new ProgressListener();
				worker.execute(pl);
				FileSystemManager.getInstance().exportTo(foldersId, filesId, targetPath, type);
				pl.c = false;
				Printer.instance.print("导出完成。");
			} catch (Exception e1) {
				if (pl != null) {
					pl.c = false;
				}
				Printer.instance.print("错误：无法导出该文件（或文件夹），请重试。");
			}
		} else {
			Printer.instance.print("错误：导出失败，导出的目标必须是一个文件夹（" + path + "）。");
		}
	}

	// 删除某一文件或文件夹
	private void doDelete(String fname) {
		ProgressListener pl = null;
		try {
			currentFolder = FileSystemManager.getInstance().getFolderView(currentFolder.getCurrent().getFolderId());
		} catch (SQLException e2) {
			openFolderError();
			return;
		}
		String id = getSelectFolderOrFileId(fname);
		try {
			if (currentFolder.getFolders().parallelStream().anyMatch((e) -> e.getFolderId().equals(id))) {
				if (confirmOpt("确认要删除该文件夹么？")) {
					Printer.instance.print("正在删除文件夹，请稍候...");
					pl = new ProgressListener();
					worker.execute(pl);
					if (FileSystemManager.getInstance().delete(new String[] { id }, new String[] {})) {
						Printer.instance.print("删除完毕。");
					} else {
						Printer.instance.print("删除失败，可能未能删除该文件夹，请重试。");
					}
					pl.c = false;
				} else {
					Printer.instance.print("已取消删除。");
				}
				return;
			}
			if (currentFolder.getFiles().parallelStream().anyMatch((e) -> e.getFileId().equals(id))) {
				if (confirmOpt("确认要删除该文件么？")) {
					Printer.instance.print("正在删除文件，请稍候...");
					pl = new ProgressListener();
					worker.execute(pl);
					if (FileSystemManager.getInstance().delete(new String[] {}, new String[] { id })) {
						Printer.instance.print("删除完毕。");
					} else {
						Printer.instance.print("删除失败，可能未能删除该文件，请重试。");
					}
					pl.c = false;
				} else {
					Printer.instance.print("已取消删除。");
				}
				return;
			}
		} catch (Exception e1) {
			if (pl != null) {
				pl.c = false;
			}
			Printer.instance.print("错误：无法删除文件，请重试。");
		}
		Printer.instance.print("错误：该文件或文件夹不存在（" + fname + "）。");
	}

	// 对于关键操作（主要就是删除）进行进一步确认，避免误操作。
	private boolean confirmOpt(String tip) {
		System.out.println("提示：" + tip + " [Y/N]");
		while (true) {
			System.out.print("> ");
			String command = reader.nextLine();
			switch (command) {
			case "Y":
				return true;
			case "N":
				return false;
			default:
				System.out.println("必须正确输入Y或N：");
				break;
			}
		}
	}

	// 这是一个专门用于显示进度的内部类，在耗时操作中应使用它，每秒显示一次进度状态。
	class ProgressListener implements Runnable {

		private boolean c = true;

		@Override
		public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			while (c) {
				System.out.println(FileSystemManager.message);
				System.out.println("当前进度：" + FileSystemManager.per + "%");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}

	}

	// 如果一个文件夹不可访问（例如被删除），则提示下列错误信息
	private void openFolderError() {
		Printer.instance.print("错误：无法读取指定文件夹，是否返回根目录？[Y/N]");
		while (true) {
			System.out.print("> ");
			String command = reader.nextLine();
			switch (command) {
			case "Y":
				try {
					currentFolder = FileSystemManager.getInstance().getFolderView("root");
				} catch (SQLException e1) {
					Printer.instance.print("错误：无法读取根目录，请尝试重新打开文件管理系统或重启kiftd。");
				}
				return;
			case "N":
				return;
			default:
				System.out.println("请输入Y或N：");
				break;
			}
		}
	}
}
