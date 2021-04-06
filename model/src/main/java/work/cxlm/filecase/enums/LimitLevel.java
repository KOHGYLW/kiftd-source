package work.cxlm.filecase.enums;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import work.cxlm.filecase.domain.Folder;
import work.cxlm.filecase.domain.User;

import java.util.Objects;

/**
 * 文件夹开放等级
 * create 2021/3/31 16:40
 *
 * @author Chiru
 */
public enum LimitLevel implements ValueEnum<Integer> {

    /**
     * 完全开放，任何用户都有读写权限
     */
    COMPLETE_OPEN(1),

    /**
     * 对创建者开放，其他用户、匿名用户使用密码后可以获得读写权限
     */
    PWD_PROTECTED_OPEN(2),

    /**
     * 对创建者开放，其他用户、匿名用户仅有读权限，无法修改其内容
     */
    READ_FREE(3),

    /**
     * 对创建者开放，其他用户、匿名用户使用密码后可以获得读权限
     */
    PWD_PROTECTED_READ(4),

    /**
     * 对创建者开放，其他用户、匿名用户无法浏览、修改
     */
    CREATOR_ONLY(5);

    /**
     * 数据库中存储的编码
     */
    private final Integer value;

    LimitLevel(Integer value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    /**
     * 判断用户是否拥有读权限，不使用密码
     *
     * @param user   指定用户
     * @param folder 指定文件夹
     * @return 该用户是否拥有读权限
     */
    public static boolean canUserRead(@Nullable User user, @NonNull Folder folder) {
        Assert.notNull(folder, "文件夹实例不能为 null");
        // 完全开放时，谁都能读，user 不传也能读
        LimitLevel folderLimitLevel = folder.getLimitLevel();
        if (COMPLETE_OPEN == folderLimitLevel || READ_FREE == folderLimitLevel) {
            return true;
        }
        if (null == user) {
            return false;
        }
        // 创建者拥有读写权限
        return user.getId().equals(folder.getCreatorId());
    }

    /**
     * 判断用户是否拥有读权限，使用密码
     *
     * @param user   指定用户
     * @param folder 指定文件夹
     * @param pwd    用户提供的密码
     * @return 该用户是否拥有读权限
     */
    public static boolean canUserRead(@Nullable User user, @NonNull Folder folder, @Nullable String pwd) {
        boolean canReadWithoutPwd = canUserRead(user, folder);
        if (canReadWithoutPwd) {
            return true;
        }
        // 限制等级为密码保护，密码匹配时，提供读权限
        LimitLevel folderLimitLevel = folder.getLimitLevel();
        String pwdMd5 = DigestUtils.md5Hex(pwd);
        boolean openWithPwd = folderLimitLevel == PWD_PROTECTED_OPEN || folderLimitLevel == PWD_PROTECTED_READ;
        return openWithPwd && Objects.equals(pwdMd5, folder.getPwd());
    }

    /**
     * 判断用户是否拥有写权限，不使用密码
     *
     * @param user   指定用户
     * @param folder 指定文件夹
     * @return 该用户是否拥有写权限
     */
    public static boolean canUserWrite(@Nullable User user, @NonNull Folder folder) {
        Assert.notNull(folder, "文件夹实例不能为 null");
        // 完全开放时，谁都能写，user 不传也能读
        LimitLevel folderLimitLevel = folder.getLimitLevel();
        if (COMPLETE_OPEN == folderLimitLevel) {
            return true;
        }
        if (null == user) {
            return false;
        }
        // 创建者拥有读写权限
        return user.getId().equals(folder.getCreatorId());
    }

    /**
     * 判断用户是否拥有写权限，使用密码
     *
     * @param user   指定用户
     * @param folder 指定文件夹
     * @param pwd    用户提供的密码
     * @return 该用户是否拥有写权限
     */
    public static boolean canUserWrite(@Nullable User user, @NonNull Folder folder, @Nullable String pwd) {
        boolean canWriteWithoutPwd = canUserWrite(user, folder);
        if (canWriteWithoutPwd) {
            return true;
        }
        // 限制等级为密码保护，密码匹配时，提供写权限
        LimitLevel folderLimitLevel = folder.getLimitLevel();
        String pwdMd5 = DigestUtils.md5Hex(pwd);
        return folderLimitLevel == PWD_PROTECTED_OPEN && Objects.equals(pwdMd5, folder.getPwd());
    }

}
