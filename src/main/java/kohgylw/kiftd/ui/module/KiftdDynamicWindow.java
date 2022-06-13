package kohgylw.kiftd.ui.module;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.swing.UIManager;

/**
 * 
 * <h2>kiftd窗口父类，所有窗口均应继承自该父类</h2>
 * <p>
 * 该类中定义了动态窗口绘制的相应操作。以便于所有继承自该类的窗口对象均能够使用动态绘制方法。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class KiftdDynamicWindow {

	/**
	 * 原始（基准）屏幕分辨率-长度
	 */
	private final int OriginResolution_W = 1440;
	/**
	 * 原始（基准）屏幕分辨率-高度
	 */
	private final int OriginResolution_H = 900;
	/**
	 * 当前屏幕分辨率
	 */
	private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	/**
	 * 计算长度比例
	 */
	private double proportionW = screenSize.getWidth() / (double) OriginResolution_W;
	/**
	 * 计算高度比例
	 */
	private double proportionH = screenSize.getHeight() / (double) OriginResolution_H;
	/**
	 * 选出较小的比例，作为显示窗口的比例
	 */
	protected double proportion = proportionW > proportionH ? proportionH : proportionW;

	/**
	 * 动态调整文件选择框的体积
	 */
	protected Dimension fileChooerSize;

	// 允许通过初始化配置文件动态修改分辨率
	public KiftdDynamicWindow() {
		// 得到conf目录，此处未使用ConfigureReader获得是因为界面的初始化要在最前。
		String path = System.getProperty("user.dir");
		String classPath = System.getProperty("java.class.path");
		if (classPath.indexOf(File.pathSeparator) < 0) {
			File f = new File(classPath);
			classPath = f.getAbsolutePath();
			if (classPath.endsWith(".jar")) {
				path = classPath.substring(0, classPath.lastIndexOf(File.separator));
			}
		}
		String confdir = path + File.separator + "conf" + File.separator;
		// 检查conf文件夹中是否包含名为“init.txt”的设置文件，若有，则使用其中定义的缩放比；否则使用程序计算的缩放比。
		File settingFile = new File(confdir, "init.txt");
		Properties settingp = new Properties();
		try {
			settingp.load(new FileInputStream(settingFile));
			String udp = settingp.getProperty("scale");// 缩放比的设置项必须是scale=?形式
			if (udp != null) {
				double udpi = Double.parseDouble(udp);
				// 设置其最大界限，避免用户错误设置导致界面显示溢出
				if (udpi > 10) {
					udpi = 10;
				}
				proportion = udpi;// 如果上述条件均满足，则使用用户定义的比例替换程序计算的比例
			}
		} catch (Exception e1) {

		}
		if (proportion < 1.0) {
			proportion = 1.0; // 设置其最小限界，防止界面显示不全
		}
		fileChooerSize = new Dimension((int) (570 * proportion), (int) (300 * proportion));
	}

	/**
	 * 
	 * <h2>自适应窗体分辨率大小方法</h2>
	 * <p>
	 * 该方法用于窗口的自适应大小调整。请在窗口组件设定完成之后、显示之前调用该方法。其基本原理为：首先计算出当前屏幕尺寸与基准屏幕尺寸的比例
	 * P=当前屏幕尺寸/基准屏幕尺寸（取长宽比例中较小的一个，以便能够在屏幕中显示完全），之后设置显示窗口大小=使用原始窗口大小*P。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param c
	 *            Container 需要动态调整的容器对象
	 */
	protected void modifyComponentSize(Container c) {
		c.setSize((int) (c.getWidth() * proportion), (int) (c.getHeight() * proportion));
	}

	/**
	 * 
	 * <h2>获取基准分辨率-宽度</h2>
	 * <p>
	 * 该宽度为kiftd开发时所使用的基准分辨率宽度。请以此宽度为基准进行窗口的设计。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return int 基准宽度-像素
	 */
	public int getOriginResolution_W() {
		return OriginResolution_W;
	}

	/**
	 * 
	 * <h2>获取基准分辨率-高度</h2>
	 * <p>
	 * 该高度为kiftd开发时所使用的基准分辨率宽度。请以此高度为基准进行窗口的设计。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return int 基准高度-像素
	 */
	public int getOriginResolution_H() {
		return OriginResolution_H;
	}

	/**
	 * 
	 * <h2>自适应分辨率字体缩放方法</h2>
	 * <p>
	 * 该方法用于定义默认的全局字体样式并自适应调整大小。该方法应该在显示之前进行。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	protected void setUIFont() {
		Font f = new Font("宋体", Font.PLAIN, (int) (13 * proportion));
		String names[] = { "Label", "CheckBox", "PopupMenu", "MenuItem", "CheckBoxMenuItem", "JRadioButtonMenuItem",
				"ComboBox", "Button", "Tree", "ScrollPane", "TabbedPane", "EditorPane", "TitledBorder", "Menu",
				"TextArea", "OptionPane", "MenuBar", "ToolBar", "ToggleButton", "ToolTip", "ProgressBar", "TableHeader",
				"Panel", "List", "ColorChooser", "PasswordField", "TextField", "Table", "Label", "Viewport",
				"RadioButtonMenuItem", "RadioButton", "DesktopPane", "InternalFrame" };
		for (String item : names) {
			UIManager.put(item + ".font", f);
		}
	}

}
