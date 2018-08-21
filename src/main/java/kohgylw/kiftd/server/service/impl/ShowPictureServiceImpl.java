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
import java.util.*;
import kohgylw.kiftd.server.util.*;
import net.coobird.thumbnailator.Thumbnails;

@Service
public class ShowPictureServiceImpl implements ShowPictureService {
	@Resource
	private NodeMapper fm;
	@Resource
	private Gson gson;

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
			if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
				final List<Node> nodes = this.fm.queryBySomeFolder(fileId);
				final List<Node> pictureViewList = new ArrayList<Node>();
				int index = 0;
				for (final Node n : nodes) {
					final String fileName = n.getFileName();
					final String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
					if (suffix.equals("jpg") || suffix.equals("jpeg") || suffix.equals("gif") || suffix.equals("bmp")
							|| suffix.equals("png")) {
						int pSize = Integer.parseInt(n.getFileSize());
						if (pSize > 1) {
							String tempPictureName = "temp_" + n.getFileId() + "_"+n.getFileName();
							String tempPicturePath = ConfigureReader.instance().getTemporaryfilePath()
									+ tempPictureName;
							if (!new File(tempPicturePath).exists()) {
								String nPath = ConfigureReader.instance().getFileBlockPath() + n.getFilePath();
								try {
									if (pSize < 3) {
										Thumbnails.of(nPath).scale(0.6).toFile(tempPicturePath);
									} else if(pSize<5){
										Thumbnails.of(nPath).scale(0.4).toFile(tempPicturePath);
									}else {
										Thumbnails.of(nPath).scale(0.4).outputQuality(0.6).toFile(tempPicturePath);
									}
									n.setFilePath(tempPictureName);
								} catch (IOException e) {
									// TODO 自动生成的 catch 块
								}
							}else {
								n.setFilePath(tempPictureName);
							}
						}
						pictureViewList.add(n);
						if (!n.getFileId().equals(fileId)) {
							continue;
						}
						index = pictureViewList.size() - 1;
					}
				}
				final PictureViewList pvl = new PictureViewList();
				pvl.setIndex(index);
				pvl.setPictureViewList(pictureViewList);
				return pvl;
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
}
