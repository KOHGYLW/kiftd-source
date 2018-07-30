package kohgylw.kiftd.server.service;

import javax.servlet.http.*;

public interface FolderViewService
{
    String getFolderViewToJson(final String fid, final HttpSession session, final HttpServletRequest request);
}
