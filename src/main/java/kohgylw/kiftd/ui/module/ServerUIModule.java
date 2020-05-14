package kohgylw.kiftd.ui.module;

import javax.imageio.*;
import java.io.*;
import java.sql.SQLException;
import java.awt.event.*;
import javax.swing.border.*;
import java.awt.*;
import javax.swing.event.*;

import javax.swing.*;
import java.text.*;
import java.util.*;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.ui.callback.*;

public class ServerUIModule extends KiftdDynamicWindow {

	protected static JFrame window;
	private static SystemTray tray;
	private static TrayIcon trayIcon;
	private static JTextArea output;
	private static ServerUIModule instance;
	private static SettingWindow sw;
	private static FSViewer fsv;
	private static OnCloseServer cs;
	private static OnStartServer ss;
	private static GetServerStatus st;
	private static GetServerTime ti;
	private static JButton start;
	private static JButton stop;
	private static JButton resatrt;
	private static JButton setting;
	private static JButton fileIOUtil;
	private static JButton exit;
	private static JLabel serverStatusLab;
	private static JLabel portStatusLab;
	private static JLabel logLevelLab;
	private static JLabel bufferSizeLab;
	private static final String S_STOP = "停止[Stopped]";
	private static final String S_START = "运行[Running]";
	private static final String S_STARTING = "启动中[Starting]...";
	private static final String S_STOPPING = "停止中[Stopping]...";
	protected static final String L_ALL = "记录全部(ALL)";
	protected static final String L_EXCEPTION = "仅异常(EXCEPTION)";
	protected static final String L_NONE = "不记录(NONE)";
	private SimpleDateFormat sdf;
	/**
	 * 窗口原始宽度
	 */
	private final int OriginSize_Width = 300;
	/**
	 * 窗口原始高度
	 */
	private final int OriginSize_Height = 570;
	private static MenuItem filesViewer;

	private ServerUIModule() throws Exception {
		setUIFont();
		(ServerUIModule.window = new JFrame("kiftd-服务器控制台")).setSize(OriginSize_Width, OriginSize_Height);
		ServerUIModule.window.setLocation(100, 100);
		ServerUIModule.window.setResizable(false);
		try {
			ServerUIModule.window.setIconImage(
					ImageIO.read(this.getClass().getResourceAsStream("/kohgylw/kiftd/ui/resource/icon.png")));
		} catch (NullPointerException ex) {
		} catch (IOException ex2) {
		}
		if (SystemTray.isSupported()) {
			ServerUIModule.window.setDefaultCloseOperation(1);
			ServerUIModule.tray = SystemTray.getSystemTray();
			String iconType = "/kohgylw/kiftd/ui/resource/icon_tray.png";
			if (System.getProperty("os.name").toLowerCase().indexOf("window") >= 0) {
				iconType = "/kohgylw/kiftd/ui/resource/icon_tray_w.png";
			}
			(ServerUIModule.trayIcon = new TrayIcon(ImageIO.read(this.getClass().getResourceAsStream(iconType))))
					.setToolTip("青阳网络文件系统-kiftd");
			trayIcon.setImageAutoSize(true);
			final PopupMenu pMenu = new PopupMenu();
			final MenuItem exit = new MenuItem("退出(Exit)");
			filesViewer = new MenuItem("文件...(Files)");
			final MenuItem show = new MenuItem("显示主窗口(Show)");
			trayIcon.addMouseListener(new MouseListener() {

				@Override
				public void mouseReleased(MouseEvent e) {
					// TODO 自动生成的方法存根

				}

				@Override
				public void mousePressed(MouseEvent e) {
					// TODO 自动生成的方法存根

				}

				@Override
				public void mouseExited(MouseEvent e) {
					// TODO 自动生成的方法存根

				}

				@Override
				public void mouseEntered(MouseEvent e) {
					// TODO 自动生成的方法存根

				}

				@Override
				public void mouseClicked(MouseEvent e) {
					// TODO 自动生成的方法存根
					if (e.getClickCount() == 2) {
						show();
					}
				}
			});
			exit.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO 自动生成的方法存根
					exit();
				}
			});
			filesViewer.addActionListener((e) -> {
				filesViewer.setEnabled(false);
				fileIOUtil.setEnabled(false);
				Thread t = new Thread(() -> {
					try {
						ServerUIModule.fsv = FSViewer.getInstance();
						fsv.show();
					} catch (SQLException e1) {
						// TODO 自动生成的 catch 块
						JOptionPane.showMessageDialog(window, "错误：无法打开文件，文件系统可能已损坏，您可以尝试重启应用。", "错误",
								JOptionPane.ERROR_MESSAGE);
					}
					filesViewer.setEnabled(true);
					fileIOUtil.setEnabled(true);
				});
				t.start();
			});
			show.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO 自动生成的方法存根
					show();
				}
			});
			pMenu.add(exit);
			pMenu.addSeparator();
			pMenu.add(filesViewer);
			pMenu.add(show);
			ServerUIModule.trayIcon.setPopupMenu(pMenu);
			ServerUIModule.tray.add(ServerUIModule.trayIcon);

		} else {
			ServerUIModule.window.setDefaultCloseOperation(1);
		}
		ServerUIModule.window.setLayout(new BoxLayout(ServerUIModule.window.getContentPane(), 3));
		final JPanel titlebox = new JPanel(new FlowLayout(1));
		titlebox.setBorder(new EmptyBorder(0, 0, (int) (-25 * proportion), 0));
		final JLabel title = new JLabel("kiftd");
		title.setFont(new Font("宋体", 1, (int) (30 * proportion)));
		titlebox.add(title);
		ServerUIModule.window.add(titlebox);
		final JPanel subtitlebox = new JPanel(new FlowLayout(1));
		subtitlebox.setBorder(new EmptyBorder(0, 0, (int) (-20 * proportion), 0));
		final JLabel subtitle = new JLabel("青阳网络文件系统-服务器");
		subtitle.setFont(new Font("宋体", 0, (int) (13 * proportion)));
		subtitlebox.add(subtitle);
		ServerUIModule.window.add(subtitlebox);
		final JPanel statusBox = new JPanel(new GridLayout(4, 1));
		statusBox.setBorder(BorderFactory.createEtchedBorder());
		final JPanel serverStatus = new JPanel(new FlowLayout());
		serverStatus.setBorder(new EmptyBorder(0, 0, (int) (-8 * proportion), 0));
		serverStatus.add(new JLabel("服务器状态(Status)："));
		serverStatus.add(ServerUIModule.serverStatusLab = new JLabel("--"));
		statusBox.add(serverStatus);
		final JPanel portStatus = new JPanel(new FlowLayout());
		portStatus.setBorder(new EmptyBorder(0, 0, (int) (-8 * proportion), 0));
		portStatus.add(new JLabel("端口号(Port)："));
		portStatus.add(ServerUIModule.portStatusLab = new JLabel("--"));
		statusBox.add(portStatus);
		final JPanel addrStatus = new JPanel(new FlowLayout());
		addrStatus.setBorder(new EmptyBorder(0, 0, (int) (-8 * proportion), 0));
		addrStatus.add(new JLabel("日志等级(LogLevel)："));
		addrStatus.add(ServerUIModule.logLevelLab = new JLabel("--"));
		statusBox.add(addrStatus);
		final JPanel bufferStatus = new JPanel(new FlowLayout());
		bufferStatus.setBorder(new EmptyBorder(0, 0, (int) (-8 * proportion), 0));
		bufferStatus.add(new JLabel("下载缓冲区(Buffer)："));
		bufferStatus.add(ServerUIModule.bufferSizeLab = new JLabel("--"));
		statusBox.add(bufferStatus);
		ServerUIModule.window.add(statusBox);
		final JPanel buttonBox = new JPanel(new GridLayout(6, 1));
		buttonBox.add(ServerUIModule.start = new JButton("开启(Start)>>"));
		buttonBox.add(ServerUIModule.stop = new JButton("关闭(Stop)||"));
		buttonBox.add(ServerUIModule.resatrt = new JButton("重启(Restart)~>"));
		buttonBox.add(ServerUIModule.fileIOUtil = new JButton("文件(Files)[*]"));
		buttonBox.add(ServerUIModule.setting = new JButton("设置(Setting)[/]"));
		buttonBox.add(ServerUIModule.exit = new JButton("退出(Exit)[X]"));
		ServerUIModule.window.add(buttonBox);
		final JPanel outputBox = new JPanel(new FlowLayout(1));
		outputBox.add(new JLabel("[输出信息(Server Message)]："));
		(ServerUIModule.output = new JTextArea()).setLineWrap(true);
		output.setRows(3 + (int) (proportion));
		output.setSize((int) (292 * proportion), 100);
		ServerUIModule.output.setEditable(false);
		ServerUIModule.output.setForeground(Color.RED);
		ServerUIModule.output.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				// TODO 自动生成的方法存根

			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				// TODO 自动生成的方法存根
				Thread t = new Thread(() -> {
					if (output.getLineCount() >= 1000) {
						int end = 0;
						try {
							end = output.getLineEndOffset(100);
						} catch (Exception exc) {
						}
						output.replaceRange("", 0, end);
					}
					output.setCaretPosition(output.getText().length());
				});
				t.start();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// TODO 自动生成的方法存根
				output.selectAll();
				output.setCaretPosition(output.getSelectedText().length());
				output.requestFocus();
			}
		});
		outputBox.add(new JScrollPane(ServerUIModule.output));
		ServerUIModule.window.add(outputBox);
		final JPanel bottombox = new JPanel(new FlowLayout(1));
		bottombox.setBorder(new EmptyBorder(0, 0, (int) (-30 * proportion), 0));
		bottombox.add(new JLabel("--青阳龙野@kohgylw--"));
		ServerUIModule.window.add(bottombox);
		ServerUIModule.start.setEnabled(false);
		ServerUIModule.stop.setEnabled(false);
		ServerUIModule.resatrt.setEnabled(false);
		ServerUIModule.setting.setEnabled(false);
		ServerUIModule.start.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				start.setEnabled(false);
				setting.setEnabled(false);
				fileIOUtil.setEnabled(false);
				if (filesViewer != null) {
					filesViewer.setEnabled(false);
				}
				printMessage("启动服务器...");
				if (ss != null) {
					serverStatusLab.setText(S_STARTING);
					Thread t = new Thread(() -> {
						if (ss.start()) {
							printMessage("启动完成。正在检查服务器状态...");
							if (st.getServerStatus()) {
								printMessage("KIFT服务器已经启动，可以正常访问了。");
							} else {
								printMessage("KIFT服务器未能成功启动，请检查设置或查看异常信息。");
							}
						} else {
							if (ConfigureReader.instance().getPropertiesStatus() != 0) {
								switch (ConfigureReader.instance().getPropertiesStatus()) {
								case ConfigureReader.INVALID_PORT:
									printMessage("KIFT无法启动：端口设置无效。");
									break;
								case ConfigureReader.INVALID_BUFFER_SIZE:
									printMessage("KIFT无法启动：缓存设置无效。");
									break;
								case ConfigureReader.INVALID_FILE_SYSTEM_PATH:
									printMessage("KIFT无法启动：文件系统路径或某一扩展存储区设置无效。");
									break;
								case ConfigureReader.INVALID_LOG:
									printMessage("KIFT无法启动：日志设置无效。");
									break;
								case ConfigureReader.INVALID_VC:
									printMessage("KIFT无法启动：登录验证码设置无效。");
									break;
								case ConfigureReader.INVALID_MUST_LOGIN_SETTING:
									printMessage("KIFT无法启动：必须登入设置无效。");
									break;
								case ConfigureReader.INVALID_CHANGE_PASSWORD_SETTING:
									printMessage("KIFT无法启动：用户修改账户密码设置无效。");
									break;
								case ConfigureReader.INVALID_FILE_CHAIN_SETTING:
									printMessage("KIFT无法启动：永久资源链接设置无效。");
									break;
								default:
									printMessage("KIFT无法启动，请检查设置或查看异常信息。");
									break;
								}
							} else {
								printMessage("KIFT无法启动，请检查设置或查看异常信息。");
							}
							serverStatusLab.setText(S_STOP);
						}
						updateServerStatus();
					});
					t.start();
				}
			}
		});
		ServerUIModule.stop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				stop.setEnabled(false);
				resatrt.setEnabled(false);
				fileIOUtil.setEnabled(false);
				if (filesViewer != null) {
					filesViewer.setEnabled(false);
				}
				printMessage("关闭服务器...");
				Thread t = new Thread(() -> {
					if (cs != null) {
						serverStatusLab.setText(S_STOPPING);
						if (cs.close()) {
							printMessage("关闭完成。正在检查服务器状态...");
							if (st.getServerStatus()) {
								printMessage("KIFT服务器未能成功关闭，如有需要，可以强制关闭程序（不安全）。");
							} else {
								printMessage("KIFT服务器已经关闭，停止所有访问。");
							}
						} else {
							printMessage("KIFT服务器无法关闭，请手动结束本程序。");
						}
						updateServerStatus();
					}
				});
				t.start();
			}
		});
		ServerUIModule.exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				fileIOUtil.setEnabled(false);
				if (filesViewer != null) {
					filesViewer.setEnabled(false);
				}
				exit();
			}
		});
		ServerUIModule.resatrt.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				stop.setEnabled(false);
				resatrt.setEnabled(false);
				fileIOUtil.setEnabled(false);
				if (filesViewer != null) {
					filesViewer.setEnabled(false);
				}
				Thread t = new Thread(() -> {
					printMessage("正在重启服务器...");
					if (cs.close()) {
						if (ss.start()) {
							printMessage("重启成功，可以正常访问了。");
						} else {
							printMessage("错误：服务器已关闭但未能重新启动，请尝试手动启动服务器。");
						}
					} else {
						printMessage("错误：无法关闭服务器，请尝试手动关闭。");
					}
					updateServerStatus();
				});
				t.start();
			}
		});
		ServerUIModule.setting.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				ServerUIModule.sw = SettingWindow.getInstance();
				Thread t = new Thread(() -> {
					sw.show();
				});
				t.start();
			}
		});
		ServerUIModule.fileIOUtil.addActionListener((e) -> {
			ServerUIModule.fileIOUtil.setEnabled(false);
			if (ServerUIModule.filesViewer != null) {
				ServerUIModule.filesViewer.setEnabled(false);
			}
			Thread t = new Thread(() -> {
				try {
					ServerUIModule.fsv = FSViewer.getInstance();
					fsv.show();
				} catch (SQLException e1) {
					// TODO 自动生成的 catch 块
					Printer.instance.print("错误：无法读取文件，文件系统可能已经损坏，您可以尝试重启应用。");
				}
				ServerUIModule.fileIOUtil.setEnabled(true);
				if (ServerUIModule.filesViewer != null) {
					ServerUIModule.filesViewer.setEnabled(true);
				}
			});
			t.start();
		});
		modifyComponentSize(ServerUIModule.window);
	}

	public void show() {
		ServerUIModule.window.setVisible(true);
		updateServerStatus();
	}

	public static void setOnCloseServer(final OnCloseServer cs) {
		ServerUIModule.cs = cs;
	}

	public static ServerUIModule getInsatnce() throws Exception {
		if (ServerUIModule.instance == null) {
			ServerUIModule.instance = new ServerUIModule();
		}
		return ServerUIModule.instance;
	}

	public static void setStartServer(final OnStartServer ss) {
		ServerUIModule.ss = ss;
	}

	public static void setGetServerStatus(final GetServerStatus st) {
		ServerUIModule.st = st;
		SettingWindow.st = st;
	}

	public void updateServerStatus() {
		if (ServerUIModule.st != null) {
			Thread t = new Thread(() -> {
				if (ServerUIModule.st.getServerStatus()) {
					ServerUIModule.serverStatusLab.setText(S_START);
					ServerUIModule.start.setEnabled(false);
					ServerUIModule.stop.setEnabled(true);
					ServerUIModule.resatrt.setEnabled(true);
					ServerUIModule.setting.setEnabled(false);
				} else {
					ServerUIModule.serverStatusLab.setText(S_STOP);
					ServerUIModule.start.setEnabled(true);
					ServerUIModule.stop.setEnabled(false);
					ServerUIModule.resatrt.setEnabled(false);
					ServerUIModule.setting.setEnabled(true);
				}
				fileIOUtil.setEnabled(true);
				if (filesViewer != null) {
					filesViewer.setEnabled(true);
				}
				ServerUIModule.portStatusLab.setText(ServerUIModule.st.getPort() + "");
				if (ServerUIModule.st.getLogLevel() != null) {
					switch (ServerUIModule.st.getLogLevel()) {
					case Event: {
						ServerUIModule.logLevelLab.setText(L_ALL);
						break;
					}
					case None: {
						ServerUIModule.logLevelLab.setText(L_NONE);
						break;
					}
					case Runtime_Exception: {
						ServerUIModule.logLevelLab.setText(L_EXCEPTION);
						break;
					}
					default: {
						ServerUIModule.logLevelLab.setText("无法获取(?)");
						break;
					}
					}
				}
				ServerUIModule.bufferSizeLab.setText(ServerUIModule.st.getBufferSize() / 1024 + " KB");
			});
			t.start();
		}
	}

	private void exit() {
		ServerUIModule.start.setEnabled(false);
		ServerUIModule.stop.setEnabled(false);
		ServerUIModule.exit.setEnabled(false);
		ServerUIModule.resatrt.setEnabled(false);
		ServerUIModule.setting.setEnabled(false);
		this.printMessage("退出程序...");
		if (ServerUIModule.cs != null) {
			final Thread t = new Thread(() -> {
				if (ServerUIModule.st.getServerStatus()) {
					ServerUIModule.cs.close();
				}
				System.exit(0);
				return;
			});
			t.start();
		} else {
			System.exit(0);
		}
	}

	public void printMessage(final String context) {
		ServerUIModule.output.append("[" + this.getFormateDate() + "]" + context + "\n");
	}

	private String getFormateDate() {
		if (null == sdf) {
			sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		}
		if (ServerUIModule.ti != null) {
			final Date d = ServerUIModule.ti.get();
			return sdf.format(d);
		}
		return sdf.format(new Date());
	}

	public static void setGetServerTime(final GetServerTime ti) {
		ServerUIModule.ti = ti;
	}

	public static void setUpdateSetting(final UpdateSetting us) {
		SettingWindow.us = us;
	}
}
