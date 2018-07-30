package kohgylw.kiftd.server.pojo;

public class LoginInfoPojo
{
    private String accountId;
    private String accountPwd;
    private String time;
    
    public String getAccountId() {
        return this.accountId;
    }
    
    public void setAccountId(final String accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountPwd() {
        return this.accountPwd;
    }
    
    public void setAccountPwd(final String accountPwd) {
        this.accountPwd = accountPwd;
    }
    
    public String getTime() {
        return this.time;
    }
    
    public void setTime(final String time) {
        this.time = time;
    }
}
