package kohgylw.kiftd.server.util;

import com.lowagie.text.FontFactory;

import fr.opensagres.xdocreport.itext.extension.font.AbstractFontRegistry;

/**
 * 
 * <h2>Docx转PDF字体格式封装类</h2>
 * <p>该类用于提供docx转PDF时所需的字体格式封装对象，该类被设计为单例模式，请使用getInstance()方法获取唯一实例。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class Docx2PDFFontProvider extends AbstractFontRegistry{
	
	private static Docx2PDFFontProvider instance;
	
	private Docx2PDFFontProvider() {
		FontFactory.setFontImp(new Docx2PDFFontFactory());
	}

	@Override
	protected String resolveFamilyName(String arg0, int arg1) {
		return arg0;
	}
	
	/**
	 * 
	 * <h2>取得字体封装类的唯一实例</h2>
	 * <p>请调用该方法获取docx转PDF功能所需的字体封装对象。</p>
	 * @author 青阳龙野(kohgylw)
	 * @return fr.opensagres.xdocreport.itext.extension.font.AbstractFontRegistry 字体封装对象
	 */
	public static Docx2PDFFontProvider getInstance() {
		if(instance==null) {
			instance=new Docx2PDFFontProvider();
		}
		return instance;
	}

}
