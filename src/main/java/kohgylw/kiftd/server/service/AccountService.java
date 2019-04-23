package kohgylw.kiftd.server.service;

import javax.servlet.http.*;

public interface AccountService
{
    String checkLoginRequest(final HttpServletRequest request, final HttpSession session);
    
    void logout(final HttpSession session);
    
    String getPublicKey();
    
    void getNewLoginVerCode(final HttpServletRequest request, final HttpServletResponse response, final HttpSession session);
}
