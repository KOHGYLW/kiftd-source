package kohgylw.kiftd.server.listener;

import javax.servlet.annotation.*;
import javax.servlet.*;
import kohgylw.kiftd.printer.*;
import java.io.*;
import org.springframework.web.context.support.*;
import kohgylw.kiftd.server.util.*;
import org.springframework.context.*;

@WebListener
public class SCListener implements ServletContextListener {
	public void contextInitialized(final ServletContextEvent sce) {
		Printer.instance.print("\u6587\u4ef6\u7cfb\u7edf\u8282\u70b9\u4fe1\u606f\u6821\u5bf9...");
		final String fsp = ConfigureReader.instance().getFileSystemPath();
		final File fspf = new File(fsp);
		if (fspf.isDirectory() && fspf.canRead() && fspf.canWrite()) {
			final ApplicationContext context = (ApplicationContext) WebApplicationContextUtils
					.getWebApplicationContext(sce.getServletContext());
			final FileBlockUtil fbu = context.getBean(FileBlockUtil.class);
			fbu.checkFileBlocks();
			final String tfPath = ConfigureReader.instance().getTemporaryfilePath();
			final File f = new File(tfPath);
			if (!f.exists()) {
				f.mkdir();
			} else {
				final File[] listFiles = f.listFiles();
				for (final File fs : listFiles) {
					fs.delete();
				}
			}
			Printer.instance.print("\u6821\u5bf9\u5b8c\u6210\u3002");
		} else {
			Printer.instance.print(
					"\u9519\u8bef\uff1a\u6587\u4ef6\u7cfb\u7edf\u8282\u70b9\u4fe1\u606f\u6821\u5bf9\u5931\u8d25\uff0c\u5b58\u50a8\u4f4d\u7f6e\u65e0\u6cd5\u8bfb\u5199\u6216\u4e0d\u5b58\u5728\u3002");
		}
	}

	public void contextDestroyed(final ServletContextEvent sce) {
		Printer.instance.print("\u6e05\u7406\u4e34\u65f6\u6587\u4ef6...");
		final String tfPath = ConfigureReader.instance().getTemporaryfilePath();
		final File f = new File(tfPath);
		final File[] listFiles = f.listFiles();
		for (final File fs : listFiles) {
			fs.delete();
		}
	}
}
