package kohgylw.kiftd.server.listener;

import javax.servlet.annotation.*;
import javax.servlet.*;
import kohgylw.kiftd.printer.*;
import kohgylw.kiftd.server.util.*;
import java.sql.*;

@WebListener
public class H2DBinitListener implements ServletContextListener {
	public void contextInitialized(final ServletContextEvent sce) {
		Printer.instance.print("初始化文件节点...");
		try {
			Class.forName("org.h2.Driver");
			final Connection conn = DriverManager.getConnection(ConfigureReader.instance().getFileNodePathURL(), "root",
					"301537gY");
			final Statement state1 = conn.createStatement();
			state1.execute(
					"CREATE TABLE IF NOT EXISTS FOLDER(folder_id VARCHAR(128) PRIMARY KEY,  folder_name VARCHAR(128) NOT NULL,folder_creation_date VARCHAR(128) NOT NULL,  folder_creator VARCHAR(128) NOT NULL,folder_parent VARCHAR(128) NOT NULL,folder_constraint INT NOT NULL)");
			state1.executeQuery("SELECT count(*) FROM FOLDER WHERE folder_id = 'root'");
			ResultSet rs=state1.getResultSet();
			if(rs.next()) {
				if(rs.getInt(1)==0) {
					final Statement state11 = conn.createStatement();
					state11.execute("INSERT INTO FOLDER VALUES('root', 'ROOT', '--', '--', 'null', 0)");
				}
			}
			state1.close();
			final Statement state2 = conn.createStatement();
			state2.execute(
					"CREATE TABLE IF NOT EXISTS FILE(file_id VARCHAR(128) PRIMARY KEY,file_name VARCHAR(128) NOT NULL,file_size VARCHAR(128) NOT NULL,file_parent_folder varchar(128) NOT NULL,file_creation_date varchar(128) NOT NULL,file_creator varchar(128) NOT NULL,file_path varchar(128) NOT NULL)");
			state2.close();
			final Statement state3 = conn.createStatement();
			state3.execute("ALTER TABLE FOLDER ADD COLUMN IF NOT EXISTS folder_constraint INT NOT NULL DEFAULT 0");
			state3.close();
			final Statement state4 = conn.createStatement();
			state4.execute("CREATE INDEX IF NOT EXISTS file_index ON FILE (file_id,file_name)");
			state4.close();
			conn.close();
			Printer.instance.print("文件节点初始化完毕。");
		} catch (Exception e) {
			Printer.instance.print("错误：文件节点初始化失败。");
			Printer.instance.print(e.getMessage());
		}
	}

	public void contextDestroyed(final ServletContextEvent sce) {
	}
}
