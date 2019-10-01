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
    
    /**
     * 
     * <h2>获取是否允许自由注册新账户</h2>
     * <p>该方法用于获取是否允许注册新账户，若允许则返回字符串“true”。</p>
     * @author 青阳龙野(kohgylw)
     * @return java.lang.String 允许与否标识，允许为“true”，否则为“false”
     */
    String isAllowSignUp();
    
    /**
     * 
     * <h2>执行账户注册</h2>
     * <p>该方法用于执行账户注册操作并返回注册结果，必须已经开启自由注册新账户功能。</p>
     * @author 青阳龙野(kohgylw)
     * @param request javax.servlet.http.HttpServletRequest 请求对象
     * @return java.lang.String 注册结果，含义如下：
     * <ul>
     * <li>success 注册成功并直接登录新账户</li>
     * <li>illegal 注册功能已被禁用</li>
     * <li>mustlogout 必须先退出当前账户</li>
     * <li>accountexists 账户名已经存在</li>
     * <li>needvercode 需要验证码验证</li>
     * <li>invalidaccount 账户名格式不合法</li>
     * <li>invalidpwd 密码格式不合法</li>
     * <li>cannotsignup 出现意外错误导致注册失败</li>
     * <li>error 加密验证失败</li>
     * </ul>
     */
    String doSignUp(final HttpServletRequest request);
}
