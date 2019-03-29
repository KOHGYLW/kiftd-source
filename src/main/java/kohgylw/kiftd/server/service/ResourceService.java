package kohgylw.kiftd.server.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * 
 * <h2>资源相关服务</h2>
 * <p>该服务内集成了预览资源相关的一些操作，用于前端进行各种资源的在线预览。对于需转化的格式也将在此转化为适合
 * 预览的流并写回前端。具体内容详见各个方法。</p>
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public interface ResourceService {
	
	/**
	 * 
	 * <h2>获取无需格式转化的流资源</h2>
	 * <p>对于各种无需进行转化格式的资源，从文件系统中获取相应文件块并以流形式写回，支持断点续传。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象，其中必须包含参数fid用于指定文件节点
	 * @param response javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getResource(HttpServletRequest request,HttpServletResponse response);
	
	/**
	 * 
	 * <h2>获取Word资源，以PDF流格式写回</h2>
	 * <p>获取指定ID的Word文档资源，并转化为PDF流供前端预览，方式同预览PDF。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param fileId java.lang.String 要读取的文件节点ID
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @param response javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getWordView(String fileId,HttpServletRequest request,HttpServletResponse response);
	
	/**
	 * 
	 * <h2>获取TXT资源，以PDF流格式写回</h2>
	 * <p>获取指定ID的TXT文档资源，并转化为PDF流供前端预览，方式同预览PDF。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param fileId java.lang.String 要读取的文件节点ID
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @param response javax.servlet.http.HttpServletResponse 响应对象
	 */
	public void getTxtView(String fileId,HttpServletRequest request,HttpServletResponse response);
	
	/**
	 * 
	 * <h2>获取视频解码状态</h2>
	 * <p>获得指定视频的解码状态，并根据返回信息提示界面下一步操作。</p>
	 * @author 青阳龙野(kohgylw)
	 * @param request javax.servlet.http.HttpServletRequest 请求对象
	 * @return java.lang.String 转码状态，若为FIN则代表转码完毕可直接播放，否则为百分制的进度，例如“1.2”代表完成了1.2%。
	 */
	public String getVideoTranscodeStatus(HttpServletRequest request);

}
