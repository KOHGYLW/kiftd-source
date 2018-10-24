package kohgylw.kiftd.ui.module;

import javax.imageio.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.awt.*;
import javax.swing.event.*;
import javax.swing.*;
import java.text.*;
import java.util.*;
import kohgylw.kiftd.ui.callback.*;

public class ServerUIModule extends KiftdDynamicWindow {

	protected static JFrame window;
	private static SystemTray tray;
	private static TrayIcon trayIcon;
	private static JTextArea output;
	private static ServerUIModule instance;
	private static SettingWindow sw;
	private static OnCloseServer cs;
	private static OnStartServer ss;
	private static GetServerStatus st;
	private static GetServerTime ti;
	private static JButton start;
	private static JButton stop;
	private static JButton resatrt;
	private static JButton setting;
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
	/**
	 * 窗口原始宽度
	 */
	private final int OriginSize_Width = 300;
	/**
	 * 窗口原始高度
	 */
	private final int OriginSize_Height = 550;

	private ServerUIModule() {
		setUIFont();
		(ServerUIModule.window = new JFrame("kiftd-服务器控制台")).setSize(OriginSize_Width, OriginSize_Height);
		ServerUIModule.window.setLocation(100, 100);
		ServerUIModule.window.setResizable(false);
		try {
			ServerUIModule.window.setIconImage(
					ImageIO.read(this.getClass().getResourceAsStream("/kohgylw/kiftd/ui/resource/icon.png")));
		} catch (FileNotFoundException ex) {
		} catch (IOException ex2) {
		}
		if (SystemTray.isSupported()) {
			ServerUIModule.window.setDefaultCloseOperation(1);
			ServerUIModule.tray = SystemTray.getSystemTray();
			try {
				(ServerUIModule.trayIcon = new TrayIcon(
						ImageIO.read(this.getClass().getResourceAsStream("/kohgylw/kiftd/ui/resource/icon_tray.png"))))
								.setToolTip("青阳网络文件系统-kiftd");
				final PopupMenu pMenu = new PopupMenu();
				final MenuItem exit = new MenuItem("退出(Exit)");
				final MenuItem show = new MenuItem("显示主窗口(Show)");
				exit.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO 自动生成的方法存根
						exit();
					}
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
				pMenu.add(show);
				ServerUIModule.trayIcon.setPopupMenu(pMenu);
				ServerUIModule.tray.add(ServerUIModule.trayIcon);
			} catch (FileNotFoundException ex3) {
			} catch (IOException ex4) {
			} catch (AWTException ex5) {
			}
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
		final JPanel buttonBox = new JPanel(new GridLayout(5, 1));
		buttonBox.add(ServerUIModule.start = new JButton("开启(Start)>>"));
		buttonBox.add(ServerUIModule.stop = new JButton("关闭(Stop)||"));
		buttonBox.add(ServerUIModule.resatrt = new JButton("重启(Restart)~>"));
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
							printMessage("KIFT无法启动，请检查设置。");
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
				exit();
			}
		});
		ServerUIModule.resatrt.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				stop.setEnabled(false);
				resatrt.setEnabled(false);
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
		ServerUIModule.sw = SettingWindow.getInstance();
		ServerUIModule.setting.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				Thread t = new Thread(() -> {
					sw.show();
				});
				t.start();
			}
		});
		modifyComponentSize(ServerUIModule.window);
	}

	public void show() {
		ServerUIModule.window.setVisible(true);
	}

	public static void setOnCloseServer(final OnCloseServer cs) {
		ServerUIModule.cs = cs;
	}

	public static ServerUIModule getInsatnce() {
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
		final Thread t = new Thread(() -> {
			if (ServerUIModule.st != null) {
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
				ServerUIModule.portStatusLab.setText(ServerUIModule.st.getPort() + "");
				switch (st.getLogLevel()) {
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
				ServerUIModule.bufferSizeLab.setText(ServerUIModule.st.getBufferSize() / 1024 + " KB");
			}
			return;
		});
		t.start();
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
		if (ServerUIModule.ti != null) {
			final Date d = ServerUIModule.ti.get();
			return new SimpleDateFormat("YYYY-MM-dd hh:mm:ss").format(d);
		}
		return new SimpleDateFormat("YYYY-MM-dd hh:mm:ss").format(new Date());
	}

	public static void setGetServerTime(final GetServerTime ti) {
		ServerUIModule.ti = ti;
	}

	public static void setUpdateSetting(final UpdateSetting us) {
		SettingWindow.us = us;
	}
}
