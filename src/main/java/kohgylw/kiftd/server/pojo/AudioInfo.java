package kohgylw.kiftd.server.pojo;

public class AudioInfo
{
    private String name;
    private String artist;
    private String url;
    private String cover;
    private String lrc;
    
    public String getName() {
        return this.name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public String getArtist() {
        return this.artist;
    }
    
    public void setArtist(final String artist) {
        this.artist = artist;
    }
    
    public String getUrl() {
        return this.url;
    }
    
    public void setUrl(final String url) {
        this.url = url;
    }
    
    public String getCover() {
        return this.cover;
    }
    
    public void setCover(final String cover) {
        this.cover = cover;
    }
    
    public String getLrc() {
        return this.lrc;
    }
    
    public void setLrc(final String lrc) {
        this.lrc = lrc;
    }
}
