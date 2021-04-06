package work.cxlm.filecase.enums;

import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import work.cxlm.filecase.domain.Folder;
import work.cxlm.filecase.domain.User;

/**
 * 单测：判断权限判定是否正确
 */
public class LimitLevelTest {

    User admin = new User(1, "admin", null);
    Folder folder = new Folder();

    private void testReadWithDataSet(TestData... testData) {
        for (TestData testDatum : testData) {
            folder.setLimitLevel(testDatum.level);
            Assert.assertEquals(LimitLevel.canUserRead(admin, folder), testDatum.exceptForAdmin);
            Assert.assertEquals(LimitLevel.canUserRead(User.ANONYMOUS_USER, folder), testDatum.exceptForAnon);
        }
    }

    private void testReadWithPwdDataSet(TestData... testData) {
        for (TestData testDatum : testData) {
            folder.setLimitLevel(testDatum.level);
            Assert.assertEquals(LimitLevel.canUserRead(admin, folder, testDatum.pwd), testDatum.exceptForAdmin);
            Assert.assertEquals(LimitLevel.canUserRead(User.ANONYMOUS_USER, folder, testDatum.pwd), testDatum.exceptForAnon);
        }
    }

    private void testWriteWithDataSet(TestData... testData) {
        for (TestData testDatum : testData) {
            folder.setLimitLevel(testDatum.level);
            Assert.assertEquals(LimitLevel.canUserWrite(admin, folder), testDatum.exceptForAdmin);
            Assert.assertEquals(LimitLevel.canUserWrite(User.ANONYMOUS_USER, folder), testDatum.exceptForAnon);
        }
    }

    private void testWriteWithPwdDataSet(TestData... testData) {
        for (TestData testDatum : testData) {
            folder.setLimitLevel(testDatum.level);
            Assert.assertEquals(LimitLevel.canUserWrite(admin, folder, testDatum.pwd), testDatum.exceptForAdmin);
            Assert.assertEquals(LimitLevel.canUserWrite(User.ANONYMOUS_USER, folder, testDatum.pwd), testDatum.exceptForAnon);
        }
    }

    @Before
    public void beforeTest() {
        folder.setLimitLevel(null);
        folder.setCreatorId(1);
    }

    @Test
    public void testCanUserRead() {
        testReadWithDataSet(
                new TestData(LimitLevel.COMPLETE_OPEN, true, true),
                new TestData(LimitLevel.PWD_PROTECTED_OPEN, true, false),
                new TestData(LimitLevel.READ_FREE, true, true),
                new TestData(LimitLevel.PWD_PROTECTED_READ, true, false),
                new TestData(LimitLevel.CREATOR_ONLY, true, false));
    }

    @Test
    public void testCanUserReadWithPwd() {
        folder.setPwd(DigestUtils.md5Hex("ChiruMori"));
        testReadWithPwdDataSet(
                // 密码正确时
                new TestData(LimitLevel.COMPLETE_OPEN, true, true, "ChiruMori"),
                new TestData(LimitLevel.PWD_PROTECTED_OPEN, true, true, "ChiruMori"),
                new TestData(LimitLevel.READ_FREE, true, true, "ChiruMori"),
                new TestData(LimitLevel.PWD_PROTECTED_READ, true, true, "ChiruMori"),
                new TestData(LimitLevel.CREATOR_ONLY, true, false, "ChiruMori"),
                // 密码错误时
                new TestData(LimitLevel.COMPLETE_OPEN, true, true, "ChiruMorj"),
                new TestData(LimitLevel.PWD_PROTECTED_OPEN, true, false, "ChiruMorj"),
                new TestData(LimitLevel.READ_FREE, true, true, "ChiruMorj"),
                new TestData(LimitLevel.PWD_PROTECTED_READ, true, false, "ChiruMorj"),
                new TestData(LimitLevel.CREATOR_ONLY, true, false, "ChiruMorj"));
    }

    @Test
    public void testCanUserWrite() {
        testWriteWithDataSet(
                new TestData(LimitLevel.COMPLETE_OPEN, true, true),
                new TestData(LimitLevel.PWD_PROTECTED_OPEN, true, false),
                new TestData(LimitLevel.READ_FREE, true, false),
                new TestData(LimitLevel.PWD_PROTECTED_READ, true, false),
                new TestData(LimitLevel.CREATOR_ONLY, true, false));
    }

    @Test
    public void testCanUserWriteWithPwd() {
        folder.setPwd(DigestUtils.md5Hex("ChiruMori"));
        testWriteWithPwdDataSet(
                // 密码正确时
                new TestData(LimitLevel.COMPLETE_OPEN, true, true, "ChiruMori"),
                new TestData(LimitLevel.PWD_PROTECTED_OPEN, true, true, "ChiruMori"),
                new TestData(LimitLevel.READ_FREE, true, false, "ChiruMori"),
                new TestData(LimitLevel.PWD_PROTECTED_READ, true, false, "ChiruMori"),
                new TestData(LimitLevel.CREATOR_ONLY, true, false, "ChiruMori"),
                // 密码错误时
                new TestData(LimitLevel.COMPLETE_OPEN, true, true, "ChiruMorj"),
                new TestData(LimitLevel.PWD_PROTECTED_OPEN, true, false, "ChiruMorj"),
                new TestData(LimitLevel.READ_FREE, true, false, "ChiruMorj"),
                new TestData(LimitLevel.PWD_PROTECTED_READ, true, false, "ChiruMorj"),
                new TestData(LimitLevel.CREATOR_ONLY, true, false, "ChiruMorj"));
    }

    @AllArgsConstructor
    private static class TestData {
        public LimitLevel level;
        public boolean exceptForAdmin;
        public boolean exceptForAnon;
        public String pwd;

        public TestData(LimitLevel level, boolean admin, boolean anon) {
            this.level = level;
            exceptForAdmin = admin;
            exceptForAnon = anon;
        }
    }
}