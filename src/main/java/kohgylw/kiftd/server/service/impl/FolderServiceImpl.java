package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;
import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import javax.servlet.http.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.util.*;
import java.util.*;

@Service
public class FolderServiceImpl implements FolderService {
	@Resource
	private FolderMapper fm;
	@Resource
	private FolderUtil fu;
	@Resource
	private LogUtil lu;

	public String newFolder(final HttpServletRequest request) {
		final String parentId = request.getParameter("parentId");
		final String folderName = request.getParameter("folderName");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (!ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER)) {
			return "noAuthorized";
		}
		if (parentId == null || folderName == null || parentId.length() <= 0 || folderName.length() <= 0) {
			return "errorParameter";
		}
		if (!TextFormateUtil.instance().matcherFolderName(folderName)) {
			return "errorParameter";
		}
		final Folder parentFolder = this.fm.queryById(parentId);
		if (parentFolder == null) {
			return "errorParameter";
		}
		final Map<String, String> map = new HashMap<String, String>();
		map.put("parentId", parentId);
		map.put("folderName", folderName);
		Folder f = this.fm.queryByParentIdAndFolderName(map);
		if (f != null) {
			return "folderAlreadyExist";
		}
		f = new Folder();
		f.setFolderId(UUID.randomUUID().toString());
		f.setFolderName(folderName);
		f.setFolderCreationDate(ServerTimeUtil.accurateToDay());
		if (account != null) {
			f.setFolderCreator(account);
		} else {
			f.setFolderCreator("\u533f\u540d\u7528\u6237");
		}
		f.setFolderParent(parentId);
		final int i = this.fm.insertNewFolder(f);
		if (i > 0) {
			this.lu.writeCreateFolderEvent(request, f);
			return "createFolderSuccess";
		}
		return "cannotCreateFolder";
	}

	public String deleteFolder(final HttpServletRequest request) {
		final String folderId = request.getParameter("folderId");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (!ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER)) {
			return "noAuthorized";
		}
		if (folderId == null || folderId.length() <= 0) {
			return "errorParameter";
		}
		final Folder folder = this.fm.queryById(folderId);
		if (folder == null) {
			return "errorParameter";
		}
		final List<Folder> l = this.fu.getParentList(folderId);
		if (this.fu.deleteAllChildFolder(request, folderId) > 0) {
			this.lu.writeDeleteFolderEvent(request, folder, l);
			return "deleteFolderSuccess";
		}
		return "cannotDeleteFolder";
	}

	public String renameFolder(final HttpServletRequest request) {
		final String folderId = request.getParameter("folderId");
		final String newName = request.getParameter("newName");
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		if (!ConfigureReader.instance().authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER)) {
			return "noAuthorized";
		}
		if (folderId == null || folderId.length() <= 0 || newName == null || newName.length() <= 0) {
			return "errorParameter";
		}
		if (!TextFormateUtil.instance().matcherFolderName(newName)) {
			return "errorParameter";
		}
		final Folder folder = this.fm.queryById(folderId);
		if (folder == null) {
			return "errorParameter";
		}
		final Map<String, String> map = new HashMap<String, String>();
		map.put("folderId", folderId);
		map.put("newName", newName);
		if (this.fm.updateFolderNameById(map) > 0) {
			this.lu.writeRenameFolderEvent(request, folder, newName);
			return "renameFolderSuccess";
		}
		return "cannotRenameFolder";
	}
}
