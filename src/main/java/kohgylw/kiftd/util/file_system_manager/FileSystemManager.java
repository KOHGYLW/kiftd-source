package kohgylw.kiftd.util.file_system_manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.exception.FilesTotalOutOfLimitException;
import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.ExtendStores;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.FileNodeUtil;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.util.file_system_manager.pojo.Folder;
import kohgylw.kiftd.util.file_system_manager.pojo.FolderView;

/**
 * 
 * <h2>kiftd文件系统管理器</h2>
 * <p>
 * 该管理器用于提供本地文件系统相关操作（例如文件的导入导出）。为确保操作安全，其中的所有方法在同一时刻应只有一个被调用。
 * 该类被设计为单例模式，请使用静态方法getInstance()获取其唯一实例。当您第一次获取它时，该工具将加载资源并开启数据库链接
 * （可能会有一定的耗时）。
 * 每次使用前都请手动调用kohgylw.kiftd.server.util.FileNodeUtil.initNodeTableToDataBase()方法以确保节点链接最新。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FileSystemManager {

	public static final String BOTH = "BOTH";
	public static final String COVER = "COVER";
	private static FileSystemManager fsm;// 唯一实例
	private static final int BUFFER_SIZE = 4096;// 缓存大小，单位为Byte(B)
	/**
	 * 单文件夹内最大允许的文件夹或文件数量上限
	 */
	public static final int MAX_FOLDERS_OR_FILES_LIMIT = Integer.MAX_VALUE;

	/**
	 * 正在进行的操作进度，请只读勿改
	 */
	public static int per;
	/**
	 * 正在进行的操作提示，请只读勿改
	 */
	public static String message;

	// 用于中途取消操作的标识
	private boolean gono;

	// 缓存各式各样的、基本的查询语句，功能见名称
	private PreparedStatement selectFolderById;
	private PreparedStatement selectNodeById;
	private PreparedStatement selectNodesByFolderId;
	private PreparedStatement selectFoldersByParentFolderId;
	private PreparedStatement insertNode;
	private PreparedStatement insertFolder;
	private PreparedStatement deleteNodeById;
	private PreparedStatement deleteFolderById;
	private PreparedStatement updateFolderById;
	private PreparedStatement countNodesByFolderId;
	private PreparedStatement countFoldersByParentFolderId;
	private PreparedStatement selectNodesByPathExcludeById;

	// 加载资源
	private FileSystemManager() {
		Connection c = FileNodeUtil.getNodeDBConnection();
		try {
			selectFolderById = c.prepareStatement("SELECT * FROM FOLDER WHERE folder_id = ?");
			selectNodeById = c.prepareStatement("SELECT * FROM FILE WHERE file_id = ?");
			selectNodesByPathExcludeById = c.prepareStatement(
					"SELECT * FROM FILE WHERE file_path = ? AND file_id <> ? LIMIT 0," + MAX_FOLDERS_OR_FILES_LIMIT);
			selectNodesByFolderId = c.prepareStatement(
					"SELECT * FROM FILE WHERE file_parent_folder = ? LIMIT 0," + MAX_FOLDERS_OR_FILES_LIMIT);
			selectFoldersByParentFolderId = c.prepareStatement(
					"SELECT * FROM FOLDER WHERE folder_parent = ? LIMIT 0," + MAX_FOLDERS_OR_FILES_LIMIT);
			insertNode = c.prepareStatement("INSERT INTO FILE VALUES(?,?,?,?,?,?,?)");
			insertFolder = c.prepareStatement("INSERT INTO FOLDER VALUES(?,?,?,?,?,?)");
			deleteNodeById = c.prepareStatement("DELETE FROM FILE WHERE file_id = ?");
			deleteFolderById = c.prepareStatement("DELETE FROM FOLDER WHERE folder_id = ?");
			updateFolderById = c.prepareStatement(
					"UPDATE FOLDER SET folder_name= ? , folder_creation_date = ? , folder_creator = ? , folder_parent = ? , folder_constraint = ? WHERE folder_id = ?");
			countNodesByFolderId = c.prepareStatement("SELECT count(file_id) FROM FILE WHERE file_parent_folder = ?");
			countFoldersByParentFolderId = c
					.prepareStatement("SELECT count(folder_id) FROM FOLDER WHERE folder_parent = ?");
		} catch (SQLException e) {
			Printer.instance.print("错误：出现未知错误，文件系统解析失败，无法浏览文件。");
		}
	}

	/**
	 * 
	 * <h2>获取文件管理器唯一实例</h2>
	 * <p>
	 * 请通过该方法获取其唯一实例并使用，首次获取时可能会耗时并加载资源。注意，请务必确保在每次使用前均已重新执行初始化节点操作。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return kohgylw.kiftd.util.file_system_manager.FileSystemManager 唯一实例
	 */
	public static FileSystemManager getInstance() {
		if (fsm == null) {
			fsm = new FileSystemManager();
		}
		return fsm;
	}

	/**
	 * 
	 * <h2>根据ID获得指定文件夹视图</h2>
	 * <p>
	 * 由文件系统得到ID代表的文件视图。
	 * </p>
	 * 
	 * @see kohgylw.kiftd.util.file_system_manager.pojo.FolderView
	 * @author 青阳龙野(kohgylw)
	 * @param folderId java.lang.String 文件夹ID
	 * @return kohgylw.kiftd.util.file_system_manager.pojo.FolderView 指定的文件夹视图
	 * @throws SQLException 获取失败
	 */
	public FolderView getFolderView(String folderId) throws SQLException {
		Folder target = selectFolderById(folderId);
		if (target != null) {
			FolderView fv = new FolderView();
			fv.setCurrent(target);
			fv.setFiles(selectNodesByFolderId(folderId));
			fv.setFolders(getFoldersByParentId(folderId));
			return fv;
		} else {
			throw new SQLException();
		}
	}

	/**
	 * 
	 * <h2>删除指定文件和文件夹</h2>
	 * <p>
	 * 该方法将直接删除指定文件夹和文件，该操作无法恢复。鉴于操作较为耗时，该操作将在一个新线程中进行。操作时，您可以根据“per”属性来实时获取该操作的进度。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param foldersId java.lang.String[] 要删除的文件夹ID
	 * @param filesId   java.lang.String[] 要删除的文件ID
	 * @return boolean 删除结果
	 * @throws SQLException
	 */
	public boolean delete(String[] foldersId, String[] filesId) throws Exception {
		gono = true;
		for (int i = 0; i < filesId.length && gono; i++) {
			deleteFile(filesId[i]);
		}
		for (int i = 0; i < foldersId.length && gono; i++) {
			deleteFolder(foldersId[i]);
		}
		return gono;
	}

	/**
	 * 
	 * <h2>将文件系统内的文件或文件夹导出至指定位置</h2>
	 * <p>
	 * 将文件夹ID和文件ID指向的内容导出至指定路径，并规定如果存在该文件时应进行何种处理。鉴于操作较为耗时，该操作将在一个新线程中进行。操作时，您可以根据“per”属性来实时获取该操作的进度。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param foldersId java.lang.String[] 要导出的文件夹ID
	 * @param filesId   java.lang.String[] 要导出的文件ID
	 * @param path      java.io.File 要导出的目标路径，必须是个文件夹
	 * @param type      java.lang.String 导出操作类型：“COVER”覆盖或“BOTH”保留两者，其他情况默认为“COVER”
	 * @return boolean 是否全部导出成功
	 * @throws Exception
	 */
	public boolean exportTo(String[] foldersId, String[] nodesId, File path, String type) throws Exception {
		gono = true;
		for (int i = 0; i < nodesId.length && gono; i++) {
			exportNode(nodesId[i], path, type);
		}
		for (int i = 0; i < foldersId.length && gono; i++) {
			exportFolder(foldersId[i], path, type);
		}
		return gono;
	}

	/**
	 * 
	 * <h2>检查文件系统中指定路径下是否存在某些名称的文件或文件夹</h2>
	 * <p>
	 * 该方法被设计为导入功能的前置，您应该在用户导入前先进行本检查以确定是否应该让用户选择导入方式。
	 * 如果返回值为0则可以直接导入，否则应选择覆盖或是保留两者或是取消。注意：该方法会阻塞线程。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param files    java.io.File[] 要检查的文件对象
	 * @param folderId java.lang.String 要检查的目标文件夹ID
	 * @return int 存在同名文件（或文件夹）的数量，当被检查的路径下不存在同名文件（或文件夹）时返回0
	 * @throws SQLException
	 */
	public int hasExistsFilesOrFolders(File[] files, String folderId) throws SQLException {
		int result = 0;
		List<Folder> folders = getFoldersByParentId(folderId);
		List<Node> nodes = selectNodesByFolderId(folderId);
		for (File f : files) {
			if (f.isDirectory() && folders.parallelStream().anyMatch((e) -> e.getFolderName()
					.equals(new String(f.getName().getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"))))) {
				result++;
			} else if (nodes.parallelStream().anyMatch((e) -> e.getFileName()
					.equals(new String(f.getName().getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"))))) {
				result++;
			}
		}
		return result;
	}

	/**
	 * 
	 * <h2>检查指定路径下是否存在与文件系统中同名的文件或文件夹</h2>
	 * <p>
	 * 该方法被设计为导出功能的前置，您应该在用户导出前先进行本检查以确定是否应该让用户选择导出方式。
	 * 如果返回值为0则可以直接导出，否则应选择覆盖或是保留两者或是取消。注意：该方法会阻塞线程。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param foldersId    java.lang.String[] 要导出的文件夹ID
	 * @param filesId      java.lang.String[] 要导出的文件ID
	 * @param java.io.File path 要检查的目标路径
	 * @return int 存在同名文件（或文件夹）的数量，当被检查的路径下不存在同名文件（或文件夹）时返回0
	 * @throws SQLException
	 */
	public int hasExistsFilesOrFolders(String[] foldersId, String[] filesId, File path) throws Exception {
		if (path.isDirectory()) {
			int c = 0;
			List<Folder> folders = new ArrayList<>();
			List<Node> nodes = new ArrayList<>();
			for (String fid : foldersId) {
				folders.add(selectFolderById(fid));
			}
			for (String nid : filesId) {
				nodes.add(selectNodeById(nid));
			}
			for (File f : path.listFiles()) {
				if (f.isDirectory() && folders.parallelStream().anyMatch((e) -> e.getFolderName().equals(
						new String(f.getName().getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"))))) {
					c++;
				} else if (nodes.parallelStream().anyMatch((e) -> e.getFileName().equals(
						new String(f.getName().getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"))))) {
					c++;
				}
			}
			return c;
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * 
	 * <h2>将本地文件导入至文件系统中</h2>
	 * <p>
	 * 将指定文件导入至文件系统，其中文件夹会自动创建，可以规定如何处理已存在的文件。鉴于操作较为耗时，该操作将在一个新线程中进行。操作时，您可以根据“per”属性来实时获取该操作的进度。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param files    java.io.File[] 要导入的文件对象
	 * @param folderId java.lang.String 目标文件夹ID
	 * @param type     java.lang.String 导入操作类型：“COVER”覆盖或“BOTH”保留两者，其他情况默认为“COVER”
	 * @return boolean 是否全部导入成功
	 * @throws Exception
	 */
	public boolean importFrom(File[] files, String folderId, String type) throws Exception {
		gono = true;
		for (int i = 0; i < files.length && gono; i++) {
			if (files[i].isDirectory()) {
				importFolderInto(files[i], folderId, type);
			} else {
				importFileInto(files[i], folderId, type);
			}
		}
		return gono;
	}

	/**
	 * 
	 * <h2>获取指定文件夹内的所有子文件夹</h2>
	 * <p>
	 * 该功能用于提供浏览文件夹功能，您可以通过迭代调用本方法来获取文件系统内的全部文件夹，注意：该操作会阻塞线程。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param folderId java.lang.String 需要查询的父文件夹ID
	 * @return java.util.List<kohgylw.kiftd.server.model.Folder> 文件夹对象列表，如果没有结果则长度为0
	 * @throws SQLException 查询失败
	 */
	public List<Folder> getFoldersByParentId(String folderId) throws SQLException {
		selectFoldersByParentFolderId.setString(1, folderId);
		ResultSet r = selectFoldersByParentFolderId.executeQuery();
		List<Folder> folders = new ArrayList<>();
		while (r.next()) {
			folders.add(resultSetAccessFolder(r));
		}
		return folders;
	}

	// 根据ID查询文件夹对象，无符合对象则返回null
	public Folder selectFolderById(String folderId) throws SQLException {
		selectFolderById.setString(1, folderId);
		ResultSet r = selectFolderById.executeQuery();
		if (r.next()) {
			return resultSetAccessFolder(r);
		}
		return null;
	}

	// 根据ID查询文件节点对象，无符合对象则返回null
	private Node selectNodeById(String nodeId) throws SQLException {
		selectNodeById.setString(1, nodeId);
		ResultSet r = selectNodeById.executeQuery();
		if (r.next()) {
			return resultSetAccessNode(r);
		}
		return null;
	}

	// 查询指定文件夹内的所有文件节点，如无符合则返回空List
	public List<Node> selectNodesByFolderId(String folderId) throws SQLException {
		List<Node> nodes = new ArrayList<>();
		selectNodesByFolderId.setString(1, folderId);
		ResultSet r = selectNodesByFolderId.executeQuery();
		while (r.next()) {
			nodes.add(resultSetAccessNode(r));
		}
		return nodes;
	}

	// 查询指定Block ID对应的所有文件节点，但不会包括指定ID的文件节点，如无符合则返回空List
	private List<Node> selectNodesByPathExcludeById(String path, String fileId) throws SQLException {
		List<Node> nodes = new ArrayList<>();
		selectNodesByPathExcludeById.setString(1, path);
		selectNodesByPathExcludeById.setString(2, fileId);
		ResultSet r = selectNodesByPathExcludeById.executeQuery();
		while (r.next()) {
			nodes.add(resultSetAccessNode(r));
		}
		return nodes;
	}

	// 插入一个新的文件节点并返回插入结果，0失败，1成功
	private int insertNode(Node n) throws SQLException {
		insertNode.setString(1, n.getFileId());
		insertNode.setString(2, n.getFileName());
		insertNode.setString(3, n.getFileSize());
		insertNode.setString(4, n.getFileParentFolder());
		insertNode.setString(5, n.getFileCreationDate());
		insertNode.setString(6, n.getFileCreator());
		insertNode.setString(7, n.getFilePath());
		insertNode.execute();
		return insertNode.getUpdateCount();
	}

	// 插入一个新的文件夹节点，返回值同上
	private int insertFolder(Folder f) throws SQLException {
		insertFolder.setString(1, f.getFolderId());
		insertFolder.setString(2, f.getFolderName());
		insertFolder.setString(3, f.getFolderCreationDate());
		insertFolder.setString(4, f.getFolderCreator());
		insertFolder.setString(5, f.getFolderParent());
		insertFolder.setInt(6, f.getFolderConstraint());
		insertFolder.execute();
		return insertFolder.getUpdateCount();
	}

	// 将SQL结果集封装为Node对象
	private Node resultSetAccessNode(ResultSet r) throws SQLException {
		Node node = new Node();
		node.setFileId(r.getString("file_id"));
		node.setFileName(r.getString("file_name"));
		node.setFileSize(r.getString("file_size"));
		node.setFileParentFolder(r.getString("file_parent_folder"));
		node.setFileCreationDate(r.getString("file_creation_date"));
		node.setFileCreator(r.getString("file_creator"));
		node.setFilePath(r.getString("file_path"));
		return node;
	}

	// 将SQL结果集封装为Folder对象
	private Folder resultSetAccessFolder(ResultSet r) throws SQLException {
		Folder folder = new Folder();
		folder.setFolderId(r.getString("folder_id"));
		folder.setFolderName(r.getString("folder_name"));
		folder.setFolderParent(r.getString("folder_parent"));
		folder.setFolderCreationDate(r.getString("folder_creation_date"));
		folder.setFolderCreator(r.getString("folder_creator"));
		folder.setFolderConstraint(r.getInt("folder_constraint"));
		return folder;
	}

	// 将一个本地文件导入到文件系统中，注意，导入的必须是文件而不是文件夹
	private void importFileInto(File f, String folderId, String type) throws Exception {
		if (f.isFile()) {
			String name = f.getName();
			String newName = name;// 这个变量记录最终使用的文件名，初始等于原文件名，如果冲突可能改为其他名称
			per = 0;// 操作进度置0
			message = "正在导入文件：" + name;// 初始化操作信息
			long size = f.length();// 获得文件体积
			// 检查目标文件夹内是否有重名文件？
			List<Node> nodes = selectNodesByFolderId(folderId);
			if (nodes.parallelStream().anyMatch((e) -> e.getFileName().equals(name))) {
				// 有？那么是覆盖还是保留两者？
				switch (type) {
				case COVER:
					// 覆盖
					Node node = nodes.parallelStream().filter((e) -> e.getFileName().equals(f.getName())).findFirst()
							.get();// 得到重名节点，删除它
					deleteFile(node.getFileId());
					if(selectNodeById(node.getFileId()) != null) {
						// 测试是否删除成功
						throw new IOException();
					}
					break;
				case BOTH:
					// 保留两者（计数命名法 foo.bar -> foo (1).bar）
					newName = FileNodeUtil.getNewNodeName(name, nodes);
					break;
				default:
					// 意外情况，比如跳过，则直接视为操作完成。
					per = 100;
					return;
				}
			}
			// 如果无重名文件，或是选择了保留两者，那么均以新建一个节点进行插入的逻辑处理
			if (getFilesTotalNumByFoldersId(folderId) >= MAX_FOLDERS_OR_FILES_LIMIT) {
				throw new FilesTotalOutOfLimitException();
			}
			// 首先，生成一个新文件节点并写入基本信息
			Node node = new Node();
			node.setFileName(newName);
			node.setFileId(UUID.randomUUID().toString());
			node.setFileParentFolder(folderId);
			node.setFileCreationDate(ServerTimeUtil.accurateToDay());
			node.setFileCreator("SYS_IN");
			int mb = (int) (size / 1024L / 1024L);
			node.setFileSize(mb + "");
			// 保存文件块并写入新节点
			File block = saveToFileBlocks(f);
			if (block == null) {
				throw new IOException();
			}
			node.setFilePath(block.getName());
			// 之后，将新节点插入文件系统数据库
			int i = 0;// 记录插入新节点的尝试次数（如果一次未成功，多试几次看看）
			while (true) {
				try {
					// 尝试插入
					if (insertNode(node) > 0) {
						// 成功后，要检查父节点是否存在以确保插入的节点一定有父节点，避免产生“死节点”问题。
						if (selectFolderById(folderId) != null) {
							return;// 一切顺利，结束操作
						}
					}
					break;
				} catch (Exception e2) {
					// 如果插入时出现异常，可能是由于主键重复或意外错误导致的，用新的主键再试
					node.setFileId(UUID.randomUUID().toString());
					i++;
				}
				if (i >= 10) {
					break;// 如果重试超过10次仍无法成功，则终止继续重试。
				}
			}
			// 如果没能从正常的执行中退出，则说明文件存入失败，此时要将残留文件块和节点清理并向上抛出异常。
			block.delete();
			deleteNodeById(node.getFileId());
			throw new SQLException();
		}
		// 如果导入目标不是个文件（文件夹/不存在），那么直接抛出参数错误异常
		throw new IllegalArgumentException();
	}

	// 将一个本地文件夹导入至文件系统，必须是文件夹而不是文件。它会自动将其中的文件和文件夹也一并导入。
	private void importFolderInto(File f, String folderId, String type) throws Exception {
		if (f.isDirectory()) {
			String name = f.getName();
			String newName = name;
			per = 0;
			message = "正在导入文件夹：" + name;
			Folder parent = selectFolderById(folderId);
			List<Folder> folders = getFoldersByParentId(folderId);
			Folder folder = null;
			if (folders.parallelStream().anyMatch((e) -> e.getFolderName().equals(name))) {
				switch (type) {
				case COVER:
					folder = folders.parallelStream().filter((e) -> e.getFolderName().equals(name)).findFirst().get();
					break;
				case BOTH:
					newName = FileNodeUtil.getNewFolderName(name, folders);
					break;
				default:
					return;
				}
			}
			per = 50;
			if (folder == null) {
				if (getFoldersTotalNumByFoldersId(folderId) >= MAX_FOLDERS_OR_FILES_LIMIT) {
					throw new FoldersTotalOutOfLimitException();// 如果已经超过了最大限值，那么不能继续导入文件夹。
				}
				folder = new Folder();
				String nFolderId = UUID.randomUUID().toString();
				folder.setFolderId(nFolderId);
				folder.setFolderName(newName);
				folder.setFolderConstraint(parent.getFolderConstraint());
				folder.setFolderParent(folderId);
				if ("root".equals(parent.getFolderId())) {
					folder.setFolderCreator("SYS_IN");
				} else {
					folder.setFolderCreator(parent.getFolderCreator());
				}
				folder.setFolderCreationDate(ServerTimeUtil.accurateToDay());
				int i = 0;
				while (true) {
					try {
						if (insertFolder(folder) == 0 || selectFolderById(folderId) == null) {
							deleteFolderById(folder.getFolderId());
							throw new SQLException();
						}
						break;
					} catch (Exception e2) {
						folder.setFolderId(UUID.randomUUID().toString());
						i++;
					}
					if (i >= 10) {
						break;
					}
				}
			} else {
				folder.setFolderCreationDate(ServerTimeUtil.accurateToDay());
				if (updateFolder(folder) == 0 || selectFolderById(folderId) == null) {
					deleteFolderById(folder.getFolderId());
					throw new SQLException();
				}
			}
			per = 100;
			File[] childs = f.listFiles();
			for (int i = 0; i < childs.length && gono; i++) {
				if (childs[i].isDirectory()) {
					importFolderInto(childs[i], folder.getFolderId(), type);
				} else {
					importFileInto(childs[i], folder.getFolderId(), type);
				}
			}
			// 迭代执行直至将全部文件夹及文件导入完毕为止
		} else {
			throw new IllegalArgumentException();
		}
	}

	// 删除一个文件节点
	private int deleteNodeById(String nodeId) throws SQLException {
		deleteNodeById.setString(1, nodeId);
		deleteNodeById.execute();
		return deleteNodeById.getUpdateCount();
	}

	// 删除一个文件夹
	private int deleteFolderById(String folderId) throws SQLException {
		deleteFolderById.setString(1, folderId);
		deleteFolderById.execute();
		return deleteFolderById.getUpdateCount();
	}

	// 修改文件夹
	private int updateFolder(Folder f) throws SQLException {
		updateFolderById.setString(1, f.getFolderName());
		updateFolderById.setString(2, f.getFolderCreationDate());
		updateFolderById.setString(3, f.getFolderCreator());
		updateFolderById.setString(4, f.getFolderParent());
		updateFolderById.setInt(5, f.getFolderConstraint());
		updateFolderById.setString(6, f.getFolderId());
		updateFolderById.execute();
		return updateFolderById.getUpdateCount();
	}

	// 删除一个文件夹
	private void deleteFolder(String folderId) throws Exception {
		Folder f = selectFolderById(folderId);
		List<Node> nodes = selectNodesByFolderId(folderId);
		int size = nodes.size();
		if (f == null) {
			return;
		}
		per = 0;
		message = "正在删除文件夹：" + f.getFolderName();
		// 删除该文件夹内的所有文件
		for (int i = 0; i < size && gono; i++) {
			deleteFile(nodes.get(i).getFileId());
		}
		List<Folder> folders = getFoldersByParentId(folderId);
		size = folders.size();
		// 迭代删除该文件夹内的所有文件夹
		for (int i = 0; i < size && gono; i++) {
			deleteFolder(folders.get(i).getFolderId());
		}
		per = 50;
		// 删除自己的数据
		if (deleteFolderById(folderId) > 0) {
			per = 100;
			return;
		}
		throw new SQLException();
	}

	// 删除一个文件
	private void deleteFile(String nodeId) throws SQLException, IOException {
		Node n = selectNodeById(nodeId);
		per = 50;
		message = "正在删除文件：" + n.getFileName();
		if (n != null) {
			if (deleteNodeById(nodeId) > 0) {
				per = 80;
				List<Node> nodes = selectNodesByPathExcludeById(n.getFilePath(), n.getFileId());
				String recycleBinPath = ConfigureReader.instance().getRecycleBinPath();
				File block = getFileFormBlocks(n);
				if (nodes == null || nodes.isEmpty()) {
					// 删除文件节点对应的数据块
					if (block != null) {
						if (recycleBinPath != null) {
							if (saveToRecycleBin(block, recycleBinPath, n.getFileName(), false)) {
								per = 100;
								return;
							}
						} else {
							if (block.delete()) {
								per = 100;
								return;
							}
						}
					}
				} else {
					if (block != null) {
						if (recycleBinPath != null) {
							if (saveToRecycleBin(block, recycleBinPath, n.getFileName(), true)) {
								per = 100;
								return;
							}
						} else {
							per = 100;
							return;
						}
					}
				}
			}
			throw new SQLException();
		}
	}

	// 留档功能，应与kohgylw.kiftd.server.util.FileBlockUtil.saveToRecycleBin(File, String,
	// String, boolean)相同
	private boolean saveToRecycleBin(File block, String recycleBinPath, String originalName, boolean isCopy)
			throws IOException {
		File recycleBinDir = new File(recycleBinPath);
		if (recycleBinDir.isDirectory()) {
			// 当留档路径合法时，查找其中是否有当前日期的留档子文件夹
			File dateDir = new File(recycleBinDir, ServerTimeUtil.accurateToLogName());
			if (dateDir.isDirectory() || dateDir.mkdir()) {
				// 如果有，则直接使用，否则创建当前日期的留档子文件夹，之后检查此文件夹内是否有重名留档文件
				int i = 0;
				List<String> fileNames = Arrays.asList(dateDir.list());
				String newName = originalName;
				while (fileNames.contains(newName)) {
					i++;
					if (originalName.indexOf(".") >= 0) {
						newName = originalName.substring(0, originalName.lastIndexOf(".")) + " (" + i + ")"
								+ originalName.substring(originalName.lastIndexOf("."));
					} else {
						newName = originalName + " (" + i + ")";
					}
				}
				// 在确保不会产生重名文件的前提下，按照移动或拷贝两种方式留档
				File saveFile = new File(dateDir, newName);
				if (isCopy) {
					Files.copy(block.toPath(), saveFile.toPath());
				} else {
					Files.move(block.toPath(), saveFile.toPath());
				}
				// 如果不抛出任何异常，则操作成功
				return true;
			}
		}
		return false;
	}

	// 导出一个文件节点
	private void exportNode(String nodeId, File path, String type) throws Exception {
		Node node = selectNodeById(nodeId);
		File target = null;
		if (node != null && path != null && path.isDirectory()) {
			per = 0;
			message = "正在导出文件：" + node.getFileName();
			if (Arrays.stream(path.listFiles()).parallel().filter((e) -> e.isFile())
					.anyMatch((f) -> new String(f.getName().getBytes()).equals(node.getFileName()))) {
				switch (type) {
				case COVER:
					target = Arrays.stream(path.listFiles()).parallel().filter((e) -> e.isFile())
							.filter((e) -> new String(e.getName().getBytes()).equals(node.getFileName())).findFirst()
							.get();
					break;
				case BOTH:
					target = new File(path, new String(FileNodeUtil.getNewNodeName(node, path).getBytes()));
					target.createNewFile();
					break;
				default:
					return;
				}
			}
			if (target == null) {
				target = new File(path, new String(node.getFileName().getBytes()));
				target.createNewFile();
			}
			File block = getFileFormBlocks(node);
			long size = block.length();
			FileInputStream in = new FileInputStream(block);
			FileOutputStream out = new FileOutputStream(target);
			FileChannel fci = in.getChannel();
			FileChannel fco = out.getChannel();
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			int length = 0;
			long finishLength = 0;
			while ((length = fci.read(buffer)) != -1 && gono) {
				buffer.flip();
				fco.write(buffer);
				buffer.clear();
				finishLength += length;
				per = (int) (((double) finishLength / (double) size) * 100);
			}
			fci.close();
			fco.close();
			in.close();
			out.close();
			return;
		}
		throw new IllegalArgumentException();
	}

	// 导出一个文件夹
	private void exportFolder(String folderId, File path, String type) throws Exception {
		Folder folder = selectFolderById(folderId);
		File target = null;
		per = 0;
		message = "正在导出文件夹：" + folder.getFolderName();
		if (folder != null && path != null && path.isDirectory()) {
			if (Arrays.stream(path.listFiles()).parallel().filter((e) -> e.isDirectory())
					.anyMatch((f) -> new String(f.getName().getBytes()).equals(folder.getFolderName()))) {
				switch (type) {
				case COVER:
					target = Arrays.stream(path.listFiles()).parallel().filter((e) -> e.isDirectory())
							.filter((e) -> new String(e.getName().getBytes()).equals(folder.getFolderName()))
							.findFirst().get();
					break;
				case BOTH:
					target = new File(path, new String(FileNodeUtil.getNewFolderName(folder, path).getBytes()));
					target.mkdir();
					break;

				default:
					return;
				}
			}
			if (Arrays.stream(path.listFiles()).parallel().filter((e) -> e.isFile())
					.anyMatch((e) -> new String(e.getName().getBytes()).equals(folder.getFolderName()))) {
				target = new File(path, new String(folder.getFolderName().getBytes()) + "_与文件同名"
						+ UUID.randomUUID().toString().replaceAll("-", ""));
				target.mkdir();
			}
			if (target == null) {
				target = new File(path, new String(folder.getFolderName().getBytes()));
				target.mkdir();
			}
			per = 100;
			List<Node> nodes = selectNodesByFolderId(folderId);
			List<Folder> folders = getFoldersByParentId(folderId);
			int size = 0;
			int i = 0;
			for (i = 0, size = nodes.size(); i < size && gono; i++) {
				exportNode(nodes.get(i).getFileId(), target, type);
			}
			for (i = 0, size = folders.size(); i < size && gono; i++) {
				exportFolder(folders.get(i).getFolderId(), target, type);
			}
			return;
		}
		throw new IllegalArgumentException();
	}

	/**
	 * 
	 * <h2>立即终止当前操作</h2>
	 * <p>
	 * 这是一个不安全的终止操作，可能会导致文件损坏，但不会污染文件节点系统。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void cannel() {
		message = "正在终止，请稍候...";
		gono = false;
	}

	private File getFileFormBlocks(Node f) {
		// 检查该节点对应的文件块存放于哪个位置（主文件系统/扩展存储区）
		try {
			File file = null;
			if (f.getFilePath().startsWith("file_")) {// 存放于主文件系统中
				// 直接从主文件系统的文件块存放区获得对应的文件块
				file = new File(ConfigureReader.instance().getFileBlockPath(), f.getFilePath());
			} else {// 存放于扩展存储区
				short index = Short.parseShort(f.getFilePath().substring(0, f.getFilePath().indexOf('_')));
				// 根据编号查到对应的扩展存储区路径，进而获取对应的文件块
				file = new File(ConfigureReader.instance().getExtendStores().parallelStream()
						.filter((e) -> e.getIndex() == index).findAny().get().getPath(), f.getFilePath());
			}
			if (file.isFile()) {
				return file;
			}
		} catch (Exception e) {
			Printer.instance.print("错误：文件数据读取失败。详细信息：" + e.getMessage());
		}
		return null;
	}

	public File saveToFileBlocks(final File f) {
		// 如果存在扩展存储区，则优先在文件块最少的扩展存储区中存放文件（避免占用主文件系统）
		List<ExtendStores> ess = ConfigureReader.instance().getExtendStores();// 得到全部扩展存储区
		if (ess.size() > 0) {// 如果存在
			Collections.sort(ess, new Comparator<ExtendStores>() {
				@Override
				public int compare(ExtendStores o1, ExtendStores o2) {
					try {
						return o1.getPath().list().length - o2.getPath().list().length;
					} catch (Exception e) {
						try {
							// 如果文件太多以至于超出数组上限，则换用如下统计方法
							long dValue = Files.list(o1.getPath().toPath()).count()
									- Files.list(o2.getPath().toPath()).count();
							return dValue > 0L ? 1 : dValue == 0 ? 0 : -1;
						} catch (IOException e1) {
							return 0;
						}
					}
				}
			});
			// 遍历这些扩展存储区，并尝试将新文件存入一个已有文件数目最少、同时容量又足够的扩展存储区中
			for (ExtendStores es : ess) {
				// 如果该存储区的空余容量大于要存放的文件
				if (es.getPath().getFreeSpace() > f.length()) {
					try {
						File file = createNewBlock(es.getIndex() + "_", es.getPath());
						if (file != null) {
							transferFile(f, file);// 则执行存放，并将文件命名为“{存储区编号}_{UUID}.block”的形式
							return file;
						} else {
							continue;
						}
					} catch (IOException e) {
						// 如果无法存入（由于体积过大或其他问题），那么继续尝试其他扩展存储区
						continue;
					} catch (Exception e) {
						Printer.instance.print(e.getMessage());
						continue;
					}
				}
			}
		}
		// 如果不存在扩展存储区或者最大的扩展存储区无法存放目标文件，则尝试将其存放至主文件系统路径下
		try {
			final File target = createNewBlock("file_", new File(ConfigureReader.instance().getFileBlockPath()));
			if (target != null) {
				transferFile(f, target);// 执行存放，并肩文件命名为“file_{UUID}.block”的形式
				return target;
			}
		} catch (Exception e) {
			Printer.instance.print("错误：文件块生成失败，无法存入新的文件数据。详细信息：" + e.getMessage());
		}
		return null;
	}

	// 生成创建一个在指定路径下名称（编号）绝对不重复的新文件块
	private File createNewBlock(String prefix, File parent) throws IOException {
		int appendIndex = 0;
		int retryNum = 0;
		String newName = prefix + UUID.randomUUID().toString().replace("-", "");
		File newBlock = new File(parent, newName + ".block");
		while (!newBlock.createNewFile()) {
			if (appendIndex >= 0 && appendIndex < Integer.MAX_VALUE) {
				newBlock = new File(parent, newName + "_" + appendIndex + ".block");
				appendIndex++;
			} else {
				if (retryNum >= 5) {
					return null;
				} else {
					newName = prefix + UUID.randomUUID().toString().replace("-", "");
					newBlock = new File(parent, newName + ".block");
					retryNum++;
				}
			}
		}
		return newBlock;
	}

	private void transferFile(File f, File target) throws Exception {
		long size = f.length();
		FileOutputStream fileOutputStream = new FileOutputStream(target);
		FileInputStream fileInputStream = new FileInputStream(f);
		FileChannel out = fileOutputStream.getChannel();
		FileChannel in = fileInputStream.getChannel();
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		int length = 0;
		long finishLength = 0;
		while ((length = in.read(buffer)) != -1 && gono) {
			buffer.flip();
			out.write(buffer);
			buffer.clear();
			finishLength += length;
			per = (int) (((double) finishLength / (double) size) * 100);
		}
		in.close();
		out.close();
		fileInputStream.close();
		fileOutputStream.close();
		return;
	}

	/**
	 * 
	 * <h2>根据父文件夹的ID获取其下已有文件数目</h2>
	 * <p>
	 * 该方法用于统计指定文件夹下的已有文件数量。应在各种存入新文件操作前先调用该方法来判断是否超过规定值。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param pfId java.lang.String 需要统计的文件夹ID
	 * @return long 统计数目
	 * @throws SQLException 各种统计失败的原因
	 */
	public long getFilesTotalNumByFoldersId(String pfId) throws SQLException {
		if (pfId != null) {
			countNodesByFolderId.setString(1, pfId);
			ResultSet rs = countNodesByFolderId.executeQuery();
			if (rs.first()) {
				return rs.getLong(1);
			}
		}
		return 0L;
	}

	/**
	 * 
	 * <h2>根据父文件夹的ID获取其下已有文件夹数目</h2>
	 * <p>
	 * 该方法用于统计指定文件夹下的已有文件夹数量。应在各种创建新文件夹操作前先调用该方法来判断是否超过规定值。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param pfId java.lang.String 需要统计的文件夹ID
	 * @return long 统计数目
	 * @throws SQLException 各种统计失败的原因
	 */
	public long getFoldersTotalNumByFoldersId(String pfId) throws SQLException {
		if (pfId != null) {
			countFoldersByParentFolderId.setString(1, pfId);
			ResultSet rs = countFoldersByParentFolderId.executeQuery();
			if (rs.first()) {
				return rs.getLong(1);
			}
		}
		return 0L;
	}
}
