package kohgylw.kiftd.server.mapper;

import kohgylw.kiftd.server.model.*;
import java.util.*;

public interface NodeMapper
{
    List<Node> queryByParentFolderId(final String pfid);
    
    int insert(final Node f);
    
    int deleteByParentFolderId(final String pfid);
    
    int deleteById(final String fileId);
    
    Node queryById(final String fileId);
    
    int updateFileNameById(final Map<String, String> map);
    
    List<Node> queryAll();
    
    Node queryByPath(final String path);
    
    List<Node> queryBySomeFolder(final String fileId);
}
