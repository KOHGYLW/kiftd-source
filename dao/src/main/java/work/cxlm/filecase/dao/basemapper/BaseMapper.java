package work.cxlm.filecase.dao.basemapper;

import tk.mybatis.mapper.common.ExampleMapper;
import tk.mybatis.mapper.common.IdsMapper;
import tk.mybatis.mapper.common.special.InsertListMapper;

/**
 * 通用 Mapper
 *
 * @param <T> 实体类
 * @author Chiru
 */
public interface BaseMapper<T> extends
        tk.mybatis.mapper.common.BaseMapper<T>,
        IdsMapper<T>,
        ExampleMapper<T>,
        InsertListMapper<T> {
}
