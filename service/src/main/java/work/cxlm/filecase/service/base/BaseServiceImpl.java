package work.cxlm.filecase.service.base;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import org.springframework.util.Assert;
import tk.mybatis.mapper.entity.Example;
import work.cxlm.filecase.dao.basemapper.BaseMapper;
import work.cxlm.filecase.domain.base.BaseModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * create 2021/4/1 17:29
 *
 * @author Chiru
 */
public abstract class BaseServiceImpl<T extends BaseModel<PK>, PK extends Serializable> implements BaseService<T, PK> {

    protected final BaseMapper<T> mapper;

    protected BaseServiceImpl(BaseMapper<T> mapper) {
        this.mapper = mapper;
    }

    @Override
    public int insertSelective(T record) {
        Assert.notNull(record, "不能插入为 null 的记录");
        initDefaultValue(record);
        return mapper.insertSelective(record);
    }

    @Override
    public int insert(T record) {
        Assert.notNull(record, "不能插入为 null 的记录");
        initDefaultValue(record);
        return mapper.insert(record);
    }

    @Override
    public int insertList(Iterable<T> records) {
        Assert.notNull(records, "要插入的列表不能为 null");
        List<T> list;
        if (records instanceof List) {
            list = (List<T>) records;
        } else {
            list = new ArrayList<>(16);
            records.forEach(list::add);
        }
        return mapper.insertList(list);
    }

    @Override
    public int update(T record) {
        Assert.notNull(record, "不能更新为 null 的实例");
        Assert.notNull(record.getPk(), "不能更新主键为 null 的记录");
        return mapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateUnchecked(T record) {
        Assert.notNull(record, "不能更新为 null 的实例");
        Assert.notNull(record.getPk(), "不能更新主键为 null 的记录");
        return mapper.updateByPrimaryKey(record);
    }

    @Override
    public int updateByExample(T record, Example example) {
        Assert.notNull(record, "不能更新为 Null 的记录");
        Assert.notNull(example, "筛选条件不能为 Null");
        return mapper.updateByExampleSelective(record, example);
    }

    @Override
    public int updateUncheckedByExample(T record, Example example) {
        Assert.notNull(record, "不能更新为 Null 的记录");
        Assert.notNull(example, "筛选条件不能为 Null");
        return mapper.updateByExample(record, example);
    }

    @Override
    public int deleteByPk(PK pk) {
        Assert.notNull(pk, "主键不能为 null");
        return mapper.deleteByPrimaryKey(pk);
    }

    @Override
    public int deleteByPks(Iterable<? extends PK> pks) {
        Assert.notNull(pks, "主键列表不能为 null");
        Assert.isTrue(!Iterables.isEmpty(pks), "必须指定至少一个主键");
        String pksStr = Joiner.on(',').skipNulls().join(pks);
        return mapper.deleteByIds(pksStr);
    }

    @Override
    public int delete(T param) {
        Assert.notNull(param, "筛选条件不能为 null");
        return mapper.delete(param);
    }

    @Override
    public int deleteAll() {
        return mapper.delete(null);
    }

    @Override
    public int deleteByExample(Example example) {
        Assert.notNull(example, "筛选条件不能为 null");
        return mapper.deleteByExample(example);
    }

    @Override
    public T selectByPk(PK pk) {
        Assert.notNull(pk, "主键不能为 null");
        return mapper.selectByPrimaryKey(pk);
    }

    @Override
    public List<T> selectByPks(Iterable<? extends PK> pks) {
        Assert.notNull(pks, "主键不能为 null");
        Assert.isTrue(!Iterables.isEmpty(pks), "请至少指定一个主键");
        String pksStr = Joiner.on(',').skipNulls().join(pks);
        return mapper.selectByIds(pksStr);
    }

    @Override
    public List<T> select(T param) {
        Assert.notNull(param, "筛选规则不能为 null");
        // 可以在这里拓展按字段排序的规则
        return mapper.select(param);
    }

    @Override
    public List<T> selectAll() {
        return mapper.selectAll();
    }

    @Override
    public List<T> selectByExample(Example example) {
        Assert.notNull(example, "筛选规则不能为 null");
        return mapper.selectByExample(example);
    }

    @Override
    public T selectOne(T param) {
        Assert.notNull(param, "筛选规则不能为 null");
        return mapper.selectOne(param);
    }

    @Override
    public T selectLimitOne(T param) {
        Assert.notNull(param, "筛选规则不能为 null");
        Page<T> page = PageHelper.offsetPage(0, 1, false).doSelectPage(
                () -> mapper.select(param)
        );
        return page.size() > 0 ? page.get(0) : null;
    }

    @Override
    public T selectOneByExample(Example example) {
        Assert.notNull(example, "筛选规则不能为 null");
        return mapper.selectOneByExample(example);
    }

    @Override
    public T selectLimitOneByExample(Example example) {
        Assert.notNull(example, "筛选规则不能为 null");
        Page<T> page = PageHelper.offsetPage(0, 1, false).doSelectPage(
                () -> mapper.selectByExample(example)
        );
        return page.size() > 0 ? page.get(0) : null;
    }

    @Override
    public long selectCount(T param) {
        Assert.notNull(param, "筛选规则不能为 null");
        return mapper.selectCount(param);
    }

    @Override
    public long selectCountByExample(Example example) {
        Assert.notNull(example, "筛选规则不能为 null");
        return mapper.selectCountByExample(example);
    }

    @Override
    public List<T> selectPageByExample(Example example, int pageNum, int pageSize) {
        Assert.notNull(example, "筛选规则不能为 null");
        return PageHelper.startPage(pageNum, pageSize, false).doSelectPage(
                () -> mapper.selectByExample(example)
        );
    }

    @Override
    public PageInfo<T> selectPageAndCount(T param) {
        return null;
    }

    @Override
    public PageInfo<T> selectPageAndCountByExample(Example example, int pageNum, int pageSize) {
        return null;
    }


    /**
     * 初始化需要初始化的字段
     *
     * @param record 要补充初始值的记录
     */
    protected void initDefaultValue(T record) {
        // 如果子类不重写就什么都不做
    }
}
