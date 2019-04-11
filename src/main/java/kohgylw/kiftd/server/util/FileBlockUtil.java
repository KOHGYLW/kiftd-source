package kohgylw.kiftd.server.util;

import org.springframework.stereotype.*;
import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import org.springframework.web.multipart.*;
import java.io.*;
import kohgylw.kiftd.server.model.*;
import java.util.*;
import java.util.zip.ZipEntry;

import org.zeroturnaround.zip.*;

/**
 * 
 * <h2>文件块整合操作工具</h2>
 * <p>
 * 该工具内包含了众多关于文件块的操作，例如索引、压缩等。详见各个方法。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class FileBlockUtil {
	@Resource
	private NodeMapper fm;
	@Resource
	private FolderMapper flm;

	private final String fileBlocks = ConfigureReader.instance().getFileBlockPath();

	/**
	 * 
	 * <h2>将上传文件存入文件节点</h2>
	 * <p>
	 * 将一个MultipartFile类型的文件对象存入节点，并返回保存的路径。路径为“UUID.block”形式。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f
	 *            MultipartFile 上传文件对象
	 * @return String 随机生成的保存路径，如果保存失败则返回“ERROR”
	 */
	public String saveToFileBlocks(final MultipartFile f) {
		final String fileBlocks = ConfigureReader.instance().getFileBlockPath();
		final String id = UUID.randomUUID().toString().replace("-", "");
		final String path = "file_" + id + ".block";
		final File file = new File(fileBlocks, path);
		try {
			f.transferTo(file);
			return path;
		} catch (Exception e) {
			e.printStackTrace();
			return "ERROR";
		}
	}

	public String getFileSize(final MultipartFile f) {
		final long size = f.getSize();
		final int mb = (int) (size / 1024L / 1024L);
		return "" + mb;
	}

	public boolean deleteFromFileBlocks(Node f) {
		final File file = new File(ConfigureReader.instance().getFileBlockPath(), f.getFilePath());
		return file.exists() && file.isFile() && file.delete();
	}

	public File getFileFromBlocks(Node f) {
		final File file = new File(ConfigureReader.instance().getFileBlockPath(), f.getFilePath());
		if (file.exists() && file.isFile()) {
			return file;
		}
		return null;
	}

	public void checkFileBlocks() {
		String fileblocks = ConfigureReader.instance().getFileBlockPath();
		Thread checkThread = new Thread(() -> {
			List<Node> nodes = fm.queryAll();
			for (Node node : nodes) {
				File block = new File(fileblocks, node.getFilePath());
				if (!block.exists()) {
					fm.deleteById(node.getFileId());
				}
			}
			File blocks = new File(fileblocks);
			String[] bn = blocks.list();
			for (String n : bn) {
				Node node = fm.queryByPath(n);
				if (node == null) {
					File f = new File(fileblocks, n);
					f.delete();
				}
			}
		});
		checkThread.start();
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
				if (fo != null) {
					folders.add(fo);
				}
			}
			final List<Node> nodes = new ArrayList<>();
			for (String id : idList) {
				Node n = fm.queryById(id);
				if (n != null) {
					nodes.add(n);
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
				zs.add((ZipEntrySource) new FileSource(node.getFileName(), new File(fileBlocks, node.getFilePath())));
			}
			for(ZipEntrySource zes:zs) {
				System.out.println(zes.getPath());
			}
			ZipUtil.pack(zs.toArray(new ZipEntrySource[0]), f);
			return zipname;
		} catch (Exception e) {
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
				zs.add(new FileSource(thisPath + node.getFileName(), new File(fileBlocks, node.getFilePath())));
			}
		}
	}
}
