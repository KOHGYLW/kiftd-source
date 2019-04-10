package kohgylw.kiftd.util.file_system_manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.model.Node;
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
	private PreparedStatement selectNodeByFolderId;
	private PreparedStatement selectFoldersByParentFolderId;
	private PreparedStatement insertNode;
	private PreparedStatement insertFolder;
	private PreparedStatement deleteNodeById;
	private PreparedStatement deleteFolderById;
	private PreparedStatement updateNodeById;
	private PreparedStatement updateFolderById;

	// 加载资源
	private FileSystemManager() {
		Connection c = FileNodeUtil.getNodeDBConnection();
		try {
			selectFolderById = c.prepareStatement("SELECT * FROM FOLDER WHERE folder_id = ?");
			selectNodeById = c.prepareStatement("SELECT * FROM FILE WHERE file_id = ?");
			selectNodeByFolderId = c.prepareStatement("SELECT * FROM FILE WHERE file_parent_folder = ?");
			selectFoldersByParentFolderId = c.prepareStatement("SELECT * FROM FOLDER WHERE folder_parent = ?");
			insertNode = c.prepareStatement("INSERT INTO FILE VALUES(?,?,?,?,?,?,?)");
			insertFolder = c.prepareStatement("INSERT INTO FOLDER VALUES(?,?,?,?,?,?)");
			deleteNodeById = c.prepareStatement("DELETE FROM FILE WHERE file_id = ?");
			deleteFolderById = c.prepareStatement("DELETE FROM FOLDER WHERE folder_id = ?");
			updateNodeById = c.prepareStatement(
					"UPDATE FILE SET file_name = ? , file_size = ? , file_parent_folder = ? , file_creation_date = ? , file_creator = ? , file_path = ? WHERE file_id = ?");
			updateFolderById = c.prepareStatement(
					"UPDATE FOLDER SET folder_name= ? , folder_creation_date = ? , folder_creator = ? , folder_parent = ? , folder_constraint = ? WHERE folder_id = ?");
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
	 * @param folderId
	 *            java.lang.String 文件夹ID
	 * @return kohgylw.kiftd.util.file_system_manager.pojo.FolderView 指定的文件夹视图
	 * @throws SQLException
	 *             获取失败
	 */
	public FolderView getFolderView(String folderId) throws SQLException {
		FolderView fv = new FolderView();
		fv.setCurrent(selectFolderById(folderId));
		fv.setFiles(selectNodesByFolderId(folderId));
		fv.setFolders(getFoldersByParentId(folderId));
		return fv;
	}

	/**
	 * 
	 * <h2>删除指定文件和文件夹</h2>
	 * <p>
	 * 该方法将直接删除指定文件夹和文件，该操作无法恢复。鉴于操作较为耗时，该操作将在一个新线程中进行。操作时，您可以根据“per”属性来实时获取该操作的进度。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param foldersId
	 *            java.lang.String[] 要删除的文件夹ID
	 * @param filesId
	 *            java.lang.String[] 要删除的文件ID
	 * @return boolean 删除结果
	 * @throws SQLException
	 */
	public boolean delete(String[] foldersId, String[] filesId) throws SQLException {
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
	 * @param foldersId
	 *            java.lang.String[] 要导出的文件夹ID
	 * @param filesId
	 *            java.lang.String[] 要导出的文件ID
	 * @param path
	 *            java.io.File 要导出的目标路径，必须是个文件夹
	 * @param type
	 *            java.lang.String 导出操作类型：“COVER”覆盖或“BOTH”保留两者，其他情况默认为“COVER”
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
	 * @param files
	 *            java.io.File[] 要检查的文件对象
	 * @param folderId
	 *            java.lang.String 要检查的目标文件夹ID
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
	 * @param foldersId
	 *            java.lang.String[] 要导出的文件夹ID
	 * @param filesId
	 *            java.lang.String[] 要导出的文件ID
	 * @param java.io.File
	 *            path 要检查的目标路径
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
	 * @param files
	 *            java.io.File[] 要导入的文件对象
	 * @param folderId
	 *            java.lang.String 目标文件夹ID
	 * @param type
	 *            java.lang.String 导入操作类型：“COVER”覆盖或“BOTH”保留两者，其他情况默认为“COVER”
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
	 * @param folderId
	 *            java.lang.String 需要查询的父文件夹ID
	 * @return java.util.List<kohgylw.kiftd.server.model.Folder> 文件夹对象列表，如果没有结果则长度为0
	 * @throws SQLException
	 *             查询失败
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
		selectNodeByFolderId.setString(1, folderId);
		ResultSet r = selectNodeByFolderId.executeQuery();
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
			String name = new String(f.getName().getBytes("UTF-8"), "UTF-8");
			String newName = name;
			per = 0;
			message = "正在导入文件：" + name;
			List<Node> nodes = selectNodesByFolderId(folderId);
			Node node = null;
			File target;
			long size = f.length();
			String fileBlocks = ConfigureReader.instance().getFileBlockPath();
			if (nodes.parallelStream().anyMatch((e) -> e.getFileName().equals(name))) {
				switch (type) {
				case COVER:
					// 覆盖
					node = nodes.parallelStream().filter((e) -> e.getFileName().equals(f.getName())).findFirst().get();
					node.setFileCreationDate(ServerTimeUtil.accurateToDay());
					node.setFileCreator("SYS_IN");
					int mb = (int) (size / 1024L / 1024L);
					node.setFileSize(mb + "");
					if (updateNode(node) == 0) {
						throw new SQLException();
					}
					break;
				case BOTH:
					// 保留两者（计数命名）
					newName = FileNodeUtil.getNewNodeName(name, nodes);
					break;
				default:
					per = 100;
					return;
				}
			}
			// 处理文件节点，有则用，没有则创建一个
			if (node == null) {
				node = new Node();
				node.setFileName(newName);
				node.setFileId(UUID.randomUUID().toString());
				node.setFileParentFolder(folderId);
				String id = UUID.randomUUID().toString().replace("-", "");
				String path = "file_" + id + ".block";
				node.setFilePath(path);
				target = new File(fileBlocks, path);
				target.createNewFile();
				node.setFileCreationDate(ServerTimeUtil.accurateToDay());
				node.setFileCreator("SYS_IN");
				int mb = (int) (size / 1024L / 1024L);
				node.setFileSize(mb + "");
				int i = 0;
				while (true) {
					try {
						if (insertNode(node) == 0) {
							throw new SQLException();
						}
						break;
					} catch (Exception e2) {
						node.setFileId(UUID.randomUUID().toString());
						i++;
					}
					if (i >= 10) {
						break;
					}
				}
			} else {
				target = new File(fileBlocks, node.getFilePath());
			}
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
		throw new IllegalArgumentException();
	}

	// 将一个本地文件夹导入至文件系统，必须是文件夹而不是文件。它会自动将其中的文件和文件夹也一并导入。
	private void importFolderInto(File f, String folderId, String type) throws Exception {
		if (f.isDirectory()) {
			String name = new String(f.getName().getBytes("UTF-8"), "UTF-8");
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
				folder = new Folder();
				String nFolderId = UUID.randomUUID().toString();
				folder.setFolderId(nFolderId);
				folder.setFolderName(newName);
				folder.setFolderConstraint(parent.getFolderConstraint());
				folder.setFolderParent(folderId);
				folder.setFolderCreator("SYS_IN");
				folder.setFolderCreationDate(ServerTimeUtil.accurateToDay());
				int i = 0;
				while (true) {
					try {
						if (insertFolder(folder) == 0) {
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
				if (updateFolder(folder) == 0) {
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

	// 修改节点
	private int updateNode(Node n) throws SQLException {
		updateNodeById.setString(1, n.getFileName());
		updateNodeById.setString(2, n.getFileSize());
		updateNodeById.setString(3, n.getFileParentFolder());
		updateNodeById.setString(4, n.getFileCreationDate());
		updateNodeById.setString(5, n.getFileCreator());
		updateNodeById.setString(6, n.getFilePath());
		updateNodeById.setString(7, n.getFileId());
		updateNodeById.execute();
		return updateNodeById.getUpdateCount();
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
	private void deleteFolder(String folderId) throws SQLException {
		Folder f = selectFolderById(folderId);
		List<Node> nodes = selectNodesByFolderId(folderId);
		int size = nodes.size();
		if (f == null) {
			return;
		}
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
		message = "正在删除文件夹：" + f.getFolderName();
		// 删除自己的数据
		if (deleteFolderById(folderId) > 0) {
			per = 100;
			return;
		}
		throw new SQLException();
	}

	// 删除一个文件
	private void deleteFile(String nodeId) throws SQLException {
		Node n = selectNodeById(nodeId);
		per = 50;
		message = "正在删除文件：" + n.getFileName();
		if (n != null) {
			// 删除文件节点对应的数据块
			if (new File(ConfigureReader.instance().getFileBlockPath(), n.getFilePath()).delete()) {
				per = 80;
				// 删除节点信息
				if (deleteNodeById(nodeId) > 0) {
					per = 100;
					return;
				}
			}
			throw new SQLException();
		}
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
			File block = new File(ConfigureReader.instance().getFileBlockPath(), node.getFilePath());
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
}
