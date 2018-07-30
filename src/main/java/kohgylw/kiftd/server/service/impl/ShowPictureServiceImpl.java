package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.*;
import org.springframework.stereotype.*;

import com.google.gson.Gson;

import kohgylw.kiftd.server.mapper.*;
import javax.annotation.*;
import javax.servlet.http.*;
import kohgylw.kiftd.server.pojo.*;
import kohgylw.kiftd.server.enumeration.*;
import kohgylw.kiftd.server.model.*;
import java.util.*;
import kohgylw.kiftd.server.util.*;

@Service
public class ShowPictureServiceImpl implements ShowPictureService
{
    @Resource
    private NodeMapper fm;
    @Resource
    private Gson gson;
    
    private PictureViewList foundPictures(final HttpServletRequest request) {
        final String fileId = request.getParameter("fileId");
        if (fileId != null && fileId.length() > 0) {
            final String account = (String)request.getSession().getAttribute("ACCOUNT");
            if (ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES)) {
                final List<Node> nodes = this.fm.queryBySomeFolder(fileId);
                final List<Node> pictureViewList = new ArrayList<Node>();
                int index = 0;
                for (final Node n : nodes) {
                    final String fileName = n.getFileName();
                    final String suffix = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                    if (suffix.equals("jpg") || suffix.equals("jpeg") || suffix.equals("gif") || suffix.equals("bmp") || suffix.equals("png")) {
                        pictureViewList.add(n);
                        if (!n.getFileId().equals(fileId)) {
                            continue;
                        }
                        index = pictureViewList.size() - 1;
                    }
                }
                final PictureViewList pvl = new PictureViewList();
                pvl.setIndex(index);
                pvl.setPictureViewList(pictureViewList);
                return pvl;
            }
        }
        return null;
    }
    
    public String getPreviewPictureJson(final HttpServletRequest request) {
        final PictureViewList pvl = this.foundPictures(request);
        if (pvl != null) {
            return gson.toJson((Object)pvl);
        }
        return "ERROR";
    }
}
