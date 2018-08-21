package kohgylw.kiftd.server.listener;

import javax.servlet.annotation.*;
import javax.servlet.*;
import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.util.*;
import java.io.*;
import java.sql.*;

@WebListener
public class H2DBinitListener implements ServletContextListener {
	public void contextInitialized(final ServletContextEvent sce) {
		Printer.instance.print("\u6587\u4ef6\u7cfb\u7edf\u521d\u59cb\u5316...");
		try {
			Class.forName("org.h2.Driver");
			final Connection conn = DriverManager.getConnection(ConfigureReader.instance().getFileNodePathURL(), "root", "301537gY");
			final Statement state1 = conn.createStatement();
			ResultSet result = state1
					.executeQuery("SELECT count(*) from INFORMATION_SCHEMA.TABLES where TABLE_NAME='FOLDER'");
			while (result.next()) {
				if (result.getInt("count(*)") > 0) {
					continue;
				}
				final Statement state2 = conn.createStatement();
				state2.execute(
						"CREATE TABLE FOLDER(folder_id VARCHAR(128) PRIMARY KEY,  folder_name VARCHAR(128) NOT NULL,folder_creation_date VARCHAR(128) NOT NULL,  folder_creator VARCHAR(128) NOT NULL,folder_parent VARCHAR(128) NOT NULL)");
				state2.execute("INSERT INTO FOLDER VALUES('root', 'ROOT', '--', '--', 'null')");
				state2.close();
			}
			result = state1.executeQuery("SELECT count(*) from INFORMATION_SCHEMA.TABLES where TABLE_NAME='FILE'");
			while (result.next()) {
				if (result.getInt("count(*)") > 0) {
					continue;
				}
				final Statement state3 = conn.createStatement();
				state3.execute(
						"CREATE TABLE FILE(file_id VARCHAR(128) PRIMARY KEY,file_name VARCHAR(128) NOT NULL,file_size VARCHAR(128) NOT NULL,file_parent_folder varchar(128) NOT NULL,file_creation_date varchar(128) NOT NULL,file_creator varchar(128) NOT NULL,file_path varchar(128) NOT NULL)");
				state3.close();
				final String fileblocks = ConfigureReader.instance().getFileBlockPath();
				final File fb = new File(fileblocks);
				if (fb.exists() && fb.isDirectory()) {
					final String[] flist = fb.list();
					for (final String s : flist) {
						new File(fb, s).delete();
					}
				} else {
					fb.mkdirs();
				}
			}
			state1.close();
			conn.close();
			Printer.instance.print("\u521d\u59cb\u5316\u5b8c\u6210\u3002");
		} catch (Exception e) {
			Printer.instance.print("\u521d\u59cb\u5316\u5931\u8d25\u3002");
			Printer.instance.print(e.getMessage());
		}
	}

	public void contextDestroyed(final ServletContextEvent sce) {
	}
}
