package work.cxlm.filecase.domain;

import lombok.*;
import work.cxlm.filecase.domain.base.BaseModel;
import work.cxlm.filecase.enums.LimitLevel;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * <h2>
 * 文件夹
 * <p>
 * 文件夹是文件层级、文件开放等级控制的基本单位
 * create 2021/3/31 16:25
 *
 * @author Chiru
 */
@Table(name = "folder")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Folder extends BaseModel<Integer> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件夹 ID
     *
     * @mbg.generated
     */
    @Column(name = "id")
    @Id
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    /**
     * 文件夹名
     *
     * @mbg.generated
     */
    @Column(name = "folder_name")
    private String folderName;

    /**
     * 创建时间
     *
     * @mbg.generated
     */
    @Column(name = "create_date")
    private Date createDate;

    /**
     * 文件夹创建用户
     *
     * @mbg.generated
     */
    @Column(name = "creator_id")
    private Integer creatorId;

    /**
     * 父文件夹
     *
     * @mbg.generated
     */
    @Column(name = "parent_folder_id")
    private String parentFolderId;

    /**
     * 文件夹开发等级权限
     *
     * @mbg.generated
     */
    @Column(name = "limit_level")
    private LimitLevel limitLevel;

    /**
     * 访问保护密码，如果有的话
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
