package kohgylw.kiftd.server.service;

import javax.servlet.http.*;

public interface AccountService
{
    String checkLoginRequest(final HttpServletRequest request, final HttpSession session);
    
    void logout(final HttpSession session);
    
    String getPublicKey();
    
    void getNewLoginVerCode(final HttpServletRequest request, final HttpServletResponse response, final HttpSession session);
    
    /**
     * 
     * <h2>应答响应逻辑</h2>
     * <p>对于需要长期保持绘画跟踪的操作，提供定时应答以确保不退出登录。若用户已登录，则响应“pong”以进行后续应答，否则响应空字符串。</p>
     * @author 青阳龙野(kohgylw)
     * @param request javax.servlet.http.HttpServletRequest 请求对象
     * @return java.lang.String 应答内容，“pong”或“”
     */
    String doPong(final HttpServletRequest request);
    
    /**
     * 
     * <h2>修改账户的密码</h2>
     * <p>该逻辑用语处理修改账户密码的请求，必须已经开启用户修改密码功能。</p>
     * @author 青阳龙野(kohgylw)
     * @param request javax.servlet.http.HttpServletRequest 请求对象，必须包含加密后的修改密码请求
     * @return java.lang.String 修改结果，含义如下：
     * <ul>
     * <li>success 修改成功</li>
     * <li>mustlogin 未登录任何账户</li>
     * <li>illegal 修改密码功能被禁止</li>
     * <li>oldpwderror 旧密码输入错误，未能通过验证</li>
     * <li>needsubmitvercode 需要提交验证码</li>
     * <li>invalidnewpwd 新密码格式不合法</li>
     * <li>cannotchangepwd 出现意外错误导致密码修改失败</li>
     * <li>error 加密验证失败</li>
     * </ul>
     */
    String changePassword(final HttpServletRequest request);
}
