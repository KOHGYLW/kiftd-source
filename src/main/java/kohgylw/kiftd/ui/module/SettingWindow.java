package kohgylw.kiftd.ui.module;

import java.io.*;

import kohgylw.kiftd.printer.Printer;
import kohgylw.kiftd.server.enumeration.LogLevel;
import kohgylw.kiftd.server.pojo.ServerSetting;
import kohgylw.kiftd.ui.callback.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class SettingWindow extends KiftdDynamicWindow {
	private static JDialog window;
	private static JTextField portinput;
	private static JTextField bufferinput;
	private static JComboBox<String> mlinput;
	private static JComboBox<String> logLevelinput;
	private static JButton cancel;
	private static JButton update;
	private static JButton changeFileSystemPath;
	private static JFileChooser changeFileSystemPathChooser;
	private static File chooserPath;
	private static JTextArea fileSystemPathArea;
	private static SettingWindow sw;
	private static final String ML_OPEN = "是(YES)";
	private static final String ML_CLOSE = "否(CLOSE)";
	protected static GetServerStatus st;
	protected static UpdateSetting us;

	private SettingWindow() {
		setUIFont();
		(SettingWindow.window = new JDialog(ServerUIModule.window, "kiftd-设置")).setModal(true);
		SettingWindow.window.setSize(410, 380);
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
		final JPanel settingbox = new JPanel(new GridLayout(5, 1, 0, 0));
		settingbox.setBorder(new EtchedBorder());
		final JPanel mlbox = new JPanel(new FlowLayout(1));
		final int interval = 0;
		mlbox.setBorder(new EmptyBorder(interval, 0, interval, 0));
		final JLabel mltitle = new JLabel("必须登入(must login)：");
		(SettingWindow.mlinput = new JComboBox<String>()).addItem(ML_OPEN);
		SettingWindow.mlinput.addItem(ML_CLOSE);
		SettingWindow.mlinput.setPreferredSize(new Dimension((int) (170 * proportion), (int) (20 * proportion)));
		mlbox.add(mltitle);
		mlbox.add(SettingWindow.mlinput);
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
		SettingWindow.changeFileSystemPath = new JButton("修改(Change)");
		changeFileSystemPath.setPreferredSize(new Dimension((int) (140 * proportion), (int) (32 * proportion)));
		(fileSystemPathArea = new JTextArea()).setLineWrap(true);
		fileSystemPathArea.setRows(2 + (int) (proportion));
		fileSystemPathArea.setEditable(false);
		filePathBox.add(filePathtitle);
		filePathBox.add(SettingWindow.changeFileSystemPath);
		settingbox.add(portbox);
		settingbox.add(mlbox);
		settingbox.add(bufferbox);
		settingbox.add(logbox);
		settingbox.add(filePathBox);
		SettingWindow.window.add(settingbox);
		window.add(new JScrollPane(fileSystemPathArea));
		final JPanel buttonbox = new JPanel(new FlowLayout(1));
		buttonbox.setBorder(new EmptyBorder((int) (0 * proportion), 0, (int) (5 * proportion), 0));
		SettingWindow.update = new JButton("应用(Update)");
		SettingWindow.cancel = new JButton("取消(Cancel)");
		update.setPreferredSize(new Dimension((int) (155 * proportion), (int) (32 * proportion)));
		cancel.setPreferredSize(new Dimension((int) (155 * proportion), (int) (32 * proportion)));
		buttonbox.add(SettingWindow.update);
		buttonbox.add(SettingWindow.cancel);
		SettingWindow.window.add(buttonbox);
		changeFileSystemPathChooser = new JFileChooser();
		changeFileSystemPathChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
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
								if (us.update(ss)) {
									ServerUIModule.getInsatnce().updateServerStatus();
									window.setVisible(false);
								}
							} catch (Exception exc) {
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
				// TODO 自动生成的方法存根
				changeFileSystemPathChooser.setPreferredSize(fileChooerSize);
				changeFileSystemPathChooser.setDialogTitle("请选择...");
				if (changeFileSystemPathChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					chooserPath = changeFileSystemPathChooser.getSelectedFile();
				}
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
				SettingWindow.bufferinput.setText(SettingWindow.st.getBufferSize() / 1024 + "");
				SettingWindow.portinput.setText(SettingWindow.st.getPort() + "");
				SettingWindow.fileSystemPathArea.setText("当前文件系统存储路径：" + SettingWindow.st.getFileSystemPath());
				chooserPath = new File(SettingWindow.st.getFileSystemPath());
				if (chooserPath.isDirectory()) {
					changeFileSystemPathChooser.setCurrentDirectory(chooserPath);
				}
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
				if (SettingWindow.st.getMustLogin()) {
					SettingWindow.mlinput.setSelectedIndex(0);
				} else {
					SettingWindow.mlinput.setSelectedIndex(1);
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
