package kohgylw.kiftd.ui.callback;

import java.util.List;

import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.ui.pojo.FileSystemPath;

public interface GetServerStatus
{
    boolean getPropertiesStatus();
    
    boolean getServerStatus();
    
    int getPort();
    
    int getBufferSize();
    
    LogLevel getLogLevel();
    
    VCLevel getVCLevel();
    
    String getFileSystemPath();
    
    boolean getMustLogin();
    
    List<FileSystemPath> getExtendStores();
}
