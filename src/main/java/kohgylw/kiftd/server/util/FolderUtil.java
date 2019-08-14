package kohgylw.kiftd.server.util;

import org.springframework.stereotype.*;
import javax.annotation.*;

import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.mapper.*;
import kohgylw.kiftd.server.model.*;
import java.util.*;

@Component
public class FolderUtil {
	@Resource
	private FolderMapper fm;
	@Resource
	private NodeMapper fim;
	@Resource
	private FileBlockUtil fbu;

	public List<Folder> getParentList(final String fid) {
		Folder f = this.fm.queryById(fid);
		final List<Folder> folderList = new ArrayList<Folder>();
		if (f != null) {
			while (!f.getFolderParent().equals("null")) {
				f = this.fm.queryById(f.getFolderParent());
				folderList.add(f);
			}
		}
		Collections.reverse(folderList);
		return folderList;
	}

	public int deleteAllChildFolder(final String folderId) {
		final Thread deleteChildFolderThread = new Thread(() -> this.iterationDeleteFolder(folderId));
		deleteChildFolderThread.start();
		return this.fm.deleteById(folderId);
	}

	private void iterationDeleteFolder(final String folderId) {
		final List<Folder> cf = (List<Folder>) this.fm.queryByParentId(folderId);
		if (cf.size() > 0) {
			for (final Folder f : cf) {
				this.iterationDeleteFolder(f.getFolderId());
			}
		}
		final List<Node> files = (List<Node>) this.fim.queryByParentFolderId(folderId);
		if (files.size() > 0) {
			this.fim.deleteByParentFolderId(folderId);
			for (final Node f2 : files) {
				this.fbu.deleteFromFileBlocks(f2);
			}
		}
		this.fm.deleteById(folderId);
	}
	
	public Folder createNewFolder(final String parentId,String account,String folderName,String folderConstraint) {
		if (!ConfigureReader.instance().authorized(account, AccountAuth.CREATE_NEW_FOLDER)) {
			return null;
		}
		if (parentId == null || folderName == null || parentId.length() <= 0 || folderName.length() <= 0) {
			return null;
		}
		if (!TextFormateUtil.instance().matcherFolderName(folderName) || folderName.indexOf(".") == 0) {
			return null;
		}
		final Folder parentFolder = this.fm.queryById(parentId);
		if (parentFolder == null) {
			return null;
		}
		if (fm.queryByParentId(parentId).parallelStream().anyMatch((e) -> e.getFolderName().equals(folderName))) {
			return null;
		}
		Folder f = new Folder();
		// 设置子文件夹约束等级，不允许子文件夹的约束等级比父文件夹低
		int pc = parentFolder.getFolderConstraint();
		if (folderConstraint != null) {
			try {
				int ifc = Integer.parseInt(folderConstraint);
				if (ifc > 0 && account == null) {
					return null;
				}
				if (ifc < pc) {
					return null;
				} else {
					f.setFolderConstraint(ifc);
				}
			} catch (Exception e) {
				// TODO: handle exception
				return null;
			}
		} else {
			return null;
		}
		f.setFolderId(UUID.randomUUID().toString());
		f.setFolderName(folderName);
		f.setFolderCreationDate(ServerTimeUtil.accurateToDay());
		if (account != null) {
			f.setFolderCreator(account);
		} else {
			f.setFolderCreator("匿名用户");
		}
		f.setFolderParent(parentId);
		int i = 0;
		while (true) {
			try {
				final int r = this.fm.insertNewFolder(f);
				if (r > 0) {
					return f;
				}
				break;
			} catch (Exception e) {
				f.setFolderId(UUID.randomUUID().toString());
				i++;
			}
			if (i >= 10) {
				break;
			}
		}
		return null;
	}
}
