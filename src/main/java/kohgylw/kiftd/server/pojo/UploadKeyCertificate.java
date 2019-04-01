package kohgylw.kiftd.server.pojo;

/**
 * 
 * <h2>上传凭证证书</h2>
 * <p>该类封装了上传时分发的证书，用于记录谁具备多少次上传权限，用以在其注销后依然能完成已经开始的上传操作。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class UploadKeyCertificate {
	
	private int term;//有效次数
	private String account;//对应用户
	
	/**
	 * 
	 * <h2>创建该证书</h2>
	 * <p>使用该构造器创建一个上传凭证，必须指定有效次数及其创建者。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param term int 有效次数，每使用（执行上传）一次会减一
	 * @param account java.lang.String 创建者
	 */
	public UploadKeyCertificate(int term,String account) {
		this.term=term;
		this.account=account;
	}
	
	/**
	 * 
	 * <h2>使用该证书</h2>
	 * <p>执行本方法代表使用了该证书一次，将有效次数减一。</p>
	 * @author 青阳龙野(kohgylw)
	 */
	public void checked() {
		term--;
	}
	
	/**
	 * 
	 * <h2>检查有效性</h2>
	 * <p>返回当前证书是否仍有效。</p>
	 * @author 青阳龙野(kohgylw)
	 * @return boolean true代表仍有效，否则无效。
	 */
	public boolean isEffective() {
		return term > 0;
	}
	
	/**
	 * 
	 * <h2>得到本证书的创建者</h2>
	 * <p>返回该证书创建者账户名。</p>
	 * @author 青阳龙野(kohgylw)
	 * @return java.lang.String 创建者
	 */
	public String getAccount() {
		return account;
	}

}
