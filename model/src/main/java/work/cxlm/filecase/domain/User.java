package work.cxlm.filecase.domain;

import lombok.*;
import work.cxlm.filecase.domain.base.BaseModel;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * 用户
 * create 2021/3/31 16:04
 *
 * @author Chiru
 */
@Table(name = "user")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User extends BaseModel<Integer> implements Serializable {

    private static final long serialVersionUID = 1L;

    public final static User ANONYMOUS_USER = new User(-1, "访客", null);

    /**
     * 用户 id
     *
     * @mbg.generated
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    /**
     * 用户显示昵称
     *
     * @mbg.generated
     */
    @Column(name = "nick_name")
    private String nickName;

    /**
     * 用户密码，在数据库中加密存储
     *
     * @mbg.generated
     */
    @Column(name = "pwd")
    private String pwd;

    @Override
    public Integer getPk() {
        return id;
    }
}
