package kohgylw.kiftd.server.util;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.*;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import org.springframework.web.multipart.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
	@Resource
	private NodeMapper fm;// 节点映射，用于遍历
	@Resource
	private FolderMapper flm;// 文件夹映射，同样用于遍历
	@Resource
	@Lazy
	private LogUtil lu;// 日志工具
	@Resource
	@Lazy
	private FolderUtil fu;// 文件夹操作工具

	/**
	 * 
	 * <h2>清理临时文件夹</h2>
	 * <p>
	 * 该方法用于清理临时文件夹（如果临时文件夹不存在，则创建它），避免运行时产生的临时文件堆积。该方法应在服务器启动时和关闭过程中调用。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void initTempDir() {
		final String tfPath = ConfigureReader.instance().getTemporaryfilePath();
		final File f = new File(tfPath);
		if (f.isDirectory()) {
			try {
				Iterator<Path> listFiles = Files.newDirectoryStream(f.toPath()).iterator();
				while (listFiles.hasNext()) {
					File tempFile = listFiles.next().toFile();
					if(!tempFile.getName().startsWith(".")) {
						tempFile.delete();
					}
				}
			} catch (IOException e) {
				lu.writeException(e);
				Printer.instance.print("错误：临时文件清理失败，请手动清理" + f.getAbsolutePath() + "文件夹内的临时文件。");
			}
		} else {
			if (!f.mkdir()) {
				Printer.instance.print("错误：无法创建临时文件夹" + f.getAbsolutePath() + "，请检查主文件系统存储路径是否可用。");
			}
		}
	}

	/**
	 * 
	 * <h2>将新上传的文件存入文件系统</h2>
	 * <p>
	 * 将一个MultipartFile类型的文件对象存入节点，并返回保存的路径名称。其中，路径名称使用“file_{UUID}.block”
	 * （存放于主文件系统中）或“{存储区编号}_{UUID}.block”（存放在指定编号的扩展存储区中）的形式。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f MultipartFile 上传文件对象
	 * @return java.io.File 生成的文件块，如果保存失败则返回null
	 */
	public File saveToFileBlocks(final MultipartFile f) {
		// 得到全部扩展存储区
		List<ExtendStores> ess = getExtendStoresBySort();
		if (ess.size() > 0) {
			// 从文件块最少的开始遍历这些扩展存储区
			for (ExtendStores es : ess) {
				if (es.getPath().getFreeSpace() > f.getSize()) {
					// 如果该存储区的空余容量大于待上传文件的体积
					File file = null;
					try {
						// 则尝试在该存储区中生成一个空文件块
						file = createNewBlock(es.getIndex() + "_", es.getPath());
						if (file != null) {
							// 生成成功，尝试存入数据
							f.transferTo(file);
							return file;
						} else {
							continue;// 如果本处无法生成新文件块，那么在其他路径下继续尝试
						}
					} catch (IOException e) {
						// 出现IO异常，则删除残留文件并继续尝试其他扩展存储区
						if (file != null) {
							file.delete();
						}
						continue;
					} catch (Exception e) {
						// 出现其他异常则记录日志
						lu.writeException(e);
						Printer.instance.print(e.getMessage());
						continue;
					}
				}
			}
		}
		// 如果不存在扩展存储区或者最大的扩展存储区的剩余容量依旧小于指定大小，则尝试在主文件系统路径下生成新文件块
		File file = null;
		try {
			file = createNewBlock("file_", new File(ConfigureReader.instance().getFileBlockPath()));
			if (file != null) {
				// 生成成功，则尝试存入数据
				f.transferTo(file);
				return file;
			}
		} catch (Exception e) {
			// 出现异常则记录日志，则删除残留数据并返回null
			if (file != null) {
				file.delete();
			}
			lu.writeException(e);
			Printer.instance.print("错误：文件块生成失败，无法存入新的文件数据。详细信息：" + e.getMessage());
		}
		// 因其他原因生成失败也返回null
		return null;
	}

	/**
	 * 
	 * <h2>将新上传的文件存入文件系统</h2>
	 * <p>
	 * 将一个java.io.File类型的文件对象存入节点，并返回保存的路径名称。其中，路径名称使用“file_{UUID}.block”
	 * （存放于主文件系统中）或“{存储区编号}_{UUID}.block”（存放在指定编号的扩展存储区中）的形式。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f 要存入的文件对象
	 * @return java.io.File 生成的文件块，如果保存失败则返回null
	 */
	public File saveToFileBlocks(final File f) {
		// 得到全部扩展存储区
		List<ExtendStores> ess = getExtendStoresBySort();
		if (ess.size() > 0) {
			// 从文件块最少的开始遍历这些扩展存储区
			for (ExtendStores es : ess) {
				if (es.getPath().getFreeSpace() > f.length()) {
					// 如果该存储区的空余容量大于待上传文件的体积
					File file = null;
					try {
						// 则尝试在该存储区中生成一个空文件块
						file = createNewBlock(es.getIndex() + "_", es.getPath());
						if (file != null) {
							// 生成成功，尝试存入数据
							Files.move(f.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
							return file;
						} else {
							continue;// 如果本处无法生成新文件块，那么在其他路径下继续尝试
						}
					} catch (IOException e) {
						// 出现IO异常，则删除残留文件并继续尝试其他扩展存储区
						if (file != null) {
							file.delete();
						}
						continue;
					} catch (Exception e) {
						// 出现其他异常则记录日志
						lu.writeException(e);
						Printer.instance.print(e.getMessage());
						continue;
					}
				}
			}
		}
		// 如果不存在扩展存储区或者最大的扩展存储区的剩余容量依旧小于指定大小，则尝试在主文件系统路径下生成新文件块
		File file = null;
		try {
			file = createNewBlock("file_", new File(ConfigureReader.instance().getFileBlockPath()));
			if (file != null) {
				// 生成成功，则尝试存入数据
				Files.move(f.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				return file;
			}
		} catch (Exception e) {
			// 出现异常则记录日志，则删除残留数据并返回null
			if (file != null) {
				file.delete();
			}
			lu.writeException(e);
			Printer.instance.print("错误：文件块生成失败，无法存入新的文件数据。详细信息：" + e.getMessage());
		}
		// 因其他原因生成失败也返回null
		return null;
	}

	/**
	 * 
	 * <h2>以剩余容量从小到大排序获取扩展存储区列表</h2>
	 * <p>
	 * 该方法用于获取所有扩展存储区的列表，并按照剩余容量从小到大排序。如果没有扩展存储区，则返回一个长度为0的列表。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.util.List&lt;ExtendStores&rt; 排序好的扩展存储区列表。
	 */
	private List<ExtendStores> getExtendStoresBySort() {
		List<ExtendStores> ess = ConfigureReader.instance().getExtendStores();
		if (ess.size() > 0) {
			// 将所有扩展存储区按照已存储文件块的数目从小到大进行排序
			Collections.sort(ess, new Comparator<ExtendStores>() {
				@Override
				public int compare(ExtendStores o1, ExtendStores o2) {
					try {
						// 通常情况下，直接比较子文件列表长度即可
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
		}
		return ess;
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

	/**
	 * 
	 * <h2>计算上传文件的体积</h2>
	 * <p>
	 * 该方法用于将上传文件的体积换算以MB表示，以便存入文件系统。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param size 文件的体积，以Byte为单位
	 * @return java.lang.String 计算出来的体积，以MB为单位
	 */
	public String getFileSize(final long size) {
		final int mb = (int) (size / 1048576L);
		return "" + mb;
	}

	/**
	 * 
	 * <h2>删除文件系统中的一个文件节点，同时清理文件块</h2>
	 * <p>
	 * 删除传入的文件节点，之后判断是否需要删除其在文件系统中保存的对应文件块，若该文件节点所对应的文件块不再有其他节点引用，
	 * 则进行删除操作，否则直接返回true。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f kohgylw.kiftd.server.model.Node 要删除的文件节点对象
	 * @return boolean 删除结果，true为成功，否则返回false。若传入节点为null，也会返回false
	 */
	public boolean deleteNode(Node f) {
		if (f != null) {
			if (fm.deleteById(f.getFileId()) > 0) {
				// 尝试清理该节点对应的文件块
				if (!clearFileBlock(f)) {
					// 如果清理节点失败，尝试回滚节点
					if (fm.insert(f) > 0) {
						// 回滚失败，则认为删除失败
						return false;
					}
				}
				return true;// 文件清理成功，或者回滚失败，都认为删除成功
			}
		}
		return false;// 若节点删除失败，或是节点为null，则返回false
	}

	/**
	 * 
	 * <h2>清理文件块</h2>
	 * <p>
	 * 该方法将清理指定节点的文件块，如果开启了“删除留档”功能，则会在清理的同时尝试留档。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param n 要清理文件块的节点
	 * @return 清理或留档结果。清理或留档成功则返回true，否则返回false。
	 */
	private boolean clearFileBlock(Node n) {
		// 获取“删除留档”功能的设置路径。
		String recycleBinPath = ConfigureReader.instance().getRecycleBinPath();
		// 获取节点对应的文件块
		File file = getFileFromBlocks(n);
		// 检查该节点引用的文件块是否被其他节点引用
		Map<String, String> map = new HashMap<>();
		map.put("path", n.getFilePath());
		map.put("fileId", n.getFileId());
		List<Node> nodes = fm.queryByPathExcludeById(map);
		if (nodes == null || nodes.isEmpty()) {
			// 若已经无任何节点再引用此文件块
			if (file != null) {
				if (recycleBinPath != null && !saveToRecycleBin(file, recycleBinPath, n.getFileName(), false)) {
					// 若开启了“删除留档”功能且留档失败，则认为清理失败
					return false;
				} else {
					// 否则直接删除此文件块
					if (!file.delete()) {
						// 如果文件块删除失败
						if (file.exists()) {
							return false;// 如果如果文件块仍存在，返回false
						}
					}
				}
			} else {
				// 如果文件块获取失败，则检查是否开启了“删除留档”功能
				if (recycleBinPath != null) {
					// 如果开启了，那么由于无法留档，因此认为清理失败
					return false;
				}
				// 否则认为清理成功
			}
		} else {
			// 若仍有其他节点引用此文件块
			if (recycleBinPath != null && !saveToRecycleBin(file, recycleBinPath, n.getFileName(), true)) {
				// 若开启了“删除留档”功能且留档失败，则认为清理失败
				return false;
			}
			// 否则认为清理成功
		}
		return true;
	}

	private boolean saveToRecycleBin(File block, String recycleBinPath, String originalName, boolean isCopy) {
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
				try {
					if (isCopy) {
						Files.copy(block.toPath(), saveFile.toPath());
					} else {
						Files.move(block.toPath(), saveFile.toPath());
					}
					// 如果不抛出任何异常，则操作成功
					return true;
				} catch (Exception e) {
					lu.writeException(e);
				}
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
	 * @param f kohgylw.kiftd.server.model.Node 要获得的文件节点对象
	 * @return java.io.File 对应的文件块抽象路径，获取失败则返回null
	 */
	public File getFileFromBlocks(Node f) {
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
			lu.writeException(e);
			Printer.instance.print("错误：文件数据读取失败。详细信息：" + e.getMessage());
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
				try (DirectoryStream<Path> ds = Files.newDirectoryStream(path.toPath())) {
					Iterator<Path> blocks = ds.iterator();
					while (blocks.hasNext()) {
						File testBlock = blocks.next().toFile();
						if (testBlock.isFile() && !testBlock.getName().startsWith(".")) {
							List<Node> nodes = fm.queryByPath(testBlock.getName());
							if (nodes == null || nodes.isEmpty()) {
								testBlock.delete();
							}
						}
					}
				} catch (IOException e) {
					Printer.instance.print("警告：文件节点效验时发生意外错误，可能未能正确完成文件节点效验。错误信息：" + e.getMessage());
					lu.writeException(e);
				}
			}
		});
		checkThread.start();
	}

	// 校对文件节点，要求某一节点必须有对应的文件块，否则将其移除（避免出现死节点）
	private void checkNodes(String fid) {
		List<Node> nodes = fm.queryByParentFolderId(fid);
		for (Node node : nodes) {
			File block = getFileFromBlocks(node);
			if (block == null) {
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
	 * @param idList  java.util.List<String> 要压缩的文件节点目标ID列表
	 * @param fidList java.util.List<String> 要压缩的文件夹目标ID列表，迭代压缩
	 * @param account java.lang.String 用户ID，用于判断压缩文件夹是否有效
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

	/**
	 * 
	 * <h2>生成指定文件块资源对应的ETag标识</h2>
	 * <p>
	 * 该方法用于生产指定文件块的ETag标识，从而方便前端控制缓存。生成规则为：{文件最后修改时间计数}_{文件路径对应的Hash码}。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param block java.io.File 需要生成的文件块对象，应为文件，但也支持文件夹，或者是null
	 * @return java.lang.String 生成的ETag值。当传入的block是null或其不存在时，返回空字符串
	 */
	public String getETag(File block) {
		if (block != null && block.exists()) {
			StringBuffer sb = new StringBuffer();
			sb.append("W\"");
			sb.append(block.length());
			sb.append("-");
			sb.append(block.lastModified());
			sb.append("\"");
			return sb.toString();
		}
		return "W\"0-0\"";
	}

	/**
	 * 
	 * <h2>插入一个新的文件节点至文件系统数据库中</h2>
	 * <p>
	 * 该方法将尝试生成一个新的文件节点并存入文件系统数据库，并确保该文件节点再插入后不会与已有节点产生冲突。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param fileName         java.lang.String 文件名称
	 * @param account          java.lang.String 创建者账户，若传入null则按匿名创建者处理
	 * @param filePath         java.lang.String 文件节点对应的文件块索引
	 * @param fileSize         java.lang.String 文件体积
	 * @param fileParentFolder java.lang.String 文件的父文件夹ID
	 * @return kohgylw.kiftd.server.model.Node 操作成功则返回节点对象，否则返回null
	 */
	public Node insertNewNode(String fileName, String account, String filePath, String fileSize,
			String fileParentFolder) {
		final Node f2 = new Node();
		f2.setFileId(UUID.randomUUID().toString());
		if (account != null) {
			f2.setFileCreator(account);
		} else {
			f2.setFileCreator("\u533f\u540d\u7528\u6237");
		}
		f2.setFileCreationDate(ServerTimeUtil.accurateToDay());
		f2.setFileName(fileName);
		f2.setFileParentFolder(fileParentFolder);
		f2.setFilePath(filePath);
		f2.setFileSize(fileSize);
		int i = 0;
		// 尽可能避免UUID重复的情况发生，重试10次
		while (true) {
			try {
				if (this.fm.insert(f2) > 0) {
					if (isValidNode(f2)) {
						return f2;
					} else {
						break;
					}
				}
				break;
			} catch (Exception e) {
				f2.setFileId(UUID.randomUUID().toString());
				i++;
			}
			if (i >= 10) {
				break;
			}
		}
		return null;
	}

	/**
	 * 
	 * <h2>检查指定的文件节点是否存在同名问题</h2>
	 * <p>
	 * 该方法用于检查传入节点是否存在冲突问题，一般在新节点插入后执行，若存在冲突会立即删除此节点，最后会返回检查结果。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param n kohgylw.kiftd.server.model.Node 待检查的节点
	 * @return boolean 通过检查则返回true，否则返回false并删除此节点
	 */
	public boolean isValidNode(Node n) {
		Node[] repeats = fm.queryByParentFolderId(n.getFileParentFolder()).parallelStream()
				.filter((e) -> e.getFileName().equals(n.getFileName())).toArray(Node[]::new);
		if (flm.queryById(n.getFileParentFolder()) == null || repeats.length > 1) {
			// 如果插入后存在：
			// 1，该节点没有有效的父级文件夹（死节点）；
			// 2，与同级的其他节点重名，
			// 那么它就是一个无效的节点，应将插入操作撤销
			// 所谓撤销，也就是将该节点的数据立即删除（如果有）
			fm.deleteById(n.getFileId());
			return false;// 返回“无效”的判定结果
		} else {
			return true;// 否则，该节点有效，返回结果
		}
	}

	/**
	 * 
	 * <h2>获取一个节点当前的逻辑路径</h2>
	 * <p>
	 * 该方法用于获取指定节点当前的完整逻辑路径，型如“/ROOT/doc/test.txt”。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param n kohgylw.kiftd.server.model.Node 要获取路径的节点
	 * @return java.lang.String 指定节点的逻辑路径，包含其完整的上级文件夹路径和自身的文件名，各级之间以“/”分割。
	 */
	public String getNodePath(Node n) {
		Folder folder = flm.queryById(n.getFileParentFolder());
		List<Folder> l = fu.getParentList(folder.getFolderId());
		StringBuffer pl = new StringBuffer();
		for (Folder i : l) {
			pl.append(i.getFolderName() + "/");
		}
		pl.append(folder.getFolderName());
		pl.append("/");
		pl.append(n.getFileName());
		return pl.toString();
	}

}
