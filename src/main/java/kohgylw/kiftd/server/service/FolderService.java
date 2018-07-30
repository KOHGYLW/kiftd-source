package kohgylw.kiftd.server.service;

import javax.servlet.http.*;

public interface FolderService
{
    String newFolder(final HttpServletRequest request);
    
    String deleteFolder(final HttpServletRequest request);
    
    String renameFolder(final HttpServletRequest request);
}
