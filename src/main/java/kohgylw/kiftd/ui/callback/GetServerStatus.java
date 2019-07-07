package kohgylw.kiftd.ui.callback;

import kohgylw.kiftd.server.enumeration.*;

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
}
