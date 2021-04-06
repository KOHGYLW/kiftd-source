package work.cxlm.filecase.domain.base;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 落库实体类的公用抽象父类
 * create 2021/4/1 17:56
 *
 * @author Chiru
 */
@Getter
@Setter
public abstract class BaseModel<PK extends Serializable> {

    /**
     * 从实体类对象中解析 ID
     *
     * @return 该记录的主键
     */
    public abstract PK getPk();
}
