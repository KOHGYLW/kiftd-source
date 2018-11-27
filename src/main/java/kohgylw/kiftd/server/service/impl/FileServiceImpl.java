package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;

import org.springframework.stereotype.*;
import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.model.*;
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
	private static final String UPLOADSUCCESS = "uploadsuccess";//上传成功标识
	private static final String UPLOADERROR = "uploaderror";//上传失败标识
	@Resource
	private NodeMapper fm;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private LogUtil lu;
	@Resource
	private Gson gson;

	private static final String CONTENT_TYPE = "application/octet-stream";

	// 检查上传文件列表的实现
	public String checkUploadFile(final HttpServletRequest request) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String nameList = request.getParameter("namelist");
		// 先行权限检查
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES)) {
			return "noAuthorized";
		}
		// 获得上传文件名列表
		final List<String> namelistObj = gson.fromJson(nameList, new TypeToken<List<String>>() {
		}.getType());
		final List<String> pereFileNameList = new ArrayList<>();
		// 查找目标目录下是否存在与待上传文件同名的文件，如果有，记录在上方的列表中
		for (final String fileName : namelistObj) {
			if (folderId == null || folderId.length() <= 0 || fileName == null || fileName.length() <= 0) {
				return "errorParameter";
			}
			final List<Node> files = this.fm.queryByParentFolderId(folderId);
			if (files.stream().parallel().anyMatch((n) -> n.getFileName().equals(fileName))) {
				pereFileNameList.add(fileName);
			}
		}
		// 如果存在同名文件，返回同名文件的JSON数据，否则直接允许上传
		if (pereFileNameList.size() > 0) {
			return "duplicationFileName:" + gson.toJson(pereFileNameList);
		}
		return "permitUpload";
	}

	// 执行上传操作，接收文件并存入文件节点
	public String doUploadFile(final HttpServletRequest request, final MultipartFile file) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		String fileName = file.getOriginalFilename();
		final String repeType = request.getParameter("repeType");
		// 再次检查上传文件名与目标目录ID
		if (folderId == null || folderId.length() <= 0 || fileName == null || fileName.length() <= 0) {
			return UPLOADERROR;
		}
		// 再次检查权限
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES)) {
			return UPLOADERROR;
		}
		// 检查是否存在同名文件。不存在：直接存入新节点；存在：检查repeType代表的上传类型：覆盖、跳过、保留两者。
		final List<Node> files = this.fm.queryByParentFolderId(folderId);
		final List<String> fileNames = Arrays
				.asList(files.stream().parallel().map((t) -> t.getFileName()).toArray(String[]::new));
		if (fileNames.contains(fileName)) {
			// 针对存在同名文件的操作
			if (repeType != null) {
				switch (repeType) {
				// 跳过则忽略上传请求并直接返回上传成功（跳过不应上传）
				case "skip":
					return UPLOADSUCCESS;
				// 覆盖则找到已存在文件节点的File并将新内容写入其中，同时更新原节点信息（除了文件名、父目录和ID之外的全部信息）
				case "cover":
					for (Node f : files) {
						if (f.getFileName().equals(fileName)) {
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
								if(fm.update(f) > 0) {
									this.lu.writeUploadFileEvent(request, f);
									return UPLOADSUCCESS;
								}else {
									return UPLOADERROR;
								}
							} catch (Exception e) {
								// TODO 自动生成的 catch 块
								return UPLOADERROR;
							}
						}
					}
					return UPLOADERROR;
				//保留两者，使用型如“xxxxx (n).xx”的形式命名新文件。其中n为计数，例如已经存在2个文件，则新文件的n记为2
				case "both":
					int i = 1;
					//计算n的取值
					String newName = fileName.substring(0, fileName.lastIndexOf(".")) + " (" + i + ")"
							+ fileName.substring(fileName.lastIndexOf("."));
					while (fileNames.contains(newName)) {
						i++;
						newName = fileName.substring(0, fileName.lastIndexOf(".")) + " (" + i + ")"
								+ fileName.substring(fileName.lastIndexOf("."));
					}
					//设置新文件名为标号形式
					fileName = newName;
					break;
				default:
					//其他声明，容错，暂无效果
					return UPLOADERROR;
				}
			} else {
				//如果既有重复文件、同时又没声明如何操作，则直接上传失败。
				return UPLOADERROR;
			}
		}
		//将文件存入节点并获取其存入生成路径，型如“UUID.block”形式。
		final String path = this.fbu.saveToFileBlocks(request, file);
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
		if (this.fm.insert(f2) > 0) {
			this.lu.writeUploadFileEvent(request, f2);
			return UPLOADSUCCESS;
		}
		return UPLOADERROR;
	}

	public String deleteFile(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER)) {
			return "noAuthorized";
		}
		if (fileId == null || fileId.length() <= 0) {
			return "errorParameter";
		}
		final Node file = this.fm.queryById(fileId);
		if (file == null) {
			return "errorParameter";
		}
		if (!this.fbu.deleteFromFileBlocks(file)) {
			return "cannotDeleteFile";
		}
		if (this.fm.deleteById(fileId) > 0) {
			this.lu.writeDeleteFileEvent(request, file);
			return "deleteFileSuccess";
		}
		return "cannotDeleteFile";
	}

	public void doDownloadFile(final HttpServletRequest request, final HttpServletResponse response) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			final String fileId = request.getParameter("fileId");
			if (fileId != null) {
				final Node f = this.fm.queryById(fileId);
				if (f != null) {
					final File fo = this.fbu.getFileFromBlocks(f);
					writeRangeFileStream(request, response, fo, f.getFileName(), CONTENT_TYPE);// 使用断点续传执行下载
					this.lu.writeDownloadFileEvent(request, f);
				}
			}
		}
	}

	public String doRenameFile(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		final String newFileName = request.getParameter("newFileName");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (!ConfigureReader.instance().authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER)) {
			return "noAuthorized";
		}
		if (fileId == null || fileId.length() <= 0 || newFileName == null || newFileName.length() <= 0) {
			return "errorParameter";
		}
		if (!TextFormateUtil.instance().matcherFileName(newFileName)) {
			return "errorParameter";
		}
		final Node file = this.fm.queryById(fileId);
		if (file == null) {
			return "errorParameter";
		}
		final Map<String, String> map = new HashMap<String, String>();
		map.put("fileId", fileId);
		map.put("newFileName", newFileName);
		if (this.fm.updateFileNameById(map) > 0) {
			this.lu.writeRenameFileEvent(request, file, newFileName);
			return "renameFileSuccess";
		}
		return "cannotRenameFile";
	}

	public String deleteCheckedFiles(final HttpServletRequest request) {
		final String strIdList = request.getParameter("strIdList");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER)) {
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				for (final String fileId : idList) {
					if (fileId == null || fileId.length() <= 0) {
						return "errorParameter";
					}
					final Node file = this.fm.queryById(fileId);
					if (file == null) {
						return "errorParameter";
					}
					if (!this.fbu.deleteFromFileBlocks(file)) {
						return "cannotDeleteFile";
					}
					if (this.fm.deleteById(fileId) <= 0) {
						return "cannotDeleteFile";
					}
					this.lu.writeDeleteFileEvent(request, file);
				}
				return "deleteFileSuccess";
			} catch (Exception e) {
				return "errorParameter";
			}
		}
		return "noAuthorized";
	}

	public String downloadCheckedFiles(final HttpServletRequest request) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			final String strIdList = request.getParameter("strIdList");
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				if (idList.size() > 0) {
					final String fileBlocks = ConfigureReader.instance().getFileBlockPath();
					final String tfPath = ConfigureReader.instance().getTemporaryfilePath();
					final String zipname = this.fbu.createZip(idList, tfPath, fileBlocks);
					this.lu.writeDownloadCheckedFileEvent(request, idList);
					return zipname;
				}
			} catch (Exception ex) {
			}
		}
		return "ERROR";
	}

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
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
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

	@Override
	public String doMoveFiles(HttpServletRequest request) {
		// TODO 自动生成的方法存根
		final String strIdList = request.getParameter("strIdList");
		final String locationpath = request.getParameter("locationpath");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES)) {
			try {
				final List<String> idList = gson.fromJson(strIdList, new TypeToken<List<String>>() {
				}.getType());
				for (final String fileId : idList) {
					if (fileId == null || fileId.length() <= 0) {
						return "errorParameter";
					}
					final Node node = this.fm.queryById(fileId);
					if (node == null) {
						return "errorParameter";
					}
					Map<String, String> map = new HashMap<>();
					map.put("fileId", fileId);
					map.put("locationpath", locationpath);
					if (this.fm.moveById(map) <= 0) {
						return "cannotMoveFiles";
					}
					this.lu.writeMoveFileEvent(request, node, locationpath);
				}
				return "moveFilesSuccess";
			} catch (Exception e) {
				return "errorParameter";
			}
		}
		return "noAuthorized";
	}

}
