package kohgylw.kiftd.server.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;

/**
 * 
 * <h2>文件节点初始化工具</h2>
 * <p>
 * 该工具负责初始化kiftd文件系统的文件节点，其会在数据库内建立文件节点相关的表（如果不存在）并提供该数据库的链接对象。
 * 该类无需生成实例，全部方法均为静态的。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FileNodeUtil {

	private FileNodeUtil() {
	};

	private static Connection conn;
	private static String url;// 当前链接的节点数据库位置

	/**
	 * 
	 * <h2>为数据库建立初始化节点表</h2>
	 * <p>
	 * 该方法将检查数据库并建立初始的文件节点表及相关索引，应在使用文件系统前先执行该操作。 仅在该操作执行后，本类提供的链接对象才会创建并可以使用。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public static void initNodeTableToDataBase() {
		Printer.instance.print("初始化文件节点...");
		try {
			if (conn == null) {
				Class.forName(ConfigureReader.instance().getFileNodePathDriver()).newInstance();
			}
			String newUrl = ConfigureReader.instance().getFileNodePathURL();
			// 判断当前位置是否初始化文件节点
			if (url == null || !url.equals(newUrl)) {
				conn = DriverManager.getConnection(newUrl, ConfigureReader.instance().getFileNodePathUserName(),
						ConfigureReader.instance().getFileNodePathPassWord());
				url = newUrl;
				final Statement state1 = conn.createStatement();
				state1.execute(
						"CREATE TABLE IF NOT EXISTS FOLDER(folder_id VARCHAR(128) PRIMARY KEY,  folder_name VARCHAR(128) NOT NULL,folder_creation_date VARCHAR(128) NOT NULL,  folder_creator VARCHAR(128) NOT NULL,folder_parent VARCHAR(128) NOT NULL,folder_constraint INT NOT NULL)");
				state1.executeQuery("SELECT count(*) FROM FOLDER WHERE folder_id = 'root'");
				ResultSet rs = state1.getResultSet();
				if (rs.next()) {
					if (rs.getInt(1) == 0) {
						final Statement state11 = conn.createStatement();
						state11.execute("INSERT INTO FOLDER VALUES('root', 'ROOT', '--', '--', 'null', 0)");
					}
				}
				state1.close();
				final Statement state2 = conn.createStatement();
				state2.execute(
						"CREATE TABLE IF NOT EXISTS FILE(file_id VARCHAR(128) PRIMARY KEY,file_name VARCHAR(128) NOT NULL,file_size VARCHAR(128) NOT NULL,file_parent_folder varchar(128) NOT NULL,file_creation_date varchar(128) NOT NULL,file_creator varchar(128) NOT NULL,file_path varchar(128) NOT NULL)");
				state2.close();
				// 为了匹配之前的版本而设计的兼容性字段设置，后续可能会删除
				if (!ConfigureReader.instance().useMySQL()) {
					final Statement state3 = conn.createStatement();
					state3.execute(
							"ALTER TABLE FOLDER ADD COLUMN IF NOT EXISTS folder_constraint INT NOT NULL DEFAULT 0");
					state3.close();
				}
				// 为数据库生成索引，此处分为MySQL和H2两种操作
				if (ConfigureReader.instance().useMySQL()) {
					final Statement state4 = conn.createStatement();
					ResultSet indexCount = state4.executeQuery("SHOW INDEX FROM FILE WHERE Key_name = 'file_index'");
					if (!indexCount.next()) {
						final Statement state41 = conn.createStatement();
						state41.execute("CREATE INDEX file_index ON FILE (file_id,file_name)");
						state41.close();
					}
					state4.close();
				} else {
					final Statement state4 = conn.createStatement();
					state4.execute("CREATE INDEX IF NOT EXISTS file_index ON FILE (file_id,file_name)");
					state4.close();
				}
			}
			Printer.instance.print("文件节点初始化完毕。");
		} catch (Exception e) {
			Printer.instance.print(e.getMessage());
			Printer.instance.print("错误：文件节点初始化失败。");
		}
	}

	/**
	 * 
	 * <h2>获取文件节点的数据库链接</h2>
	 * <p>
	 * 在执行initNodeTableToDataBase方法后，可通过本方法获取文件节点的数据库链接以便继续操作，否则返回null。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.sql.Connection 文件节点数据库的链接，除非程序关闭，否则该链接不应关闭。
	 */
	public static Connection getNodeDBConnection() {
		return conn;
	}

	/**
	 * 
	 * <h2>生成不与已存在文件同名的、带计数的新文件名</h2>
	 * <p>
	 * 针对需要保留两个文件至一个路径下的行为，可使用该方法生成新文件名，其格式为“{原文件名} (计数).{后缀}”的格式。
	 * 例如，某路径下已存在“test1.txt”，再传入一个“test1.txt”时，会返回“test1 (1).txt”，继续传入“test1
	 * (1).txt” 则返回“test1 (2).txt”，以此类推。当文件列表中不含同名文件时，返回原始文件名。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param originalName
	 *            java.lang.String 原始文件名
	 * @param nodes
	 *            java.util.List Node 要检查的文件节点列表
	 * @return java.lang.String 新文件名
	 */
	public static String getNewNodeName(String originalName, List<Node> nodes) {
		int i = 0;
		List<String> fileNames = Arrays
				.asList(nodes.stream().parallel().map((t) -> t.getFileName()).toArray(String[]::new));
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
		return newName;
	}

	/**
	 * 
	 * <h2>生成不与已存在文件夹同名的、带计数的新文件夹名</h2>
	 * <p>
	 * 功能与得到新文件名类似，当文件夹列表中存在“doc”文件夹时，传入“doc”则返回“doc 2”，以此类推。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param originalName
	 *            java.lang.String 原始文件夹名
	 * @param nodes
	 *            java.util.List Folder 要检查的文件夹列表
	 * @return java.lang.String 新文件夹名
	 */
	public static String getNewFolderName(String originalName, List<? extends Folder> folders) {
		int i = 0;
		List<String> fileNames = Arrays
				.asList(folders.stream().parallel().map((t) -> t.getFolderName()).toArray(String[]::new));
		String newName = originalName;
		while (fileNames.contains(newName)) {
			i++;
			newName = originalName + " " + i;
		}
		return newName;
	}

	/**
	 * 
	 * <h2>生成不与已存在文件夹同名的、带计数的新文件夹名</h2>
	 * <p>
	 * 功能与得到新文件名类似，当文件夹列表中存在“doc”文件夹时，传入“doc”则返回“doc 2”，以此类推。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param folder
	 *            kohgylw.kiftd.server.model.Folder 原始文件夹
	 * @param parentfolder
	 *            java.io.File 要检查的文件夹
	 * @return java.lang.String 新文件夹名
	 */
	public static String getNewFolderName(Folder folder, File parentfolder) {
		int i = 0;
		List<String> fileNames = Arrays.asList(Arrays.stream(parentfolder.listFiles()).parallel()
				.filter((e) -> e.isDirectory()).map((t) -> t.getName()).toArray(String[]::new));
		String newName = folder.getFolderName();
		while (fileNames.contains(newName)) {
			i++;
			newName = folder.getFolderName() + " " + i;
		}
		return newName;
	}

	/**
	 * 
	 * <h2>生成不与已存在文件同名的、带计数的新文件名</h2>
	 * <p>
	 * 针对需要保留两个文件至一个路径下的行为，可使用该方法生成新文件名，其格式为“{原文件名} (计数).{后缀}”的格式。
	 * 例如，某路径下已存在“test1.txt”，再传入一个“test1.txt”时，会返回“test1 (1).txt”，继续传入“test1
	 * (1).txt” 则返回“test1 (2).txt”，以此类推。当文件列表中不含同名文件时，返回原始文件名。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param n
	 *            kohgylw.kiftd.server.model.Node 要重命名的文件节点
	 * @param folder
	 *            java.io.File 要检查的本地文件夹
	 * @return java.lang.String 新文件名
	 */
	public static String getNewNodeName(Node n, File folder) {
		int i = 0;
		List<String> fileNames = Arrays.asList(Arrays.stream(folder.listFiles()).parallel().filter((e) -> e.isFile())
				.map((t) -> t.getName()).toArray(String[]::new));
		String newName = n.getFileName();
		while (fileNames.contains(newName)) {
			i++;
			if (n.getFileName().indexOf(".") >= 0) {
				newName = n.getFileName().substring(0, n.getFileName().lastIndexOf(".")) + " (" + i + ")"
						+ n.getFileName().substring(n.getFileName().lastIndexOf("."));
			} else {
				newName = n.getFileName() + " (" + i + ")";
			}
		}
		return newName;
	}

}
