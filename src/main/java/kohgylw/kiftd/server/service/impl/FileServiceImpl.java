package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;

import org.springframework.stereotype.*;
import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.pojo.CheckUploadFilesRespons;
import kohgylw.kiftd.server.pojo.UploadKeyCertificate;

import org.springframework.web.multipart.*;

import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.Charset;

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
	private static final String ERROR_PARAMETER = "errorParameter";// 参数错误标识
	private static final String NO_AUTHORIZED = "noAuthorized";// 权限错误标识
	private static final String UPLOADSUCCESS = "uploadsuccess";// 上传成功标识
	private static final String UPLOADERROR = "uploaderror";// 上传失败标识

	private static Map<String, UploadKeyCertificate> keyEffecMap = new HashMap<>();// 上传次数凭证表，用于记录用次数有限但时间不限的上传凭证

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

	private static final String CONTENT_TYPE = "application/octet-stream";

	// 检查上传文件列表的实现
	public String checkUploadFile(final HttpServletRequest request, final HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String nameList = request.getParameter("namelist");
		// 先行权限检查
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES)) {
			return NO_AUTHORIZED;
		}
		// 获得上传文件名列表
		final List<String> namelistObj = gson.fromJson(nameList, new TypeToken<List<String>>() {
		}.getType());
		final List<String> pereFileNameList = new ArrayList<>();
		// 查找目标目录下是否存在与待上传文件同名的文件（或文件夹），如果有，记录在上方的列表中
		for (final String fileName : namelistObj) {
			if (folderId == null || folderId.length() <= 0 || fileName == null || fileName.length() <= 0) {
				return ERROR_PARAMETER;
			}
			final List<Node> files = this.fm.queryByParentFolderId(folderId);
			if (files.stream().parallel().anyMatch((n) -> n.getFileName()
					.equals(new String(fileName.getBytes(Charset.forName("UTF-8")), Charset.forName("UTF-8"))))) {
				pereFileNameList.add(fileName);
			}
		}
		// 为客户端分发一个凭证，该凭证可使用本次声明要上传的次数，但不限时长。
		String key = UUID.randomUUID().toString();
		synchronized (keyEffecMap) {
			keyEffecMap.put(key, new UploadKeyCertificate(namelistObj.size(), account));
		}
		// 装订检查结果
		CheckUploadFilesRespons cufr = new CheckUploadFilesRespons();
		cufr.setUploadKey(key);// 分配一个凭证
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

	// 执行上传操作，接收文件并存入文件节点
	public String doUploadFile(final HttpServletRequest request, final HttpServletResponse response,
			final MultipartFile file) {
		String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String originalFileName = new String(file.getOriginalFilename().getBytes(Charset.forName("UTF-8")),
				Charset.forName("UTF-8"));
		String fileName = originalFileName;
		final String repeType = request.getParameter("repeType");
		// 再次检查上传文件名与目标目录ID
		if (folderId == null || folderId.length() <= 0 || originalFileName == null || originalFileName.length() <= 0) {
			return UPLOADERROR;
		}
		// 检查上传凭证，如果有则允许上传，否则丢弃该资源。该凭证用完后立即销毁。
		String uploadKey = request.getParameter("uploadKey");
		if (uploadKey != null) {
			synchronized (keyEffecMap) {
				UploadKeyCertificate c = keyEffecMap.get(uploadKey);
				if (c != null && c.isEffective()) {// 比对凭证有效性
					c.checked();// 使用一次
					account = c.getAccount();
					if (!c.isEffective()) {
						keyEffecMap.remove(uploadKey);// 用完后销毁这个凭证
					}
				} else {
					return UPLOADERROR;
				}
			}
		} else {
			return UPLOADERROR;
		}
		// 检查是否存在同名文件。不存在：直接存入新节点；存在：检查repeType代表的上传类型：覆盖、跳过、保留两者。
		final List<Node> files = this.fm.queryByParentFolderId(folderId);
		if (files.parallelStream().anyMatch((e) -> e.getFileName().equals(originalFileName))) {
			// 针对存在同名文件的操作
			if (repeType != null) {
				switch (repeType) {
				// 跳过则忽略上传请求并直接返回上传成功（跳过不应上传）
				case "skip":
					return UPLOADSUCCESS;
				// 覆盖则找到已存在文件节点的File并将新内容写入其中，同时更新原节点信息（除了文件名、父目录和ID之外的全部信息）
				case "cover":
					// 其中覆盖操作同时要求用户必须具备删除权限
					if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER)) {
						return UPLOADERROR;
					}
					for (Node f : files) {
						if (f.getFileName().equals(originalFileName)) {
							File file2 = fbu.getFileFromBlocks(f);
							try {
								file.transferTo(file2);
								f.setFileSize(fbu.getFileSize(file));
								f.setFileCreationDate(ServerTimeUtil.accurateToDay());
								if (account != null) {
									f.setFileCreator(account);
								} else {
									f.setFileCreator("\u533f\u540d\u7528\u6237");
								}
								if (fm.update(f) > 0) {
									this.lu.writeUploadFileEvent(f, account);
									return UPLOADSUCCESS;
								} else {
									return UPLOADERROR;
								}
							} catch (Exception e) {
								// TODO 自动生成的 catch 块
								return UPLOADERROR;
							}
						}
					}
					return UPLOADERROR;
				// 保留两者，使用型如“xxxxx (n).xx”的形式命名新文件。其中n为计数，例如已经存在2个文件，则新文件的n记为2
				case "both":
					// 设置新文件名为标号形式
					fileName = FileNodeUtil.getNewNodeName(originalFileName, files);
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
		// 将文件存入节点并获取其存入生成路径，型如“UUID.block”形式。
		final String path = this.fbu.saveToFileBlocks(file);
		if (path.equals("ERROR")) {
			return UPLOADERROR;
		}
		final String fsize = this.fbu.getFileSize(file);
		final Node f2 = new Node();
		f2.setFileId(UUID.randomUUID().toString());
		if (account != null) {
			f2.setFileCreator(account);
		} else {
			f2.setFileCreator("\u533f\u540d\u7528\u6237");
		}
		f2.setFileCreationDate(ServerTimeUtil.accurateToDay());
		f2.setFileName(fileName);
		f2.setFileParentFolder(folderId);
		f2.setFilePath(path);
		f2.setFileSize(fsize);
		int i = 0;
		// 尽可能避免UUID重复的情况发生，重试10次
		while (true) {
			try {
				if (this.fm.insert(f2) > 0) {
					this.lu.writeUploadFileEvent(f2, account);
					return UPLOADSUCCESS;
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
		return UPLOADERROR;
	}

	// 删除单个文件，该功能与删除多个文件重复，计划合并二者
	public String deleteFile(final HttpServletRequest request) {
		// 接收参数并接续要删除的文件
		final String fileId = request.getParameter("fileId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER)) {
			return NO_AUTHORIZED;
		}
		if (fileId == null || fileId.length() <= 0) {
			return ERROR_PARAMETER;
		}
		// 确认要删除的文件存在
		final Node file = this.fm.queryById(fileId);
		if (file == null) {
			return ERROR_PARAMETER;
		}
		// 从文件块删除
		if (!this.fbu.deleteFromFileBlocks(file)) {
			return "cannotDeleteFile";
		}
		// 从节点删除
		if (this.fm.deleteById(fileId) > 0) {
			this.lu.writeDeleteFileEvent(request, file);
			return "deleteFileSuccess";
		}
		return "cannotDeleteFile";
	}

	// 普通下载：下载单个文件
	public void doDownloadFile(final HttpServletRequest request, final HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 权限检查
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			// 找到要下载的文件节点
			final String fileId = request.getParameter("fileId");
			if (fileId != null) {
				final Node f = this.fm.queryById(fileId);
				if (f != null) {
					// 执行写出
					final File fo = this.fbu.getFileFromBlocks(f);
					writeRangeFileStream(request, response, fo, f.getFileName(), CONTENT_TYPE);
					// 日志记录（仅针对一次下载）
					if (request.getHeader("Range") == null) {
						this.lu.writeDownloadFileEvent(request, f);
					}
					return;
				}
			}
		}
		try {
			//  处理无法下载的资源
			response.sendError(404);
		} catch (IOException e) {
			// TODO 自动生成的 catch 块

		}
	}

	// 重命名文件
	public String doRenameFile(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		final String newFileName = request.getParameter("newFileName");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 权限检查
		if (!ConfigureReader.instance().authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER)) {
			return NO_AUTHORIZED;
		}
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
		this.lu.writeRenameFileEvent(request, file, newFileName);
		return "renameFileSuccess";
	}

	// 删除所有选中文件和文件夹
	public String deleteCheckedFiles(final HttpServletRequest request) {
		final String strIdList = request.getParameter("strIdList");
		final String strFidList = request.getParameter("strFidList");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 权限检查
		if (ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER)) {
			try {
				// 得到要删除的文件ID列表
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				// 对每个要删除的文件节点进行确认并删除
				for (final String fileId : idList) {
					if (fileId == null || fileId.length() <= 0) {
						return ERROR_PARAMETER;
					}
					final Node file = this.fm.queryById(fileId);
					if (file == null) {
						return "deleteFileSuccess";
					}
					// 删除文件块
					if (!this.fbu.deleteFromFileBlocks(file)) {
						return "cannotDeleteFile";
					}
					// 删除文件节点
					if (this.fm.deleteById(fileId) <= 0) {
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
					final List<Folder> l = this.fu.getParentList(fid);
					if (fu.deleteAllChildFolder(fid) <= 0) {
						return "cannotDeleteFile";
					} else {
						this.lu.writeDeleteFolderEvent(request, folder, l);
					}
				}
				return "deleteFileSuccess";
			} catch (Exception e) {
				return ERROR_PARAMETER;
			}
		}
		return NO_AUTHORIZED;
	}

	// 打包下载功能：前置——压缩要打包下载的文件
	public String downloadCheckedFiles(final HttpServletRequest request) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 权限检查
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
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
					this.lu.writeDownloadCheckedFileEvent(request, idList);
					// 返回生成的压缩包路径
					return zipname;
				}
			} catch (Exception ex) {
			}
		}
		return "ERROR";
	}

	// 打包下载功能：执行——下载压缩好的文件
	public void downloadCheckedFilesZip(final HttpServletRequest request, final HttpServletResponse response)
			throws Exception {
		final String zipname = request.getParameter("zipId");
		if (zipname != null && !zipname.equals("ERROR")) {
			final String tfPath = ConfigureReader.instance().getTemporaryfilePath();
			final File zip = new File(tfPath, zipname);
			String fname = "kiftd_" + ServerTimeUtil.accurateToDay() + "_\u6253\u5305\u4e0b\u8f7d.zip";
			if (zip.exists()) {
				writeRangeFileStream(request, response, zip, fname, CONTENT_TYPE);
				zip.delete();
			}
		}
	}

	public String getPackTime(final HttpServletRequest request) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			final String strIdList = request.getParameter("strIdList");
			final String strFidList = request.getParameter("strFidList");
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				for (String fid : fidList) {
					countFolderFilesId(account, fid, fidList);
				}
				long packTime = 0L;
				for (final String fid : idList) {
					final Node n = this.fm.queryById(fid);
					final File f = new File(ConfigureReader.instance().getFileBlockPath(), n.getFilePath());
					if (f.exists()) {
						packTime += f.length() / 25000000L;
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
			}
		}
		return "0";
	}

	// 用于迭代获得全部文件夹内的文件ID（方便预测耗时）
	private void countFolderFilesId(String account, String fid, List<String> idList) {
		Folder f = flm.queryById(fid);
		if (ConfigureReader.instance().accessFolder(f, account)) {
			idList.addAll(Arrays.asList(
					fm.queryByParentFolderId(fid).parallelStream().map((e) -> e.getFileId()).toArray(String[]::new)));
			List<Folder> cFolders = flm.queryByParentId(fid);
			for (Folder cFolder : cFolders) {
				countFolderFilesId(account, cFolder.getFolderId(), idList);
			}
		}
	}

	@Override
	public String doMoveFiles(HttpServletRequest request) {
		// TODO 自动生成的方法存根
		final String strIdList = request.getParameter("strIdList");
		final String strFidList = request.getParameter("strFidList");
		final String strOptMap = request.getParameter("strOptMap");
		final String locationpath = request.getParameter("locationpath");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES)) {
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final Map<String, String> optMap = gson.fromJson(strOptMap, new TypeToken<Map<String, String>>() {
				}.getType());
				for (final String id : idList) {
					if (id == null || id.length() <= 0) {
						return ERROR_PARAMETER;
					}
					final Node node = this.fm.queryById(id);
					if (node == null) {
						return ERROR_PARAMETER;
					}
					if (node.getFileParentFolder().equals(locationpath)) {
						break;
					}
					if (fm.queryByParentFolderId(locationpath).parallelStream()
							.anyMatch((e) -> e.getFileName().equals(node.getFileName()))) {
						if (optMap.get(id) == null) {
							return ERROR_PARAMETER;
						}
						switch (optMap.get(id)) {
						case "cover":
							if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER)) {
								return NO_AUTHORIZED;
							}
							Node n = fm.queryByParentFolderId(locationpath).parallelStream()
									.filter((e) -> e.getFileName().equals(node.getFileName())).findFirst().get();
							if (fm.deleteById(n.getFileId()) > 0) {
								Map<String, String> map = new HashMap<>();
								map.put("fileId", node.getFileId());
								map.put("locationpath", locationpath);
								if (this.fm.moveById(map) <= 0) {
									return "cannotMoveFiles";
								}
							} else {
								return "cannotMoveFiles";
							}
							this.lu.writeMoveFileEvent(request, node);
							break;
						case "both":
							node.setFileName(FileNodeUtil.getNewNodeName(node.getFileName(),
									fm.queryByParentFolderId(locationpath)));
							if (fm.update(node) <= 0) {
								return "cannotMoveFiles";
							}
							Map<String, String> map = new HashMap<>();
							map.put("fileId", node.getFileId());
							map.put("locationpath", locationpath);
							if (this.fm.moveById(map) <= 0) {
								return "cannotMoveFiles";
							}
							this.lu.writeMoveFileEvent(request, node);
							break;
						case "skip":
							break;
						default:
							return ERROR_PARAMETER;
						}
					} else {
						Map<String, String> map = new HashMap<>();
						map.put("fileId", node.getFileId());
						map.put("locationpath", locationpath);
						if (this.fm.moveById(map) <= 0) {
							return "cannotMoveFiles";
						}
						this.lu.writeMoveFileEvent(request, node);
					}
				}
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				for (final String fid : fidList) {
					if (fid == null || fid.length() <= 0) {
						return ERROR_PARAMETER;
					}
					final Folder folder = this.flm.queryById(fid);
					if (folder == null) {
						return ERROR_PARAMETER;
					}
					if (folder.getFolderParent().equals(locationpath)) {
						break;
					}
					if (fid.equals(locationpath) || fu.getParentList(locationpath).parallelStream()
							.anyMatch((e) -> e.getFolderId().equals(folder.getFolderId()))) {
						return ERROR_PARAMETER;
					}
					if (flm.queryByParentId(locationpath).parallelStream()
							.anyMatch((e) -> e.getFolderName().equals(folder.getFolderName()))) {
						if (optMap.get(fid) == null) {
							return ERROR_PARAMETER;
						}
						switch (optMap.get(fid)) {
						case "cover":
							if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER)) {
								return NO_AUTHORIZED;
							}
							Folder f = flm.queryByParentId(locationpath).parallelStream()
									.filter((e) -> e.getFolderName().equals(folder.getFolderName())).findFirst().get();
							Map<String, String> map = new HashMap<>();
							map.put("folderId", folder.getFolderId());
							map.put("locationpath", locationpath);
							if (this.flm.moveById(map) > 0) {
								if (fu.deleteAllChildFolder(f.getFolderId()) > 0) {
									this.lu.writeMoveFileEvent(request, folder);
									break;
								}
							}
							return "cannotMoveFiles";
						case "both":
							Map<String, String> map3 = new HashMap<>();
							map3.put("folderId", folder.getFolderId());
							map3.put("locationpath", locationpath);
							if (this.flm.moveById(map3) > 0) {
								Map<String, String> map2 = new HashMap<String, String>();
								map2.put("folderId", folder.getFolderId());
								map2.put("newName", FileNodeUtil.getNewFolderName(folder.getFolderName(),
										flm.queryByParentId(locationpath)));
								if (flm.updateFolderNameById(map2) <= 0) {
									return "cannotMoveFiles";
								}
								this.lu.writeMoveFileEvent(request, folder);
								break;
							}
							this.lu.writeMoveFileEvent(request, folder);
							break;
						case "skip":
							break;
						default:
							return ERROR_PARAMETER;
						}
					} else {
						Map<String, String> map = new HashMap<>();
						map.put("folderId", folder.getFolderId());
						map.put("locationpath", locationpath);
						if (this.flm.moveById(map) > 0) {
							this.lu.writeMoveFileEvent(request, folder);
						} else {
							return "cannotMoveFiles";
						}
					}
				}
				return "moveFilesSuccess";
			} catch (Exception e) {
				return ERROR_PARAMETER;
			}
		}
		return NO_AUTHORIZED;
	}

	@Override
	public String confirmMoveFiles(HttpServletRequest request) {
		// TODO 自动生成的方法存根
		final String strIdList = request.getParameter("strIdList");
		final String strFidList = request.getParameter("strFidList");
		final String locationpath = request.getParameter("locationpath");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES)) {
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				final List<String> fidList = gson.fromJson(strFidList, new TypeToken<List<String>>() {
				}.getType());
				List<Node> repeNodes = new ArrayList<>();
				List<Folder> repeFolders = new ArrayList<>();
				for (final String fileId : idList) {
					if (fileId == null || fileId.length() <= 0) {
						return ERROR_PARAMETER;
					}
					final Node node = this.fm.queryById(fileId);
					if (node == null) {
						return ERROR_PARAMETER;
					}
					if (node.getFileParentFolder().equals(locationpath)) {
						break;
					}
					if (fm.queryByParentFolderId(locationpath).parallelStream()
							.anyMatch((e) -> e.getFileName().equals(node.getFileName()))) {
						repeNodes.add(node);
					}
				}
				for (final String folderId : fidList) {
					if (folderId == null || folderId.length() <= 0) {
						return ERROR_PARAMETER;
					}
					final Folder folder = this.flm.queryById(folderId);
					if (folder == null) {
						return ERROR_PARAMETER;
					}
					if (folder.getFolderParent().equals(locationpath)) {
						break;
					}
					if (folderId.equals(locationpath) || fu.getParentList(locationpath).parallelStream()
							.anyMatch((e) -> e.getFolderId().equals(folder.getFolderId()))) {
						return "CANT_MOVE_TO_INSIDE:" + folder.getFolderName();
					}
					if (flm.queryByParentId(locationpath).parallelStream()
							.anyMatch((e) -> e.getFolderName().equals(folder.getFolderName()))) {
						repeFolders.add(folder);
					}
				}
				if (repeNodes.size() > 0 || repeFolders.size() > 0) {
					Map<String, List<? extends Object>> repeMap = new HashMap<>();
					repeMap.put("repeFolders", repeFolders);
					repeMap.put("repeNodes", repeNodes);
					return "duplicationFileName:" + gson.toJson(repeMap);
				}
				return "confirmMoveFiles";
			} catch (Exception e) {
				return ERROR_PARAMETER;
			}
		}
		return NO_AUTHORIZED;
	}

}
