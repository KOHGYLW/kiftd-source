package kohgylw.kiftd.server.util;

import org.springframework.stereotype.*;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import org.springframework.web.multipart.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.pojo.ExtendStores;

import java.util.*;
import java.util.zip.ZipEntry;

import org.zeroturnaround.zip.*;

/**
 * 
 * <h2>文件块整合操作工具</h2>
 * <p>
 * 该工具内包含了对文件系统中文件块的所有操作，使用IOC容器进行管理。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.1
 */
@Component
public class FileBlockUtil {
	private static final String EXTEND_PATH_NAME = "extend";
	@Resource
	private NodeMapper fm;// 节点映射，用于遍历
	@Resource
	private FolderMapper flm;// 文件夹映射，同样用于遍历
	@Resource
	private LogUtil lu;// 日志工具
	@Resource
	private FolderUtil fu;// 文件夹操作工具

	/**
	 * 每一层级文件夹内允许存放的最大文件块阈值。如果当前路径内存放的文件块数目超过该值，
	 * 则应创建一个新的路径层级继续存储更多文件块，从而避免因在一个文件夹内存储过多的文件
	 * 而导致的性能下降问题。
	 */
	private static final int MAX_BLOCKS_TOTAL_POR_PATH = 8191;

	/**
	 * 
	 * <h2>将新上传的文件存入文件系统</h2>
	 * <p>
	 * 将一个MultipartFile类型的文件对象存入节点，并返回保存的路径名称。其中，路径名称使用“file_{UUID}.block”
	 * （存放于主文件系统中）或“{存储区编号}_{UUID}.block”（存放在指定编号的扩展存储区中）的形式。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f
	 *            MultipartFile 上传文件对象
	 * @return String 随机生成的保存路径，如果保存失败则返回“ERROR”
	 */
	public String saveToFileBlocks(final MultipartFile f) {
		// 如果存在扩展存储区，则优先在最大的扩展存储区中存放文件（避免占用主文件系统）
		List<ExtendStores> ess = ConfigureReader.instance().getExtendStores();// 得到全部扩展存储区
		if (ess.size() > 0) {// 如果存在
			// 将所有扩展存储区按照已存储文件的数目从小到大进行排序
			Collections.sort(ess, new Comparator<ExtendStores>() {
				@Override
				public int compare(ExtendStores o1, ExtendStores o2) {
					return o1.getPath().list().length - o2.getPath().list().length;
				}
			});
			// 遍历这些扩展存储区，并尝试将新文件存入一个已有文件数目最少、同时容量又足够的扩展存储区中
			for (ExtendStores es : ess) {
				// 如果该存储区的空余容量大于要存放的文件
				if (es.getPath().getFreeSpace() > f.getSize()) {
					final String newName = createNewBlockName(es.getIndex() + "_", es.getPath());
					final File extendPath = getExtendStoragePath(es.getPath());
					final File file = new File(extendPath, newName);
					try {
						f.transferTo(file);// 则执行存放，并将文件命名为“{存储区编号}_{UUID}.block”的形式
						return newName;
					} catch (IOException e) {
						// 如果无法存入（由于体积过大或其他问题），那么继续尝试其他扩展存储区
						continue;
					} catch (Exception e) {
						lu.writeException(e);
						Printer.instance.print(e.getMessage());
						continue;
					}
				}
			}
		}
		// 如果不存在扩展存储区或者最大的扩展存储区无法存放目标文件，则尝试将其存放至主文件系统路径下
		try {
			final File fileBlocks = new File(ConfigureReader.instance().getFileBlockPath());
			File extendPath = getExtendStoragePath(fileBlocks);
			final String newName = createNewBlockName("file_", fileBlocks);
			if (newName == null) {
				return "ERROR";
			}
			final File file = new File(extendPath, newName);
			f.transferTo(file);// 执行存放，并肩文件命名为“file_{UUID}.block”的形式
			return newName;
		} catch (Exception e) {
			lu.writeException(e);
			Printer.instance.print(e.getMessage());
			return "ERROR";
		}
	}

	// 获取（生成）目标存储路径，这一方法主要是为了避免在单一文件夹内存储过多的文件块设计的。
	// 从存储路径本级开始，如果该级内存储的文件块数量已达上限（MAX_BLOCKS_TOTAL_POR_PATH），
	// 则在其中创建一个名为“extend”的文件夹，然后再在这一文件夹内存储新的文件节点，当这一级也存满后，
	// 则继续在其中创建“extend”文件夹，以此类推。
	private File getExtendStoragePath(File rootPath) {
		File targetPath = rootPath;
		while (targetPath.list().length >= MAX_BLOCKS_TOTAL_POR_PATH) {
			targetPath = new File(targetPath, EXTEND_PATH_NAME);
			if (!targetPath.isDirectory()) {
				targetPath.mkdir();
			}
		}
		return targetPath;
	}

	// 生成一个在指定路径下绝对不重复的文件块名称，便于作为新文件块的名称
	private String createNewBlockName(String prefix, File rootPath) {
		int appendIndex = 0;
		int tryThreshold = 0;
		String newName = prefix + UUID.randomUUID().toString().replace("-", "");
		File testPath = rootPath;
		while (testPath.isDirectory()) {
			if (new File(testPath, newName + ".block").isFile()) {
				if (appendIndex >= 0 && appendIndex < Integer.MAX_VALUE) {
					newName = newName + "_" + appendIndex;
					testPath = rootPath;
					appendIndex++;
				} else {
					if (tryThreshold < 10) {
						newName = prefix + UUID.randomUUID().toString().replace("-", "");
					} else {
						return null;
					}
				}
			} else {
				testPath = new File(testPath, EXTEND_PATH_NAME);
			}
		}
		return newName + ".block";
	}

	/**
	 * 
	 * <h2>计算上传文件的体积</h2>
	 * <p>
	 * 该方法用于将上传文件的体积换算以MB表示，以便存入文件系统。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f
	 *            org.springframework.web.multipart.MultipartFile 上传文件对象
	 * @return java.lang.String 计算出来的体积，以MB为单位
	 */
	public String getFileSize(final MultipartFile f) {
		final long size = f.getSize();
		final int mb = (int) (size / 1048576L);
		return "" + mb;
	}

	/**
	 * 
	 * <h2>删除文件系统中的一个文件块</h2>
	 * <p>
	 * 根据传入的文件节点对象，删除其在文件系统中保存的对应文件块。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f
	 *            kohgylw.kiftd.server.model.Node 要删除的文件节点对象
	 * @return boolean 删除结果，true为成功
	 */
	public boolean deleteFromFileBlocks(Node f) {
		// 先判断一下文件块所在的存储区
		File rootPath = new File(ConfigureReader.instance().getFileBlockPath());
		if (!f.getFilePath().startsWith("file_")) {// 存放于主文件系统中
			short index = Short.parseShort(f.getFilePath().substring(0, f.getFilePath().indexOf('_')));
			rootPath = ConfigureReader.instance().getExtendStores().parallelStream()
					.filter((e) -> e.getIndex() == index).findAny().get().getPath();
		}
		// 获取对应的文件块对象
		File file = getFileFromBlocks(f);
		if (file != null) {
			if (file.delete()) {
				File parentPath = file.getParentFile();
				while (parentPath != null && parentPath.isDirectory() && (!parentPath.equals(rootPath))
						&& parentPath.list().length == 0) {
					File thisPath = parentPath;
					parentPath = thisPath.getParentFile();
					thisPath.delete();
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * <h2>得到文件系统中的一个文件块</h2>
	 * <p>
	 * 根据传入的文件节点对象，得到其在文件系统中保存的对应文件块。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f
	 *            kohgylw.kiftd.server.model.Node 要获得的文件节点对象
	 * @return java.io.File 对应的文件块抽象路径，获取失败则返回null
	 */
	public File getFileFromBlocks(Node f) {
		// 检查该节点对应的文件块存放于哪个位置（主文件系统/扩展存储区）
		try {
			File file = null;
			if (f.getFilePath().startsWith("file_")) {// 存放于主文件系统中
				// 直接从主文件系统的文件块存放区获得对应的文件块
				file = getBlockFromExtendPath(new File(ConfigureReader.instance().getFileBlockPath()), f.getFilePath());
			} else {// 存放于扩展存储区
				short index = Short.parseShort(f.getFilePath().substring(0, f.getFilePath().indexOf('_')));
				// 根据编号查到对应的扩展存储区路径，进而获取对应的文件块
				file = getBlockFromExtendPath(ConfigureReader.instance().getExtendStores().parallelStream()
						.filter((e) -> e.getIndex() == index).findAny().get().getPath(), f.getFilePath());
			}
			if (file.isFile()) {
				return file;
			}
		} catch (Exception e) {
			lu.writeException(e);
			Printer.instance.print("错误：文件数据读取失败。详细信息：" + e.getMessage());
		}
		return null;
	}

	// 从扩展文件夹（如果有）里获取指定的文件节点名称
	private File getBlockFromExtendPath(File rootPath, String blockName) {
		File targetPath = rootPath;
		while (targetPath.isDirectory()) {
			File block = new File(targetPath, blockName);
			if (block.isFile()) {
				return block;
			} else {
				targetPath = new File(targetPath, EXTEND_PATH_NAME);
			}
		}
		return null;
	}

	/**
	 * 
	 * <h2>校对文件块与文件节点</h2>
	 * <p>
	 * 将文件系统中不可用的文件块移除，以便保持文件系统的整洁。该操作应在服务器启动或出现问题时执行。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void checkFileBlocks() {
		Thread checkThread = new Thread(() -> {
			// 检查是否存在未正确对应文件块的文件节点信息，若有则删除，从而确保文件节点信息不出现遗留问题
			checkNodes("root");
			// 检查是否存在未正确对应文件节点的文件块，若有则删除，从而确保文件块不出现遗留问题
			List<File> paths = new ArrayList<>();
			paths.add(new File(ConfigureReader.instance().getFileBlockPath()));
			for (ExtendStores es : ConfigureReader.instance().getExtendStores()) {
				paths.add(es.getPath());
			}
			for (File path : paths) {
				File extendPath = path;
				while (extendPath.isDirectory()) {
					try {
						Iterator<Path> blocks = Files.newDirectoryStream(path.toPath()).iterator();
						while (blocks.hasNext()) {
							File testBlock = blocks.next().toFile();
							Node node = fm.queryByPath(testBlock.getName());
							if (node == null) {
								if (testBlock.isFile()) {
									testBlock.delete();
								}
							}
						}
					} catch (IOException e) {
						Printer.instance.print("警告：文件节点效验时发生意外错误，可能未能正确完成文件节点效验。错误信息：" + e.getMessage());
						lu.writeException(e);
					}
					extendPath = new File(extendPath, EXTEND_PATH_NAME);
				}
			}
		});
		checkThread.start();
	}

	// 逐文件夹校对文件节点——要求某一节点必须有对应的文件块，否则将其移除（避免出现死节点）
	private void checkNodes(String fid) {
		List<Node> nodes = fm.queryByParentFolderId(fid);
		for (Node node : nodes) {
			File block = getFileFromBlocks(node);
			if (block == null || !block.exists()) {
				fm.deleteById(node.getFileId());
			}
		}
		List<Folder> folders = flm.queryByParentId(fid);
		for (Folder fl : folders) {
			checkNodes(fl.getFolderId());
		}
	}

	/**
	 * 
	 * <h2>将指定节点及文件夹打包为ZIP压缩文件。</h2>
	 * <p>
	 * 该功能用于创建ZIP压缩文件，线程阻塞。如果压缩目标中存在同名情况，则使用“{文件名} (n).{后缀}”或“{文件夹名} n”的形式重命名。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param idList
	 *            java.util.List<String> 要压缩的文件节点目标ID列表
	 * @param fidList
	 *            java.util.List<String> 要压缩的文件夹目标ID列表，迭代压缩
	 * @param account
	 *            java.lang.String 用户ID，用于判断压缩文件夹是否有效
	 * @return java.lang.String
	 *         压缩后产生的文件名称，命名规则为“tf_{UUID}.zip”，存放于文件系统中的temporaryfiles目录下
	 */
	public String createZip(final List<String> idList, final List<String> fidList, String account) {
		final String zipname = "tf_" + UUID.randomUUID().toString() + ".zip";
		final String tempPath = ConfigureReader.instance().getTemporaryfilePath();
		final File f = new File(tempPath, zipname);
		try {
			final List<ZipEntrySource> zs = new ArrayList<>();
			// 避免压缩时出现同名文件导致打不开：
			final List<Folder> folders = new ArrayList<>();
			for (String fid : fidList) {
				Folder fo = flm.queryById(fid);
				if (ConfigureReader.instance().accessFolder(fo, account) && ConfigureReader.instance()
						.authorized(account, AccountAuth.DOWNLOAD_FILES, fu.getAllFoldersId(fo.getFolderParent()))) {
					if (fo != null) {
						folders.add(fo);
					}
				}
			}
			final List<Node> nodes = new ArrayList<>();
			for (String id : idList) {
				Node n = fm.queryById(id);
				if (ConfigureReader.instance().accessFolder(flm.queryById(n.getFileParentFolder()), account)
						&& ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
								fu.getAllFoldersId(n.getFileParentFolder()))) {
					if (n != null) {
						nodes.add(n);
					}
				}
			}
			for (Folder fo : folders) {
				int i = 1;
				String flname = fo.getFolderName();
				while (true) {
					if (folders.parallelStream().filter((e) -> e.getFolderName().equals(fo.getFolderName()))
							.count() > 1) {
						fo.setFolderName(flname + " " + i);
						i++;
					} else {
						break;
					}
				}
				addFoldersToZipEntrySourceArray(fo, zs, account, "");
			}
			for (Node node : nodes) {
				if (ConfigureReader.instance().accessFolder(flm.queryById(node.getFileParentFolder()), account)) {
					int i = 1;
					String fname = node.getFileName();
					while (true) {
						if (nodes.parallelStream().filter((e) -> e.getFileName().equals(node.getFileName())).count() > 1
								|| folders.parallelStream().filter((e) -> e.getFolderName().equals(node.getFileName()))
										.count() > 0) {
							if (fname.indexOf(".") >= 0) {
								node.setFileName(fname.substring(0, fname.lastIndexOf(".")) + " (" + i + ")"
										+ fname.substring(fname.lastIndexOf(".")));
							} else {
								node.setFileName(fname + " (" + i + ")");
							}
							i++;
						} else {
							break;
						}
					}
					zs.add((ZipEntrySource) new FileSource(node.getFileName(), getFileFromBlocks(node)));
				}
			}
			ZipUtil.pack(zs.toArray(new ZipEntrySource[0]), f);
			return zipname;
		} catch (Exception e) {
			lu.writeException(e);
			Printer.instance.print(e.getMessage());
			return null;
		}
	}

	// 迭代生成ZIP文件夹单元，将一个文件夹内的文件和文件夹也进行打包
	private void addFoldersToZipEntrySourceArray(Folder f, List<ZipEntrySource> zs, String account, String parentPath) {
		if (f != null && ConfigureReader.instance().accessFolder(f, account)) {
			String folderName = f.getFolderName();
			String thisPath = parentPath + folderName + "/";
			zs.add(new ZipEntrySource() {

				@Override
				public String getPath() {
					return thisPath;
				}

				@Override
				public InputStream getInputStream() throws IOException {
					return null;
				}

				@Override
				public ZipEntry getEntry() {
					return new ZipEntry(thisPath);
				}
			});
			List<Folder> folders = flm.queryByParentId(f.getFolderId());
			for (Folder fo : folders) {
				int i = 1;
				String flname = fo.getFolderName();
				while (true) {
					if (folders.parallelStream().filter((e) -> e.getFolderName().equals(fo.getFolderName()))
							.count() > 1) {
						fo.setFolderName(flname + " " + i);
						i++;
					} else {
						break;
					}
				}
				addFoldersToZipEntrySourceArray(fo, zs, account, thisPath);
			}
			List<Node> nodes = fm.queryByParentFolderId(f.getFolderId());
			for (Node node : nodes) {
				int i = 1;
				String fname = node.getFileName();
				while (true) {
					if (nodes.parallelStream().filter((e) -> e.getFileName().equals(node.getFileName())).count() > 1
							|| folders.parallelStream().filter((e) -> e.getFolderName().equals(node.getFileName()))
									.count() > 0) {
						if (fname.indexOf(".") >= 0) {
							node.setFileName(fname.substring(0, fname.lastIndexOf(".")) + " (" + i + ")"
									+ fname.substring(fname.lastIndexOf(".")));
						} else {
							node.setFileName(fname + " (" + i + ")");
						}
						i++;
					} else {
						break;
					}
				}
				zs.add(new FileSource(thisPath + node.getFileName(), getFileFromBlocks(node)));
			}
		}
	}
}
