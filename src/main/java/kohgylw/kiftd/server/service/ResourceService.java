package kohgylw.kiftd.server.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * <h2>资源相关服务</h2>
 * <p>
 * 该服务内集成了预览资源相关的一些操作，用于前端进行各种资源的在线预览。对于需转化的格式也将在此转化为适合 预览的流并写回前端。具体内容详见各个方法。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface ResourceService {

	/**
	 * 
	 * <h2>获取无需格式转化的流资源</h2>
	 * <p>
	 * 对于各种无需进行转化格式的资源，从文件系统中获取相应文件块并以流形式写回，支持断点续传。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param fid
	 *            java.lang.String 目标资源的fid，用于指定文件节点
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @param response
	 *            javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getResource(String fid, HttpServletRequest request, HttpServletResponse response);

	/**
	 * 
	 * <h2>获取Word资源，以PDF流格式写回</h2>
	 * <p>
	 * 获取指定ID的Word文档资源，并转化为PDF流供前端预览，方式同预览PDF。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param fileId
	 *            java.lang.String 要读取的文件节点ID
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @param response
	 *            javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getWordView(String fileId, HttpServletRequest request, HttpServletResponse response);

	/**
	 * 
	 * <h2>获取TXT资源，以PDF流格式写回</h2>
	 * <p>
	 * 获取指定ID的TXT文档资源，并转化为PDF流供前端预览，方式同预览PDF。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param fileId
	 *            java.lang.String 要读取的文件节点ID
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @param response
	 *            javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getTxtView(String fileId, HttpServletRequest request, HttpServletResponse response);

	/**
	 * 
	 * <h2>获取PPT资源，以PDF流格式写回</h2>
	 * <p>
	 * 获取指定ID的PPT文档资源，并转化为PDF流提供前端预览，方式同预览PDF。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param fileId
	 *            java.lang.String 要读取的文件节点ID
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @param response
	 *            javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getPPTView(String fileId, HttpServletRequest request, HttpServletResponse response);

	/**
	 * 
	 * <h2>获取视频解码状态</h2>
	 * <p>
	 * 获得指定视频的解码状态，并根据返回信息提示界面下一步操作。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String 转码状态，若为FIN则代表转码完毕可直接播放，否则为百分制的进度，例如“1.2”代表完成了1.2%。
	 */
	public String getVideoTranscodeStatus(HttpServletRequest request);

	/**
	 * 
	 * <h2>获取LRC文本内容</h2>
	 * <p>
	 * 以标准UTF-8编码获取LRC歌词，该方法不支持断点续传。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param fileId
	 *            java.lang.String 要获取LRC歌词文件的节点ID
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @param response
	 *            javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getLRContextByUTF8(String fileId, HttpServletRequest request, HttpServletResponse response);

	/**
	 * 
	 * <h2>获取公告信息的MD5</h2>
	 * <p>
	 * 该方法用于获取公告信息的md5值，如果无公告则会返回null。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.lang.String 公告信息的md5值，无公告时返回null。
	 */
	public String getNoticeMD5();

	/**
	 * 
	 * <h2>获取公告信息的HTML内容</h2>
	 * <p>
	 * 该方法将以text/html格式返回公告信息的HTML内容，将其加入到div容器内即可直接显示了。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request
	 *            javax.servlet.http.HttpServletRequest 请求对象
	 * @param response
	 *            javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getNoticeContext(HttpServletRequest request, HttpServletResponse response);

}
