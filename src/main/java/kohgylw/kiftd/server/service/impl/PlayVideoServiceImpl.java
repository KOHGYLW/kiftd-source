package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;

import com.google.gson.Gson;

import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import javax.servlet.http.*;
import kohgylw.kiftd.server.model.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.util.*;

@Service
public class PlayVideoServiceImpl implements PlayVideoService
{
    @Resource
    private NodeMapper fm;
    @Resource
    private Gson gson;
    
    private Node foundVideo(final HttpServletRequest request) {
        final String fileId = request.getParameter("fileId");
        if (fileId != null && fileId.length() > 0) {
            final Node f = this.fm.queryById(fileId);
            if (f != null) {
                final String account = (String)request.getSession().getAttribute("ACCOUNT");
                if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
                    final String fileName = f.getFileName();
                    final String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                    if (suffix.equals("mp4") || suffix.equals("webm")) {
                        return f;
                    }
                }
            }
        }
        return null;
    }
    
    @Override
    public String getPlayVideoJson(final HttpServletRequest request) {
        final Node f = this.foundVideo(request);
        if (f != null) {
            return gson.toJson((Object)f);
        }
        return "ERROR";
    }
}
