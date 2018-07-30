package kohgylw.kiftd.server.service;

import javax.servlet.http.*;

public interface ShowPictureService
{
    String getPreviewPictureJson(final HttpServletRequest request);
}
