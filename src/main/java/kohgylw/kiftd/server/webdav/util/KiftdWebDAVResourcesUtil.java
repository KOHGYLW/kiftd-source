package kohgylw.kiftd.server.webdav.util;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.webdav.pojo.KiftdWebDAVResource;

/**
 * 
 * <h2>kiftd虚拟文件系统资源操作工具</h2>
 * <p>
 * 该工具用于实现基于逻辑路径的各类操作。例如根据逻辑路径获取文件、文件夹以及封装为KiftdResource的资源对象等。 具体功能详见其中各个方法的注释。
 * 该工具使用Spring IOC容器进行管理。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class KiftdWebDAVResourcesUtil {

	@Resource
	private FolderMapper fm;
	@Resource
	private NodeMapper nm;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private FolderUtil fu;
	@Resource
	private LogUtil lu;

	/**
	 * 
	 * <h2>根据逻辑路径获取一个文件夹</h2>
	 * <p>
	 * 该方法用于根据逻辑路径获得一个文件夹对象，当逻辑路径正确时，返回其对应的文件夹，否则返回null。
	 * 注意，此方法在性能上相较于直接根据ID获取文件夹对象要差。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path 逻辑路径，必须以“/”起始。例如“/”、“/foo/”或“/foo/bar”均合法。
	 * @return kohgylw.kiftd.server.model.Folder
	 *         逻辑路径对应的文件夹对象。如果文件夹不存在或逻辑路径不合法，则返回null
	 */
	public Folder getFolderByPath(String path) {
		if (path != null && path.startsWith("/")) {
			// 如果以“/”结尾且不为“/”，则去掉最后的“/”
			String currentPath = path.endsWith("/") && path.length() > 1 ? path.substring(0, path.lastIndexOf('/'))
					: path;
			currentPath = currentPath.substring(1);
			// 从根目录开始按照声明的逻辑路径逐级搜索文件夹
			String[] folders = currentPath.split("/");
			Folder currentFolder = fm.queryById("root");
			for (String f : folders) {
				if (!f.isEmpty()) {
					Map<String, String> queryMap = new HashMap<>();
					queryMap.put("parentId", currentFolder.getFolderId());
					queryMap.put("folderName", f);
					currentFolder = fm.queryByParentIdAndFolderName(queryMap);
					if (currentFolder == null) {
						// 如果找到某一级就不存在了，则视为找不到该文件夹
						return null;
					}
				}
			}
			// 如果能顺利找到，则返回此文件夹对象
			return currentFolder;
		}
		return null;
	}

	/**
	 * 
	 * <h2>根据逻辑路径获取一个文件</h2>
	 * <p>
	 * 该方法用于根据逻辑路径获得一个文件节点对象，当逻辑路径正确时，返回其对应的文件节点，否则返回null。
	 * 注意，此方法在性能上相较于直接根据ID获取文件节点对象要差。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path 逻辑路径，必须以“/”起始，但不能以“/”结尾。例如“/foo”或“/foo/bar”均合法。
	 * @return kohgylw.kiftd.server.model.Node 逻辑路径对应的文件节点对象。如果文件不存在或逻辑路径不合法，则返回null
	 */
	public Node getNodeByPath(String path) {
		if (path != null && path.startsWith("/")) {
			// 首先，根据文件的父路径获取其所在的文件夹
			String parentPath = path.substring(0, path.lastIndexOf('/') + 1);
			Folder parentFolder = getFolderByPath(parentPath);
			if (parentFolder != null) {
				// 之后，再在这个文件夹中查找到文件
				String fileName = path.substring(path.lastIndexOf('/') + 1);
				try {
					Node file = nm.queryByParentFolderId(parentFolder.getFolderId()).parallelStream()
							.filter((e) -> e.getFileName().equals(fileName)).findAny().get();
					return file;
				} catch (NoSuchElementException e2) {
					// 如果没找到则说明逻辑路径有错，返回null即可
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * <h2>根据逻辑路径获取资源对象</h2>
	 * <p>
	 * 该方法通过逻辑路径返回一个封装好的资源对象，该资源对象是逻辑路径对应的存储在kiftd的一个虚拟文件。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path 逻辑路径，必须以“/”起始。例如“/foo”或“/foo/bar/”均合法。
	 * @return kohgylw.kiftd.server.webdav.pojo.KiftdWebDAVResource
	 *         资源对象，封装了kiftd虚拟文件系统中文件或文件夹。
	 *         该方法永远不会返回null，当路径不正确时，将会返回一个不存在的WebDAVResource对象。
	 */
	public KiftdWebDAVResource getResource(String path) {
		if (path != null && !path.endsWith("/")) {
			// 从文件（或文件夹）中获取资源对象，Windows客户端在请求文件夹时并不会以“/”结尾，因此无法判定是否为文件夹
			kohgylw.kiftd.server.model.Node node = getNodeByPath(path);
			if (node != null) {
				// 如果该逻辑路径确实对应一个文件，则返回文件节点的资源对象
				return new KiftdWebDAVResource(path, node, fbu.getFileFromBlocks(node));
			}
		}
		// 否则尝试获取文件夹的资源对象
		Folder folder = getFolderByPath(path);
		return new KiftdWebDAVResource(path, folder);
	}

	/**
	 * 
	 * <h2>获取指定逻辑路径下全部文件和文件夹的名称</h2>
	 * <p>
	 * 该方法能够获取指定逻辑路径（应对应一个文件夹）下所有允许指定账户访问的文件和文件夹名称，并以字符串数组的形式返回。 其中，文件夹的名称会以“/”结尾。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path 逻辑路径，必须以“/”起始，应当对应一个文件夹。
	 * @return java.lang.String[] 指定逻辑路径下所有允许指定用户访问的文件和文件夹名称数组。其中文件夹名称会以“/”结尾。
	 *         若指定逻辑路径未指向一个合法的文件夹或该文件夹不允许指定用户访问，则会返回一个长度为0的数组。
	 */
	public String[] list(String path, String account) {
		Folder f = getFolderByPath(path);
		// 这个文件夹存在么？
		if (f != null) {
			// 如果存在，它本身是否可以被访问？
			if (ConfigureReader.instance().accessFolder(f, account)) {
				// 如果可以，那么它之下有多少文件夹可以被访问？
				String[] folders = fm.queryByParentId(f.getFolderId()).parallelStream()
						.filter((e) -> ConfigureReader.instance().accessFolder(e, account))
						.map((e) -> e.getFolderName() + "/").toArray(String[]::new);
				String[] files = nm.queryByParentFolderId(f.getFolderId()).parallelStream().map((e) -> e.getFileName())
						.toArray(String[]::new);
				String[] result = new String[folders.length + files.length];
				System.arraycopy(folders, 0, result, 0, folders.length);
				System.arraycopy(files, 0, result, folders.length, files.length);
				return result;
			}
		}
		return new String[0];
	}

	/**
	 * 
	 * <h2>在指定逻辑路径上创建新文件夹</h2>
	 * <p>
	 * 该方法将尝试在指定逻辑路径上创建一个新的文件夹，其访问限制将设置为默认等级（父级文件夹允许的最高访问等级）。
	 * 此方法同时还会记录创建文件夹日志（如果创建成功的话）。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param folderName 要创建的文件夹的名称。
	 * @param parentFolder 要在哪个文件夹内创建此文件夹。
	 * @param account 进行该操作的账户，可以传入null（匿名访问者）或合法的账户名称。
	 * @return kohgylw.kiftd.server.model.Folder 如果创建成功则返回文件夹对象，否则返回null（包括未授权的情况）。
	 */
	public Folder mkdir(String folderName, Folder parentFolder, String account) {
		if (parentFolder != null) {
			try {
				return fu.createNewFolder(parentFolder.getFolderId(), account, folderName,
						"" + parentFolder.getFolderConstraint());
			} catch (FoldersTotalOutOfLimitException e) {
			}
		}
		return null;
	}
}
