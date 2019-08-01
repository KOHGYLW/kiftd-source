package kohgylw.kiftd.server.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.hslf.model.Slide;
import org.apache.poi.hslf.model.TextRun;
import org.apache.poi.hslf.usermodel.RichTextRun;
import org.apache.poi.hslf.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import kohgylw.kiftd.server.enumeration.PowerPointType;

/**
 * 
 * <h2>PPT转PDF工具</h2>
 * <p>
 * 该工具是实现PPT在线预览功能的核心，能将ppt/pptx格式的文档以流形式转化为PDF。如果该文档中存在系统未安装的字体，则使用开源免费字体“文泉驿正黑”替代显示，以确保在非Windows系统下也能正确转换。该工具应由Spring
 * IOC容器管理。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class PowerPoint2PDFUtil {
	
	/**
	 * 
	 * <h2>执行PPT格式转换（ppt/pptx）</h2>
	 * <p>将输入流中的PPT文件转换为PDF格式并输出至指定输出流，该方法线程阻塞。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param in java.io.InputStream 输入流，输入ppt/pptx格式
	 * @param out java.io.OutputStream 输出流，输出PDF格式
	 * @param type kohgylw.kiftd.server.enumeration.PowerPointType 使用何种标准进行解析（ppt/pptx）
	 */
	public void convertPdf(InputStream in, OutputStream out, PowerPointType type) throws Exception {
		double zoom = 2;
		AffineTransform at = new AffineTransform();
		at.setToScale(zoom, zoom);
		Document pdfDocument = new Document();
		PdfWriter pdfWriter = PdfWriter.getInstance(pdfDocument, out);
		PdfPTable table = new PdfPTable(1);
		pdfWriter.open();
		pdfDocument.open();
		Dimension pgsize = null;
		Image slideImage = null;
		BufferedImage img = null;
		if (type.equals(PowerPointType.PPT)) {
			SlideShow ppt = new SlideShow(in);
			in.close();
			pgsize = ppt.getPageSize();
			Slide slide[] = ppt.getSlides();
			pdfDocument.setPageSize(new Rectangle((float) pgsize.getWidth(), (float) pgsize.getHeight()));
			pdfWriter.open();
			pdfDocument.open();
			for (int i = 0; i < slide.length; i++) {
				TextRun[] truns = slide[i].getTextRuns();
				for (int k = 0; k < truns.length; k++) {
					RichTextRun[] rtruns = truns[k].getRichTextRuns();
					for (int l = 0; l < rtruns.length; l++) {
						if(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains(rtruns[l].getFontName())) {
							continue;
						}
						rtruns[l].setFontIndex(1);
						rtruns[l].setFontName("WenQuanYi Zen Hei");
					}
				}

				img = new BufferedImage((int) Math.ceil(pgsize.width * zoom), (int) Math.ceil(pgsize.height * zoom),
						BufferedImage.TYPE_INT_RGB);
				Graphics2D graphics = img.createGraphics();
				graphics.setTransform(at);

				graphics.setPaint(Color.white);
				graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));
				slide[i].draw(graphics);
				graphics.getPaint();
				slideImage = Image.getInstance(img, null);
				table.addCell(new PdfPCell(slideImage, true));
			}
		}
		if (type.equals(PowerPointType.PPTX)) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			XMLSlideShow ppt = new XMLSlideShow(in);
			pgsize = ppt.getPageSize();
			XSLFSlide slide[] = ppt.getSlides();
			pdfDocument.setPageSize(new Rectangle((float) pgsize.getWidth(), (float) pgsize.getHeight()));
			pdfWriter.open();
			pdfDocument.open();

			for (int i = 0; i < slide.length; i++) {
				for (XSLFShape shape : slide[i].getShapes()) {
					if (shape instanceof XSLFTextShape) {
						XSLFTextShape txtshape = (XSLFTextShape) shape;
						for (XSLFTextParagraph textPara : txtshape.getTextParagraphs()) {
							List<XSLFTextRun> textRunList = textPara.getTextRuns();
							for (XSLFTextRun textRun : textRunList) {
								if (Arrays.stream(ge.getAvailableFontFamilyNames()).parallel()
										.anyMatch((e) -> e.equals(textRun.getFontFamily()))) {
									continue;
								}
								textRun.setFontFamily("WenQuanYi Zen Hei");
							}
						}
					}
				}
				img = new BufferedImage((int) Math.ceil(pgsize.width * zoom), (int) Math.ceil(pgsize.height * zoom),
						BufferedImage.TYPE_INT_RGB);
				Graphics2D graphics = img.createGraphics();
				graphics.setTransform(at);
				graphics.setPaint(Color.white);
				graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));
				slide[i].draw(graphics);

				graphics.getPaint();
				slideImage = Image.getInstance(img, null);
				table.addCell(new PdfPCell(slideImage, true));
			}
		}
		pdfDocument.add(table);
		pdfDocument.close();
		pdfWriter.close();
	}

}
