package kohgylw.kiftd.server.pojo;

import java.util.*;
import kohgylw.kiftd.server.model.*;

public class FolderView{

	private Folder folder;
    private List<Folder> parentList;
    private List<Folder> folderList;
    private List<Node> fileList;
    private String account;
    private List<String> authList;
    private String publishTime;
    
    public Folder getFolder() {
        return this.folder;
    }
    
    public void setFolder(final Folder folder) {
        this.folder = folder;
    }
    
    public List<Folder> getParentList() {
        return this.parentList;
    }
    
    public void setParentList(final List<Folder> parentList) {
        this.parentList = parentList;
    }
    
    public List<Folder> getFolderList() {
        return this.folderList;
    }
    
    public void setFolderList(final List<Folder> folderList) {
        this.folderList = folderList;
    }
    
    public List<Node> getFileList() {
        return this.fileList;
    }
    
    public void setFileList(final List<Node> fileList) {
        this.fileList = fileList;
    }
    
    public List<String> getAuthList() {
        return this.authList;
    }
    
    public void setAuthList(final List<String> authList) {
        this.authList = authList;
    }
    
    public String getAccount() {
        return this.account;
    }
    
    public void setAccount(final String account) {
        this.account = account;
    }
    
    public String getPublishTime() {
        return this.publishTime;
    }
    
    public void setPublishTime(final String publishTime) {
        this.publishTime = publishTime;
    }
}
