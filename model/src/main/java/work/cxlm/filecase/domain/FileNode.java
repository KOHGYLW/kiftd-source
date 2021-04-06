package work.cxlm.filecase.domain;

import lombok.*;
import work.cxlm.filecase.domain.base.BaseModel;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * <h2>
 * 文件节点模型
 * <p>
 * 该模型描述了 file-case 文件管理机制中的一个文件节点。
 * <br>
 * 即一个文件的抽象对象，所有外部操作均应基于此对象进行而不是直接操作文件块。
 * <br>
 * 该模型对应了文件系统数据库中的FILE表。
 *
 * @author 青阳龙野(kohgylw)
 * @author Chiru
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "job_info")
public class FileNode extends BaseModel<Long> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件 ID
     *
     * @mbg.generated
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "JDBC")
    private Long id;

    /**
     * 文件名
     *
     * @mbg.generated
     */
    @Column(name = "file_name")
    private String filename;

    /**
     * 文件大小，B
     *
     * @mbg.generated
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 所属文件夹 ID
     *
     * @mbg.generated
     */
    @Column(name = "folder_id")
    private String folderId;

    /**
     * 文件创建时间
     *
     * @mbg.generated
     */
    @Column(name = "create_date")
    private Date createDate;

    /**
     * 文件创建用户
     *
     * @mbg.generated
     */
    @Column(name = "creator_id")
    private Integer creatorId;

    /**
     * 文件在服务器存储的位置
     * 不需要返回前端、仅应在后端中使用的字段
     *
     * @mbg.generated
     */
    @Column(name = "file_path")
    @Id
    @GeneratedValue(generator = "JDBC")
    private transient String filePath;

    @Override
    public Long getPk() {
        return id;
    }
}
