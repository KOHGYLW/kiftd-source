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
    
    /**
     * <h2>解析播放视频文件</h2>
     * <p>根据视频文件的ID查询视频文件节点并返回节点JSON信息，以便发起播放请求。</p>
     * <pre>
     * “从此用我双眼，替你看这世界；云万里、山千叠、天尽头、城不夜……”
     * </pre>
     * @author kohgylw
     * @param request javax.servlet.http.HttpServletRequest 请求对象
     * @return String 视频节点的JSON字符串
     */
    @Override
    public String getPlayVideoJson(final HttpServletRequest request) {
        final Node f = this.foundVideo(request);
        if (f != null) {
            return gson.toJson((Object)f);
        }
        return "ERROR";
    }
}
