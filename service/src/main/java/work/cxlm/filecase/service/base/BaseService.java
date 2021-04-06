package work.cxlm.filecase.service.base;

import com.github.pagehelper.PageInfo;
import tk.mybatis.mapper.entity.Example;
import work.cxlm.filecase.domain.base.BaseModel;

import java.io.Serializable;
import java.util.List;

/**
 * create 2021/4/1 15:57
 *
 * @param <PK> 主键类型
 * @param <T>  记录类型
 * @author Chiru
 */
public interface BaseService<T extends BaseModel<PK>, PK extends Serializable> {

    //========== 增 ============

    /**
     * 新增数据，值为 null 的字段不会保存（可以保留字段的默认值）
     * <p>
     * SQL 示例:
     * INSERT INTO xxx(id,c1,c2) VALUES(?,?,?)
     *
     * @param record 待保存的数据
     * @return int 影响行数
     */
    int insertSelective(T record);

    /**
     * 新增数据，值为 null 的字段也会保存
     * <p>
     * 底层使用 #{@link tk.mybatis.mapper.common.base.insert.InsertMapper#insert(Object)} 方法，
     * 与 #{@link tk.mybatis.mapper.common.MySqlMapper#insertUseGeneratedKeys(Object)} 的区别是
     * 适用于所有数据库，且可以直接插入主键，insertUseGeneratedKeys 要求主键必须为自增且字段名称为 id
     * <p>
     * SQL 示例:
     * INSERT INTO xxx(id,c1,c2,...,cn) VALUES (?,?,...,?)
     *
     * @param record 待保存的数据
     * @return int 影响行数
     */
    int insert(T record);

    /**
     * 批量新增数据，值为 null 的字段不会保存（可以保留字段的默认值）
     *
     * @param records 待保存的数据集合
     * @return int 影响行数
     */
    int insertList(Iterable<T> records);

    //============= 更新 ==============

    /**
     * 根据主键更新数据，值为 null 的字段不会更新
     * <p>
     * 必须提供主键，否则 SQL 会执行成功但不会更新任何数据
     * <p>
     * SQL 示例:
     * UPDATE xxx SET c1 = ? WHERE id = ?
     *
     * @param record 待更新的数据
     * @return int 影响行数
     */
    int update(T record);

    /**
     * 根据主键更新数据，值为 null 的字段会更新为 null
     * <p>
     * 必须提供主键，否则 SQL 会执行成功但不会更新任何数据
     * <p>
     * SQL 示例:
     * UPDATE xxx SET c1 = ?, c2 = ?,..., cn = ? WHERE id = ?
     *
     * @param record 待更新的数据
     * @return int 影响行数
     */
    int updateUnchecked(T record);

    /**
     * 根据 Example 条件更新数据，值为 null 的字段不会更新
     * <p>
     * SQL 示例:
     * UPDATE xxx SET c1 = ? WHERE c2 = ?
     *
     * @param record  待更新的数据
     * @param example 更新条件
     * @return int 影响行数
     */
    int updateByExample(T record, Example example);

    /**
     * 根据 Example 条件更新数据，值为 null 的字段会更新为 null
     * <p>
     * SQL 示例:
     * UPDATE xxx SET c1 = ?, c3 = ?,...,cn = ? WHERE c2 = ?
     *
     * @param record  待更新的数据
     * @param example 更新条件
     * @return int 影响行数
     */
    int updateUncheckedByExample(T record, Example example);

    //============ 删除 ==============

    /**
     * 根据主键删除数据
     * <p>
     * SQL 示例:
     * DELETE FROM xxx WHERE id = ?
     *
     * @param pk 主键
     * @return int 影响行数
     */
    int deleteByPk(PK pk);

    /**
     * 根据主键集合删除数据
     * <p>
     * SQL 示例:
     * DELETE FROM xxx WHERE id IN (?,?,?)
     *
     * @param pks 主键集合
     * @return int 影响行数
     */
    int deleteByPks(Iterable<? extends PK> pks);

    /**
     * 根据条件删除数据
     * <p>
     * SQL 示例:
     * DELETE FROM xxx WHERE c1 = ?
     *
     * @param param 删除条件
     * @return int 影响行数
     */
    int delete(T param);

    /**
     * 删除全部数据
     * <p>
     * SQL 示例:
     * DELETE FROM xxx
     *
     * @return int 影响行数
     */
    int deleteAll();

    /**
     * 根据 Example 条件删除数据
     * <p>
     * SQL 示例:
     * DELETE FROM xxx WHERE c1 IN (?,?,?)
     *
     * @param example 删除条件
     * @return int 影响行数
     */
    int deleteByExample(Example example);

    //============ 查询 ==============

    /**
     * 根据主键查询数据
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE id = ?
     *
     * @param pk 主键
     * @return T 实体
     */
    T selectByPk(PK pk);

    /**
     * 根据主键集合查询数据
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE id IN (?,?,?)
     *
     * @param pks 主键集合
     * @return List<T> 实体集合
     */
    List<T> selectByPks(Iterable<? extends PK> pks);

    /**
     * 根据条件查询数据集合
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE c1 = ?
     *
     * @param param 查询条件
     * @return List<T> 实体集合
     */
    List<T> select(T param);

    /**
     * 查询全部数据
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx
     *
     * @return List<T> 实体集合
     */
    List<T> selectAll();

    /**
     * 根据 Example 条件查询数据集合
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE c1 IN (?,?,?)
     *
     * @param example 查询条件
     * @return List<T> 实体集合
     */
    List<T> selectByExample(Example example);

    /**
     * 根据条件查询单条数据
     * <p>
     * 该 SQL 只能有一个返回值，否则会抛出异常
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE c1 = ?
     *
     * @param param 查询条件
     * @return T 实体
     */
    T selectOne(T param);

    /**
     * 根据条件查询单条数据
     * <p>
     * 该 SQL 会取第一条数据返回
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE c1 = ? LIMIT 1
     *
     * @param param 查询条件
     * @return T 实体
     */
    T selectLimitOne(T param);

    /**
     * 根据 Example 条件查询单条数据
     * <p>
     * 该 SQL 只能有一个返回值，否则会抛出异常
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE c1 IN (?)
     *
     * @param example 查询条件
     * @return T 实体
     */
    T selectOneByExample(Example example);

    /**
     * 根据 Example 条件查询单条数据
     * <p>
     * 该 SQL 只能有一个返回值，否则会抛出异常
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE c1 IN (?) LIMIT 1
     *
     * @param example 查询条件
     * @return T 实体
     */
    T selectLimitOneByExample(Example example);

    /**
     * 根据 `=` 条件查询数据数量
     * <p>
     * SQL 示例:
     * SELECT COUNT(id) FROM xxx WHERE c1 = ?
     *
     * @param param 查询条件
     * @return int 数据量
     */
    long selectCount(T param);

    /**
     * 根据 Example 条件查询数据数量
     * <p>
     * SQL 示例:
     * SELECT COUNT(id) FROM xxx WHERE c1 IN (?,?,?)
     *
     * @param example 查询条件
     * @return int 数据量
     */
    long selectCountByExample(Example example);

    /**
     * 根据 Example 条件分页查询，不会查询 count
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE c1 IN (?,?,?) LIMIT ?,?
     *
     * @param example  查询条件
     * @param pageNum  分页页码
     * @param pageSize 分页数量
     * @return List<T> 分页实体
     */
    List<T> selectPageByExample(Example example, int pageNum, int pageSize);

    /**
     * 根据条件分页查询，同时查询 count
     * 若同时需要排序，可手动指定PageHelper.orderBy()
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE c1 = ? LIMIT ?,?
     * SELECT COUNT(id) FROM xxx WHERE c1 = ?
     *
     * @param param  查询条件
     * @return PageInfo<T> 分页实体
     */
    PageInfo<T> selectPageAndCount(T param);

    /**
     * 根据 Example 条件分页查询，同时查询 count
     * <p>
     * SQL 示例:
     * SELECT id, c1, c2,...,cn FROM xxx WHERE c1 IN (?,?,?) LIMIT ?,?
     * SELECT COUNT(id) FROM xxx WHERE c1 IN (?,?,?)
     *
     * @param example  查询条件
     * @param pageNum  分页页码
     * @param pageSize 分页数量
     * @return PageInfo<T> 分页实体
     */
    PageInfo<T> selectPageAndCountByExample(Example example, int pageNum, int pageSize);

}
