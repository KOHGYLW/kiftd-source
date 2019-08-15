package kohgylw.kiftd.server.pojo;

/**
 * 
 * <h2>上传文件夹时，“保留两者”操作使用新文件夹名的响应信息封装</h2>
 * <p>
 * 该对象用于标明创建新名称文件夹的结果，包括是否创建成功及新名称为何。
 * 其中，result属性用于表示：error失败，success成功；
 * 若成功，则可从newName属性获取创建成功的文件夹新名称，
 * 并应该使用该名称作为“newFolderName”属性进行文件夹上传。
 * </p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class CreateNewFolderByNameRespons {
	
	private String result;
	private String newName;
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getNewName() {
		return newName;
	}
	public void setNewName(String newName) {
		this.newName = newName;
	}

}
