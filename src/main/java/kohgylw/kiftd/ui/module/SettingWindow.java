package kohgylw.kiftd.ui.module;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.enumeration.VCLevel;
import kohgylw.kiftd.server.pojo.ExtendStores;
import kohgylw.kiftd.server.pojo.ServerSetting;
import kohgylw.kiftd.ui.callback.*;
import kohgylw.kiftd.ui.pojo.FileSystemPath;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;

/**
 * 
 * <h2>界面模块——设置</h2>
 * <p>
 * 设置界面类，负责图形化界面下的设置界面显示。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class SettingWindow extends KiftdDynamicWindow {
	protected static JDialog window;
	private static JTextField portinput;
	private static JTextField bufferinput;
	private static JComboBox<String> mlinput;
	private static JComboBox<String> vcinput;
	private static JComboBox<String> logLevelinput;
	private static JButton cancel;
	private static JButton update;
	private static JButton changeFileSystemPath;
	protected static File chooserPath;
	protected static List<FileSystemPath> extendStores;
	private static SettingWindow sw;
	private static final String ML_OPEN = "是(YES)";
	private static final String ML_CLOSE = "否(CLOSE)";
	private static final String VC_STANDARD = "标准(STANDARD)";
	private static final String VC_SIMP = "简化(SIMPLIFIED)";
	private static final String VC_CLOSE = "关闭(CLOSE)";
	protected static GetServerStatus st;
	protected static UpdateSetting us;
	private static FileSystemPathViewer fspv;

	private SettingWindow() {
		setUIFont();// 全局字体设置
		// 窗口主体相关设置
		(SettingWindow.window = new JDialog(ServerUIModule.window, "kiftd-设置")).setModal(true);
		SettingWindow.window.setSize(410, 360);
		SettingWindow.window.setLocation(150, 150);
		SettingWindow.window.setDefaultCloseOperation(1);
		SettingWindow.window.setResizable(false);
		SettingWindow.window.setLayout(new BoxLayout(SettingWindow.window.getContentPane(), 3));
		final JPanel titlebox = new JPanel(new FlowLayout(1));
		titlebox.setBorder(new EmptyBorder(0, 0, (int) (7 * proportion), 0));
		final JLabel title = new JLabel("服务器设置 Server Setting");
		title.setFont(new Font("宋体", 1, (int) (20 * proportion)));
		titlebox.add(title);
		SettingWindow.window.add(titlebox);
		// 窗口组件排布
		final JPanel settingbox = new JPanel(new GridLayout(6, 1, 0, 0));
		settingbox.setBorder(new EtchedBorder());
		final int interval = 0;
		// 必须登入下拉框
		final JPanel mlbox = new JPanel(new FlowLayout(1));
		mlbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel mltitle = new JLabel("必须登入(must login)：");
		(SettingWindow.mlinput = new JComboBox<String>()).addItem(ML_OPEN);
		SettingWindow.mlinput.addItem(ML_CLOSE);
		SettingWindow.mlinput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (20 * proportion)));
		mlbox.add(mltitle);
		mlbox.add(SettingWindow.mlinput);
		// 登录验证码下拉框
		final JPanel vcbox = new JPanel(new FlowLayout(1));
		vcbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel vctitle = new JLabel("登录验证码(VC type)：");
		(SettingWindow.vcinput = new JComboBox<>()).addItem(VC_STANDARD);
		SettingWindow.vcinput.addItem(VC_SIMP);
		SettingWindow.vcinput.addItem(VC_CLOSE);
		SettingWindow.mlinput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (20 * proportion)));
		vcbox.add(vctitle);
		vcbox.add(SettingWindow.vcinput);
		// 端口号输入框
		final JPanel portbox = new JPanel(new FlowLayout(1));
		portbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel porttitle = new JLabel("端口(port)：");
		(SettingWindow.portinput = new JTextField())
				.setPreferredSize(new Dimension((int) (120 * proportion), (int) (25 * proportion)));
		portbox.add(porttitle);
		portbox.add(SettingWindow.portinput);
		final JPanel bufferbox = new JPanel(new FlowLayout(1));
		bufferbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel buffertitle = new JLabel("缓存大小(buffer)：");
		(SettingWindow.bufferinput = new JTextField())
				.setPreferredSize(new Dimension((int) (170 * proportion), (int) (25 * proportion)));
		final JLabel bufferUnit = new JLabel("KB");
		bufferbox.add(buffertitle);
		bufferbox.add(SettingWindow.bufferinput);
		bufferbox.add(bufferUnit);
		final JPanel logbox = new JPanel(new FlowLayout(1));
		logbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel logtitle = new JLabel("日志等级(port)：");
		(SettingWindow.logLevelinput = new JComboBox<String>()).addItem("记录全部(ALL)");
		SettingWindow.logLevelinput.addItem("仅异常(EXCEPTION)");
		SettingWindow.logLevelinput.addItem("不记录(NONE)");
		SettingWindow.logLevelinput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (20 * proportion)));
		logbox.add(logtitle);
		logbox.add(SettingWindow.logLevelinput);
		final JPanel filePathBox = new JPanel(new FlowLayout(1));
		filePathBox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel filePathtitle = new JLabel("文件系统路径(file system path)：");
		SettingWindow.changeFileSystemPath = new JButton("管理(Manage)");
		changeFileSystemPath.setPreferredSize(new Dimension((int) (140 * proportion), (int) (32 * proportion)));
		filePathBox.add(filePathtitle);
		filePathBox.add(SettingWindow.changeFileSystemPath);
		settingbox.add(portbox);
		settingbox.add(mlbox);
		settingbox.add(vcbox);
		settingbox.add(bufferbox);
		settingbox.add(logbox);
		settingbox.add(filePathBox);
		SettingWindow.window.add(settingbox);
		final JPanel buttonbox = new JPanel(new FlowLayout(1));
		buttonbox.setBorder(new EmptyBorder((int) (0 * proportion), 0, (int) (5 * proportion), 0));
		SettingWindow.update = new JButton("应用(Update)");
		SettingWindow.cancel = new JButton("取消(Cancel)");
		update.setPreferredSize(new Dimension((int) (155 * proportion), (int) (32 * proportion)));
		cancel.setPreferredSize(new Dimension((int) (155 * proportion), (int) (32 * proportion)));
		buttonbox.add(SettingWindow.update);
		buttonbox.add(SettingWindow.cancel);
		SettingWindow.window.add(buttonbox);
		SettingWindow.cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				window.setVisible(false);
			}
		});
		SettingWindow.update.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO 自动生成的方法存根
				// 仅在服务器停止时才可以进行修改
				if (st.getServerStatus()) {
					getServerStatus();
				} else {
					Thread t = new Thread(() -> {
						if (us != null) {
							try {
								ServerSetting ss = new ServerSetting();
								ss.setPort(Integer.parseInt(portinput.getText()));
								ss.setBuffSize(Integer.parseInt(bufferinput.getText()) * 1024);
								if (chooserPath.isDirectory()) {
									ss.setFsPath(chooserPath.getAbsolutePath());
								}
								List<ExtendStores> ess = new ArrayList<>();
								for (FileSystemPath fsp : extendStores) {
									ExtendStores es = new ExtendStores();
									es.setIndex(fsp.getIndex());
									es.setPath(fsp.getPath());
									ess.add(es);
								}
								ss.setExtendStores(ess);
								switch (logLevelinput.getSelectedIndex()) {
								case 0:
									ss.setLog(LogLevel.Event);
									break;
								case 1:
									ss.setLog(LogLevel.Runtime_Exception);
									break;
								case 2:
									ss.setLog(LogLevel.None);
									break;

								default:
									// 注意，当选择未知的日志等级时，不做任何操作
									break;
								}
								switch (mlinput.getSelectedIndex()) {
								case 0:
									ss.setMustLogin(true);
									break;
								case 1:
									ss.setMustLogin(false);
									break;
								default:
									break;
								}
								switch (vcinput.getSelectedIndex()) {
								case 0: {
									ss.setVc(VCLevel.Standard);
									break;
								}
								case 1: {
									ss.setVc(VCLevel.Simplified);
									break;
								}
								case 2: {
									ss.setVc(VCLevel.Close);
									break;
								}
								default:
									break;
								}
								if (us.update(ss)) {
									ServerUIModule.getInsatnce().updateServerStatus();
									window.setVisible(false);
								}
							} catch (Exception exc) {
								exc.printStackTrace();
								Printer.instance.print("错误：无法应用设置");
							}
						} else {
							window.setVisible(false);
						}
					});
					t.start();
				}
			}
		});
		SettingWindow.changeFileSystemPath.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fspv = FileSystemPathViewer.getInstance();
				fspv.show();
			}
		});
		modifyComponentSize(window);
	}

	protected void show() {
		this.getServerStatus();
		SettingWindow.window.setVisible(true);
	}

	private void getServerStatus() {
		final Thread t = new Thread(() -> {
			if (SettingWindow.st != null) {
				SettingWindow.bufferinput
						.setText(SettingWindow.st.getBufferSize() == 0 ? SettingWindow.st.getInitBufferSize()
								: SettingWindow.st.getBufferSize() / 1024 + "");
				SettingWindow.portinput.setText(SettingWindow.st.getPort() == 0 ? SettingWindow.st.getInitProt() + ""
						: SettingWindow.st.getPort() + "");
				if (SettingWindow.st.getFileSystemPath() != null) {
					chooserPath = new File(SettingWindow.st.getFileSystemPath());
				} else {
					chooserPath = new File(SettingWindow.st.getInitFileSystemPath());
				}
				extendStores = SettingWindow.st.getExtendStores();
				if (st.getLogLevel() != null) {
					switch (st.getLogLevel()) {
					case Event: {
						SettingWindow.logLevelinput.setSelectedIndex(0);
						break;
					}
					case Runtime_Exception: {
						SettingWindow.logLevelinput.setSelectedIndex(1);
						break;
					}
					case None: {
						SettingWindow.logLevelinput.setSelectedIndex(2);
						break;
					}
					}
				} else {
					switch (st.getInitLogLevel()) {
					case Event: {
						SettingWindow.logLevelinput.setSelectedIndex(0);
						break;
					}
					case Runtime_Exception: {
						SettingWindow.logLevelinput.setSelectedIndex(1);
						break;
					}
					case None: {
						SettingWindow.logLevelinput.setSelectedIndex(2);
						break;
					}
					}
				}
				if (SettingWindow.st.getMustLogin()) {
					SettingWindow.mlinput.setSelectedIndex(0);
				} else {
					SettingWindow.mlinput.setSelectedIndex(1);
				}
				if (SettingWindow.st.getVCLevel() != null) {
					switch (SettingWindow.st.getVCLevel()) {
					case Standard: {
						SettingWindow.vcinput.setSelectedIndex(0);
						break;
					}
					case Simplified: {
						SettingWindow.vcinput.setSelectedIndex(1);
						break;
					}
					case Close: {
						SettingWindow.vcinput.setSelectedIndex(2);
						break;
					}
					}
				} else {
					switch (SettingWindow.st.getInitVCLevel()) {
					case Standard: {
						SettingWindow.vcinput.setSelectedIndex(0);
						break;
					}
					case Simplified: {
						SettingWindow.vcinput.setSelectedIndex(1);
						break;
					}
					case Close: {
						SettingWindow.vcinput.setSelectedIndex(2);
						break;
					}
					}
				}
			}
			return;
		});
		t.start();
	}

	protected static SettingWindow getInstance() {
		if (SettingWindow.sw == null) {
			SettingWindow.sw = new SettingWindow();
		}
		return SettingWindow.sw;
	}
}
