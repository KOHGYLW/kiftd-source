package kohgylw.kiftd.server.util;

import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

/**
 * 
 * <h2>Docx转PDF工具</h2>
 * <p>
 * 该工具是实现word在线预览功能的核心，能将docx格式的文档以流形式转化为PDF。如果该文档中存在系统未安装的字体，则使用开源免费字体“文泉驿正黑”替代显示，以确保在非Windows系统下也能正确转换。该工具应由Spring
 * IOC容器管理。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class Docx2PDFUtil {
	
	/**
	 * 
	 * <h2>执行word格式转换（docx）</h2>
	 * <p>将输入流中的word文件转换为PDF格式并输出至指定输出流，该方法线程阻塞。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param in java.io.InputStream 输入流，输入docx格式
	 * @param out java.io.OutputStream 输出流，输出PDF格式
	 */
	public void convertPdf(InputStream in, OutputStream out) throws Exception {
		XWPFDocument document = new XWPFDocument(in);
		//获取系统已安装的所有字体
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for (XWPFParagraph p : document.getParagraphs()) {
			for (XWPFRun r : p.getRuns()) {
				//判断文档中的字体是否安装
				if (Arrays.stream(ge.getAvailableFontFamilyNames()).parallel()
						.anyMatch((e) -> e.equals(r.getFontFamily()))) {
					continue;
				}
				//如未安装，则使用程序自带的“文泉驿正黑”替代
				r.setFontFamily("WenQuanYi Zen Hei");
			}
		}
		PdfConverter.getInstance().convert(document, out, PdfOptions.create().fontProvider(Docx2PDFFontProvider.getInstance()));
	}

}
