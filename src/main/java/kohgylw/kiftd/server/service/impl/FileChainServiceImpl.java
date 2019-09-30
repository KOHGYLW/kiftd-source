package kohgylw.kiftd.server.service.impl;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.service.FileChainService;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.ContentTypeMap;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.RangeFileStreamWriter;

@Service
public class FileChainServiceImpl extends RangeFileStreamWriter implements FileChainService {

	@Resource
	private NodeMapper nm;
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private ContentTypeMap ctm;
	@Resource
	private LogUtil lu;

	@Override
	public void getResourceBychain(HttpServletRequest request, HttpServletResponse response) {
		int statusCode = 403;
		if (ConfigureReader.instance().isOpenFileChain()) {
			final String fId = request.getParameter("fid");
			// 权限凭证有效性并确认其对应的资源
			if (fId != null) {
				Node f = this.nm.queryById(fId);
				if (f != null) {
					File target = this.fbu.getFileFromBlocks(f);
					if (target != null && target.isFile()) {
						String fileName = f.getFileName();
						String suffix = "";
						if (fileName.indexOf(".") >= 0) {
							suffix = fileName.substring(fileName.indexOf("."));
						}
						writeRangeFileStream(request, response, target, f.getFileName(), ctm.getContentType(suffix));
						if (request.getHeader("Range") == null) {
							this.lu.writeChainEvent(request, f);
						}
						return;
					}
				}
				statusCode = 404;
			}
		} else {
			statusCode = 403;
		}
		try {
			//  处理无法下载的资源
			response.sendError(statusCode);
		} catch (IOException e) {

		}
	}

}
