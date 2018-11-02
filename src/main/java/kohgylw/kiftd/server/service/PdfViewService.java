package kohgylw.kiftd.server.service;

import javax.servlet.http.*;

public interface PdfViewService
{
    void getPdfAsStream(final HttpServletRequest request,final HttpServletResponse response,String fileId);
    
}
