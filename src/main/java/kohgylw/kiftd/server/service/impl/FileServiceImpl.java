package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;
import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.model.*;
import org.springframework.web.multipart.*;
import javax.servlet.http.*;
import java.net.*;
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
public class FileServiceImpl implements FileService {
	@Resource
	private NodeMapper fm;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private LogUtil lu;
	@Resource
	private Gson gson;

	public String checkUploadFile(final HttpServletRequest request) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String namelist = request.getParameter("namelist");
		final List<String> namelistObj = gson.fromJson(namelist, new TypeToken<List<String>>() {
		}.getType());
		for (final String filename : namelistObj) {
			if (folderId == null || folderId.length() <= 0 || filename == null || filename.length() <= 0) {
				return "errorParameter";
			}
			if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES)) {
				return "noAuthorized";
			}
			final List<Node> files = this.fm.queryByParentFolderId(folderId);
			boolean duplication = false;
			for (final Node f : files) {
				if (f.getFileName().equals(filename)) {
					duplication = true;
				}
			}
			if (!duplication) {
				continue;
			}
			return "duplicationFileName:" + filename;
		}
		return "permitUpload";
	}

	public String doUploadFile(final HttpServletRequest request, final MultipartFile file) {
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		final String folderId = request.getParameter("folderId");
		final String filename = file.getOriginalFilename();
		if (folderId == null || folderId.length() <= 0 || filename == null || filename.length() <= 0) {
			return "uploaderror";
		}
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES)) {
			return "uploaderror";
		}
		final List<Node> files = this.fm.queryByParentFolderId(folderId);
		boolean duplication = false;
		for (final Node f : files) {
			if (f.getFileName().equals(filename)) {
				duplication = true;
			}
		}
		if (duplication) {
			return "uploaderror";
		}
		final String path = this.fbu.saveToFileBlocks(request, file);
		final String fsize = this.fbu.getFileSize(file);
		if (path.equals("ERROR")) {
			return "uploaderror";
		}
		final Node f2 = new Node();
		f2.setFileId(UUID.randomUUID().toString());
		if (account != null) {
			f2.setFileCreator(account);
		} else {
			f2.setFileCreator("\u533f\u540d\u7528\u6237");
		}
		f2.setFileCreationDate(ServerTimeUtil.accurateToDay());
		f2.setFileName(filename);
		f2.setFileParentFolder(folderId);
		f2.setFilePath(path);
		f2.setFileSize(fsize);
		if (this.fm.insert(f2) > 0) {
			this.lu.writeUploadFileEvent(request, f2);
			return "uploadsuccess";
		}
		return "uploaderror";
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
		final String fileblocks = ConfigureReader.instance().getFileBlockPath();
		if (!this.fbu.deleteFromFileBlocks(fileblocks, file.getFilePath())) {
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
					final String fileBlocks = ConfigureReader.instance().getFileBlockPath();
					final File fo = this.fbu.getFileFromBlocks(fileBlocks, f.getFilePath());
					downloadRangeFile(request, response, fo, f.getFileName());// 使用断点续传执行下载
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
					final String fileblocks = ConfigureReader.instance().getFileBlockPath();
					if (!this.fbu.deleteFromFileBlocks(fileblocks, file.getFilePath())) {
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
			String fname = URLEncoder
					.encode("kiftd_" + ServerTimeUtil.accurateToDay() + "_\u6253\u5305\u4e0b\u8f7d.zip", "UTF-8");
			if (zip.exists()) {
				downloadRangeFile(request, response, zip, fname);
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

	// 使用断点续传技术提供文件下载服务
	private void downloadRangeFile(HttpServletRequest request, HttpServletResponse response, File fo, String fname) {
		long skipLength = 0;// 下载时跳过的字节数
		long downLength = 0;// 需要继续下载的字节数
		boolean hasEnd = false;// 是否具备结束字节声明
		try {
			response.setHeader("Accept-Ranges", "bytes");// 支持断点续传声明
			// 获取已下载字节数和需下载字节数
			String rangeLabel = request.getHeader("Range");// 获取下载长度声明
			if (null != rangeLabel) {
				// 当进行断点续传时，返回响应码206
				response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
				// 解析下载跳过长度和继续长度
				rangeLabel = request.getHeader("Range").replaceAll("bytes=", "");
				if (rangeLabel.indexOf('-') == rangeLabel.length() - 1) {
					hasEnd = false;
					rangeLabel = rangeLabel.substring(0, rangeLabel.indexOf('-'));
					skipLength = Long.parseLong(rangeLabel.trim());
				} else {
					hasEnd = true;
					String startBytes = rangeLabel.substring(0, rangeLabel.indexOf('-'));
					String endBytes = rangeLabel.substring(rangeLabel.indexOf('-') + 1, rangeLabel.length());
					skipLength = Long.parseLong(startBytes.trim());
					downLength = Long.parseLong(endBytes);
				}
			}
			// 设置响应中文件块声明
			long fileLength = fo.length();// 文件长度
			if (0 != skipLength) {
				String contentRange = "";
				if (hasEnd) {
					contentRange = new StringBuffer(rangeLabel).append("/").append(new Long(fileLength).toString())
							.toString();
				} else {
					contentRange = new StringBuffer("bytes").append(new Long(skipLength).toString()).append("-")
							.append(new Long(fileLength - 1).toString()).append("/")
							.append(new Long(fileLength).toString()).toString();
				}
				response.setHeader("Content-Range", contentRange);
			}
			// 开始执行下载
			response.setContentType("application/force-download");
			response.setHeader("Content-Length", "" + fileLength);
			response.addHeader("Content-Disposition", "attachment;fileName=" + URLEncoder.encode(fname, "UTF-8"));
			final int buffersize = ConfigureReader.instance().getBuffSize();
			final byte[] buffer = new byte[buffersize];
			final RandomAccessFile raf = new RandomAccessFile(fo, "r");
			final OutputStream os = (OutputStream) response.getOutputStream();
			raf.seek(skipLength);// 跳过已经下载的字节数
			if (hasEnd) {
				while (raf.getFilePointer() < downLength) {
					os.write(raf.read());
				}
			} else {
				int index = 0;
				while ((index = raf.read(buffer)) != -1) {
					os.write(buffer, 0, index);
				}
			}
			raf.close();
			os.close();
		} catch (Exception ex) {
		}
	}
}
