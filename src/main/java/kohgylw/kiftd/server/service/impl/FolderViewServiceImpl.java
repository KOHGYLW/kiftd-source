package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;
import javax.annotation.*;
import kohgylw.kiftd.server.mapper.*;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;

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
		if (fid == null || fid.length() == 0) {
			return "ERROR";
		}
		Folder vf = this.fm.queryById(fid);
		final String account = (String) session.getAttribute("ACCOUNT");
		// 检查访问文件夹视图请求是否合法
		if (!ConfigureReader.instance().accessFolder(vf, account)) {
			return "notAccess";// 如无访问权限则直接返回该字段，令页面回到ROOT视图。
		}
		final FolderView fv = new FolderView();
		fv.setFolder(vf);
		fv.setParentList(this.fu.getParentList(fid));
		List<Folder> fs = new LinkedList<>();
		for (Folder f : this.fm.queryByParentId(fid)) {
			if (ConfigureReader.instance().accessFolder(f, account)) {
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

	@Override
	public String getSreachViewToJson(HttpServletRequest request) {
		final ConfigureReader cr = ConfigureReader.instance();
		String fid = request.getParameter("fid");
		String keyWorld = request.getParameter("keyworld");
		if (fid == null || fid.length() == 0 || keyWorld == null) {
			return "ERROR";
		}
		//如果啥么也不查，那么直接返回指定文件夹标准视图
		if(keyWorld.length() == 0) {
			return getFolderViewToJson(fid, request.getSession(), request);
		}
		Folder vf = this.fm.queryById(fid);
		final String account = (String) request.getSession().getAttribute("ACCOUNT");
		// 检查访问文件夹视图请求是否合法
		if (!ConfigureReader.instance().accessFolder(vf, account)) {
			return "notAccess";// 如无访问权限则直接返回该字段，令页面回到ROOT视图。
		}
		final SreachView sv = new SreachView();
		// 先准备搜索视图的文件夹信息
		Folder sf = new Folder();
		sf.setFolderId(vf.getFolderId());// 搜索视图的主键设置与搜索路径一致
		sf.setFolderName("在“" + vf.getFolderName() + "”内搜索“" + keyWorld + "”的结果...");// 名称就是搜索的描述
		sf.setFolderParent(vf.getFolderId());// 搜索视图的父级也与搜索路径一致
		sf.setFolderCreator("--");// 搜索视图是虚拟的，没有这些
		sf.setFolderCreationDate("--");
		sf.setFolderConstraint(vf.getFolderConstraint());// 其访问等级也与搜索路径一致
		sv.setFolder(sf);// 搜索视图的文件夹信息已经准备就绪
		// 设置上级路径为搜索路径
		List<Folder> pl = this.fu.getParentList(fid);
		pl.add(vf);
		sv.setParentList(pl);
		// 设置所有搜索到的文件夹和文件，该方法迭查找：
		List<Node> ns = new LinkedList<>();
		List<Folder> fs = new LinkedList<>();
		sreachFilesAndFolders(fid, keyWorld, account, ns, fs);
		sv.setFileList(ns);
		sv.setFolderList(fs);
		// 账户视图与文件夹相同
		if (account != null) {
			sv.setAccount(account);
		}
		// 设置操作权限，对于搜索视图而言，只能进行下载操作（因为是虚拟的）
		final List<String> authList = new ArrayList<String>();
		//搜索结果只接受“下载”操作
		if (cr.authorized(account, AccountAuth.DOWNLOAD_FILES)) {
			authList.add("L");
		}
		//同时额外具备普通文件夹没有的“定位”功能。
		authList.add("O");
		sv.setAuthList(authList);
		// 写入实时系统时间
		sv.setPublishTime(ServerTimeUtil.accurateToMinute());
		//设置查询字段
		sv.setKeyWorld(keyWorld);
		return gson.toJson(sv);
	}

	// 迭代查找所有匹配项，参数分别是：从哪找、找啥、谁要找、添加的前缀是啥（便于分辨不同路径下的同名文件）、找到的文件放哪、找到的文件夹放哪
	private void sreachFilesAndFolders(String fid, String key, String account, List<Node> ns, List<Folder> fs) {
		for (Folder f : this.fm.queryByParentId(fid)) {
			if (ConfigureReader.instance().accessFolder(f, account)) {
				if (f.getFolderName().indexOf(key) >= 0) {
					f.setFolderName(f.getFolderName());
					fs.add(f);
				}
				sreachFilesAndFolders(f.getFolderId(), key, account, ns, fs);
			}
		}
		for (Node n : this.flm.queryByParentFolderId(fid)) {
			if (n.getFileName().indexOf(key) >= 0) {
				n.setFileName(n.getFileName());
				ns.add(n);
			}
		}
	}
}
