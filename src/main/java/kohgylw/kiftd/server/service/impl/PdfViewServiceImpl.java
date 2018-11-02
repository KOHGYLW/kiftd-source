package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.tools.TextToPDF;
import org.springframework.stereotype.*;
import kohgylw.kiftd.server.mapper.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.annotation.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.util.*;
import kohgylw.kiftd.server.enumeration.*;

@Service
public class PdfViewServiceImpl implements PdfViewService {
	@Resource
	private NodeMapper fm;

	@Resource
	private FileBlockUtil fbu;

	// ？？？？
	@Override
	public void getPdfAsStream(final HttpServletRequest request, final HttpServletResponse response, String fileId) {
		if (fileId != null && fileId.length() > 0) {
			final Node f = this.fm.queryById(fileId);
			if (f != null) {
				final String account = (String) request.getSession().getAttribute("ACCOUNT");
				if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
					final String fileName = f.getFileName();
					final String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
					response.setContentType("application/pdf");
					if (suffix.equals("pdf")) {
						try {
							request.getRequestDispatcher("/fileblocks/" + f.getFilePath()).forward(request, response);
						} catch (IOException e) {

						} catch (ServletException e) {
							// TODO 自动生成的 catch 块
						}
					} else if (suffix.equals("txt")) {
						outPutTxtToPDF(f, response, request);
					}
				}
			}
		}
	}

	private void outPutTxtToPDF(Node f, HttpServletResponse response, HttpServletRequest request) {
		TextToPDF ttp = new TextToPDF();
		try {
			PDDocument doc = new PDDocument();
			PDFont font = PDType0Font.load(doc, new File(ConfigureReader.instance().getPath(),
					"externalResources" + File.separator + "txtToPDFont.ttf"));
			ttp.setFont(font);
			ttp.createPDFFromText(doc,
					new FileReader(new File(ConfigureReader.instance().getFileBlockPath(), f.getFilePath())));
			doc.save(response.getOutputStream());
			doc.close();
		} catch (Exception e) {
			// TODO 自动生成的 catch 块
			try {
				request.getRequestDispatcher("/fileblocks/").forward(request, response);
			} catch (IOException e1) {
				// TODO 自动生成的 catch 块
			} catch (ServletException e1) {
				// TODO 自动生成的 catch 块
			}
		}
	}
}
