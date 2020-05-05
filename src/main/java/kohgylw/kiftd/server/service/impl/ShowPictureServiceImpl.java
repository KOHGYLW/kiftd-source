package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;

import com.google.gson.Gson;

import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import javax.servlet.http.*;
import kohgylw.kiftd.server.pojo.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import kohgylw.kiftd.server.util.*;
import net.coobird.thumbnailator.Thumbnails;

@Service
public class ShowPictureServiceImpl implements ShowPictureService {
	@Resource
	private NodeMapper fm;
	@Resource
	private Gson gson;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private FolderUtil fu;
	@Resource
	private FolderMapper flm;
	@Resource
	private LogUtil lu;

	/**
	 * 
	 * <h2>获取所有同级目录下的图片并封装为PictureViewList对象</h2>
	 * <p>
	 * 该方法用于根据请求获取预览图片列表并进行封装，对于过大图片会进行压缩。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request
	 *            HttpServletRequest 请求对象，需包含fileId字段（需预览的图片ID）。
	 * @return PictureViewList 预览列表封装对象，详见其注释。
	 * @see kohgylw.kiftd.server.pojo.PictureViewList
	 */
	private PictureViewList foundPictures(final HttpServletRequest request) {
		final String fileId = request.getParameter("fileId");
		if (fileId != null && fileId.length() > 0) {
			final String account = (String) request.getSession().getAttribute("ACCOUNT");
			Node p = fm.queryById(fileId);
			if (p != null) {
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(p.getFileParentFolder()))
						&& ConfigureReader.instance().accessFolder(flm.queryById(p.getFileParentFolder()), account)) {
					final List<Node> nodes = this.fm.queryBySomeFolder(fileId);
					final List<PictureInfo> pictureViewList = new ArrayList<>();
					int index = 0;
					for (final Node n : nodes) {
						final String fileName = n.getFileName();
						final String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
						switch (suffix) {
						case "jpg":
						case "jpeg":
						case "bmp":
						case "png":
						case "gif":
							// 对于静态图片格式，如果体积超过2 MB则要进行压缩处理，以加快加载速度
							PictureInfo pi = new PictureInfo();
							pi.setFileName(fileName);
							int pSize = Integer.parseInt(n.getFileSize());
							File block = fbu.getFileFromBlocks(n);
							long lastModified = block.lastModified();// 尽可能地让覆盖后的图片也能立即更新
							if (pSize > 1 && !suffix.equals("gif")) {
								pi.setUrl("homeController/showCondensedPicture.do?fileId=" + n.getFileId()
										+ "&lastmodified=" + lastModified);
							} else {
								pi.setUrl("resourceController/getResource/" + n.getFileId() + "?lastmodified=" + lastModified);
							}
							pictureViewList.add(pi);
							if (n.getFileId().equals(fileId)) {
								index = pictureViewList.size() - 1;// 如果是正要预览的图片，记录位置
							}
							break;
						default:
							break;
						}
					}
					final PictureViewList pvl = new PictureViewList();
					pvl.setIndex(index);
					pvl.setPictureViewList(pictureViewList);
					return pvl;
				}
			}
		}
		return null;
	}

	public String getPreviewPictureJson(final HttpServletRequest request) {
		final PictureViewList pvl = this.foundPictures(request);
		if (pvl != null) {
			return gson.toJson((Object) pvl);
		}
		return "ERROR";
	}

	@Override
	public void getCondensedPicture(final HttpServletRequest request, final HttpServletResponse response) {
		// TODO 自动生成的方法存根
		String fileId = request.getParameter("fileId");
		String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (fileId != null) {
			Node node = fm.queryById(fileId);
			if (node != null) {
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
						fu.getAllFoldersId(node.getFileParentFolder()))
						&& ConfigureReader.instance().accessFolder(flm.queryById(node.getFileParentFolder()),
								account)) {
					File pBlock = fbu.getFileFromBlocks(node);
					if (pBlock != null && pBlock.exists()) {
						try {
							int pSize = Integer.parseInt(node.getFileSize());
							String format = "JPG";// 压缩后的格式
							if (pSize < 3) {
								Thumbnails.of(pBlock).size(1080, 1080).outputFormat(format)
										.toOutputStream(response.getOutputStream());
							} else if (pSize < 5) {
								Thumbnails.of(pBlock).size(1440, 1440).outputFormat(format)
										.toOutputStream(response.getOutputStream());
							} else {
								Thumbnails.of(pBlock).size(1680, 1680).outputFormat(format)
										.toOutputStream(response.getOutputStream());
							}
						} catch (IOException e) {
							// 压缩失败时，尝试以源文件进行预览
							try {
								Files.copy(pBlock.toPath(), response.getOutputStream());
							} catch (IOException e1) {
								lu.writeException(e1);
							}
						}
					}
				}
			}
		}
	}
}
