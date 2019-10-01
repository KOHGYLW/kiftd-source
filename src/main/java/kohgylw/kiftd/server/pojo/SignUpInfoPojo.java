package kohgylw.kiftd.server.pojo;

/**
 * 
 * <h2>注册信息封装类</h2>
 * <p>该封装类用于封装前端发送的JSON注册信息。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class SignUpInfoPojo {
	
	private String account;
	private String pwd;
	private String time;
	public String getAccount() {
		return account;
	}
	public void setAccount(String account) {
		this.account = account;
	}
	public String getPwd() {
		return pwd;
	}
	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}

}
