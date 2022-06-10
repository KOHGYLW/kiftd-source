package kohgylw.kiftd.server.mapper;

import kohgylw.kiftd.server.model.*;
import java.util.*;

public interface NodeMapper {
	/**
	 * 
	 * <h2>根据文件夹ID查询其中的所有文件节点</h2>
	 * <p>
	 * 该方法用于一次性将目标文件夹下的全部文件节点查询出来，如果超过限值，则只查询限值内的节点数量。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param pfid
	 *            java.lang.String 目标文件夹ID
	 * @return java.util.List 文件节点列表
	 */
	List<Node> queryByParentFolderId(final String pfid);

	/**
	 * 
	 * <h2>按照父文件夹的ID查找其下的所有文件（分页）</h2>
	 * <p>
	 * 该方法需要传入一个Map作为查询条件，其中需要包含pid（父文件夹的ID），offset（起始偏移），rows（查询行数）。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param keyMap
	 *            java.util.Map 封装查询条件的Map对象
	 * @return java.util.List 查询结果
	 */
	List<Node> queryByParentFolderIdSection(final Map<String, Object> keyMap);

	/**
	 * 
	 * <h2>按照父文件夹的ID统计其下的所有文件数目</h2>
	 * <p>
	 * 该方法主要用于配合queryByParentFolderIdSection方法实现分页加载。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param pfid
	 *            java.lang.String 父文件夹ID
	 * @return long 文件总数
	 */
	long countByParentFolderId(final String pfid);

	int insert(final Node f);

	int update(final Node f);

	int deleteById(final String fileId);

	Node queryById(final String fileId);

	int updateFileNameById(final Map<String, String> map);

	/**
	 * 
	 * <h2>根据文件块DI查询对所有对应的节点</h2>
	 * <p>
	 * 该方法用于查询某个文件块ID所对应的所有节点副本，如果超过限值，则只查询限值内的节点数量。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path
	 *            java.lang.String 目标文件块ID
	 * @return java.util.List 文件节点列表
	 */
	List<Node> queryByPath(final String path);

	/**
	 * 
	 * <h2>根据文件块DI查询对所有对应的节点，并排除指定节点</h2>
	 * <p>
	 * 该方法用于查询某个文件块ID所对应的所有节点副本，结果中不会包括指定ID的节点，如果超过限值，则只查询限值内的节点数量。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param map
	 *            java.util.Map 其中必须包含：path 目标文件块ID，fileId 要排除的文件节点ID
	 * @return java.util.List 文件节点列表
	 */
	List<Node> queryByPathExcludeById(final Map<String, String> map);

	/**
	 * 
	 * <h2>查询与目标文件节点处于同一文件夹下的全部文件节点</h2>
	 * <p>
	 * 该方法用于一次性将与目标文件同文件夹的文件节点查询出来，如果超过限值，则只查询限值内的节点数量。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param fileId
	 *            java.lang.String 目标文件ID
	 * @return java.util.List 文件节点列表
	 */
	List<Node> queryBySomeFolder(final String fileId);

}
