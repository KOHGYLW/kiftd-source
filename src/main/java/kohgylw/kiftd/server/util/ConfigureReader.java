package kohgylw.kiftd.server.util;

import java.util.*;

import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.pojo.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * 
 * <h2>kiftd配置解析器</h2>
 * <p>
 * 该工具负责读取并解析配置文件，并将结果随时提供给kiftd服务器业务逻辑以完成相应功能（例如用户认证、权限判定、配置启动端口等）。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class ConfigureReader {
	private static ConfigureReader cr;
	private Properties serverp;
	private Properties accountp;
	private int propertiesStatus;
	private String path;
	private String fileSystemPath;
	private String confdir;
	private String mustLogin;
	private int port;
	private String log;
	private String FSPath;
	private int bufferSize;
	private String fileBlockPath;
	private String fileNodePath;
	private String TFPath;
	private final String ACCOUNT_PROPERTIES_FILE = "account.properties";
	private final String SERVER_PROPERTIES_FILE = "server.properties";
	private final int DEFAULT_BUFFER_SIZE = 1048576;
	private final int DEFAULT_PORT = 8080;
	private final String DEFAULT_LOG_LEVEL = "E";
	private final String DEFAULT_MUST_LOGIN = "O";
	private final String DEFAULT_FILE_SYSTEM_PATH;
	private final String DEFAULT_FILE_SYSTEM_PATH_SETTING = "DEFAULT";
	private final String DEFAULT_ACCOUNT_ID = "admin";
	private final String DEFAULT_ACCOUNT_PWD = "000000";
	private final String DEFAULT_ACCOUNT_AUTH = "cudrm";
	private final String DEFAULT_AUTH_OVERALL = "l";
	public static final int INVALID_PORT = 1;
	public static final int INVALID_LOG = 2;
	public static final int INVALID_FILE_SYSTEM_PATH = 3;
	public static final int INVALID_BUFFER_SIZE = 4;
	public static final int CANT_CREATE_FILE_BLOCK_PATH = 5;
	public static final int CANT_CREATE_FILE_NODE_PATH = 6;
	public static final int CANT_CREATE_TF_PATH = 7;
	public static final int LEGAL_PROPERTIES = 0;
	private static Thread accountPropertiesUpdateDaemonThread;

	private ConfigureReader() {
		this.propertiesStatus = -1;
		this.path = System.getProperty("user.dir");
		this.DEFAULT_FILE_SYSTEM_PATH = this.path + File.separator + "filesystem" + File.separator;
		this.confdir = this.path + File.separator + "conf" + File.separator;
		this.serverp = new Properties();
		this.accountp = new Properties();
		final File serverProp = new File(this.confdir + SERVER_PROPERTIES_FILE);
		if (!serverProp.isFile()) {
			Printer.instance.print("服务器配置文件不存在，需要初始化服务器配置。");
			this.createDefaultServerPropertiesFile();
		}
		final File accountProp = new File(this.confdir + ACCOUNT_PROPERTIES_FILE);
		if (!accountProp.isFile()) {
			Printer.instance.print("用户账户配置文件不存在，需要初始化账户配置。");
			this.createDefaultAccountPropertiesFile();
		}
		try {
			Printer.instance.print("正在载入配置文件...");
			final FileInputStream serverPropIn = new FileInputStream(serverProp);
			this.serverp.load(serverPropIn);
			final FileInputStream accountPropIn = new FileInputStream(accountProp);
			this.accountp.load(accountPropIn);
			Printer.instance.print("配置文件载入完毕。正在检查配置...");
			this.propertiesStatus = this.testServerPropertiesAndEffect();
			if (this.propertiesStatus == LEGAL_PROPERTIES) {
				Printer.instance.print("准备就绪。");
				startAccountRealTimeUpdateListener();
			}
		} catch (Exception e) {
			Printer.instance.print("错误：无法加载一个或多个配置文件（位于" + this.confdir + "路径下），请尝试删除旧的配置文件并重新启动本应用或查看安装路径的权限（必须可读写）。");
		}
	}

	public static ConfigureReader instance() {
		if (ConfigureReader.cr == null) {
			ConfigureReader.cr = new ConfigureReader();
		}
		return ConfigureReader.cr;
	}

	public boolean foundAccount(final String account) {
		final String accountPwd = this.accountp.getProperty(account + ".pwd");
		return accountPwd != null && accountPwd.length() > 0;
	}

	public boolean checkAccountPwd(final String account, final String pwd) {
		final String apwd = this.accountp.getProperty(account + ".pwd");
		return apwd != null && apwd.equals(pwd);
	}

	public boolean authorized(final String account, final AccountAuth auth) {
		if (account != null && account.length() > 0) {
			final StringBuffer auths = new StringBuffer();
			final String accauth = this.accountp.getProperty(account + ".auth");
			final String overall = this.accountp.getProperty("authOverall");
			if (accauth != null) {
				auths.append(accauth);
			}
			if (overall != null) {
				auths.append(overall);
			}
			switch (auth) {
			case CREATE_NEW_FOLDER: {
				return auths.indexOf("c") != -1;
			}
			case UPLOAD_FILES: {
				return auths.indexOf("u") != -1;
			}
			case DELETE_FILE_OR_FOLDER: {
				return auths.indexOf("d") != -1;
			}
			case RENAME_FILE_OR_FOLDER: {
				return auths.indexOf("r") != -1;
			}
			case DOWNLOAD_FILES: {
				return auths.indexOf("l") != -1;
			}
			case MOVE_FILES: {
				return auths.indexOf("m") != -1;
			}
			default: {
				return false;
			}
			}
		} else {
			final String overall2 = this.accountp.getProperty("authOverall");
			if (overall2 == null) {
				return false;
			}
			switch (auth) {
			case CREATE_NEW_FOLDER: {
				return overall2.indexOf("c") != -1;
			}
			case UPLOAD_FILES: {
				return overall2.indexOf("u") != -1;
			}
			case DELETE_FILE_OR_FOLDER: {
				return overall2.indexOf("d") != -1;
			}
			case RENAME_FILE_OR_FOLDER: {
				return overall2.indexOf("r") != -1;
			}
			case DOWNLOAD_FILES: {
				return overall2.indexOf("l") != -1;
			}
			default: {
				return false;
			}
			}
		}
	}

	public int getBuffSize() {
		return this.bufferSize;
	}

	public boolean inspectLogLevel(final LogLevel l) {
		int o = 0;
		int m = 0;
		if (l == null) {
			return false;
		}
		switch (l) {
		case None: {
			m = 0;
			break;
		}
		case Runtime_Exception: {
			m = 1;
		}
		case Event: {
			m = 2;
			break;
		}
		default: {
			m = 0;
			break;
		}
		}
		if (this.log == null) {
			this.log = "";
		}
		final String log = this.log;
		switch (log) {
		case "N": {
			o = 0;
			break;
		}
		case "R": {
			o = 1;
			break;
		}
		case "E": {
			o = 2;
			break;
		}
		default: {
			o = 1;
			break;
		}
		}
		return o >= m;
	}

	public boolean mustLogin() {
		return this.mustLogin != null && this.mustLogin.equals("N");
	}

	public String getFileSystemPath() {
		return this.fileSystemPath;
	}

	public String getFileBlockPath() {
		return this.fileBlockPath;
	}

	public String getFileNodePath() {
		return this.fileNodePath;
	}

	public String getTemporaryfilePath() {
		return this.TFPath;
	}

	public String getPath() {
		return this.path;
	}

	public LogLevel getLogLevel() {
		if (this.log == null) {
			this.log = "";
		}
		final String log = this.log;
		switch (log) {
		case "N": {
			return LogLevel.None;
		}
		case "R": {
			return LogLevel.Runtime_Exception;
		}
		case "E": {
			return LogLevel.Event;
		}
		default: {
			return null;
		}
		}
	}

	public int getPort() {
		return this.port;
	}

	public int getPropertiesStatus() {
		return this.propertiesStatus;
	}

	public boolean doUpdate(final ServerSetting ss) {
		if (ss != null) {
			Printer.instance.print("正在更新服务器配置...");
			this.serverp.setProperty("mustLogin", ss.isMustLogin() ? "N" : "O");
			this.serverp.setProperty("buff.size", ss.getBuffSize() + "");
			String loglevelCode = "E";
			switch (ss.getLog()) {
			case Event: {
				loglevelCode = "E";
				break;
			}
			case Runtime_Exception: {
				loglevelCode = "R";
				break;
			}
			case None: {
				loglevelCode = "N";
				break;
			}
			}
			this.serverp.setProperty("log", loglevelCode);
			this.serverp.setProperty("port", ss.getPort() + "");
			this.serverp.setProperty("FS.path",
					(ss.getFsPath() + File.separator).equals(this.DEFAULT_FILE_SYSTEM_PATH) ? "DEFAULT"
							: ss.getFsPath());
			if (this.testServerPropertiesAndEffect() == 0) {
				try {
					this.serverp.store(new FileOutputStream(this.confdir + SERVER_PROPERTIES_FILE),
							"<Kiftd server setting file is update.>");
					Printer.instance.print("配置更新完毕，准备就绪。");
					return true;
				} catch (Exception e) {
					Printer.instance.print("错误：更新设置失败，无法存入设置文件。");
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * <h2>验证配置并完成赋值</h2>
	 * <p>
	 * 该方法用于对配置文件进行验证并将正确的值赋予相应的属性，必须在构造器中执行本方法。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return int 验证结果代码
	 */
	private int testServerPropertiesAndEffect() {
		Printer.instance.print("正在检查服务器配置...");
		this.mustLogin = this.serverp.getProperty("mustLogin");
		if (this.mustLogin == null) {
			Printer.instance.print("警告：未找到是否必须登录配置，将采用默认值（O）。");
			this.mustLogin = "O";
		}
		final String ports = this.serverp.getProperty("port");
		if (ports == null) {
			Printer.instance.print("警告：未找到端口配置，将采用默认值（8080）。");
			this.port = 8080;
		} else {
			try {
				this.port = Integer.parseInt(ports);
				if (this.port <= 0 || this.port > 65535) {
					return 1;
				}
			} catch (Exception e) {
				return 1;
			}
		}
		final String logs = this.serverp.getProperty("log");
		if (logs == null) {
			Printer.instance.print("警告：未找到日志等级配置，将采用默认值（E）。");
			this.log = "E";
		} else {
			if (!logs.equals("N") && !logs.equals("R") && !logs.equals("E")) {
				return 2;
			}
			this.log = logs;
		}
		final String bufferSizes = this.serverp.getProperty("buff.size");
		if (bufferSizes == null) {
			Printer.instance.print("警告：未找到缓冲大小配置，将采用默认值（1048576）。");
			this.bufferSize = 1048576;
		} else {
			try {
				this.bufferSize = Integer.parseInt(bufferSizes);
				if (this.bufferSize <= 0) {
					Printer.instance.print("错误：缓冲区大小设置无效。");
					return 4;
				}
			} catch (Exception e2) {
				Printer.instance.print("错误：缓冲区大小设置无效。");
				return 4;
			}
		}
		this.FSPath = this.serverp.getProperty("FS.path");
		if (this.FSPath == null) {
			Printer.instance.print("警告：未找到文件系统配置，将采用默认值。");
			this.fileSystemPath = this.DEFAULT_FILE_SYSTEM_PATH;
		} else if (this.FSPath.equals("DEFAULT")) {
			this.fileSystemPath = this.DEFAULT_FILE_SYSTEM_PATH;
		} else {
			this.fileSystemPath = this.FSPath;
		}
		if (!fileSystemPath.endsWith(File.separator)) {
			fileSystemPath = fileSystemPath + File.separator;
		}
		final File fsFile = new File(this.fileSystemPath);
		if (!fsFile.isDirectory() || !fsFile.canRead() || !fsFile.canWrite()) {
			Printer.instance.print("错误：文件系统路径[" + this.fileSystemPath + "]无效，该路径必须指向一个具备读写权限的文件夹。");
			return 3;
		}
		this.fileBlockPath = this.fileSystemPath + "fileblocks" + File.separator;
		final File fbFile = new File(this.fileBlockPath);
		if (!fbFile.isDirectory() && !fbFile.mkdirs()) {
			Printer.instance.print("错误：无法创建文件块存放区[" + this.fileBlockPath + "]。");
			return 5;
		}
		this.fileNodePath = this.fileSystemPath + "filenodes" + File.separator;
		final File fnFile = new File(this.fileNodePath);
		if (!fnFile.isDirectory() && !fnFile.mkdirs()) {
			Printer.instance.print("错误：无法创建文件节点存放区[" + this.fileNodePath + "]。");
			return 6;
		}
		this.TFPath = this.fileSystemPath + "temporaryfiles" + File.separator;
		final File tfFile = new File(this.TFPath);
		if (!tfFile.isDirectory() && !tfFile.mkdirs()) {
			Printer.instance.print("错误：无法创建临时文件存放区[" + this.TFPath + "]。");
			return 7;
		}
		Printer.instance.print("检查完毕。");
		return 0;
	}

	public void createDefaultServerPropertiesFile() {
		Printer.instance.print("正在生成初始服务器配置文件（" + this.confdir + SERVER_PROPERTIES_FILE + "）...");
		final Properties dsp = new Properties();
		dsp.setProperty("mustLogin", DEFAULT_MUST_LOGIN);
		dsp.setProperty("port", DEFAULT_PORT + "");
		dsp.setProperty("log", DEFAULT_LOG_LEVEL);
		dsp.setProperty("FS.path", DEFAULT_FILE_SYSTEM_PATH_SETTING);
		dsp.setProperty("buff.size", DEFAULT_BUFFER_SIZE + "");
		try {
			dsp.store(new FileOutputStream(this.confdir + SERVER_PROPERTIES_FILE),
					"<This is the default kiftd server setting file. >");
			Printer.instance.print("初始服务器配置文件生成完毕。");
		} catch (FileNotFoundException e) {
			Printer.instance.print("错误：无法生成初始服务器配置文件，存储路径不存在。");
		} catch (IOException e2) {
			Printer.instance.print("错误：无法生成初始服务器配置文件，写入失败。");
		}
	}

	private void createDefaultAccountPropertiesFile() {
		Printer.instance.print("正在生成初始账户配置文件（" + this.confdir + ACCOUNT_PROPERTIES_FILE + "）...");
		final Properties dap = new Properties();
		dap.setProperty(DEFAULT_ACCOUNT_ID + ".pwd", DEFAULT_ACCOUNT_PWD);
		dap.setProperty(DEFAULT_ACCOUNT_ID + ".auth", DEFAULT_ACCOUNT_AUTH);
		dap.setProperty("authOverall", DEFAULT_AUTH_OVERALL);
		try {
			dap.store(new FileOutputStream(this.confdir + ACCOUNT_PROPERTIES_FILE),
					"<This is the default kiftd account setting file. >");
			Printer.instance.print("初始账户配置文件生成完毕。");
		} catch (FileNotFoundException e) {
			Printer.instance.print("错误：无法生成初始账户配置文件，存储路径不存在。");
		} catch (IOException e2) {
			Printer.instance.print("错误：无法生成初始账户配置文件，写入失败。");
		}
	}

	/**
	 * 
	 * <h2>获取文件节点数据库链接位置</h2>
	 * <p>
	 * 该位置为存储文件系统的数据库的链接位置，其表示为一个文件路径。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return String 用于数据源或jdbc进行连接的文件节点数据库URL地址
	 */
	public String getFileNodePathURL() {
		return "jdbc:h2:file:" + ConfigureReader.instance().getFileNodePath() + File.separator + "kift";
	}

	/**
	 * 
	 * <h2>检查某一用户是否有权限访问某一文件夹</h2>
	 * <p>
	 * 当访问文件夹的约束等级为“公开的”（0）时，永远返回true； 当为“仅小组”（1）时， 如果文件夹创建者不为“匿名用户”且当前有登录账户，
	 * 则比对当前登录账户与创建者的小组是否相同或登录账户是否与创建者相同，相同返回true。其余情况均返回false；
	 * 当为“仅创建者”（2）时，如果文件夹创建者不为“匿名用户”且与当前登录账户相同，返回true，其余情况均返回false。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f
	 *            Folder 要访问的文件夹对象
	 * @param account
	 *            String 要访问的账户
	 * @return boolean true允许访问，false不允许访问
	 */
	public boolean accessFolder(Folder f, String account) {
		int cl = f.getFolderConstraint();
		if (cl == 0) {
			return true;
		} else {
			if (account != null) {
				if (cl == 1) {
					if (f.getFolderCreator().equals(account)) {
						return true;
					}
					String vGroup = accountp.getProperty(account + ".group");
					String cGroup = accountp.getProperty(f.getFolderCreator() + ".group");
					if (vGroup != null && cGroup != null) {
						if ("*".equals(vGroup) || "*".equals(cGroup)) {
							return true;
						}
						String[] vgs = vGroup.split(";");
						String[] cgs = cGroup.split(";");
						for (String vs : vgs) {
							for (String cs : cgs) {
								if (vs.equals(cs)) {
									return true;
								}
							}
						}
					}
				}
				if (cl == 2) {
					if (f.getFolderCreator().equals(account)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * 
	 * <h2>启动账户配置文件实时更新监听</h2>
	 * <p>
	 * 该方法负责启动对账户配置文件account.properties的修改监听，并自动载入最新配置，设计在应用运行时执行，应用关闭自动结束。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void startAccountRealTimeUpdateListener() {
		if (accountPropertiesUpdateDaemonThread == null) {
			Path confPath = Paths.get(confdir);// 获取配置文件存放路径以对其中的变动进行监听
			accountPropertiesUpdateDaemonThread = new Thread(() -> {
				try {
					while (true) {
						WatchService ws = confPath.getFileSystem().newWatchService();
						confPath.register(ws, StandardWatchEventKinds.ENTRY_MODIFY);
						WatchKey wk = ws.take();
						List<WatchEvent<?>> es = wk.pollEvents();
						for (WatchEvent<?> we : es) {
							if (we.kind() == StandardWatchEventKinds.ENTRY_MODIFY && ACCOUNT_PROPERTIES_FILE.equals(we.context().toString())) {
								Printer.instance.print("正在更新账户配置信息...");
								final File accountProp = new File(this.confdir + ACCOUNT_PROPERTIES_FILE);
								final FileInputStream accountPropIn = new FileInputStream(accountProp);
								this.accountp.load(accountPropIn);
								Printer.instance.print("账户配置更新完成，已加载最新配置。");
							}
						}
					}
				} catch (Exception e) {
					// TODO 自动生成的 catch 块
					Printer.instance.print("错误：用户配置文件更改监听失败，该功能已失效，kiftd可能无法实时更新用户配置（重启应用可恢复该功能）。");
				}
			});
			accountPropertiesUpdateDaemonThread.setDaemon(true);
			accountPropertiesUpdateDaemonThread.start();
		}
	}

}
