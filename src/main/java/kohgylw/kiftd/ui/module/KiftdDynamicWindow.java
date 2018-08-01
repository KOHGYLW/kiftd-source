package kohgylw.kiftd.ui.module;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

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
	private double proportion = proportionW > proportionH ? proportionH : proportionW;

	/**
	 * 
	 * <h2>自适应分辨率大小方法</h2>
	 * <p>
	 * 该方法用于窗口的自适应大小调整。请在窗口组件设定完成之后、显示之前调用该方法。其基本原理为：首先计算出当前屏幕尺寸与基准屏幕尺寸的比例
	 * P=当前屏幕尺寸/基准屏幕尺寸（取长宽比例中较小的一个，以便能够在屏幕中显示完全），之后设置显示窗口大小=使用原始窗口大小*P，
	 * 同时将字体等比扩大或缩小。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param c
	 *            Container 需要动态调整的容器对象
	 */
	protected void modifyComponentSize(Container c) {
		try {
			Component[] components = c.getComponents();
			for (Component co : components) {
				double locX = co.getX() * proportion;
				double locY = co.getY() * proportion;
				double width = co.getWidth() * proportion;
				double height = co.getHeight() * proportion;
				co.setLocation((int) locX, (int) locY);
				co.setSize((int) width, (int) height);
				int size = (int) (co.getFont().getSize() * proportion);
				Font font = new Font(co.getFont().getFontName(), co.getFont().getStyle(), size);
				co.setFont(font);
			}
		} catch (Exception e) {

		}
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

}
