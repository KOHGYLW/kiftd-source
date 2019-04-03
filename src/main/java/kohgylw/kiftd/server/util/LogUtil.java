package kohgylw.kiftd.server.util;

import org.springframework.stereotype.*;
import javax.annotation.*;
import kohgylw.kiftd.server.mapper.*;
import kohgylw.kiftd.server.enumeration.*;
import javax.servlet.http.*;
import java.util.*;
import kohgylw.kiftd.server.model.*;

import java.io.*;

@Component
public class LogUtil {

	@Resource
	private FolderUtil fu;
	@Resource
	private FolderMapper fm;
	@Resource
	private NodeMapper fim;

	private String sep = "";
	private String logs = "";

	public LogUtil() {
		sep = File.separator;
		logs = ConfigureReader.instance().getPath() + sep + "logs";
		File l = new File(logs);
		if (!l.exists()) {
			l.mkdir();
		} else {
			if (!l.isDirectory()) {
				l.delete();
				l.mkdir();
			}
		}
	}

	/**
	 * 以格式化记录异常信息
	 * <p>
	 * 创建日志文件并写入异常信息，当同日期的日志文件存在时，则在其后面追加该信息
	 * </p>
	 * 
	 * @param e
	 *            Exception 需要记录的异常对象
	 */
	public void writeException(Exception e) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Runtime_Exception)) {
			writeToLog("Exception", "[" + e + "]:" + e.getMessage());
		}
	}

	/**
	 * 以格式化记录新建文件夹日志
	 * <p>
	 * 写入新建文件夹信息，包括操作者、路劲及新文件夹名称
	 * </p>
	 */
	public void writeCreateFolderEvent(HttpServletRequest request, Folder f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;// 方便下方使用终态操作
			Thread t = new Thread(() -> {
				List<Folder> l = fu.getParentList(f.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Create new folder]\r\n>PATH [" + pl + "]\r\n>NAME ["
						+ f.getFolderName() + "]，CONSTRAINT [" + f.getFolderConstraint() + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}

	/**
	 * 以格式化记录重命名文件夹日志
	 * <p>
	 * 写入重命名文件夹信息
	 * </p>
	 */
	public void writeRenameFolderEvent(HttpServletRequest request, Folder f, String newName, String newConstraint) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				List<Folder> l = fu.getParentList(f.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Edit folder]\r\n>PATH [" + pl + "]\r\n>NAME ["
						+ f.getFolderName() + "]->[" + newName + "]，CONSTRAINT [" + f.getFolderConstraint() + "]->["
						+ newConstraint + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}

	/**
	 * 以格式化记录删除文件夹日志
	 * <p>
	 * 写入删除文件夹信息
	 * </p>
	 */
	public void writeDeleteFolderEvent(HttpServletRequest request, Folder f, List<Folder> l) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Delete folder]\r\n>PATH [" + pl + "]\r\n>NAME ["
						+ f.getFolderName() + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}

	/**
	 * 以格式化记录删除文件日志
	 * <p>
	 * 写入删除文件信息
	 * </p>
	 */
	public void writeDeleteFileEvent(HttpServletRequest request, Node f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Delete file]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}

	/**
	 * 以格式化记录上传文件日志
	 * <p>
	 * 写入上传文件信息
	 * </p>
	 */
	public void writeUploadFileEvent(Node f, String account) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Upload file]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}

	/**
	 * 以格式化记录下载文件日志
	 * <p>
	 * 写入下载文件信息
	 * </p>
	 */
	public void writeDownloadFileEvent(HttpServletRequest request, Node f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Download file]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}
	
	/**
	 * 
	 * <h2>简述</h2>
	 * <p>详细描述</p>
	 * @author 青阳龙野(kohgylw)
	 * @param xxx 参数描述
	 * @return xxx
	 */
	public void writeDownloadFileByKeyEvent(Node f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			Thread t = new Thread(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">OPERATE [Download file By Shared URL]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}
	
	/**
	 * 
	 * <h2>记录分享下载链接事件</h2>
	 * <p>当用户试图获取一个资源的下载链接时，记录此事件。</p>
	 * @author 青阳龙野(kohgylw)
	 */
	public void writeShareFileURLEvent(HttpServletRequest request, Node f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Share Download file URL]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}

	/**
	 * 以格式化记录重命名文件日志
	 * <p>
	 * 写入重命名文件信息
	 * </p>
	 */
	public void writeRenameFileEvent(HttpServletRequest request, Node f, String newName) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Rename file]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]->[" + newName + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}

	/**
	 * 
	 * <h2>日志记录：移动文件</h2>
	 * <p>
	 * 记录移动文件操作，谁、在什么时候、将哪个文件移动到哪。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request
	 *            HttpServletRequest 请求对象
	 * @param f
	 *            Node 被移动的文件节点
	 * @param locationpath
	 *            String 被移动到的位置
	 */
	public void writeMoveFileEvent(HttpServletRequest request, Node f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Move file]\r\n>NEW PATH [" + pl
						+ folder.getFolderName() + "/" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}

	public void writeMoveFileEvent(HttpServletRequest request, Folder f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				Folder folder = fm.queryById(f.getFolderParent());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">ACCOUNT [" + a + "]\r\n>OPERATE [Move Folder]\r\n>NEW PATH [" + pl
						+ folder.getFolderName() + "/" + f.getFolderName() + "]";
				writeToLog("Event", content);
			});
			t.start();
		}
	}

	private void writeToLog(String type, String content) {
		String t = ServerTimeUtil.accurateToLogName();
		File f = new File(logs, t + ".klog");
		FileWriter fw = null;
		if (f.exists()) {
			try {
				fw = new FileWriter(f, true);
				fw.write("\r\n\r\nTIME:\r\n" + ServerTimeUtil.accurateToSecond() + "\r\nTYPE:\r\n" + type
						+ "\r\nCONTENT:\r\n" + content);
				fw.close();
			} catch (Exception e1) {
				System.out.println("KohgylwIFT:[Log]Cannt write to file,message:" + e1.getMessage());
			}
		} else {
			try {
				fw = new FileWriter(f, false);
				fw.write("TIME:\r\n" + ServerTimeUtil.accurateToSecond() + "\r\nTYPE:\r\n" + type + "\r\nCONTENT:\r\n"
						+ content);
				fw.close();
			} catch (IOException e1) {
				System.out.println("KohgylwIFT:[Log]Cannt write to file,message:" + e1.getMessage());
			}
		}
	}

	/**
	 * 以格式化记录下载文件日志
	 * <p>
	 * 写入下载文件信息
	 * </p>
	 */
	public void writeDownloadCheckedFileEvent(HttpServletRequest request, List<String> idList) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			Thread t = new Thread(() -> {
				StringBuffer content = new StringBuffer(
						">ACCOUNT [" + a + "]\r\n>OPERATE [Download checked file]\r\n----------------\r\n");
				for (String fid : idList) {
					Node f = fim.queryById(fid);
					if (f != null) {
						Folder folder = fm.queryById(f.getFileParentFolder());
						List<Folder> l = fu.getParentList(folder.getFolderId());
						String pl = new String();
						for (Folder i : l) {
							pl = pl + i.getFolderName() + "/";
						}
						content.append(
								">PATH [" + pl + folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]\r\n");
					}
				}
				content.append("----------------");
				writeToLog("Event", content.toString());
			});
			t.start();
		}

	}

}
