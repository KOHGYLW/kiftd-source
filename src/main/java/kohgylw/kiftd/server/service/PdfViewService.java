package kohgylw.kiftd.server.service;

import javax.servlet.http.*;
import kohgylw.kiftd.server.model.*;

public interface PdfViewService
{
    Node foundPdf(final HttpServletRequest request);
}
