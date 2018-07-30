package kohgylw.kiftd.server.pojo;

public class PublicKeyInfo
{
    private String publicKey;
    private long time;
    
    public String getPublicKey() {
        return this.publicKey;
    }
    
    public void setPublicKey(final String publicKey) {
        this.publicKey = publicKey;
    }
    
    public long getTime() {
        return this.time;
    }
    
    public void setTime(final long time) {
        this.time = time;
    }
}
