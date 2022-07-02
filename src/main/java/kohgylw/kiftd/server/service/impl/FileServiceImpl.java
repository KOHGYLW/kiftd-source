package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;

import org.springframework.stereotype.*;
import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.server.listener.ServerInitListener;
import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.pojo.CheckImportFolderRespons;
import kohgylw.kiftd.server.pojo.CheckUploadFilesRespons;

import org.springframework.web.multipart.*;

import javax.servlet.http.*;
import java.io.*;

import kohgylw.kiftd.server.util.*;
import java.util.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * <h2>文件服务功能实现类</h2>
 * <p>
 * 该类负责对文件相关的服务进行实现操作，例如下载和上传等，各方法功能详见接口定义。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 * @see kohgylw.kiftd.server.service.FileService
 */
@Service
public class FileServiceImpl extends RangeFileStreamWriter implements FileService {
	private static final String FOLDERS_TOTAL_OUT_OF_LIMIT = "foldersTotalOutOfLimit";// 文件夹数量超限标识
	private static final String FILES_TOTAL_OUT_OF_LIMIT = "filesTotalOutOfLimit";// 文件数量超限标识
	private static final String ERROR_PARAMETER = "errorParameter";// 参数错误标识
	private static final String NO_AUTHORIZED = "noAuthorized";// 权限错误标识
	private static final String UPLOADSUCCESS = "uploadsuccess";// 上传成功标识
	private static final String UPLOADERROR = "uploaderror";// 上传失败标识

	@Resource
	private NodeMapper fm;
	@Resource
	private FolderMapper flm;
	@Resource
	private LogUtil lu;
	@Resource
	private Gson gson;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private FolderUtil fu;
	@Resource
	private IpAddrGetter idg;

	private static final String CONTENT_TYPE = "application/octet-stream";

	// 检查上传文件列表的实现（上传文件的前置操作）
	public String checkUploadFile(final HttpServletRequest request, final HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String nameList = request.getParameter("namelist");
		final String maxUploadFileSize = request.getParameter("maxSize");
		final String maxUploadFileIndex = request.getParameter("maxFileIndex");
		// 目标文件夹合法性检查
		if (folderId == null || folderId.length() == 0) {
			return ERROR_PARAMETER;
		}
		// 获取上传目标文件夹，如果没有直接返回错误
		Folder folder = flm.queryById(folderId);
		if (folder == null) {
			return ERROR_PARAMETER;
		}
		// 权限检查
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().accessFolder(folder, account)) {
			return NO_AUTHORIZED;
		}
		// 获得上传文件名列表
		final List<String> namelistObj = gson.fromJson(nameList, new TypeToken<List<String>>() {
		}.getType());
		// 准备一个检查结果对象
		CheckUploadFilesRespons cufr = new CheckUploadFilesRespons();
		// 开始文件上传体积限制检查
		try {
			// 获取最大文件体积（以Byte为单位）
			long mufs = Long.parseLong(maxUploadFileSize);
			// 获取最大文件的名称
			String mfname = namelistObj.get(Integer.parseInt(maxUploadFileIndex));
			long pMaxUploadSize = ConfigureReader.instance().getUploadFileSize(account);
			if (pMaxUploadSize >= 0) {
				if (mufs > pMaxUploadSize) {
					cufr.setCheckResult("fileTooLarge");
					cufr.setMaxUploadFileSize(formatMaxUploadFileSize(pMaxUploadSize));
					cufr.setOverSizeFile(mfname);
					return gson.toJson(cufr);
				}
			}
		} catch (Exception e) {
			return ERROR_PARAMETER;
		}
		// 开始文件命名冲突检查
		final List<String> pereFileNameList = new ArrayList<>();
		// 查找目标目录下是否存在与待上传文件同名的文件（或文件夹），如果有，记录在上方的列表中
		for (final String fileName : namelistObj) {
			if (folderId == null || folderId.length() <= 0 || fileName == null || fileName.length() <= 0) {
				return ERROR_PARAMETER;
			}
			final List<Node> files = this.fm.queryByParentFolderId(folderId);
			if (files.stream().parallel().anyMatch((n) -> n.getFileName().equals(fileName))) {
				pereFileNameList.add(fileName);
			}
		}
		// 判断如果上传了这一批文件的话，会不会引起文件数量超限
		long estimatedTotal = fm.countByParentFolderId(folderId) - pereFileNameList.size() + namelistObj.size();
		if (estimatedTotal > FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER || estimatedTotal < 0) {
			return "filesTotalOutOfLimit";
		}
		// 如果存在同名文件，则写入同名文件的列表；否则，直接允许上传
		if (pereFileNameList.size() > 0) {
			cufr.setCheckResult("hasExistsNames");
			cufr.setPereFileNameList(pereFileNameList);
		} else {
			cufr.setCheckResult("permitUpload");
			cufr.setPereFileNameList(new ArrayList<String>());
		}
		return gson.toJson(cufr);// 以JSON格式写回该结果
	}

	// 格式化存储体积，便于返回上传文件体积的检查提示信息（如果传入0，则会直接返回错误提示信息，将该提示信息发送至前端即可）。
	private String formatMaxUploadFileSize(long size) {
		double result = (double) size;
		String unit = "B";
		if (size <= 0) {
			return "设置无效，请联系管理员";
		}
		if (size >= 1024 && size < 1048576) {
			result = (double) size / 1024;
			unit = "KB";
		} else if (size >= 1048576 && size < 1073741824) {
			result = (double) size / 1048576;
			unit = "MB";
		} else if (size >= 1073741824) {
			result = (double) size / 1073741824;
			unit = "GB";
		}
		return String.format("%.1f", result) + " " + unit;
	}

	// 执行上传操作，接收文件并存入文件节点
	public String doUploadFile(final HttpServletRequest request, final HttpServletResponse response,
			final MultipartFile file) {
		String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String fname = request.getParameter("fname");
		final String originalFileName = (fname != null ? fname : file.getOriginalFilename());
		String fileName = originalFileName;
		final String repeType = request.getParameter("repeType");
		// 再次检查上传文件名与目标目录ID
		if (folderId == null || folderId.length() <= 0 || originalFileName == null || originalFileName.length() <= 0) {
			return UPLOADERROR;
		}
		Folder folder = flm.queryById(folderId);
		if (folder == null) {
			return UPLOADERROR;
		}
		// 检查上传权限
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().accessFolder(folder, account)) {
			return UPLOADERROR;
		}
		// 检查上传文件体积是否超限
		long mufs = ConfigureReader.instance().getUploadFileSize(account);
		if (mufs >= 0 && file.getSize() > mufs) {
			return UPLOADERROR;
		}
		// 检查是否存在同名文件。不存在：直接存入新节点；存在：检查repeType代表的上传类型：覆盖、跳过、保留两者。
		final List<Node> nodes = this.fm.queryByParentFolderId(folderId);
		if (nodes.parallelStream().anyMatch((e) -> e.getFileName().equals(originalFileName))) {
			// 针对存在同名文件的操作
			if (repeType != null) {
				switch (repeType) {
				// 跳过则忽略上传请求并直接返回上传成功（跳过不应上传）
				case "skip":
					return UPLOADSUCCESS;
				// 如果要覆盖的文件不存在与其他节点共享文件块的情况，则找到该文件块并将新内容写入其中，同时更新原节点信息（除了文件名、父目录和ID之外的全部信息）。
				// 如果要覆盖的文件是某个文件块的众多副本之一，那么“覆盖”就是新存入一个文件块，然后再更新原节点信息（除了文件名、父目录和ID之外的全部信息）。
				case "cover":
					// 特殊操作权限检查，“覆盖”操作同时还要求用户必须具备删除权限，否则不能执行
					if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
							fu.getAllFoldersId(folderId))) {
						return UPLOADERROR;
					}
					for (Node f : nodes) {
						// 找到要覆盖的节点，尝试将其删除
						if (f.getFileName().equals(originalFileName)) {
							if (!fbu.deleteNode(f)) {
								// 如果删除失败，则终止上传
								return UPLOADERROR;
							}
						}
					}
					break;
				// 保留两者，使用型如“xxxxx (n).xx”的形式命名新文件。其中n为计数，例如已经存在2个文件，则新文件的n记为2
				case "both":
					// 设置新文件名为标号形式
					fileName = FileNodeUtil.getNewNodeName(originalFileName, nodes);
					break;
				default:
					// 其他声明，容错，暂无效果
					return UPLOADERROR;
				}
			} else {
				// 如果既有重复文件、同时又没声明如何操作，则直接上传失败。
				return UPLOADERROR;
			}
		}
		// 判断该文件夹内的文件数量是否超限
		if (fm.countByParentFolderId(folderId) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			return FILES_TOTAL_OUT_OF_LIMIT;
		}
		// 将文件存入节点并获取其存入生成路径，型如“UUID.block”形式。
		final File block = this.fbu.saveToFileBlocks(file);
		if (block == null) {
			return UPLOADERROR;
		}
		final String fsize = this.fbu.getFileSize(file.getSize());
		Node newNode = fbu.insertNewNode(fileName, account, block.getName(), fsize, folderId);
		if (newNode != null) {
			// 存入成功，则写入日志并返回成功提示
			this.lu.writeUploadFileEvent(request, newNode, account);
			return UPLOADSUCCESS;
		} else {
			// 存入失败则删除残余文件块，并返回失败提示
			block.delete();
			return UPLOADERROR;
		}
	}

	// 删除单个文件
	public String deleteFile(final HttpServletRequest request) {
		// 接收参数并接续要删除的文件
		final String fileId = request.getParameter("fileId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (fileId == null || fileId.length() <= 0) {
			return ERROR_PARAMETER;
		}
		// 确认要删除的文件存在
		final Node node = this.fm.queryById(fileId);
		if (node == null) {
			return "deleteFileSuccess";
		}
		final Folder f = this.flm.queryById(node.getFileParentFolder());
		// 权限检查
		if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
				fu.getAllFoldersId(node.getFileParentFolder()))
				|| !ConfigureReader.instance().accessFolder(f, account)) {
			return NO_AUTHORIZED;
		}
		// 删除文件节点
		if (this.fbu.deleteNode(node)) {
			this.lu.writeDeleteFileEvent(request, node);
			return "deleteFileSuccess";
		}
		return "cannotDeleteFile";
	}

	// 普通下载：下载单个文件
	public void doDownloadFile(final HttpServletRequest request, final HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 权限检查
		// 找到要下载的文件节点
		final String fileId = request.getParameter("fileId");
		if (fileId != null) {
			final Node f = this.fm.queryById(fileId);
			if (f != null) {
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(f.getFileParentFolder()))) {
					Folder folder = flm.queryById(f.getFileParentFolder());
					if (ConfigureReader.instance().accessFolder(folder, account)) {
						// 执行写出
						final File fo = this.fbu.getFileFromBlocks(f);
						final String ip = idg.getIpAddr(request);
						final String range = request.getHeader("Range");
						if (fo != null) {
							int status = writeRangeFileStream(request, response, fo, f.getFileName(), CONTENT_TYPE,
									ConfigureReader.instance().getDownloadMaxRate(account), fbu.getETag(fo), true);
							// 日志记录（仅针对一次下载）
							if (status == HttpServletResponse.SC_OK
									|| (range != null && range.startsWith("bytes=0-"))) {
								this.lu.writeDownloadFileEvent(account, ip, f);
							}
							return;
						}
					}
				}
			}
		}
		try {
			//  处理无法下载的资源
			response.sendError(404);
		} catch (IOException e) {
		}
	}

	// 重命名文件
	public String doRenameFile(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		final String newFileName = request.getParameter("newFileName");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 参数检查
		if (fileId == null || fileId.length() <= 0 || newFileName == null || newFileName.length() <= 0) {
			return ERROR_PARAMETER;
		}
		if (!TextFormateUtil.instance().matcherFileName(newFileName) || newFileName.indexOf(".") == 0) {
			return ERROR_PARAMETER;
		}
		final Node file = this.fm.queryById(fileId);
		if (file == null) {
			return ERROR_PARAMETER;
		}
		final Folder folder = flm.queryById(file.getFileParentFolder());
		if (!ConfigureReader.instance().accessFolder(folder, account)) {
			return NO_AUTHORIZED;
		}
		// 权限检查
		if (!ConfigureReader.instance().authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER,
				fu.getAllFoldersId(file.getFileParentFolder()))) {
			return NO_AUTHORIZED;
		}
		if (!file.getFileName().equals(newFileName)) {
			// 不允许重名
			if (fm.queryBySomeFolder(fileId).parallelStream().anyMatch((e) -> e.getFileName().equals(newFileName))) {
				return "nameOccupied";
			}
			// 更新文件名
			final Map<String, String> map = new HashMap<String, String>();
			map.put("fileId", fileId);
			map.put("newFileName", newFileName);
			if (this.fm.updateFileNameById(map) == 0) {
				// 并写入日志
				return "cannotRenameFile";
			}
		}
		this.lu.writeRenameFileEvent(account, idg.getIpAddr(request), file.getFileParentFolder(), file.getFileName(),
				newFileName);
		return "renameFileSuccess";
	}

	// 删除所有选中文件和文件夹
	public String deleteCheckedFiles(final HttpServletRequest request) {
		final String strIdList = request.getParameter("strIdList");
		final String strFidList = request.getParameter("strFidList");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		try {
			// 得到要删除的文件ID列表
			final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
			}.getType());
			// 对每个要删除的文件节点进行确认并删除
			for (final String fileId : idList) {
				if (fileId == null || fileId.length() == 0) {
					return ERROR_PARAMETER;
				}
				final Node file = this.fm.queryById(fileId);
				if (file == null) {
					continue;
				}
				final Folder folder = flm.queryById(file.getFileParentFolder());
				if (!ConfigureReader.instance().accessFolder(folder, account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						fu.getAllFoldersId(file.getFileParentFolder()))) {
					return NO_AUTHORIZED;
				}
				// 删除文件节点
				if (!this.fbu.deleteNode(file)) {
					return "cannotDeleteFile";
				}
				// 日志记录
				this.lu.writeDeleteFileEvent(request, file);
			}
			// 删完选中的文件，再去删文件夹
			final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
			}.getType());
			for (String fid : fidList) {
				Folder folder = flm.queryById(fid);
				if (folder == null) {
					continue;
				}
				if (!ConfigureReader.instance().accessFolder(folder, account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
						fu.getAllFoldersId(folder.getFolderParent()))) {
					return NO_AUTHORIZED;
				}
				final List<Folder> l = this.fu.getParentList(fid);
				if (flm.deleteById(fid) <= 0) {
					return "cannotDeleteFile";
				} else {
					fu.deleteAllChildFolder(fid);
					this.lu.writeDeleteFolderEvent(request, folder, l);
				}
			}
			if (fidList.size() > 0) {
				ServerInitListener.needCheck = true;
			}
			return "deleteFileSuccess";
		} catch (Exception e) {
			return ERROR_PARAMETER;
		}
	}

	// 打包下载功能：前置——压缩要打包下载的文件
	public String downloadCheckedFiles(final HttpServletRequest request) {
		if (ConfigureReader.instance().isEnableDownloadByZip()) {
			final String account = (String) request.getSession().getAttribute("ACCOUNT");
			final String strIdList = request.getParameter("strIdList");
			final String strFidList = request.getParameter("strFidList");
			try {
				// 获得要打包下载的文件ID
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				// 创建ZIP压缩包并将全部文件压缩
				if (idList.size() > 0 || fidList.size() > 0) {
					final String zipname = this.fbu.createZip(idList, fidList, account);
					this.lu.writeDownloadCheckedFileEvent(request, idList, fidList);
					// 返回生成的压缩包路径
					return zipname;
				}
			} catch (Exception ex) {
				lu.writeException(ex);
			}
		}
		return "ERROR";
	}

	// 打包下载功能：执行——下载压缩好的文件
	public void downloadCheckedFilesZip(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {
		final String zipname = request.getParameter("zipId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (zipname != null && !zipname.equals("ERROR")) {
			final String tfPath = ConfigureReader.instance().getTemporaryfilePath();
			final File zip = new File(tfPath, zipname);
			String fname = "kiftd_" + ServerTimeUtil.accurateToDay() + "_\u6253\u5305\u4e0b\u8f7d.zip";
			if (zip.exists()) {
				writeRangeFileStream(request, response, zip, fname, CONTENT_TYPE,
						ConfigureReader.instance().getDownloadMaxRate(account), fbu.getETag(zip), true);
				zip.delete();
			}
		}
	}

	public String getPackTime(final HttpServletRequest request) {
		if (ConfigureReader.instance().isEnableDownloadByZip()) {
			final String account = (String) request.getSession().getAttribute("ACCOUNT");
			final String strIdList = request.getParameter("strIdList");
			final String strFidList = request.getParameter("strFidList");
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				for (String fid : fidList) {
					countFolderFilesId(account, fid, idList);
				}
				long packTime = 0L;
				for (final String fid : idList) {
					final Node n = this.fm.queryById(fid);
					if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
							fu.getAllFoldersId(n.getFileParentFolder()))
							&& ConfigureReader.instance().accessFolder(flm.queryById(n.getFileParentFolder()),
									account)) {
						final File f = fbu.getFileFromBlocks(n);
						if (f != null && f.exists()) {
							packTime += f.length() / 25000000L;
						}
					}
				}
				if (packTime < 4L) {
					return "\u9a6c\u4e0a\u5b8c\u6210";
				}
				if (packTime >= 4L && packTime < 10L) {
					return "\u5927\u7ea610\u79d2";
				}
				if (packTime >= 10L && packTime < 35L) {
					return "\u4e0d\u5230\u534a\u5206\u949f";
				}
				if (packTime >= 35L && packTime < 65L) {
					return "\u5927\u7ea61\u5206\u949f";
				}
				if (packTime >= 65L) {
					return "\u8d85\u8fc7" + packTime / 60L
							+ "\u5206\u949f\uff0c\u8017\u65f6\u8f83\u957f\uff0c\u5efa\u8bae\u76f4\u63a5\u4e0b\u8f7d";
				}
			} catch (Exception ex) {
				lu.writeException(ex);
			}
		}
		return "0";
	}

	// 用于迭代获得全部文件夹内的文件ID（方便预测耗时）
	private void countFolderFilesId(String account, String fid, List<String> idList) {
		Folder f = flm.queryById(fid);
		if (ConfigureReader.instance().accessFolder(f, account)) {
			try {
				idList.addAll(Arrays.asList(fm.queryByParentFolderId(fid).parallelStream().map((e) -> e.getFileId())
						.toArray(String[]::new)));
				List<Folder> cFolders = flm.queryByParentId(fid);
				for (Folder cFolder : cFolders) {
					countFolderFilesId(account, cFolder.getFolderId(), idList);
				}
			} catch (Exception e2) {
				// 超限？那就不再加了。
			}
		}
	}

	// 执行移动文件操作
	@Override
	public String doMoveFiles(HttpServletRequest request) {
		// 先获得必要的操作参数
		final String strIdList = request.getParameter("strIdList");// 涉及的文件ID列表
		final String strFidList = request.getParameter("strFidList");// 涉及的文件夹ID列表
		final String strOptMap = request.getParameter("strOptMap");// 冲突文件或文件夹的处理列表（若存在）
		final String locationpath = request.getParameter("locationpath");// 目标文件夹ID
		// 操作方式，仅当传入“COPY”时才会作为复制执行，否则均按移动处理
		final String method = request.getParameter("method");
		boolean isCopy = "COPY".equals(method);// 是否为复制模式
		final String account = (String) request.getSession().getAttribute("ACCOUNT");// 操作账户
		// 先检查目标文件夹的合法性
		Folder targetFolder = flm.queryById(locationpath);
		if (targetFolder == null) {
			return ERROR_PARAMETER;
		}
		// 再检查是否有权访问目标文件夹
		if (!ConfigureReader.instance().accessFolder(targetFolder, account)) {
			return NO_AUTHORIZED;
		}
		// 以及是否具备操作权限
		if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES, fu.getAllFoldersId(locationpath))) {
			return NO_AUTHORIZED;
		}
		// 对涉及的文件和文件夹逐一进行移动或复制操作
		try {
			// 获取存在冲突的文件的对应处理表
			final Map<String, String> optMap = gson.fromJson(strOptMap, new TypeToken<Map<String, String>>() {
			}.getType());
			// 获取涉及移动的文件节点的ID数组
			final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
			}.getType());
			// 对涉及的文件节点逐一进行操作
			for (final String id : idList) {
				// 先对涉及的原节点进行合法性检查
				if (id == null || id.length() <= 0) {
					return ERROR_PARAMETER;// 文件节点的ID格式不正确
				}
				final Node node = this.fm.queryById(id);
				if (node == null) {
					return ERROR_PARAMETER;// 该文件节点不存在
				}
				// 目标路径检查
				// 在移动模式下，如果原节点已经在目标文件夹中了，则直接跳过它
				// 复制模式则继续检查
				if (node.getFileParentFolder().equals(locationpath) && !isCopy) {
					continue;
				}
				// 权限检查
				if (!ConfigureReader.instance().accessFolder(flm.queryById(node.getFileParentFolder()), account)) {
					return NO_AUTHORIZED;// 无权访问节点所在的文件夹
				}
				if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES,
						fu.getAllFoldersId(node.getFileParentFolder()))) {
					return NO_AUTHORIZED;// 无操作权限
				}
				// 记录原始的文件路径，便于执行后记录日志
				String originPath = fbu.getNodePath(node);
				// 记录操作者IP地址
				String ip = idg.getIpAddr(request);
				// 执行文件移动操作
				if (fm.queryByParentFolderId(locationpath).parallelStream()
						.anyMatch((e) -> e.getFileName().equals(node.getFileName()))) {
					// 如果节点存在冲突，但又未声明对应的处理方法，则执行失败
					if (optMap.get(id) == null) {
						return ERROR_PARAMETER;
					}
					// 否则，按照处理声明进行处理
					switch (optMap.get(id)) {
					case "cover":
						// 覆盖，需要额外的“删除”操作权限
						if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
								fu.getAllFoldersId(locationpath))) {
							return NO_AUTHORIZED;// 无删除权限不能执行
						}
						// 得到冲突节点
						Node n = fm.queryByParentFolderId(locationpath).parallelStream()
								.filter((e) -> e.getFileName().equals(node.getFileName())).findFirst().get();
						if (n.getFileId().equals(node.getFileId())) {
							// 如果冲突节点就是原节点自身，则直接跳过，且无需记录日志（因为操作无效）
							continue;
						}
						// 否则，先将冲突节点删除
						if (fbu.deleteNode(n)) {
							// 判断是否是复制模式
							if (isCopy) {
								// 若是，则新建一个与操作节点使用相同文件块的新节点添加到目标文件夹下
								Node copyNode = fbu.insertNewNode(node.getFileName(), account, node.getFilePath(),
										node.getFileSize(), locationpath);
								if (copyNode == null) {
									return "cannotMoveFiles";
								}
								// 成功，记录日志
								this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(copyNode), isCopy);
							} else {
								// 否则，修改操作节点的父文件夹为目标文件夹
								node.setFileParentFolder(locationpath);
								if (this.fm.update(node) <= 0) {
									return "cannotMoveFiles";
								}
								// 成功，记录日志
								this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(node), isCopy);
							}
						} else {
							// 如果原节点删除失败，则操作失败
							return "cannotMoveFiles";
						}
						// 到这里应该是覆盖成功了，继续执行后面的操作
						break;
					case "both":
						// 保留两者，由于会导致节点数目增加，因此要先判断移动后是否会导致文件列表超限
						if (fm.countByParentFolderId(locationpath) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
							return FILES_TOTAL_OUT_OF_LIMIT;
						}
						// 如果不超限，则判断是否为复制模式
						if (isCopy) {
							// 是，则创建一个新节点并与原节点引用相同的文件块
							Node copyNode = fbu.insertNewNode(
									FileNodeUtil.getNewNodeName(node.getFileName(),
											fm.queryByParentFolderId(locationpath)),
									account, node.getFilePath(), node.getFileSize(), locationpath);
							if (copyNode == null) {
								return "cannotMoveFiles";
							}
							this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(copyNode), isCopy);
						} else {
							// 不是，则将原节点重新命名为原名+序号，再移动至目标文件夹下
							node.setFileName(FileNodeUtil.getNewNodeName(node.getFileName(),
									fm.queryByParentFolderId(locationpath)));
							node.setFileParentFolder(locationpath);
							if (fm.update(node) <= 0) {
								return "cannotMoveFiles";
							}
							this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(node), isCopy);
						}
						// 到这里应该是保留成功了，继续执行后面的操作
						break;
					case "skip":
						// 跳过，不做任何处理
						break;
					default:
						// 其他处理方法为错误参数，必须中断操作以免损坏文件系统
						return ERROR_PARAMETER;
					}
				} else {
					// 如果节点不存在冲突
					// 那么移入会导致节点数目增加，也要先检查移动后是否会导致文件列表超过限额
					if (fm.countByParentFolderId(locationpath) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
						return FILES_TOTAL_OUT_OF_LIMIT;
					}
					// 如果不超限，则判断是否为复制模式
					if (isCopy) {
						// 是，则创建一个新节点并与原节点引用相同的文件块
						Node newNode = fbu.insertNewNode(node.getFileName(), account, node.getFilePath(),
								node.getFileSize(), locationpath);
						if (newNode == null) {
							return "cannotMoveFiles";
						}
						// 成功后，记录日志
						this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(newNode), isCopy);
					} else {
						// 不是，移动原节点至目标文件夹内
						node.setFileParentFolder(locationpath);
						if (this.fm.update(node) <= 0) {
							return "cannotMoveFiles";
						}
						// 成功后，记录日志
						this.lu.writeMoveFileEvent(account, ip, originPath, fbu.getNodePath(node), isCopy);
					}
				}
				// 完成了一个原节点的操作，继续循环直至所有涉及节点均操作完毕
			}
			// 获取涉及移动的文件夹的ID数组
			final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
			}.getType());
			// 对涉及的文件夹节点逐一进行操作
			for (final String fid : fidList) {
				// 该过程与移动文件节点的流程类似
				if (fid == null || fid.length() <= 0) {
					return ERROR_PARAMETER;
				}
				final Folder folder = this.flm.queryById(fid);
				if (folder == null) {
					return ERROR_PARAMETER;
				}
				if (folder.getFolderParent().equals(locationpath) && !isCopy) {
					continue;
				}
				if (!ConfigureReader.instance().accessFolder(folder, account)) {
					return NO_AUTHORIZED;
				}
				if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES,
						fu.getAllFoldersId(folder.getFolderParent()))) {
					return NO_AUTHORIZED;
				}
				// 对于非复制操作，还必须确保移动目标不在被移动文件夹的内部，否则移动后就永远无法访问它了
				if (!isCopy) {
					if (fid.equals(locationpath) || fu.getParentList(locationpath).parallelStream()
							.anyMatch((e) -> e.getFolderId().equals(folder.getFolderId()))) {
						return ERROR_PARAMETER;
					}
				}
				// 记录原始的文件夹路径和操作者IP地址
				String originPath = fu.getFolderPath(folder);
				String ip = idg.getIpAddr(request);
				// 判断目标文件夹内是否有文件夹与待移入文件夹冲突？
				if (flm.queryByParentId(locationpath).parallelStream()
						.anyMatch((e) -> e.getFolderName().equals(folder.getFolderName()))) {
					// 存在冲突，则根据声明的措施进行处理
					if (optMap.get(fid) == null) {
						return ERROR_PARAMETER;
					}
					switch (optMap.get(fid)) {
					case "cover":
						// 覆盖，需要额外的“删除”权限
						if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
								fu.getAllFoldersId(locationpath))) {
							return NO_AUTHORIZED;
						}
						// 获得冲突的文件夹
						Folder f = flm.queryByParentId(locationpath).parallelStream()
								.filter((e) -> e.getFolderName().equals(folder.getFolderName())).findFirst().get();
						// 先删除冲突文件夹的节点
						if (flm.deleteById(f.getFolderId()) > 0) {
							// 判断是否为复制模式
							if (isCopy) {
								// 是，则先在目标文件夹内复制整个原文件夹的节点树
								Folder newFolder = fu.copyFolderByNewNameToPath(folder, account, targetFolder, null);
								// 之后删除冲突文件夹的所有子文件夹，必须在复制后执行！否则可能会出现还没复制完就被删了的问题
								fu.deleteAllChildFolder(f.getFolderId());
								if (newFolder != null) {
									// 成功后，记录日志
									// 注意，上述过程均不可颠倒！否则可能会导致文件夹名冲突或复制内容不全的问题
									this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(newFolder),
											isCopy);
									break;
								}
							} else {
								// 不是，直接删除冲突文件夹及其所有子文件夹
								fu.deleteAllChildFolder(f.getFolderId());
								// 再将原文件夹移入目标文件夹内
								folder.setFolderParent(locationpath);
								// 如果原文件夹的访问级别比目标文件夹小，则还需要将访问级别升高至与目标文件夹一致
								boolean needChangeChildsConstranint = false;
								if (folder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
									folder.setFolderConstraint(targetFolder.getFolderConstraint());
									needChangeChildsConstranint = true;
								}
								if (this.flm.update(folder) > 0) {
									// 如果升高了文件夹的访问级别，那么子文件夹的访问级别也要一起升高
									if (needChangeChildsConstranint) {
										fu.changeChildFolderConstraint(folder.getFolderId(),
												targetFolder.getFolderConstraint());
									}
									// 成功后，记录日志
									this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(folder),
											isCopy);
									break;
								}
							}
						}
						// 上述操作没从正常的位置break，则说明操作出错，返回错误提示
						return "cannotMoveFiles";
					case "both":
						// 保留两者，需要先判断移动后是否会导致目标文件夹的文件列表超限
						if (flm.countByParentId(locationpath) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
							return FOLDERS_TOTAL_OUT_OF_LIMIT;
						}
						// 接下来，判断是否为拷贝模式
						if (isCopy) {
							// 是，则以新名称生成对应的原文件夹副本在目标文件夹里面
							Folder newFolder = fu.copyFolderByNewNameToPath(folder, account, targetFolder, FileNodeUtil
									.getNewFolderName(folder.getFolderName(), flm.queryByParentId(locationpath)));
							if (newFolder == null) {
								return "cannotMoveFiles";
							}
							// 成功，记录日志
							this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(newFolder), isCopy);
						} else {
							// 不是，将原节点的名称改为计数名称，父文件夹改为目标文件夹
							folder.setFolderParent(locationpath);
							folder.setFolderName(FileNodeUtil.getNewFolderName(folder.getFolderName(),
									flm.queryByParentId(locationpath)));
							boolean needChangeChildsConstranint = false;
							if (folder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
								folder.setFolderConstraint(targetFolder.getFolderConstraint());
								needChangeChildsConstranint = true;
							}
							if (this.flm.update(folder) <= 0) {
								return "cannotMoveFiles";
							}
							// 如果升高了文件夹的访问级别，那么子文件夹的访问级别也要一起升高
							if (needChangeChildsConstranint) {
								fu.changeChildFolderConstraint(folder.getFolderId(),
										targetFolder.getFolderConstraint());
							}
							// 成功，记录日志
							this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(folder), isCopy);
						}
						// 保留两者成功，继续后面的操作
						break;
					case "skip":
						// 跳过，无需进行任何操作
						break;
					default:
						// 意外的应对声明，终止操作
						return ERROR_PARAMETER;
					}
					// 冲突情况处理完成
				} else {
					// 不存在冲突，直接移动或复制
					// 仍然是先检查移动后是否会引发文件列表超限
					if (flm.countByParentId(locationpath) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
						return FOLDERS_TOTAL_OUT_OF_LIMIT;
					}
					// 是否是复制模式？
					if (isCopy) {
						// 是，复制原文件夹的结构树至目标文件夹内
						Folder newFolder = fu.copyFolderByNewNameToPath(folder, account, targetFolder, null);
						if (newFolder == null) {
							return "cannotMoveFiles";
						}
						// 操作成功，记录日志
						this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(newFolder), isCopy);
					} else {
						// 否，直接将原文件夹移入目标文件夹内
						folder.setFolderParent(locationpath);
						// 确保移入后访问级别不越界
						boolean needChangeChildsConstranint = false;
						if (folder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
							folder.setFolderConstraint(targetFolder.getFolderConstraint());
							needChangeChildsConstranint = true;
						}
						if (this.flm.update(folder) <= 0) {
							return "cannotMoveFiles";
						}
						if (needChangeChildsConstranint) {
							fu.changeChildFolderConstraint(folder.getFolderId(), targetFolder.getFolderConstraint());
						}
						// 操作成功，记录日志
						this.lu.writeMoveFolderEvent(account, ip, originPath, fu.getFolderPath(folder), isCopy);
					}
					// 无冲突情况处理完成
				}
				// 至此，一个文件夹的移动或复制操作结束，继续循环直至全部完成
			}
			// 由于移动文件夹可能会导致文件夹被删除，因此要将“检查特定文件夹额外权限设置”标志置为true，用于清理失效的设置。
			if (fidList.size() > 0) {
				ServerInitListener.needCheck = true;
			}
			// 上述操作全部成功而未中途退出的话，则证明移动任务顺利结束，返回成功提示信息
			return "moveFilesSuccess";
		} catch (Exception e) {
			// 如果中途产生了异常，那么返回失败提示
			return ERROR_PARAMETER;
		}
	}

	// 移动文件前的确认检查（可视作移动的前置操作）
	@Override
	public String confirmMoveFiles(HttpServletRequest request) {
		// 接收必要的参数
		final String strIdList = request.getParameter("strIdList");// 涉及的文件ID列表
		final String strFidList = request.getParameter("strFidList");// 涉及的文件夹ID列表
		final String locationpath = request.getParameter("locationpath");// 目标文件夹
		final String method = request.getParameter("method");// 移动方式，除非为“COPY”否则都按“MOVE”处理
		final String account = (String) request.getSession().getAttribute("ACCOUNT");// 操作账户
		// 确定移动模式是否为复制模式
		boolean isCopy = "COPY".equals(method);
		// 判断目标文件夹是否合法及文件是否会重名
		Folder targetFolder = flm.queryById(locationpath);
		int needMovefilesCount = 0;// 记录可以合法移动（或复制）的文件数目
		int needMoveFoldersCount = 0;// 同理，记录可以合法移动（或复制）的文件夹数目
		if (ConfigureReader.instance().accessFolder(targetFolder, account) && ConfigureReader.instance()
				.authorized(account, AccountAuth.MOVE_FILES, fu.getAllFoldersId(locationpath))) {
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				List<Node> repeNodes = new ArrayList<>();
				List<Folder> repeFolders = new ArrayList<>();
				// 检查每个涉及的文件节点是否合法
				for (final String fileId : idList) {
					if (fileId == null || fileId.length() <= 0) {
						return ERROR_PARAMETER;// ID格式不对
					}
					final Node node = this.fm.queryById(fileId);
					if (node == null) {
						return ERROR_PARAMETER;// 查无此节点
					}
					if (node.getFileParentFolder().equals(locationpath) && !isCopy) {
						continue;// 又不是复制模式，又已经在目标文件夹里了，则直接跳过
					}
					if (!ConfigureReader.instance().accessFolder(flm.queryById(node.getFileParentFolder()), account)) {
						return NO_AUTHORIZED;// 无权访问目标文件夹
					}
					if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES,
							fu.getAllFoldersId(node.getFileParentFolder()))) {
						return NO_AUTHORIZED;// 无权操作
					}
					if (fm.queryByParentFolderId(locationpath).parallelStream()
							.anyMatch((e) -> e.getFileName().equals(node.getFileName()))) {
						repeNodes.add(node);// 与目标文件夹里的某个文件夹重名？重名列表加一
					} else {
						needMovefilesCount++;// 上述问题都没出现？合法文件加一
					}
				}
				// 检查每个涉及的文件夹节点是否合法
				for (final String folderId : fidList) {
					// 下面的判断与文件基本类似
					if (folderId == null || folderId.length() <= 0) {
						return ERROR_PARAMETER;
					}
					final Folder folder = this.flm.queryById(folderId);
					if (folder == null) {
						return ERROR_PARAMETER;
					}
					if (folder.getFolderParent().equals(locationpath) && !isCopy) {
						continue;
					}
					if (!ConfigureReader.instance().accessFolder(folder, account)) {
						return NO_AUTHORIZED;
					}
					if (!ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES,
							fu.getAllFoldersId(folder.getFolderParent()))) {
						return NO_AUTHORIZED;
					}
					// 对于文件夹而言，在移动模式下还要检查是否将一个文件夹移动到自己内部了，避免死循环
					// 复制模式无需检查这一项
					if (!isCopy) {
						if (folderId.equals(locationpath) || fu.getParentList(locationpath).parallelStream()
								.anyMatch((e) -> e.getFolderId().equals(folder.getFolderId()))) {
							return "CANT_MOVE_TO_INSIDE:" + folder.getFolderName();
						}
					}
					if (flm.queryByParentId(locationpath).parallelStream()
							.anyMatch((e) -> e.getFolderName().equals(folder.getFolderName()))) {
						repeFolders.add(folder);// 与目标文件夹里的某个文件夹重名？重名列表加一
					} else {
						needMoveFoldersCount++;// 上述问题都没出现？合法文件夹加一
					}
				}
				// 计算移动后会不会超出文件列表的最大限额
				long estimateFilesTotal = fm.countByParentFolderId(locationpath) + needMovefilesCount;
				if (estimateFilesTotal > FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER || estimateFilesTotal < 0) {
					return FILES_TOTAL_OUT_OF_LIMIT;// 如果会超限，则不允许此次移动
				}
				long estimateFoldersTotal = flm.countByParentId(locationpath) + needMoveFoldersCount;
				if (estimateFoldersTotal > FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER || estimateFoldersTotal < 0) {
					return FOLDERS_TOTAL_OUT_OF_LIMIT;// 文件夹也要做相同的检查
				}
				// 是否有冲突的文件或文件夹？
				if (repeNodes.size() > 0 || repeFolders.size() > 0) {
					Map<String, List<? extends Object>> repeMap = new HashMap<>();
					repeMap.put("repeFolders", repeFolders);
					repeMap.put("repeNodes", repeNodes);
					return "duplicationFileName:" + gson.toJson(repeMap);// 若有，则将冲突列表返回前端
				}
				return "confirmMoveFiles";
			} catch (Exception e) {
				return ERROR_PARAMETER;
			}
		}
		return NO_AUTHORIZED;
	}

	// 上传文件夹的先行检查
	@Override
	public String checkImportFolder(HttpServletRequest request) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String folderName = request.getParameter("folderName");
		final String maxUploadFileSize = request.getParameter("maxSize");
		CheckImportFolderRespons cifr = new CheckImportFolderRespons();
		// 基本文件夹名称合法性检查
		if (folderName == null || folderName.length() == 0) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		// 上传目标参数检查
		if (folderId == null || folderId.length() == 0) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		// 检查上传的目标文件夹是否存在
		Folder folder = flm.queryById(folderId);
		if (folder == null) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		// 先行权限检查
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
						fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().accessFolder(folder, account)) {
			cifr.setResult(NO_AUTHORIZED);
			return gson.toJson(cifr);
		}
		// 开始文件上传体积限制检查
		try {
			// 获取最大文件体积（以Byte为单位）
			long mufs = Long.parseLong(maxUploadFileSize);
			long pMaxUploadSize = ConfigureReader.instance().getUploadFileSize(account);
			if (pMaxUploadSize >= 0) {
				if (mufs > pMaxUploadSize) {
					cifr.setResult("fileOverSize");
					cifr.setMaxSize(formatMaxUploadFileSize(ConfigureReader.instance().getUploadFileSize(account)));
					return gson.toJson(cifr);
				}
			}
		} catch (Exception e) {
			cifr.setResult(ERROR_PARAMETER);
			return gson.toJson(cifr);
		}
		// 开始文件夹命名冲突检查，若无重名则允许上传。否则检查该文件夹是否具备覆盖条件（具备该文件夹的访问权限且具备删除权限），如无则可选择保留两者或取消
		final List<Folder> folders = flm.queryByParentId(folderId);
		try {
			Folder testFolder = folders.stream().parallel().filter((n) -> n.getFolderName().equals(folderName))
					.findAny().get();
			if (ConfigureReader.instance().accessFolder(testFolder, account) && ConfigureReader.instance()
					.authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER, fu.getAllFoldersId(folderId))) {
				cifr.setResult("repeatFolder_coverOrBoth");
			} else {
				cifr.setResult("repeatFolder_Both");
			}
			return gson.toJson(cifr);
		} catch (NoSuchElementException e) {
			if (flm.countByParentId(folderId) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
				// 检查目标文件夹内的文件夹数目是否超限
				cifr.setResult(FOLDERS_TOTAL_OUT_OF_LIMIT);
			} else {
				// 通过所有检查，允许上传
				cifr.setResult("permitUpload");
			}
			return gson.toJson(cifr);
		}
	}

	// 执行文件夹上传逻辑
	@Override
	public String doImportFolder(HttpServletRequest request, MultipartFile file) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		String folderId = request.getParameter("folderId");
		final String originalFileName = request.getParameter("originalFileName");
		String folderConstraint = request.getParameter("folderConstraint");
		String newFolderName = request.getParameter("newFolderName");
		// 再次检查上传文件名与目标目录ID
		if (folderId == null || folderId.length() <= 0 || originalFileName == null || originalFileName.length() <= 0) {
			return UPLOADERROR;
		}
		// 检查上传的目标文件夹是否存在
		Folder folder = flm.queryById(folderId);
		if (folder == null) {
			return UPLOADERROR;
		}
		// 检查上传权限
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER,
						fu.getAllFoldersId(folderId))
				|| !ConfigureReader.instance().accessFolder(folder, account)) {
			return UPLOADERROR;
		}
		// 检查上传文件体积是否超限
		long mufs = ConfigureReader.instance().getUploadFileSize(account);
		if (mufs >= 0 && file.getSize() > mufs) {
			return UPLOADERROR;
		}
		// 检查是否具备创建文件夹权限，若有则使用请求中提供的文件夹访问级别，否则使用默认访问级别
		int pc = folder.getFolderConstraint();
		if (folderConstraint != null) {
			try {
				int ifc = Integer.parseInt(folderConstraint);
				if (ifc != 0 && account == null) {
					return UPLOADERROR;
				}
				if (ifc < pc) {
					return UPLOADERROR;
				}
			} catch (Exception e) {
				return UPLOADERROR;
			}
		} else {
			return UPLOADERROR;
		}
		// 计算相对路径的文件夹ID（即真正要保存的文件夹目标）
		String[] paths = getParentPath(originalFileName);
		// 检查上传路径是否正确（必须包含至少一层文件夹）
		if (paths.length == 0) {
			return UPLOADERROR;
		}
		// 若声明了替代文件夹名称，则使用替代文件夹名称作为最上级文件夹名称
		if (newFolderName != null && newFolderName.length() > 0) {
			paths[0] = newFolderName;
		}
		// 执行创建文件夹和上传文件操作
		for (String pName : paths) {
			Folder newFolder;
			try {
				newFolder = fu.createNewFolder(folderId, account, pName, folderConstraint);
			} catch (FoldersTotalOutOfLimitException e1) {
				return FOLDERS_TOTAL_OUT_OF_LIMIT;
			}
			if (newFolder == null) {
				Map<String, String> key = new HashMap<String, String>();
				key.put("parentId", folderId);
				key.put("folderName", pName);
				Folder target = flm.queryByParentIdAndFolderName(key);
				if (target != null) {
					folderId = target.getFolderId();// 向下迭代直至将父路径全部迭代完毕并找到最终路径
				} else {
					return UPLOADERROR;
				}
			} else {
				if (!fu.isValidFolder(newFolder)) {
					return UPLOADERROR;
				}
				folderId = newFolder.getFolderId();
			}
		}
		String fileName = getFileNameFormPath(originalFileName);
		// 检查是否存在同名文件。存在则直接失败（确保上传的文件夹内容的原始性）
		final List<Node> files = this.fm.queryByParentFolderId(folderId);
		if (files.parallelStream().anyMatch((e) -> e.getFileName().equals(fileName))) {
			return UPLOADERROR;
		}
		// 判断上传数目是否超过限额
		if (fm.countByParentFolderId(folderId) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
			return FILES_TOTAL_OUT_OF_LIMIT;
		}
		// 将文件存入节点并获取其存入生成路径，型如“UUID.block”形式。
		final File block = this.fbu.saveToFileBlocks(file);
		if (block == null) {
			return UPLOADERROR;
		}
		final String fsize = this.fbu.getFileSize(file.getSize());
		Node newNode = fbu.insertNewNode(fileName, account, block.getName(), fsize, folderId);
		if (newNode != null) {
			// 成功，则记录日志并返回成功提示
			this.lu.writeUploadFileEvent(request, newNode, account);
			return UPLOADSUCCESS;
		} else {
			// 失败，则清理残留文件块并返回失败提示
			block.delete();
			return UPLOADERROR;
		}
	}

	/**
	 * 
	 * <h2>解析相对路径字符串</h2>
	 * <p>
	 * 根据相对路径获得文件夹的层级名称，并以数组的形式返回。若无层级则返回空数组，若层级名称为空字符串则忽略。
	 * </p>
	 * <p>
	 * 示例1：输入"aaa/bbb/ccc.c"，返回["aaa","bbb"]。
	 * </p>
	 * <p>
	 * 示例2：输入"bbb.c"，返回[]。
	 * </p>
	 * <p>
	 * 示例3：输入"aaa//bbb/ccc.c"，返回["aaa","bbb"]。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path java.lang.String 原路径字符串
	 * @return java.lang.String[] 解析出的目录层级
	 */
	private String[] getParentPath(String path) {
		if (path != null) {
			String[] paths = path.split("/");
			List<String> result = new ArrayList<String>();
			for (int i = 0; i < paths.length - 1; i++) {
				if (paths[i].length() > 0) {
					result.add(paths[i]);
				}
			}
			return result.toArray(new String[0]);
		}
		return new String[0];
	}

	/**
	 * 
	 * <h2>解析相对路径中的文件名</h2>
	 * <p>
	 * 从相对路径中获得文件名，若解析失败则返回null。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param java.lang.String 需要解析的相对路径
	 * @return java.lang.String 文件名
	 */
	private String getFileNameFormPath(String path) {
		if (path != null) {
			String[] paths = path.split("/");
			if (paths.length > 0) {
				return paths[paths.length - 1];
			}
		}
		return null;
	}

}
