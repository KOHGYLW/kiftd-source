package kohgylw.kiftd.server.ftp;

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

/**
 * 
 * <h2>适用于kift文件系统的FTP文件系统工厂</h2>
 * <p>
 * 该工厂用于创建适用于kift文件系统的、基于特定用户的 {@link FileSystemView} ，以供内置FTP服务器使用。
 * </p>
 * 
 * @see FileSystemView
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class KiftFileSystemFactory implements FileSystemFactory {

	/**
	 * 
	 * 
	 * <h2>创建基于特定用户的FTP文件视图</h2>
	 * <p>
	 * 该方法用于创建基于特定用户的FTP文件视图。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @version 1.0
	 *
	 */
	@Override
	public FileSystemView createFileSystemView(User user) throws FtpException {
		synchronized (user) {

			return null;
		}
	}
	
	/**
	 * 
	 * <h2>自动补全路径结尾的“/”</h2>
	 * <p>
	 * 本方法可以确保路径以“/”结尾。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @version 1.0
	 * @return String 输出路径，必定以“/”结尾。
	 *
	 */
	private String appendSlash(String path) {
        if (path.charAt(path.length() - 1) != '/') {
            return path + '/';
        } else {
            return path;
        }
    }

}
