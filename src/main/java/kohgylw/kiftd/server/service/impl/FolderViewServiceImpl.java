package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;
import javax.annotation.*;
import kohgylw.kiftd.server.mapper.*;
import kohgylw.kiftd.server.model.Folder;

import javax.servlet.http.*;
import kohgylw.kiftd.server.pojo.*;
import java.util.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.util.*;
import com.google.gson.*;

@Service
public class FolderViewServiceImpl implements FolderViewService {
	@Resource
	private FolderUtil fu;
	@Resource
	private FolderMapper fm;
	@Resource
	private NodeMapper flm;
	@Resource
	private Gson gson;

	@Override
	public String getFolderViewToJson(final String fid, final HttpSession session, final HttpServletRequest request) {
		final ConfigureReader cr = ConfigureReader.instance();
		Folder vf=this.fm.queryById(fid);
		final String account = (String) session.getAttribute("ACCOUNT");
		//检查访问文件夹视图请求是否合法
		if(!ConfigureReader.instance().accessFolder(vf, account)) {
			return "notAccess";//如无访问权限则直接返回该字段，令页面回到ROOT视图。
		}
		final FolderView fv = new FolderView();
		fv.setFolder(vf);
		fv.setParentList(this.fu.getParentList(fid));
		List<Folder> fs=new LinkedList<>();
		for(Folder f:this.fm.queryByParentId(fid)) {
			if(ConfigureReader.instance().accessFolder(f, account)) {
				fs.add(f);
			}
		}
		fv.setFolderList(fs);
		fv.setFileList(this.flm.queryByParentFolderId(fid));
		if (account != null) {
			fv.setAccount(account);
		}
		final List<String> authList = new ArrayList<String>();
		if (cr.authorized(account, AccountAuth.UPLOAD_FILES)) {
			authList.add("U");
		}
		if (cr.authorized(account, AccountAuth.CREATE_NEW_FOLDER)) {
			authList.add("C");
		}
		if (cr.authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER)) {
			authList.add("D");
		}
		if (cr.authorized(account, AccountAuth.RENAME_FILE_OR_FOLDER)) {
			authList.add("R");
		}
		if (cr.authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			authList.add("L");
		}
		if (cr.authorized(account, AccountAuth.MOVE_FILES)) {
			authList.add("M");
		}
		fv.setAuthList(authList);
		fv.setPublishTime(ServerTimeUtil.accurateToMinute());
		return gson.toJson(fv);
	}
}
