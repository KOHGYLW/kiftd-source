package kohgylw.kiftd.server.mapper;

import kohgylw.kiftd.server.model.*;
import java.util.*;

public interface FolderMapper
{
    Folder queryById(final String fid);
    
    List<Folder> queryByParentId(final String pid);
    
    Folder queryByParentIdAndFolderName(final Map<String, String> map);
    
    int insertNewFolder(final Folder f);
    
    int deleteById(final String folderId);
    
    int updateFolderNameById(final Map<String, String> map);
    
    int updateFolderConstraintById(final Map<String, Object> map);

	int moveById(Map<String, String> map);
}
