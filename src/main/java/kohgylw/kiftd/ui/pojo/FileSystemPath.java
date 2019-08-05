package kohgylw.kiftd.ui.pojo;

import java.io.File;

public class FileSystemPath {
	
	public static final String MAIN_FILE_SYSTEM_NAME="主文件系统";
	public static final String EXTEND_STORES_NAME="扩展存储区";
	
	private String type;
	private File path;
	private short index;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public short getIndex() {
		return index;
	}
	public void setIndex(short index) {
		this.index = index;
	}
	public File getPath() {
		return path;
	}
	public void setPath(File path) {
		this.path = path;
	}
	
}
