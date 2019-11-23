package kohgylw.kiftd.ui.callback;

import java.util.List;

import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.ui.pojo.FileSystemPath;

public interface GetServerStatus
{
    int getPropertiesStatus();
    
    boolean getServerStatus();
    
    int getPort();
    
    String getInitProt();
    
    int getBufferSize();
    
    String getInitBufferSize();
    
    LogLevel getLogLevel();
    
    LogLevel getInitLogLevel();
    
    VCLevel getVCLevel();
    
    VCLevel getInitVCLevel();
    
    String getFileSystemPath();
    
    String getInitFileSystemPath();
    
    boolean getMustLogin();
    
    boolean isAllowChangePassword();
    
    boolean isOpenFileChain();
    
    List<FileSystemPath> getExtendStores();
    
    int getMaxExtendStoresNum();
}
