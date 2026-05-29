package kohgylw.kiftd.server.ftp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.springframework.context.ApplicationContext;

import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.KiftdResource;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.PathUtil;
import kohgylw.kiftd.server.util.ServerTimeUtil;

/**
 * 
 * <h2>适用于kift文件系统的 {@link FtpFile} ，以供内置FTP服务器使用。</h2>
 * <p>
 * 该类是基于特定用户的、面向FTP服务的文件封装类，应由 {@link KiftFileSystemView} 创建，以供内置FTP服务器使用。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class KiftFtpFile implements FtpFile {

	// 访问该文件的用户
	private final User user;

	// 该文件的逻辑路径（存储）
	private String path;// 虚拟路径，指客户端以“/”作为起始路径访问的路径

	// 该文件在kift文件系统中对应的文件节点或文件夹对象（有且只有一个不为null）
	private KiftdResource resource;
	private PathUtil pu;
	private FolderMapper flm;
	private NodeMapper nm;
	private FolderUtil fu;
	private FileBlockUtil fbu;
	private LogUtil lu;

	public KiftFtpFile(final User user, final String path, final ApplicationContext context) {
		this.user = user;
		this.path = path;
		this.pu = context.getBean(PathUtil.class);
		this.resource = pu.getResource(this.path);
		this.flm = context.getBean(FolderMapper.class);
		this.nm = context.getBean(NodeMapper.class);
		this.fu = context.getBean(FolderUtil.class);
		this.fbu = context.getBean(FileBlockUtil.class);
		this.lu = context.getBean(LogUtil.class);
	}

	@Override
	public String getAbsolutePath() {
		// 获得绝对路径（这里指的是逻辑路径，如“/foo.bar”或“/foo/bar”）
		String fullName = path;
		// 按照规范，路径结尾不应有“/”
		int filelen = fullName.length();
		if ((filelen != 1) && (fullName.charAt(filelen - 1) == '/')) {
			fullName = fullName.substring(0, filelen - 1);
		}
		return fullName;
	}

	@Override
	public String getName() {
		return resource.getName();
	}

	@Override
	public boolean isHidden() {
		// kift文件系统中无隐藏文件功能（不能访问的文件对于用户而言应该是“不存在”的而不是“隐藏”的）
		return false;
	}

	@Override
	public boolean isDirectory() {
		// 判定是否为文件夹
		return resource.isDirectory();
	}

	@Override
	public boolean isFile() {
		// 判断是否为文件
		return resource.isFile();
	}

	@Override
	public boolean doesExist() {
		// 判断是否存在
		return resource.exists();
	}

	@Override
	public boolean isReadable() {
		// 首先该资源本身要可读
		if (resource.canRead()) {
			// 根据用户判断权限
			String account = user.getName();
			if (isFile()) {
				// 对于文件，可读是指能够访问该文件所在的文件夹，同时具备该文件的下载权限
				Node n = resource.getNode();
				Folder parentFolder = flm.queryById(n.getFileParentFolder());
				if (ConfigureReader.instance().accessFolder(parentFolder, account)) {
					return ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
							fu.getAllFoldersId(n.getFileParentFolder()));
				}
			}
			if (isDirectory()) {
				// 对于文件夹，可读是指具备该文件夹的访问权限
				Folder f = resource.getFolder();
				return ConfigureReader.instance().accessFolder(f, account);
			}
			// 如果既不是文件又不是文件夹，则为意外情况，统一返回false
		}
		return false;
	}

	@Override
	public boolean isWritable() {
		// 根据用户判断权限
		String account = user.getName();
		if (isFile()) {
			// 对于文件，可写是指：1，能够访问该文件所在的文件夹
			Node n = resource.getNode();
			Folder parentFolder = flm.queryById(n.getFileParentFolder());
			if (ConfigureReader.instance().accessFolder(parentFolder, account)) {
				// 2，具备上传权限
				if (ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES,
						fu.getAllFoldersId(n.getFileParentFolder()))) {
					File b = fbu.getFileFromBlocks(n);
					if (b != null && b.length() > 0L) {
						// 3，如果文件已经存在且不为空，则还要具备删除权限
						return ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
								fu.getAllFoldersId(parentFolder.getFolderId()));
					} else {
						// 3，如果文件不存在或者为空，则只需要上传权限
						return true;
					}
				}
			}
		}
		if (isDirectory()) {
			// 对于文件夹，可写是指具备该文件夹的访问权限
			Folder f = resource.getFolder();
			return ConfigureReader.instance().accessFolder(f, account);
		}
		// 如果既不是文件又不是文件夹，则统一返回false
		return false;
	}

	@Override
	public boolean isRemovable() {
		String account = user.getName();
		if (isFile()) {
			// 对于文件，允许删除是具备父文件夹的访问权限以及删除权限
			Node n = resource.getNode();
			Folder parentFolder = flm.queryById(n.getFileParentFolder());
			if (ConfigureReader.instance().accessFolder(parentFolder, account)) {
				return ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						fu.getAllFoldersId(parentFolder.getFolderId()));
			}
		}
		if (isDirectory()) {
			// 对于文件夹，允许删除是不能为根目录、具备访问权限及删除权限
			Folder f = resource.getFolder();
			if (!"root".equals(f.getFolderId())) {
				if (ConfigureReader.instance().accessFolder(f, account)) {
					return ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
							fu.getAllFoldersId(f.getFolderId()));
				}
			}
		}
		return false;
	}

	@Override
	public String getOwnerName() {
		if (isFile()) {
			return resource.getNode().getFileCreator();
		}
		if (isDirectory()) {
			return resource.getFolder().getFolderCreator();
		}
		return "user";
	}

	@Override
	public String getGroupName() {
		return "kiftd";
	}

	@Override
	public int getLinkCount() {
		return isDirectory() ? 3 : 1;
	}

	@Override
	public long getLastModified() {
		return resource.getLastModified();
	}

	@Override
	public boolean setLastModified(long time) {
		if (isFile()) {
			Node n = resource.getNode();
			n.setFileCreationDate(ServerTimeUtil.accurateToDay(time));
			if (nm.update(n) > 0) {
				File b = fbu.getFileFromBlocks(n);
				if (b.setLastModified(time)) {
					return true;
				}
			}
		}
		if (isDirectory()) {
			Folder f = resource.getFolder();
			f.setFolderCreationDate(ServerTimeUtil.accurateToDay(time));
			if (flm.update(f) > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public long getSize() {
		return resource.getContentLength();
	}

	@Override
	public Object getPhysicalFile() {
		if (isFile()) {
			Node n = resource.getNode();
			return fbu.getFileFromBlocks(n);
		}
		return null;
	}

	@Override
	public boolean mkdir() {
		// 首先，不能存在同名的文件夹
		if (doesExist() && isDirectory()) {

		}
		return false;
	}

	@Override
	public boolean delete() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean move(FtpFile destination) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<? extends FtpFile> listFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream createInputStream(long offset) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
