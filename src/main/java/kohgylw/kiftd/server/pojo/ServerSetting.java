package kohgylw.kiftd.server.pojo;

import java.util.List;

import kohgylw.kiftd.server.enumeration.*;

public class ServerSetting {
	private boolean mustLogin;
	private VCLevel vc;
	private int buffSize;
	private LogLevel log;
	private int port;
	private String fsPath;
	private boolean fileChain;
	private boolean changePassword;
	private List<ExtendStores> extendStores;

	public boolean isMustLogin() {
		return this.mustLogin;
	}

	public void setMustLogin(final boolean mustLogin) {
		this.mustLogin = mustLogin;
	}

	public int getBuffSize() {
		return this.buffSize;
	}

	public void setBuffSize(final int buffSize) {
		this.buffSize = buffSize;
	}

	public LogLevel getLog() {
		return this.log;
	}

	public void setLog(final LogLevel log) {
		this.log = log;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public String getFsPath() {
		return this.fsPath;
	}

	public void setFsPath(final String fsPath) {
		this.fsPath = fsPath;
	}

	public VCLevel getVc() {
		return this.vc;
	}

	public void setVc(VCLevel vc) {
		this.vc = vc;
	}

	public List<ExtendStores> getExtendStores() {
		return extendStores;
	}

	public void setExtendStores(List<ExtendStores> extendStores) {
		this.extendStores = extendStores;
	}

	public boolean isOpenFileChain() {
		return fileChain;
	}

	public void setFileChain(boolean fileChain) {
		this.fileChain = fileChain;
	}

	public boolean isAllowChangePassword() {
		return changePassword;
	}

	public void setChangePassword(boolean changePassword) {
		this.changePassword = changePassword;
	}
}
