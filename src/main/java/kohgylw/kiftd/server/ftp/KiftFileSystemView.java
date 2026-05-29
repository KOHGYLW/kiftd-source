package kohgylw.kiftd.server.ftp;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.springframework.context.ApplicationContext;

/**
 * 
 * <h2>适用于kift文件系统的FTP文件视图</h2>
 * <p>
 * 该文件视图是基于特定用户的 {@link FileSystemView} ，应由 {@link KiftFileSystemFactory}
 * 创建，以供内置FTP服务器使用。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class KiftFileSystemView implements FileSystemView {

	// 用户的根目录，在kift文件系统中，所有用户的根目录均为“/ROOT/”
	private static final String ROOT_DIR = "/ROOT/";

	// 用户当前操作的目录
	private String currDir;

	// 用户是谁
	private final User user;

	// Spring上下文，用于获取文件系统的功能对象
	private final ApplicationContext context;

	public KiftFileSystemView(User user, ApplicationContext context) {
		if (user == null) {
			throw new IllegalArgumentException("FTP服务：未能正确获取用户信息。");
		}
		if (context == null) {
			throw new IllegalArgumentException("FTP服务：未能正确初始化Spring上下文。");
		}
		// 设定用户
		this.user = user;
		// 初始情况下，用户的当前目录设定为根目录
		this.currDir = ROOT_DIR;
		// 设定Spring上下文
		this.context = context;
	}

	/**
	 * 
	 * 
	 * <h2>获取根目录</h2>
	 * <p>
	 * 获取用户的根目录——在kift文件系统中，任何用户的根目录均为“/ROOT/”。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @version 1.0
	 *
	 */
	@Override
	public FtpFile getHomeDirectory() throws FtpException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 
	 * 
	 * <h2>获取当前工作目录</h2>
	 * <p>
	 * 详细说明
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @version 1.0
	 *
	 */
	@Override
	public FtpFile getWorkingDirectory() throws FtpException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean changeWorkingDirectory(String dir) throws FtpException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FtpFile getFile(String file) throws FtpException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRandomAccessible() throws FtpException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

}
