package kohgylw.kiftd.server.util;

import org.springframework.stereotype.*;
import javax.annotation.*;
import kohgylw.kiftd.server.mapper.*;
import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kohgylw.kiftd.server.model.*;

import java.io.*;

/**
 * 
 * <h2>日志生成工具</h2>
 * <p>
 * 该工具用于生成日志文件并在其中添加标准化日志。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class LogUtil {

	@Resource
	private FolderUtil fu;
	@Resource
	private FolderMapper fm;
	@Resource
	private NodeMapper fim;
	@Resource
	private IpAddrGetter idg;
	@Resource
	private FileBlockUtil fbu;

	private ExecutorService writerThread;
	private FileWriter writer;
	private String logName;

	private String sep = "";
	private String logs = "";

	public LogUtil() {
		sep = File.separator;
		logs = ConfigureReader.instance().getPath() + sep + "logs";
		writerThread = Executors.newSingleThreadExecutor();
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
	 * @param e Exception 需要记录的异常对象
	 */
	public void writeException(Exception e) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Runtime_Exception)) {
			StringBuffer exceptionInfo = new StringBuffer(e.toString());
			StackTraceElement[] stes = e.getStackTrace();
			for (int i = 0; i < stes.length && i < 10; i++) {
				StackTraceElement ste = stes[i];
				exceptionInfo.append("\r\n	at " + ste.getClassName() + "." + ste.getMethodName() + "("
						+ ste.getFileName() + ":" + ste.getLineNumber() + ")");
			}
			if (stes.length > 10) {
				exceptionInfo.append("\r\n......");
			}
			writeToLog("Exception", exceptionInfo.toString());
		}
	}

	/**
	 * 以格式化记录新建文件夹日志
	 * <p>
	 * 写入新建文件夹信息，包括操作者、路劲及新文件夹名称
	 * </p>
	 */
	public void writeCreateFolderEvent(String account, String ip, Folder f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;// 方便下方使用终态操作
			writerThread.execute(() -> {
				List<Folder> l = fu.getParentList(f.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a + "]\r\n>OPERATE [Create new folder]\r\n>PATH ["
						+ pl + "]\r\n>NAME [" + f.getFolderName() + "],CONSTRAINT [" + f.getFolderConstraint() + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 以格式化记录重命名文件夹日志
	 * <p>
	 * 写入重命名文件夹信息
	 * </p>
	 */
	public void writeRenameFolderEvent(String account, String ip, String folderId, String oldName, String newName,
			String oldConstraint, String newConstraint) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			writerThread.execute(() -> {
				List<Folder> l = fu.getParentList(folderId);
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a + "]\r\n>OPERATE [Edit folder]\r\n>PATH [" + pl
						+ "]\r\n>NAME [" + oldName + "]->[" + newName + "],CONSTRAINT [" + oldConstraint + "]->["
						+ newConstraint + "]";
				writeToLog("Event", content);
			});
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
			String ip = idg.getIpAddr(request);
			writerThread.execute(() -> {
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a + "]\r\n>OPERATE [Delete folder]\r\n>PATH [" + pl
						+ "]\r\n>NAME [" + f.getFolderName() + "]";
				writeToLog("Event", content);
			});
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
			String ip = idg.getIpAddr(request);
			writerThread.execute(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a + "]\r\n>OPERATE [Delete file]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 以格式化记录上传文件日志
	 * <p>
	 * 写入上传文件信息
	 * </p>
	 */
	public void writeUploadFileEvent(HttpServletRequest request, Node f, String account) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			String ip = idg.getIpAddr(request);
			writerThread.execute(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				if (folder == null) {
					return;
				}
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a + "]\r\n>OPERATE [Upload file]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 以格式化记录下载文件日志
	 * <p>
	 * 写入下载文件信息
	 * </p>
	 */
	public void writeDownloadFileEvent(String account, String ip, Node f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			writerThread.execute(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a + "]\r\n>OPERATE [Download file]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 以格式化记录永久资源链接请求日志
	 * <p>
	 * 写入永久资源链接被请求的信息
	 * </p>
	 */
	public void writeChainEvent(HttpServletRequest request, Node f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String ip = idg.getIpAddr(request);
			writerThread.execute(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>OPERATE [Request Chain]\r\n>PATH [" + pl + folder.getFolderName()
						+ "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 
	 * <h2>记录使用链接下载文件的操作日志</h2>
	 * <p>
	 * 写入一个下载文件日志，该操作由使用外部链接触发。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param f kohgylw.kiftd.server.model.Node 下载目标
	 */
	public void writeDownloadFileByKeyEvent(HttpServletRequest request, Node f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String ip = idg.getIpAddr(request);
			writerThread.execute(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>OPERATE [Download file By Shared URL]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 
	 * <h2>记录分享下载链接事件</h2>
	 * <p>
	 * 当用户试图获取一个资源的下载链接时，记录此事件。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void writeShareFileURLEvent(HttpServletRequest request, Node f) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			String ip = idg.getIpAddr(request);
			writerThread.execute(() -> {
				Folder folder = fm.queryById(f.getFileParentFolder());
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a
						+ "]\r\n>OPERATE [Share Download file URL]\r\n>PATH [" + pl + folder.getFolderName()
						+ "]\r\n>NAME [" + f.getFileName() + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 以格式化记录重命名文件日志
	 * <p>
	 * 写入重命名文件信息
	 * </p>
	 */
	public void writeRenameFileEvent(String account, String ip, String parentFolderId, String oldName, String newName) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			writerThread.execute(() -> {
				Folder folder = fm.queryById(parentFolderId);
				List<Folder> l = fu.getParentList(folder.getFolderId());
				String pl = new String();
				for (Folder i : l) {
					pl = pl + i.getFolderName() + "/";
				}
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a + "]\r\n>OPERATE [Rename file]\r\n>PATH [" + pl
						+ folder.getFolderName() + "]\r\n>NAME [" + oldName + "]->[" + newName + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 
	 * <h2>日志记录：移动/复制文件</h2>
	 * <p>
	 * 记录移动/复制文件操作，谁、在什么时候、将哪个文件移动/复制到哪。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param account    java.lang.String 操作者账户名
	 * @param ip         java.lang.String 操作者IP地址
	 * @param finalPath  java.lang.String 被操作后的节点完整路径
	 * @param originPath java.lang.String 未操作前的节点完整路径
	 * @param isCopy     boolean 是否为复制模式
	 */
	public void writeMoveFileEvent(String account, String ip, String originPath, String finalPath, boolean isCopy) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			writerThread.execute(() -> {
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a + "]\r\n>OPERATE ["
						+ (isCopy ? "Copy file" : "Move file") + "]\r\n>FROM [" + originPath + "]\r\n>TO   ["
						+ finalPath + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 
	 * <h2>日志记录：移动/复制文件夹</h2>
	 * <p>
	 * 记录移动/复制文件夹操作，谁、在什么时候、将哪个文件夹移动/复制到哪。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param account    java.lang.String 操作者账户名
	 * @param ip         java.lang.String 操作者IP地址
	 * @param finalPath  java.lang.String 被操作后的文件夹完整路径
	 * @param originPath java.lang.String 未操作前的文件夹完整路径
	 * @param isCopy     boolean 是否为复制模式
	 */
	public void writeMoveFolderEvent(String account, String ip, String originPath, String finalPath, boolean isCopy) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			writerThread.execute(() -> {
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + a + "]\r\n>OPERATE ["
						+ (isCopy ? "Copy Folder" : "Move Folder") + "]\r\n>FROM [" + originPath + "]\r\n>TO   ["
						+ finalPath + "]";
				writeToLog("Event", content);
			});
		}
	}

	// 将文本信息以格式化标准写入日志文件中
	private void writeToLog(String type, String content) {
		String t = ServerTimeUtil.accurateToLogName();
		String finalContent = "\r\n\r\nTIME:\r\n" + ServerTimeUtil.accurateToSecond() + "\r\nTYPE:\r\n" + type
				+ "\r\nCONTENT:\r\n" + content;
		try {
			if (t.equals(logName) && writer != null) {
				writer.write(finalContent);
				writer.flush();
			} else {
				File f = new File(logs, t + ".klog");
				logName = t;
				if (writer != null) {
					writer.close();
				}
				writer = new FileWriter(f, true);
				writer.write(finalContent);
				writer.flush();
			}
		} catch (Exception e1) {
			if (Printer.instance != null) {
				Printer.instance.print("KohgylwIFT:[Log]Cannt write to file,message:" + e1.getMessage());
			} else {
				System.out.println("KohgylwIFT:[Log]Cannt write to file,message:" + e1.getMessage());
			}
		}
	}

	/**
	 * 以格式化记录打包下载文件日志
	 * <p>
	 * 写入打包下载文件信息
	 * </p>
	 */
	public void writeDownloadCheckedFileEvent(HttpServletRequest request, List<String> idList, List<String> fidList) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String account = (String) request.getSession().getAttribute("ACCOUNT");
			if (account == null || account.length() == 0) {
				account = "Anonymous";
			}
			String a = account;
			String ip = idg.getIpAddr(request);
			writerThread.execute(() -> {
				StringBuffer content = new StringBuffer(">IP [" + ip + "]\r\n>ACCOUNT [" + a
						+ "]\r\n>OPERATE [Download package]\r\n----------------\r\n");
				for (String fid : idList) {
					Node f = fim.queryById(fid);
					if (f != null) {
						content.append(">File [" + fbu.getNodePath(f) + "]\r\n");
					}
				}
				for (String ffid : fidList) {
					Folder fl = fm.queryById(ffid);
					if (fl != null) {
						content.append(">Folder [" + fu.getFolderPath(fl) + "]\r\n");
					}
				}
				content.append("----------------");
				writeToLog("Event", content.toString());
			});
		}
	}

	/**
	 * 以格式化记录账户修改密码日志
	 * <p>
	 * 写入修改密码的信息
	 * </p>
	 */
	public void writeChangePasswordEvent(HttpServletRequest request, String account, String newPassword) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String ip = idg.getIpAddr(request);
			writerThread.execute(() -> {
				String content = ">IP [" + ip + "]\r\n>ACCOUNT [" + account
						+ "]\r\n>OPERATE [Change Password]\r\n>NEW PASSWORD [" + newPassword + "]";
				writeToLog("Event", content);
			});
		}
	}

	/**
	 * 以格式化记录新账户注册日志
	 * <p>
	 * 写入新账户的注册信息
	 * </p>
	 */
	public void writeSignUpEvent(HttpServletRequest request, String account, String password) {
		if (ConfigureReader.instance().inspectLogLevel(LogLevel.Event)) {
			String ip = idg.getIpAddr(request);
			writerThread.execute(() -> {
				String content = ">IP [" + ip + "]\r\n>OPERATE [Sign Up]\r\n>NEW ACCOUNT [" + account
						+ "]\r\n>PASSWORD [" + password + "]";
				writeToLog("Event", content);
			});
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (writer != null) {
			writer.close();
		}
		writerThread.shutdown();
	}

}
