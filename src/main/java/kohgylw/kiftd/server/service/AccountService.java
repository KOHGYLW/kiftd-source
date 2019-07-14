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
}
