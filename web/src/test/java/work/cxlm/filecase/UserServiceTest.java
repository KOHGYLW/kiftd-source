package work.cxlm.filecase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import work.cxlm.filecase.domain.User;
import work.cxlm.filecase.service.UserService;

import java.util.List;

/**
 * create 2021/4/1 21:44
 *
 * @author Chiru
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FileCaseApplication.class)
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    public void applicationTest() {
        Assert.assertNotNull("UserService 注入失败", userService);
        User param = User.builder().id(User.ANONYMOUS_USER.getId()).build();
        List<User> selectedUser = userService.select(param);
        Assert.assertEquals(selectedUser.get(0).getNickName(), User.ANONYMOUS_USER.getNickName());
    }

    @Test
    public void crudTest() {
        Integer invalidUserId = Integer.MIN_VALUE;
        // Create
        User invalidUser = User.builder().id(invalidUserId).nickName("Chiru").build();
        userService.insert(invalidUser);
        // Read
        User uidParam = User.builder().id(invalidUserId).build();
        User selectedUser = userService.selectOne(uidParam);
        Assert.assertEquals(selectedUser.getNickName(), invalidUser.getNickName());
        // Update
        selectedUser.setNickName("Mori");
        userService.update(selectedUser);
        User updated = userService.selectOne(uidParam);
        Assert.assertEquals(updated.getNickName(), selectedUser.getNickName());
        // Delete
        userService.delete(uidParam);
        User userAfterDeleted = userService.selectOne(uidParam);
        Assert.assertNull(userAfterDeleted);
    }
}
