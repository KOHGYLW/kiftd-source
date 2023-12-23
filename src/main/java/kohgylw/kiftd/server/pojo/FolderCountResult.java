package kohgylw.kiftd.server.pojo;

/**
 * 
 * <h2>文件夹内容统计信息封装类</h2>
 * <p>
 * 该类用于封装文件夹内容的统计信息，可在JSON化后传至前端。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class FolderCountResult {

	private long totalSize;
	private long folderNum;
	private long fileNum;

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	public long getFolderNum() {
		return folderNum;
	}

	public void setFolderNum(long folderNum) {
		this.folderNum = folderNum;
	}

	public long getFileNum() {
		return fileNum;
	}

	public void setFileNum(long fileNum) {
		this.fileNum = fileNum;
	}

}
