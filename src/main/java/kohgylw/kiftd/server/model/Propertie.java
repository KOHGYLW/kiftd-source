package kohgylw.kiftd.server.model;

/**
 * 
 * <h2>文件系统相关设置项的模型</h2>
 * <p>该模型用于描述文件系统数据库中的PROPERTIES表。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class Propertie {
	
	private String propertieKey;
	private String propertieValue;
	
	public String getPropertieKey() {
		return propertieKey;
	}
	public void setPropertieKey(String propertieKey) {
		this.propertieKey = propertieKey;
	}
	public String getPropertieValue() {
		return propertieValue;
	}
	public void setPropertieValue(String propertieValue) {
		this.propertieValue = propertieValue;
	}
	
}
