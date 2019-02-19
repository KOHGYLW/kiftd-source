package kohgylw.kiftd.server.util;

import java.io.File;

import com.lowagie.text.FontFactoryImp;

/**
 * 
 * <h2>docx格式转PDF格式字体工厂</h2>
 * <p>该工厂用于自动加载所有系统字体和程序内置字体。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class Docx2PDFFontFactory extends FontFactoryImp{
	
	//指定各种系统可能的字体存放路径，并定义程序内置字体存放路径
	@Override
	public int registerDirectories() {
		int i = 0;
		i += registerDirectory("c:/windows/Fonts");
		i += registerDirectory("c:/winnt/fonts");
		i += registerDirectory("d:/windows/fonts");
		i += registerDirectory("d:/winnt/fonts");
		i += registerDirectory("/usr/share/X11/fonts", true);
		i += registerDirectory("/usr/X/lib/X11/fonts", true);
		i += registerDirectory("/usr/openwin/lib/X11/fonts", true);
		i += registerDirectory("/usr/share/fonts", true);
		i += registerDirectory("/usr/X11R6/lib/X11/fonts", true);
		i += registerDirectory("/Library/Fonts");
		i += registerDirectory("/System/Library/Fonts");
		i += registerDirectory(ConfigureReader.instance().getPath()+File.separator+"fonts");
		i += registerDirectory(System.getenv("LICENSE_HOME"), true);
		return i;
	}
}
