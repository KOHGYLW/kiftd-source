package kohgylw.kiftd.server.pojo;

import java.util.List;

/**
 * 
 * <h2>上传文件检查结果封装</h2>
 * <p>
 * 该POJO用于封装一次上传文件前置检查的结果，并以JSON格式传回前端。其中包括字段：
 * checkResult（代表结果，如为hasExistsNames则存在同名文件，如为permitUpload则可直接上传）、
 * uploadKey（代表上传凭证，必须使用该凭证上传）
 * 和pereFileNameList（重名列表，仅当checkResult为hasExistsNames时需要检查，否则可忽略）
 * </p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class CheckUploadFilesRespons {
	
	private String checkResult;//检查结果
	private List<String> pereFileNameList;//重复列表
	private String overSizeFile;//超限文件
	private String maxUploadFileSize;//最大上传体积
	
	public String getCheckResult() {
		return checkResult;
	}
	public void setCheckResult(String checkResult) {
		this.checkResult = checkResult;
	}
	public List<String> getPereFileNameList() {
		return pereFileNameList;
	}
	public void setPereFileNameList(List<String> pereFileNameList) {
		this.pereFileNameList = pereFileNameList;
	}
	public String getOverSizeFile() {
		return overSizeFile;
	}
	public void setOverSizeFile(String overSizeFile) {
		this.overSizeFile = overSizeFile;
	}
	public String getMaxUploadFileSize() {
		return maxUploadFileSize;
	}
	public void setMaxUploadFileSize(String maxUploadFileSize) {
		this.maxUploadFileSize = maxUploadFileSize;
	}
}
