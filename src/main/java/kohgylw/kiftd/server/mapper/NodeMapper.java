package kohgylw.kiftd.server.mapper;

import kohgylw.kiftd.server.model.*;
import java.util.*;

public interface NodeMapper
{
    List<Node> queryByParentFolderId(final String pfid);
    
    /**
     * 
     * <h2>按照父文件夹的ID查找其下的所有文件（分页）</h2>
     * <p>该方法需要传入一个Map作为查询条件，其中需要包含pid（父文件夹的ID），offset（起始偏移），rows（查询行数）。</p>
     * @author 青阳龙野(kohgylw)
     * @param keyMap java.util.Map 封装查询条件的Map对象
     * @return java.util.List 查询结果
     */
    List<Node> queryByParentFolderIdSection(final Map<String, Object> keyMap);
    
    /**
     * 
     * <h2>按照父文件夹的ID统计其下的所有文件数目</h2>
     * <p>该方法主要用于配合queryByParentFolderIdSection方法实现分页加载。</p>
     * @author 青阳龙野(kohgylw)
     * @param pfid java.lang.String 父文件夹ID
     * @return long 文件总数
     */
    long countByParentFolderId(final String pfid);
    
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
