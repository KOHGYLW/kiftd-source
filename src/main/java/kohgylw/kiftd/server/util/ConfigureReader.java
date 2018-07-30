package kohgylw.kiftd.server.util;

import java.util.*;
import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.pojo.*;
import java.io.*;

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
	private final String DEFAULT_ACCOUNT_AUTH = "cudr";
	private final String DEFAULT_AUTH_OVERALL = "l";
	public static final int INVALID_PORT = 1;
	public static final int INVALID_LOG = 2;
	public static final int INVALID_FILE_SYSTEM_PATH = 3;
	public static final int INVALID_BUFFER_SIZE = 4;
	public static final int CANT_CREATE_FILE_BLOCK_PATH = 5;
	public static final int CANT_CREATE_FILE_NODE_PATH = 6;
	public static final int CANT_CREATE_TF_PATH = 7;
	public static final int LEGAL_PROPERTIES = 0;

	private ConfigureReader() {
		this.propertiesStatus = -1;
		this.path = System.getProperty("user.dir");
		this.DEFAULT_FILE_SYSTEM_PATH = this.path + File.separator + "filesystem" + File.separator;
		this.confdir = this.path + File.separator + "conf" + File.separator;
		this.serverp = new Properties();
		this.accountp = new Properties();
		final File serverProp = new File(this.confdir + SERVER_PROPERTIES_FILE);
		if (!serverProp.isFile()) {
			Printer.instance.print(
					"\u670d\u52a1\u5668\u914d\u7f6e\u6587\u4ef6\u4e0d\u5b58\u5728\uff0c\u9700\u8981\u521d\u59cb\u5316\u670d\u52a1\u5668\u914d\u7f6e\u3002");
			this.createDefaultServerPropertiesFile();
		}
		final File accountProp = new File(this.confdir + ACCOUNT_PROPERTIES_FILE);
		if (!accountProp.isFile()) {
			Printer.instance.print(
					"\u7528\u6237\u8d26\u6237\u914d\u7f6e\u6587\u4ef6\u4e0d\u5b58\u5728\uff0c\u9700\u8981\u521d\u59cb\u5316\u8d26\u6237\u914d\u7f6e\u3002");
			this.createDefaultAccountPropertiesFile();
		}
		try {
			Printer.instance.print("\u6b63\u5728\u8f7d\u5165\u914d\u7f6e\u6587\u4ef6...");
			final FileInputStream serverPropIn = new FileInputStream(serverProp);
			this.serverp.load(serverPropIn);
			final FileInputStream accountPropIn = new FileInputStream(accountProp);
			this.accountp.load(accountPropIn);
			Printer.instance.print(
					"\u914d\u7f6e\u6587\u4ef6\u8f7d\u5165\u5b8c\u6bd5\u3002\u6b63\u5728\u68c0\u67e5\u914d\u7f6e...");
			this.propertiesStatus = this.testServerPropertiesAndEffect();
			if (this.propertiesStatus == 0) {
				Printer.instance.print("\u51c6\u5907\u5c31\u7eea\u3002");
			}
		} catch (Exception e) {
			Printer.instance.print(
					"\u9519\u8bef\uff1a\u65e0\u6cd5\u52a0\u8f7d\u4e00\u4e2a\u6216\u591a\u4e2a\u914d\u7f6e\u6587\u4ef6\uff08\u4f4d\u4e8e"
							+ this.confdir
							+ "\u8def\u5f84\u4e0b\uff09\uff0c\u8bf7\u5c1d\u8bd5\u5220\u9664\u65e7\u7684\u914d\u7f6e\u6587\u4ef6\u5e76\u91cd\u65b0\u542f\u52a8\u672c\u5e94\u7528\u6216\u67e5\u770b\u5b89\u88c5\u8def\u5f84\u7684\u6743\u9650\uff08\u5fc5\u987b\u53ef\u8bfb\u5199\uff09\u3002");
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
			Printer.instance.print("\u6b63\u5728\u66f4\u65b0\u670d\u52a1\u5668\u914d\u7f6e...");
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
					Printer.instance.print("\u914d\u7f6e\u66f4\u65b0\u5b8c\u6bd5\uff0c\u51c6\u5907\u5c31\u7eea\u3002");
					return true;
				} catch (Exception e) {
					Printer.instance.print(
							"\u9519\u8bef\uff1a\u66f4\u65b0\u8bbe\u7f6e\u5931\u8d25\uff0c\u65e0\u6cd5\u5b58\u5165\u8bbe\u7f6e\u6587\u4ef6\u3002");
				}
			}
		}
		return false;
	}

	private int testServerPropertiesAndEffect() {
		Printer.instance.print("\u6b63\u5728\u68c0\u67e5\u670d\u52a1\u5668\u914d\u7f6e...");
		this.mustLogin = this.serverp.getProperty("mustLogin");
		if (this.mustLogin == null) {
			Printer.instance.print(
					"\u8b66\u544a\uff1a\u672a\u627e\u5230\u662f\u5426\u5fc5\u987b\u767b\u5f55\u914d\u7f6e\uff0c\u5c06\u91c7\u7528\u9ed8\u8ba4\u503c\uff08O\uff09\u3002");
			this.mustLogin = "O";
		}
		final String ports = this.serverp.getProperty("port");
		if (ports == null) {
			Printer.instance.print(
					"\u8b66\u544a\uff1a\u672a\u627e\u5230\u7aef\u53e3\u914d\u7f6e\uff0c\u5c06\u91c7\u7528\u9ed8\u8ba4\u503c\uff088080\uff09\u3002");
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
			Printer.instance.print(
					"\u8b66\u544a\uff1a\u672a\u627e\u5230\u65e5\u5fd7\u7b49\u7ea7\u914d\u7f6e\uff0c\u5c06\u91c7\u7528\u9ed8\u8ba4\u503c\uff08E\uff09\u3002");
			this.log = "E";
		} else {
			if (!logs.equals("N") && !logs.equals("R") && !logs.equals("E")) {
				return 2;
			}
			this.log = logs;
		}
		final String bufferSizes = this.serverp.getProperty("buff.size");
		if (bufferSizes == null) {
			Printer.instance.print(
					"\u8b66\u544a\uff1a\u672a\u627e\u5230\u7f13\u51b2\u5927\u5c0f\u914d\u7f6e\uff0c\u5c06\u91c7\u7528\u9ed8\u8ba4\u503c\uff081048576\uff09\u3002");
			this.bufferSize = 1048576;
		} else {
			try {
				this.bufferSize = Integer.parseInt(bufferSizes);
				if (this.bufferSize <= 0) {
					Printer.instance
							.print("\u9519\u8bef\uff1a\u7f13\u51b2\u533a\u5927\u5c0f\u8bbe\u7f6e\u65e0\u6548\u3002");
					return 4;
				}
			} catch (Exception e2) {
				Printer.instance
						.print("\u9519\u8bef\uff1a\u7f13\u51b2\u533a\u5927\u5c0f\u8bbe\u7f6e\u65e0\u6548\u3002");
				return 4;
			}
		}
		this.FSPath = this.serverp.getProperty("FS.path");
		if (this.FSPath == null) {
			Printer.instance.print(
					"\u8b66\u544a\uff1a\u672a\u627e\u5230\u6587\u4ef6\u7cfb\u7edf\u914d\u7f6e\uff0c\u5c06\u91c7\u7528\u9ed8\u8ba4\u503c\u3002");
			this.fileSystemPath = this.DEFAULT_FILE_SYSTEM_PATH;
		} else if (this.FSPath.equals("DEFAULT")) {
			this.fileSystemPath = this.DEFAULT_FILE_SYSTEM_PATH;
		} else {
			this.fileSystemPath = this.FSPath;
		}
		final File fsFile = new File(this.fileSystemPath);
		if (!fsFile.isDirectory() || !fsFile.canRead() || !fsFile.canWrite()) {
			Printer.instance.print("\u9519\u8bef\uff1a\u6587\u4ef6\u7cfb\u7edf\u8def\u5f84[" + this.fileSystemPath
					+ "]\u65e0\u6548\uff0c\u8be5\u8def\u5f84\u5fc5\u987b\u6307\u5411\u4e00\u4e2a\u5177\u5907\u8bfb\u5199\u6743\u9650\u7684\u6587\u4ef6\u5939\u3002");
			return 3;
		}
		this.fileBlockPath = this.fileSystemPath + "fileblocks" + File.separator;
		final File fbFile = new File(this.fileBlockPath);
		if (!fbFile.isDirectory() && !fbFile.mkdirs()) {
			Printer.instance.print("\u9519\u8bef\uff1a\u65e0\u6cd5\u521b\u5efa\u6587\u4ef6\u5757\u5b58\u653e\u533a["
					+ this.fileBlockPath + "]\u3002");
			return 5;
		}
		this.fileNodePath = this.fileSystemPath + "filenodes" + File.separator;
		final File fnFile = new File(this.fileNodePath);
		if (!fnFile.isDirectory() && !fnFile.mkdirs()) {
			Printer.instance
					.print("\u9519\u8bef\uff1a\u65e0\u6cd5\u521b\u5efa\u6587\u4ef6\u8282\u70b9\u5b58\u653e\u533a["
							+ this.fileNodePath + "]\u3002");
			return 6;
		}
		this.TFPath = this.fileSystemPath + File.separator + "temporaryfiles";
		final File tfFile = new File(this.TFPath);
		if (!tfFile.isDirectory() && !tfFile.mkdirs()) {
			Printer.instance
					.print("\u9519\u8bef\uff1a\u65e0\u6cd5\u521b\u5efa\u4e34\u65f6\u6587\u4ef6\u5b58\u653e\u533a["
							+ this.TFPath + "]\u3002");
			return 7;
		}
		Printer.instance.print("\u68c0\u67e5\u5b8c\u6bd5\u3002");
		return 0;
	}

	private void createDefaultServerPropertiesFile() {
		Printer.instance.print("\u6b63\u5728\u751f\u6210\u521d\u59cb\u670d\u52a1\u5668\u914d\u7f6e\u6587\u4ef6\uff08"
				+ this.confdir + SERVER_PROPERTIES_FILE + "\uff09...");
		final Properties dsp = new Properties();
		dsp.setProperty("mustLogin", DEFAULT_MUST_LOGIN);
		dsp.setProperty("port", DEFAULT_PORT + "");
		dsp.setProperty("log", DEFAULT_LOG_LEVEL);
		dsp.setProperty("FS.path", DEFAULT_FILE_SYSTEM_PATH_SETTING);
		dsp.setProperty("buff.size", DEFAULT_BUFFER_SIZE + "");
		try {
			dsp.store(new FileOutputStream(this.confdir + SERVER_PROPERTIES_FILE),
					"<This is the default kiftd server setting file. >");
			Printer.instance
					.print("\u521d\u59cb\u670d\u52a1\u5668\u914d\u7f6e\u6587\u4ef6\u751f\u6210\u5b8c\u6bd5\u3002");
		} catch (FileNotFoundException e) {
			Printer.instance.print(
					"\u9519\u8bef\uff1a\u65e0\u6cd5\u751f\u6210\u521d\u59cb\u670d\u52a1\u5668\u914d\u7f6e\u6587\u4ef6\uff0c\u5b58\u50a8\u8def\u5f84\u4e0d\u5b58\u5728\u3002");
		} catch (IOException e2) {
			Printer.instance.print(
					"\u9519\u8bef\uff1a\u65e0\u6cd5\u751f\u6210\u521d\u59cb\u670d\u52a1\u5668\u914d\u7f6e\u6587\u4ef6\uff0c\u5199\u5165\u5931\u8d25\u3002");
		}
	}

	private void createDefaultAccountPropertiesFile() {
		Printer.instance.print("\u6b63\u5728\u751f\u6210\u521d\u59cb\u8d26\u6237\u914d\u7f6e\u6587\u4ef6\uff08"
				+ this.confdir + ACCOUNT_PROPERTIES_FILE + "\uff09...");
		final Properties dap = new Properties();
		dap.setProperty(DEFAULT_ACCOUNT_ID + ".pwd", DEFAULT_ACCOUNT_PWD);
		dap.setProperty(DEFAULT_ACCOUNT_ID + ".auth", DEFAULT_ACCOUNT_AUTH);
		dap.setProperty("authOverall", DEFAULT_AUTH_OVERALL);
		try {
			dap.store(new FileOutputStream(this.confdir + ACCOUNT_PROPERTIES_FILE),
					"<This is the default kiftd account setting file. >");
			Printer.instance.print("\u521d\u59cb\u8d26\u6237\u914d\u7f6e\u6587\u4ef6\u751f\u6210\u5b8c\u6bd5\u3002");
		} catch (FileNotFoundException e) {
			Printer.instance.print(
					"\u9519\u8bef\uff1a\u65e0\u6cd5\u751f\u6210\u521d\u59cb\u8d26\u6237\u914d\u7f6e\u6587\u4ef6\uff0c\u5b58\u50a8\u8def\u5f84\u4e0d\u5b58\u5728\u3002");
		} catch (IOException e2) {
			Printer.instance.print(
					"\u9519\u8bef\uff1a\u65e0\u6cd5\u751f\u6210\u521d\u59cb\u8d26\u6237\u914d\u7f6e\u6587\u4ef6\uff0c\u5199\u5165\u5931\u8d25\u3002");
		}
	}
}
