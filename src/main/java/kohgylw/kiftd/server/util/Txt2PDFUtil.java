package kohgylw.kiftd.server.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsPSMDetector;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;

/**
 * 
 * <h2>Txt转PDF工具</h2>
 * <p>
 * 该工具是实现txt在线预览功能的核心，能将txt格式的文档以流形式转化为PDF。文档内容将使用开源免费字体“文泉驿正黑”显示。该工具应由Spring
 * IOC容器管理。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class Txt2PDFUtil {

	/**
	 * 
	 * <h2>执行word格式转换（docx）</h2>
	 * <p>将输入流中的word文件转换为PDF格式并输出至指定输出流，该方法线程阻塞。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param in java.io.InputStream 输入流，输入docx格式
	 * @param out java.io.OutputStream 输出流，输出PDF格式
	 * @throws Exception 无法完成转码
	 */
	public void convertPdf(File in, OutputStream out) throws Exception {
		Rectangle rect = new Rectangle(PageSize.A4);//以A4页面显示文本
		Document doc = new Document(rect);
		PdfWriter pw= PdfWriter.getInstance(doc, out);//开始转换
		doc.open();
		BaseFont songFont = BaseFont.createFont("fonts/wqy-zenhei.ttc,0", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
		Font font = new Font(songFont, 12, Font.NORMAL);//设置字体格式
		Paragraph paragraph = new Paragraph();
		paragraph.setFont(font);
		String charset=getTxtCharset(new FileInputStream(in));//判断编码格式
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(in), charset));
		String line = null;
		while ((line = reader.readLine()) != null) {
			paragraph.add(line + "\n");//将文本逐行写入PDF段落
		}
		reader.close();
		doc.add(paragraph);//写入段落至文档
		doc.close();//关闭文档
		pw.flush();
		pw.close();
	}
	
	//自动判别文本编码集，如果能判断则返回准确编码集名称，例如“UTF-8”，否则返回最大概率的编码集。
	private String getTxtCharset(InputStream in) throws Exception {
		int lang = nsPSMDetector.CHINESE;
		nsDetector det = new nsDetector(lang);
		CharsetDetectionObserverImpl cdoi=new CharsetDetectionObserverImpl();
		det.Init(cdoi);
		BufferedInputStream imp = new BufferedInputStream(in);
		byte[] buf = new byte[1024];
		int len;
		boolean isAscii = true;
		while ((len = imp.read(buf, 0, buf.length)) != -1) {
			if (isAscii) {
				isAscii = det.isAscii(buf, len);
			}
			if (!isAscii) {
				if (det.DoIt(buf, len, false)) {
					break;
				}
			}
		}
		imp.close();
		in.close();
		det.DataEnd();
		if (isAscii) {
			return "ASCII";
		} else if (cdoi.getCharset()!=null) {
			return cdoi.getCharset();
		} else {
			String[] prob=det.getProbableCharsets();
			if(prob!=null&&prob.length>0) {
				return prob[0];
			}
			return "GBK";
		}
	}

}
