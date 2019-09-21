package kohgylw.kiftd.server.util;

import java.util.*;

import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.pojo.*;
import java.io.*;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.Connection;
import java.sql.DriverManager;

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

	private static ConfigureReader cr;// 自体实体
	private KiftdProperties serverp;// 配置设置
	private KiftdProperties accountp;// 账户设置
	private int propertiesStatus;// 当前配置检查结果
	private String path;// 程序主目录路径
	private String fileSystemPath;// 主文件系统路径
	private String confdir;// 设置文件夹路径
	private String mustLogin;// 必须登录
	private int port;// 端口号
	private String log;// 日志等级
	private String vc;// 验证码类型
	private String FSPath;// 主文件系统路径（设置的原始值）
	private List<ExtendStores> extendStores;// 扩展存储区路径列表
	private int bufferSize;// 缓存大小（byte）
	private String fileBlockPath;// 文件块存储路径（位于文件系统中）
	private String fileNodePath;// 文件节点存储路径（位于文件系统中）
	private String TFPath;
	private String dbURL;
	private String dbDriver;
	private String dbUser;
	private String dbPwd;
	private boolean allowChangePassword;// 是否允许用户修改密码
	private boolean openFileChain;// 是否开启永久外部链接
	private boolean allowSignUp;// 是否允许自由注册新账户（高级）
	private final String ACCOUNT_PROPERTIES_FILE = "account.properties";
	private final String SERVER_PROPERTIES_FILE = "server.properties";
	private final int DEFAULT_BUFFER_SIZE = 1048576;
	private final int DEFAULT_PORT = 8080;
	private final String DEFAULT_LOG_LEVEL = "E";
	private final String DEFAULT_VC_LEVEL = "STANDARD";
	private final String DEFAULT_MUST_LOGIN = "O";
	private final String DEFAULT_FILE_SYSTEM_PATH;
	private final String DEFAULT_FILE_SYSTEM_PATH_SETTING = "DEFAULT";
	private final String DEFAULT_ACCOUNT_ID = "admin";
	private final String DEFAULT_ACCOUNT_PWD = "000000";
	private final String DEFAULT_ACCOUNT_AUTH = "cudrm";
	private final String DEFAULT_AUTH_OVERALL = "l";
	private final String DEFAULT_PASSWORD_CHANGE_SETTING = "N";
	private final String DEFAULT_FILE_CHAIN_SETTING = "CLOSE";
	public static final int INVALID_PORT = 1;
	public static final int INVALID_LOG = 2;
	public static final int INVALID_FILE_SYSTEM_PATH = 3;
	public static final int INVALID_BUFFER_SIZE = 4;
	public static final int CANT_CREATE_FILE_BLOCK_PATH = 5;
	public static final int CANT_CREATE_FILE_NODE_PATH = 6;
	public static final int CANT_CREATE_TF_PATH = 7;
	public static final int CANT_CONNECT_DB = 8;
	public static final int HTTPS_SETTING_ERROR = 9;
	public static final int INVALID_VC = 10;
	public static final int INVALID_CHANGE_PASSWORD_SETTING = 11;
	private static final int INNVALID_FILE_CHAIN_SETTING = 12;
	public static final int LEGAL_PROPERTIES = 0;
	private static Thread accountPropertiesUpdateDaemonThread;
	private String timeZone;
	private boolean openHttps;
	private String httpsKeyFile;
	private String httpsKeyType;
	private String httpsKeyPass;
	private int httpsPort;

	private ConfigureReader() {
		this.propertiesStatus = -1;
		this.path = System.getProperty("user.dir");// 开发环境下
		String classPath = System.getProperty("java.class.path");
		if (classPath.indexOf(File.pathSeparator) < 0) {
			File f = new File(classPath);
			classPath = f.getAbsolutePath();
			if (classPath.endsWith(".jar")) {
				this.path = classPath.substring(0, classPath.lastIndexOf(File.separator));// 使用环境下
			}
		}
		this.DEFAULT_FILE_SYSTEM_PATH = this.path + File.separator + "filesystem" + File.separator;
		this.confdir = this.path + File.separator + "conf" + File.separator;
		this.serverp = new KiftdProperties();
		this.accountp = new KiftdProperties();
		extendStores = new ArrayList<>();
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

	/**
	 * 
	 * <h2>操作权限判别方法</h2>
	 * <p>
	 * 该方法用于判断账户是否具备执行指定操作的权限，若具备则返回true，否则返回false。在每一个用户操作执行前，均应先使用本方法进行判别。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param account
	 *            java.lang.String 账户的ID，如果是匿名访问可传入null
	 * @param auth
	 *            kohgylw.kiftd.server.enumeration.AccountAuth
	 *            要判断的操作类型，使用枚举类中定义的各种操作作为参数传入
	 * @param folders
	 *            该操作所发生的文件夹序列，其中应包含该操作对应的文件夹和其所有上级文件夹的ID
	 * @return boolean 是否具备该操作的权限，若具备返回true，否则返回false
	 */
	public boolean authorized(final String account, final AccountAuth auth, List<String> folders) {
		if (account != null && account.length() > 0) {
			final StringBuffer auths = new StringBuffer();
			for (String id : folders) {
				String addedAuth = accountp.getProperty(account + ".auth." + id);
				if (addedAuth != null) {
					auths.append(addedAuth);
				}
			}
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
				return auths.indexOf("c") >= 0;
			}
			case UPLOAD_FILES: {
				return auths.indexOf("u") >= 0;
			}
			case DELETE_FILE_OR_FOLDER: {
				return auths.indexOf("d") >= 0;
			}
			case RENAME_FILE_OR_FOLDER: {
				return auths.indexOf("r") >= 0;
			}
			case DOWNLOAD_FILES: {
				return auths.indexOf("l") >= 0;
			}
			case MOVE_FILES: {
				return auths.indexOf("m") >= 0;
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
				return overall2.indexOf("c") >= 0;
			}
			case UPLOAD_FILES: {
				return overall2.indexOf("u") >= 0;
			}
			case DELETE_FILE_OR_FOLDER: {
				return overall2.indexOf("d") >= 0;
			}
			case RENAME_FILE_OR_FOLDER: {
				return overall2.indexOf("r") >= 0;
			}
			case DOWNLOAD_FILES: {
				return overall2.indexOf("l") >= 0;
			}
			case MOVE_FILES: {
				return overall2.indexOf("m") >= 0;
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

	public String getInitBuffSize() {
		if (this.serverp != null && serverp.getProperty("buff.size") != null) {
			return serverp.getProperty("buff.size");
		} else {
			return DEFAULT_BUFFER_SIZE + "";
		}
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

	public String getInitFileSystemPath() {
		if (this.serverp != null && serverp.getProperty("FS.path") != null) {
			return serverp.getProperty("FS.path").equals("DEFAULT") ? DEFAULT_FILE_SYSTEM_PATH
					: serverp.getProperty("FS.path");
		} else {
			return DEFAULT_FILE_SYSTEM_PATH;
		}
	}

	public String getFileBlockPath() {
		return this.fileBlockPath;
	}

	/**
	 * 
	 * <h2>获取全部扩展存储区</h2>
	 * <p>
	 * 得到全部扩展存储区列表，以便进行文件块的存取操作。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.util.List<kohgylw.kiftd.server.pojo.ExtendStores> 所有扩展存储区对象的列表
	 */
	public List<ExtendStores> getExtendStores() {
		return extendStores;
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

	public LogLevel getInitLogLevel() {
		if (serverp != null && serverp.getProperty("log") != null) {
			switch (serverp.getProperty("log")) {
			case "N": {
				return LogLevel.None;
			}
			case "R": {
				return LogLevel.Runtime_Exception;
			}
			case "E": {
				return LogLevel.Event;
			}
			default:
				return LogLevel.Event;
			}
		} else {
			return LogLevel.Event;
		}
	}

	/**
	 * 
	 * <h2>获得验证码等级</h2>
	 * <p>
	 * 返回设置的验证码等级枚举类（kohgylw.kiftd.server.enumeration.VCLevel），包括：关闭（CLOSE）、简单（Simplified）、标准（Standard）
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return kohgylw.kiftd.server.enumeration.VCLevel 验证码等级
	 */
	public VCLevel getVCLevel() {
		if (this.vc == null) {
			this.vc = "";
		}
		final String vc = this.vc;
		switch (vc) {
		case "STANDARD": {
			return VCLevel.Standard;
		}
		case "SIMP": {
			return VCLevel.Simplified;
		}
		case "CLOSE": {
			return VCLevel.Close;
		}
		default: {
			return null;
		}
		}
	}

	public VCLevel getInitVCLevel() {
		if (serverp != null && serverp.getProperty("VC.level") != null) {
			switch (serverp.getProperty("VC.level")) {
			case "STANDARD":
				return VCLevel.Standard;
			case "SIMP":
				return VCLevel.Simplified;
			case "CLOSE":
				return VCLevel.Close;
			default:
				return VCLevel.Standard;
			}
		} else {
			return VCLevel.Standard;
		}
	}

	public int getPort() {
		return this.port;
	}

	public String getInitPort() {
		if (this.serverp != null && serverp.getProperty("port") != null) {
			return serverp.getProperty("port");
		} else {
			return DEFAULT_PORT + "";
		}
	}

	public int getPropertiesStatus() {
		return this.propertiesStatus;
	}

	/**
	 * 
	 * <h2>重新检查各项设置</h2>
	 * <p>
	 * 在服务器启动前再次检查各设置，实现某些设置的“即插即用”。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void reTestServerPropertiesAndEffect() {
		this.propertiesStatus = testServerPropertiesAndEffect();
	}

	public boolean doUpdate(final ServerSetting ss) {
		if (ss != null) {
			Printer.instance.print("正在更新服务器配置...");
			this.serverp.setProperty("mustLogin", ss.isMustLogin() ? "N" : "O");
			this.serverp.setProperty("buff.size", ss.getBuffSize() + "");
			this.serverp.setProperty("password.change", ss.isAllowChangePassword() ? "Y" : "N");
			this.serverp.setProperty("openFileChain", ss.isOpenFileChain() ? "OPEN" : "CLOSE");
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
			switch (ss.getVc()) {
			case Standard: {
				this.serverp.setProperty("VC.level", "STANDARD");
				break;
			}
			case Close: {
				this.serverp.setProperty("VC.level", "CLOSE");
				break;
			}
			case Simplified: {
				this.serverp.setProperty("VC.level", "SIMP");
				break;
			}
			}
			this.serverp.setProperty("port", ss.getPort() + "");
			this.serverp.setProperty("FS.path",
					(ss.getFsPath() + File.separator).equals(this.DEFAULT_FILE_SYSTEM_PATH) ? "DEFAULT"
							: ss.getFsPath());
			for (short i = 1; i < 32; i++) {
				this.serverp.removeProperty("FS.extend." + i);// 清空旧的扩展存储区设置
			}
			for (ExtendStores es : ss.getExtendStores()) {
				this.serverp.setProperty("FS.extend." + es.getIndex(), es.getPath().getAbsolutePath());
			}
			if (this.testServerPropertiesAndEffect() == 0) {
				try {
					this.serverp.store(new FileOutputStream(this.confdir + SERVER_PROPERTIES_FILE), null);
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
					Printer.instance.print("错误：端口号配置不正确，必须使用1-65535之间的整数。");
					return 1;
				}
			} catch (Exception e) {
				Printer.instance.print("错误：端口号配置不正确，必须使用1-65535之间的整数。");
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
		final String vcl = this.serverp.getProperty("VC.level");
		if (vcl == null) {
			Printer.instance.print("警告：未找到登录验证码配置，将采用默认值（STANDARD）。");
			this.vc = DEFAULT_VC_LEVEL;
		} else {
			switch (vcl) {
			case "STANDARD":
			case "SIMP":
			case "CLOSE":
				this.vc = vcl;
				break;
			default:
				return INVALID_VC;
			}
		}
		// 是否允许用户修改密码
		final String changePassword = this.serverp.getProperty("password.change");
		if (changePassword == null) {
			Printer.instance.print("警告：未找到用户修改密码功能配置，将采用默认值（禁止）。");
			this.allowChangePassword = false;
		} else {
			switch (changePassword) {
			case "Y":
				this.allowChangePassword = true;
				break;
			case "N":
				this.allowChangePassword = false;
				break;
			default:
				Printer.instance.print("错误：用户修改密码功能设置无效。");
				return INVALID_CHANGE_PASSWORD_SETTING;
			}
		}
		// 是否提供永久资源链接
		final String fileChain = this.serverp.getProperty("openFileChain");
		if (fileChain == null) {
			Printer.instance.print("警告：未找到永久资源链接功能配置，将采用默认值（关闭）。");
			this.openFileChain = false;
		} else {
			switch (fileChain) {
			case "OPEN":
				this.openFileChain = true;
				break;
			case "CLOSE":
				this.openFileChain = false;
				break;
			default:
				Printer.instance.print("错误：永久资源链接功能设置无效。");
				return INNVALID_FILE_CHAIN_SETTING;
			}
		}
		// 是否允许访问者自由注册账户
		final String signUp = this.accountp.getProperty("signUpAuth");
		if (signUp != null) {
			this.allowSignUp = true;
		} else {
			this.allowSignUp = false;
		}
		// 缓存大小
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
		extendStores.clear();
		for (short i = 1; i < 32; i++) {
			if (serverp.getProperty("FS.extend." + i) != null) {
				ExtendStores es = new ExtendStores();
				es.setPath(new File(serverp.getProperty("FS.extend." + i)));
				es.setIndex(i);
				extendStores.add(es);
			}
		}
		final File fsFile = new File(this.fileSystemPath);
		if (!fsFile.isDirectory() || !fsFile.canRead() || !fsFile.canWrite()) {
			Printer.instance.print("错误：文件系统路径[" + this.fileSystemPath + "]无效，该路径必须指向一个具备读写权限的文件夹。");
			return 3;
		}
		for (ExtendStores es : extendStores) {
			if (!es.getPath().isDirectory() || !es.getPath().canRead() || !es.getPath().canWrite()) {
				Printer.instance.print("错误：扩展存储区路径[" + es.getPath().getAbsolutePath() + "]无效，该路径必须指向一个具备读写权限的文件夹。");
				return 3;
			}
		}
		for (int i = 0; i < extendStores.size() - 1; i++) {
			for (int j = i + 1; j < extendStores.size(); j++) {
				if (extendStores.get(i).getPath().equals(extendStores.get(j).getPath())) {
					Printer.instance.print(
							"错误：扩展存储区路径[" + extendStores.get(j).getPath().getAbsolutePath() + "]无效，该路径已被其他扩展存储区占用。");
					return 3;
				}
			}
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

		if ("true".equals(serverp.getProperty("mysql.enable"))) {
			dbDriver = "com.mysql.cj.jdbc.Driver";
			String url = serverp.getProperty("mysql.url", "127.0.0.1/kift");
			if (url.indexOf("/") <= 0 || url.substring(url.indexOf("/")).length() == 1) {
				Printer.instance.print("错误：自定义数据库的URL中必须指定数据库名称。");
				return 8;
			}
			dbURL = "jdbc:mysql://" + url + "?useUnicode=true&characterEncoding=utf8";
			dbUser = serverp.getProperty("mysql.user", "root");
			dbPwd = serverp.getProperty("mysql.password", "");
			timeZone = serverp.getProperty("mysql.timezone");
			if (timeZone != null) {
				dbURL = dbURL + "&serverTimezone=" + timeZone;
			}
			try {
				Class.forName(dbDriver).newInstance();
				Connection testConn = DriverManager.getConnection(dbURL, dbUser, dbPwd);
				testConn.close();
			} catch (Exception e) {
				Printer.instance.print(
						"错误：无法连接至自定义数据库：" + dbURL + "（user=" + dbUser + ",password=" + dbPwd + "），请确重新配置MySQL数据库相关项。");
				return 8;
			}
		} else {
			dbDriver = "org.h2.Driver";
			dbURL = "jdbc:h2:file:" + fileNodePath + File.separator + "kift";
			dbUser = "root";
			dbPwd = "301537gY";
		}
		// https支持检查及生效处理
		if ("true".equals(serverp.getProperty("https.enable"))) {
			File keyFile = new File(path, "https.p12");
			if (keyFile.isFile()) {
				httpsKeyType = "PKCS12";
			} else {
				keyFile = new File(path, "https.jks");
				if (keyFile.isFile()) {
					httpsKeyType = "JKS";
				} else {
					Printer.instance.print(
							"错误：无法启用https支持，因为kiftd未能找到https证书文件。您必须在应用主目录内放置PKCS12（必须命名为https.p12）或JKS（必须命名为https.jks）证书。");
					return HTTPS_SETTING_ERROR;
				}
			}
			httpsKeyFile = keyFile.getAbsolutePath();
			httpsKeyPass = serverp.getProperty("https.keypass", "");
			String httpsports = serverp.getProperty("https.port");
			if (httpsports == null) {
				Printer.instance.print("警告：未找到https端口配置，将采用默认值（443）。");
				httpsPort = 443;
			} else {
				try {
					this.httpsPort = Integer.parseInt(httpsports);
					if (httpsPort <= 0 || httpsPort > 65535) {
						Printer.instance.print("错误：无法启用https支持，https访问端口号配置不正确。");
						return HTTPS_SETTING_ERROR;
					}
				} catch (Exception e) {
					Printer.instance.print("错误：无法启用https支持，https访问端口号配置不正确。");
					return HTTPS_SETTING_ERROR;
				}
			}
			openHttps = true;
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
		dsp.setProperty("VC.level", DEFAULT_VC_LEVEL);
		dsp.setProperty("FS.path", DEFAULT_FILE_SYSTEM_PATH_SETTING);
		dsp.setProperty("buff.size", DEFAULT_BUFFER_SIZE + "");
		dsp.setProperty("password.change", DEFAULT_PASSWORD_CHANGE_SETTING);
		dsp.setProperty("openFileChain", DEFAULT_FILE_CHAIN_SETTING);
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

		try (FileOutputStream accountSettingOut = new FileOutputStream(this.confdir + ACCOUNT_PROPERTIES_FILE)) {
			FileLock lock = accountSettingOut.getChannel().lock();
			dap.store(accountSettingOut, "<This is the default kiftd account setting file. >");
			lock.release();
			Printer.instance.print("初始账户配置文件生成完毕。");
		} catch (FileNotFoundException e) {
			Printer.instance.print("错误：无法生成初始账户配置文件，存储路径不存在。");
		} catch (IOException e2) {
			Printer.instance.print("错误：无法生成初始账户配置文件，写入失败。");
		}
	}

	/**
	 * 
	 * <h2>检查是否开启了mysql数据库</h2>
	 * <p>
	 * 用于检查是否使用自定义的外部MySQL数据库。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return boolean 是否使用了外部MySQL数据库
	 */
	public boolean useMySQL() {
		return serverp == null ? false : "true".equals(serverp.getProperty("mysql.enable"));
	}

	/**
	 * 
	 * <h2>获取文件节点数据库链接URL</h2>
	 * <p>
	 * 该位置为存储文件系统的数据库的链接URL，如果使用内嵌数据库则表示为一个文件路径；
	 * 如果定义了MySQL数据库位置，则使用用户自定义URL，该数据库必须使用UTF-8编码集。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return String 用于数据源或jdbc进行连接的文件节点数据库URL地址
	 */
	public String getFileNodePathURL() {
		return dbURL;
	}

	/**
	 * 
	 * <h2>获取文件节点数据库链接驱动类型</h2>
	 * <p>
	 * 如设定使用MySQL，则使用外置MySQL-connector 8.0，否则使用默认内置数据库驱动。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.lang,String 数据库驱动类型
	 */
	public String getFileNodePathDriver() {
		return dbDriver;
	}

	/**
	 * 
	 * <h2>获取文件节点数据库链接用户名</h2>
	 * <p>
	 * 如设定使用MySQL，则使用用户自定义用户名，否则使用默认内置数据库用户名。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.lang,String 数据库用户名
	 */
	public String getFileNodePathUserName() {
		return dbUser;
	}

	/**
	 * 
	 * <h2>获取文件节点数据库链接密码</h2>
	 * <p>
	 * 如设定使用MySQL，则使用用户自定义密码，否则使用默认内置数据库密码。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.lang,String 数据库密码
	 */
	public String getFileNodePathPassWord() {
		return dbPwd;
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
		if (f == null) {
			return false;// 访问不存在的文件夹肯定是没权限
		}
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
							if (we.kind() == StandardWatchEventKinds.ENTRY_MODIFY
									&& ACCOUNT_PROPERTIES_FILE.equals(we.context().toString())) {
								Printer.instance.print("正在更新账户配置信息...");
								final File accountProp = new File(this.confdir + ACCOUNT_PROPERTIES_FILE);
								final FileInputStream accountPropIn = new FileInputStream(accountProp);
								FileLock lock = accountPropIn.getChannel().lock(0L, Long.MAX_VALUE, true);
								this.accountp.load(accountPropIn);
								lock.release();
								Printer.instance.print("账户配置更新完成，已加载最新配置。");
							}
						}
					}
				} catch (Exception e) {
					Printer.instance.print("错误：用户配置文件更改监听失败，该功能已失效，kiftd无法实时更新用户配置（可尝试重启程序以恢复该功能）。");
				}
			});
			accountPropertiesUpdateDaemonThread.setDaemon(true);
			accountPropertiesUpdateDaemonThread.start();
		}
	}

	/**
	 * 
	 * <h2>是否开启Https支持</h2>
	 * <p>
	 * 该方法将返回用户是否开启了https的设置项。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return boolean 是否开启
	 */
	public boolean openHttps() {
		return openHttps;
	}

	public String getHttpsKeyType() {
		return httpsKeyType;
	}

	public String getHttpsKeyFile() {
		return httpsKeyFile;
	}

	public String getHttpsKeyPass() {
		return httpsKeyPass;
	}

	public int getHttpsPort() {
		return httpsPort;
	}

	/**
	 * 
	 * <h2>获得某一账户的上传文件体积限制</h2>
	 * <p>
	 * 该方法用于判断指定用户上传的文件是否超过规定值。使用时需传入用户账户名字符串，返回该用户的最大上传限制。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param account
	 *            java.lang.String 需要检查的账户名
	 * @return long 以byte为单位的最大阈值，若返回0则设置错误，若小于0则不限制。
	 */
	public long getUploadFileSize(String account) {
		String defaultMaxSizeP = accountp.getProperty("defaultMaxSize");
		if (account == null) {
			return getMaxSizeByString(defaultMaxSizeP);
		} else {
			String accountMaxSizeP = accountp.getProperty(account + ".maxSize");
			return accountMaxSizeP == null ? getMaxSizeByString(defaultMaxSizeP) : getMaxSizeByString(accountMaxSizeP);
		}
	}

	/**
	 * 
	 * <h2>上传文件大小设置值转化方法</h2>
	 * <p>
	 * 该方法用于将配置文件中的设置值转化为long类型的数值，例如当输入字符串“1 KB”时，输出1024，输入“5GB”时，输出5368709120。
	 * </p>
	 * <p>
	 * 输入字符串格式规则：{数值}{存储单位（可选）}。其中，存储单位可使用下列字符串之一指代（不区分大小写）：
	 * </p>
	 * <ul>
	 * <li>KB 或 K</li>
	 * <li>MB 或 M</li>
	 * <li>GB 或 G</li>
	 * </ul>
	 * <p>
	 * 当不写存储单位时，则以“B”（byte）为单位进行转换。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param in
	 *            java.lang.String 要转换的字符串内容，格式应为“{数值}{存储单位（可选）}”，例如“1024KB”或“10mb”。
	 * @return long 以Byte为单位计算的体积值，若为0则代表设置错误，若为负数则代表无限制
	 */
	private long getMaxSizeByString(String in) {
		long r = 0L;
		// 首先，判断是否为null，若是，则直接返回-1。
		if (in == null || in.length() <= 0) {
			return -1L;
		}
		// 接下来判断是否有单位，若字符串总长小于1，则必无单位，否则可能有单位。
		try {
			if (in.length() > 1) {
				String value = in.substring(0, in.length() - 1).trim();
				String unit = in.substring(in.length() - 1).toLowerCase();
				if (in.length() > 2) {
					if (in.toLowerCase().charAt(in.length() - 1) == 'b') {
						unit = in.substring(in.length() - 2, in.length() - 1).toLowerCase();
						value = in.substring(0, in.length() - 2).trim();
					}
				}
				switch (unit) {
				case "k":
					r = Integer.parseInt(value) * 1024L;
					break;
				case "m":
					r = Integer.parseInt(value) * 1048576L;
					break;
				case "g":
					r = Integer.parseInt(value) * 1073741824L;
					break;
				default:
					r = Integer.parseInt(in.trim());
					break;
				}
			} else {
				r = Integer.parseInt(in.trim());
			}
		} catch (Exception e) {
		}
		return r;
	}

	public List<String> getAllAddedAuthFoldersId() {
		List<String> foldersId = new ArrayList<>();
		for (Iterator<String> it = accountp.stringPropertieNames().iterator(); it.hasNext();) {
			String config = it.next();
			int index = config.lastIndexOf(".auth.");
			if (index >= 0) {
				foldersId.add(config.substring(index + 6));
			}
		}
		return foldersId;
	}

	public boolean removeAddedAuthByFolderId(List<String> fIds) {
		if (fIds == null || fIds.size() == 0) {
			return false;
		}
		Set<String> configs = accountp.stringPropertieNames();
		List<String> invalidConfigs = new ArrayList<>();
		for (String fId : fIds) {
			for (String config : configs) {
				if (config.endsWith(".auth." + fId)) {
					invalidConfigs.add(config);
				}
			}
		}
		for (String config : invalidConfigs) {
			accountp.removeProperty(config);
		}
		try (FileOutputStream accountSettingOut = new FileOutputStream(this.confdir + ACCOUNT_PROPERTIES_FILE)) {
			FileLock lock = accountSettingOut.getChannel().lock();
			accountp.store(accountSettingOut, null);
			lock.release();
			return true;
		} catch (Exception e) {
			Printer.instance.print("错误：更新账户配置文件时出现错误，请立即检查账户配置文件。");
			return false;
		}
	}

	/**
	 * 
	 * <h2>是否允许用户修改密码</h2>
	 * <p>
	 * 判断是否允许用户修改密码，若允许则开启用户修改密码功能。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return boolean 允许则返回true，否则返回false
	 */
	public boolean isAllowChangePassword() {
		return allowChangePassword;
	}

	/**
	 * 
	 * <h2>是否开启永久资源链接</h2>
	 * <p>
	 * 判断是否开启永久资源链接，若开启则提供每个可下载文件的外部链接。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return boolean 开启则返回true，否则返回false
	 */
	public boolean isOpenFileChain() {
		return openFileChain;
	}

	/**
	 * 
	 * <h2>是否允许注册新账户</h2>
	 * <p>
	 * 判断是否允许注册新账户，若允许则访问者可以自由注册新的账户。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return boolean 允许则返回true，否则返回false
	 */
	public boolean signUp() {
		return allowSignUp;
	}
}
