package kohgylw.kiftd.ui.module;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import kohgylw.kiftd.util.file_system_manager.FileSystemManager;

public class FSProgressDialog extends KiftdDynamicWindow {

	private JDialog window;//窗体
	private static JLabel message;//显示的文本信息
	private static JProgressBar pBar;//进度条
	private static JButton cancel;//取消按钮
	private static boolean listen;// 是否继续监听

	private FSProgressDialog() {
		setUIFont();//自动设置字体大小
		(window = new JDialog(FSViewer.window, "执行中...")).setModal(true);
		window.setSize(380, 120);
		window.setLocation(200, 200);
		window.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		window.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
				// TODO 自动生成的方法存根
			}

			@Override
			public void windowIconified(WindowEvent e) {
				// TODO 自动生成的方法存根
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO 自动生成的方法存根
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO 自动生成的方法存根
			}

			@Override
			public void windowClosing(WindowEvent e) {
				// TODO 自动生成的方法存根
				canncel();
			}

			@Override
			public void windowClosed(WindowEvent e) {
				// TODO 自动生成的方法存根
			}

			@Override
			public void windowActivated(WindowEvent e) {
				// TODO 自动生成的方法存根
			}
		});
		window.setResizable(false);
		window.setLayout(new BoxLayout(window.getContentPane(), 3));
		JPanel messageBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
		message = new JLabel("请稍候...");
		messageBox.add(message);
		pBar = new JProgressBar(0, 100);
		pBar.setStringPainted(false);
		JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		cancel = new JButton("终止");
		cancel.setPreferredSize(new Dimension((int) (90 * proportion), (int) (27 * proportion)));
		cancel.addActionListener((e)->{
			canncel();
		});
		btnBox.add(cancel);
		//配置组件
		window.add(messageBox);
		window.add(pBar);
		window.add(btnBox);
		//自动适应屏幕分辨率
		modifyComponentSize(window);
	}
	
	/**
	 * 
	 * <h2>打开进度监听窗口并开启自动监听线程</h2>
	 * <p>该方法将显示监听窗口，并开启一个针对文件管理器的监听线程用于自动监听进度。注意：该方法将阻塞线程直到窗口关闭。</p>
	 * @author 青阳龙野(kohgylw)
	 */
	protected void show() {
		listen = true;
		pBar.setValue(0);
		message.setText("请稍候...");
		//启动监听线程用于监听进度，该线程结束后会自动关闭窗口。
		Thread lt=new Thread(()->{
			while (listen) {
				pBar.setValue(FileSystemManager.per);
				message.setText(FileSystemManager.message);
				try {
					Thread.sleep(16);
				} catch (InterruptedException e) {
					listen=false;
				}
			}
			window.dispose();
		});
		lt.start();
		window.setVisible(true);//必须先开启监听，否则将阻塞线程
	}
	
	/**
	 * 
	 * <h2>关闭该进度监听窗口</h2>
	 * <p>在窗口打开后，可以随时调用该方法停止其中的监听线程并关闭窗口。</p>
	 * @author 青阳龙野(kohgylw)
	 */
	protected void close() {
		listen = false;
	}
	
	//终止当前操作
	private void canncel() {
		if(JOptionPane.showConfirmDialog(window, "操作仍在进行中，确认要立即终止？", "警告", JOptionPane.YES_NO_OPTION)==0) {
			FileSystemManager.getInstance().cannel();
			window.dispose();
		}
	}
	
	/**
	 * 
	 * <h2>获取一个新的进度窗口</h2>
	 * <p>通过该方法获取一个新的进度窗口，与前几个窗口不同，每次进行进度监听操作均应获取新窗口而不能使用前一个。</p>
	 * @author 青阳龙野(kohgylw)
	 * @return kohgylw.kiftd.ui.module.FSProgressDialog 新的窗口对象
	 */
	protected static FSProgressDialog getNewInstance() {
		return new FSProgressDialog();
	}

}
