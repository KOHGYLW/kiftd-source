package kohgylw.kiftd.server.mapper;

import kohgylw.kiftd.server.model.*;
import java.util.*;

public interface NodeMapper
{
    List<Node> queryByParentFolderId(final String pfid);
    
    int insert(final Node f);
    
    int update(final Node f);
    
    int deleteByParentFolderId(final String pfid);
    
    int deleteById(final String fileId);
    
    Node queryById(final String fileId);
    
    int updateFileNameById(final Map<String, String> map);
    
    List<Node> queryAll();
    
    Node queryByPath(final String path);
    
    List<Node> queryBySomeFolder(final String fileId);
    
    /**
     * 
     * <h2>移动文件节点至指定的文件夹节点</h2>
     * <p>该映射用于实现移动文件（剪切-粘贴）功能，将某一文件节点的父级路径更改为新的父级路径。</p>
     * @author 青阳龙野(kohgylw)
     * @param Map<String,String> 其中要包括要移动的文件节点id（fileId）以及新的父级文件夹id（locationpath）
     * @return 影响数目
     */
    int moveById(final Map<String, String> map);
}
