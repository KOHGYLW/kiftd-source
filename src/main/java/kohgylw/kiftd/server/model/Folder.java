package kohgylw.kiftd.server.model;

public class Folder
{
    private String folderId;
    private String folderName;
    private String folderCreationDate;
    private String folderCreator;
    private String folderParent;
    private int folderConstraint;
    
    public String getFolderId() {
        return this.folderId;
    }
    
    public void setFolderId(final String folderId) {
        this.folderId = folderId;
    }
    
    public String getFolderName() {
        return this.folderName;
    }
    
    public void setFolderName(final String folderName) {
        this.folderName = folderName;
    }
    
    public String getFolderCreationDate() {
        return this.folderCreationDate;
    }
    
    public void setFolderCreationDate(final String folderCreationDate) {
        this.folderCreationDate = folderCreationDate;
    }
    
    public String getFolderCreator() {
        return this.folderCreator;
    }
    
    public void setFolderCreator(final String folderCreator) {
        this.folderCreator = folderCreator;
    }
    
    public String getFolderParent() {
        return this.folderParent;
    }
    
    public void setFolderParent(final String folderParent) {
        this.folderParent = folderParent;
    }

	public int getFolderConstraint() {
		return folderConstraint;
	}

	public void setFolderConstraint(int folderConstraint) {
		this.folderConstraint = folderConstraint;
	}
}
