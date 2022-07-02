package kohgylw.kiftd.server.webdav;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.catalina.WebResource;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.util.XMLWriter;
import org.apache.tomcat.util.http.RequestUtil;
import org.apache.tomcat.util.security.ConcurrentMessageDigest;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import kohgylw.kiftd.server.enumeration.AccountAuth;
import kohgylw.kiftd.server.exception.FoldersTotalOutOfLimitException;
import kohgylw.kiftd.server.listener.ServerInitListener;
import kohgylw.kiftd.server.mapper.FolderMapper;
import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.util.ConfigureReader;
import kohgylw.kiftd.server.util.FileBlockUtil;
import kohgylw.kiftd.server.util.FileNodeUtil;
import kohgylw.kiftd.server.util.FolderUtil;
import kohgylw.kiftd.server.util.IpAddrGetter;
import kohgylw.kiftd.server.util.LogUtil;
import kohgylw.kiftd.server.util.RangeFileStreamWriter;
import kohgylw.kiftd.server.webdav.date.ConcurrentDateFormat;
import kohgylw.kiftd.server.webdav.date.FastHttpDateFormat;
import kohgylw.kiftd.server.webdav.dom.DOMWriter;
import kohgylw.kiftd.server.webdav.exception.UnAuthorizedException;
import kohgylw.kiftd.server.webdav.pojo.KiftdWebDAVResource;
import kohgylw.kiftd.server.webdav.range.ContentRange;
import kohgylw.kiftd.server.webdav.url.HttpPathUtil;
import kohgylw.kiftd.server.webdav.url.URLEncoder;
import kohgylw.kiftd.server.webdav.util.KiftdWebDAVResourcesUtil;

/**
 * 
 * <h2>负责提供WebDAV服务的Servlet</h2>
 * <p>
 * 该Servlet将被单独注册至内置Tomcat（而不借助Spring MVC框架管理）并映射至/dav/*路径上，以便更好地处理WebDAV请求。
 * 具体功能的实现请见本类中 protected void service(HttpServletRequest req,
 * HttpServletResponse resp) 方法。
 * </p>
 * <p>
 * 本类主要参考了Apache
 * Tomcat内置的org.apache.catalina.servlets.WebdavServlet类，部分代码直接拷贝自该类。
 * </p>
 * <p>
 * 注意：由于本类提供的服务面向WebDAV客户端，因此所有为响应对象设置响应码的方法均应使用 void
 * javax.servlet.http.HttpServletResponse.setStatus(int sc) 实现， 尤其是对于错误处理。
 * 这样当出现错误时（例如找不到资源），只需告知客户端响应码而无需返回任何诸如错误页面之类的响应体。
 * </p>
 * <p>
 * 附件 Apache Tomcat 源代码声明的开源协议内容：<br/>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * </p>
 * <p>
 * 其中对于WebDAV协议的实现规范参考了Microsoft提供的说明文档： <a href=
 * "https://docs.microsoft.com/en-us/previous-versions/office/developer/exchange-server-2003/aa486282(v=exchg.65)">
 * WebDAV Reference | Microsoft Docs</a>
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class KiftdWebDAVServlet extends HttpServlet {

	// 默认串行标识，无需改动此项。
	private static final long serialVersionUID = 1L;

	// URL专用编码器，功能类似于java.net.URLEncoder，但去除了不应编码的字符。此类完整拷贝自Tomcat 9的源代码。
	private static final URLEncoder URL_ENCODER_XML;

	// 初始化URL专用编码器并去除对&字符的支持（因为它在XML格式中不能直接使用，应进行替换）。
	static {
		URL_ENCODER_XML = (URLEncoder) URLEncoder.DEFAULT.clone();
		URL_ENCODER_XML.removeSafeCharacter('&');
	}

	// 各种请求方法名的特定表示
	private static final String METHOD_DELETE = "DELETE";
	private static final String METHOD_HEAD = "HEAD";
	private static final String METHOD_GET = "GET";
	private static final String METHOD_OPTIONS = "OPTIONS";
	private static final String METHOD_POST = "POST";
	private static final String METHOD_PUT = "PUT";
	private static final String METHOD_PROPFIND = "PROPFIND";
	private static final String METHOD_PROPPATCH = "PROPPATCH";
	private static final String METHOD_MKCOL = "MKCOL";
	private static final String METHOD_COPY = "COPY";
	private static final String METHOD_MOVE = "MOVE";
	private static final String METHOD_LOCK = "LOCK";
	private static final String METHOD_UNLOCK = "UNLOCK";

	// 一些特定的设置项
	private static final int FIND_BY_PROPERTY = 0;
	private static final int FIND_ALL_PROP = 1;
	private static final int FIND_PROPERTY_NAMES = 2;
	private static final int LOCK_CREATION = 0;
	private static final int LOCK_REFRESH = 1;
	private static final int DEFAULT_TIMEOUT = 3600;// 锁默认超时时间
	private static final int MAX_TIMEOUT = 604800;// 锁最大超时时间
	private int maxDepth = 3;// depth允许的默认最大层级
	protected static final String DEFAULT_NAMESPACE = "DAV:";
	protected static final String AUTH_HEADER_NAME = "WWW-Authenticate";// HTTP授权声明的返回头名称
	protected static final String REALM_NAME = "kiftd webdav";// HTTP授权声明的返回头中要返回的realm名称
	protected static final String AUTH_CHARSET_NAME = "UTF-8";// HTTP授权声明应使用的编码格式
	protected static final ConcurrentDateFormat creationDateFormat = new ConcurrentDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US, TimeZone.getTimeZone("GMT"));
	private static final String CONTENT_STREAM = "application/octet-stream";// 返回文件流的格式声明
	private static final Range IGNORE = new Range();// 代表忽略Range声明的Range对象
	private static File tempDir;// 临时文件夹对象，用于暂时存放各种临时数据
	protected static final int BUFFER_SIZE = 4096;// 默认的上传缓存大小，byte

	// 锁相关映射表，这些表用于实现对文件的上锁功能。
	private final Hashtable<String, LockInfo> resourceLocks = new Hashtable<>();
	private final Hashtable<String, Vector<String>> lockNullResources = new Hashtable<>();
	private final Vector<LockInfo> collectionLocks = new Vector<>();

	// 锁的默认安全ID
	private String secret = "catalina";

	// 一些由IOC容器管理的工具类
	private LogUtil lu;
	private KiftdWebDAVResourcesUtil resources;
	private FolderMapper fm;
	private NodeMapper nm;
	private IpAddrGetter idg;
	private FolderUtil fu;
	private FileBlockUtil fbu;

	// 初始化工作，进行一些必要的赋值或对象获取
	@Override
	public void init(ServletConfig config) throws ServletException {
		// 获取Spring IOC容器并从中获得由IOC容器管理的工具类。
		ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
		lu = context.getBean(LogUtil.class);// 获取日志生成工具对象
		resources = context.getBean(KiftdWebDAVResourcesUtil.class);// 获取kiftd虚拟文件系统资源操作工具
		fm = context.getBean(FolderMapper.class);// 获取文件夹查询映射
		idg = context.getBean(IpAddrGetter.class);// 获取IP地址解析器
		fu = context.getBean(FolderUtil.class);// 获取文件夹操作工具
		nm = context.getBean(NodeMapper.class);// 获取文件节点查询映射
		fbu = context.getBean(FileBlockUtil.class);// 获取文件块操作工具
		tempDir = new File(ConfigureReader.instance().getTemporaryfilePath());// 初始化临时文件夹对象
		super.init(config);
	}

	/**
	 * 
	 * <h2>WebDAV请求处理方法</h2>
	 * <p>
	 * 该方法用于处理WebDAV请求从而提供访问服务，详细过程可参考方法内部实现。
	 * </p>
	 * 
	 * @param javax.servlet.http.HttpServletRequest  req 请求对象
	 * @param javax.servlet.http.HttpServletResponse resp 响应对象
	 * @exception javax.servlet.ServletException 如果无法处理相应的请求则抛出此异常
	 * @exception java.io.IOException            如果发生的IO问题则抛出此异常
	 * @author 青阳龙野(kohgylw)
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 判断是否启用WebDAV功能
		if (!ConfigureReader.instance().isEnableWebDAV()) {
			// 若未启用，则拒绝提供相应的服务
			resp.setStatus(WebdavStatus.SC_FORBIDDEN);
			return;
		}
		// 若已启用WebDAV功能，则根据请求类型进行相应的处理
		final String method = req.getMethod();
		switch (method) {
		case METHOD_OPTIONS:
			doOptions(req, resp);// 获取允许的请求方法，该方法同时也会被客户端用于测试是否支持WebDAV协议
			break;
		case METHOD_PROPFIND:
			doPropfind(req, resp);// 检索文件或文件夹的属性（包括检索文件夹内容）
			break;
		case METHOD_PROPPATCH:
			doProppatch(req, resp);// 修改文件或文件夹的属性
			break;
		case METHOD_GET:
		case METHOD_POST:
			doGet(req, resp);// 获取文件
			break;
		case METHOD_HEAD:
			doHead(req, resp);// 获取文件，但不返回文件内容，仅返回响应头
			break;
		case METHOD_PUT:
			doPut(req, resp);// 上传文件（对于一些客户端而言，实现文件上传还需要借助lock、unlock和proppatch功能）
			break;
		case METHOD_DELETE:
			doDelete(req, resp);// 删除文件或文件夹
			break;
		case METHOD_MKCOL:
			doMkcol(req, resp); // 创建文件夹
			break;
		case METHOD_COPY:
			doCopy(req, resp);// 复制文件或文件夹至指定路径
			break;
		case METHOD_MOVE:
			doMove(req, resp);// 移动文件或文件夹至指定路径
			break;
		case METHOD_LOCK:
			doLock(req, resp);// 对文件或文件夹上锁
			break;
		case METHOD_UNLOCK:
			doUnlock(req, resp);// 解除文件或文件夹的锁定
			break;
		default:
			super.service(req, resp);// 如果是Trace或是其他未知的请求类型，则交由父级实现处理。
			break;
		}
		// 请求处理完毕
	}

	/**
	 * 
	 * <h2>从HTTP请求头authorization中解析账户信息</h2>
	 * <p>
	 * 该方法用于从authorization请求头中解析出账户信息，如果授权信息对应一个已存在的账户且密码验证通过时，则返回此账户名；
	 * 如果授权信息对应一个不存在的账户且密码为空时，则返回null（可认为是匿名访问）。 该方法的解析过程适用于HTTP
	 * BASIC授权方式，如果客户端未正确发送此信息，应告知客户端使用此授权方式发送授权信息。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req 请求对象
	 * @throws IllegalArgumentException 授权信息的内容有误，例如方式不为BASIC或格式不正确
	 * @throws UnAuthorizedException    授权信息不合法（例如密码不正确）或未给出授权
	 * @return java.lang.String 解析出的kiftd账户信息，如果授权信息正确，则返回具体的账户名，如果是匿名访问，则返回null
	 */
	private String getAccountFromRequestHeader(HttpServletRequest req)
			throws UnAuthorizedException, IllegalArgumentException {
		// 首先检查Session中是否已经存在了账户信息
		String account = (String) req.getSession().getAttribute("ACCOUNT");
		if (account != null) {
			// 若有，则以此为标准返回
			return account;
		}
		// 否则，进一步检查HTTP中的请求头
		String authHeader = req.getHeader("authorization");
		if (authHeader != null) {
			// 如果authorization请求头不为空，检查其授权方式是否为BASIC方式
			if (authHeader.toLowerCase().startsWith("basic ")) {
				// 如果授权方式的确为BASIC，则进行解析
				// 该内容的格式应为“Basic {Base64编码的授权信}”，去掉前面的“Basic ”得到“{Base64编码的授权信}”部分
				String base64Auth = authHeader.substring(6);
				// 将“{Base64编码的授权信}”进行解码
				Decoder decoder = Base64.getUrlDecoder();
				String authMsg = new String(decoder.decode(base64Auth), Charset.forName(AUTH_CHARSET_NAME));
				// 其内容格式应为“{userName}:{userPwd}”
				// 以“:”为界解析出包含userName和userPwd的信息，允许userPwd不存在，但“:”和userName必须存在
				int index = authMsg.indexOf(':');
				if (index >= 0) {
					String userName = authMsg.substring(0, index);
					String userPwd = authMsg.substring(index + 1);
					if (ConfigureReader.instance().foundAccount(userName)) {
						// 对userName进行检查，如果它对应的是一个存在的kiftd账户，则进一步检查其密码
						if (ConfigureReader.instance().checkAccountPwd(userName, userPwd)) {
							// 如果密码正确，则承认此用户为一个合法的账户，存入Session并返回
							req.getSession().setAttribute("ACCOUNT", userName);
							return userName;
						} else {
							// 如果密码不正确，则抛出为授权异常
							throw new UnAuthorizedException();
						}
					} else {
						// 对于不存在的账户，仅当密码为空时认为其要匿名访问
						if ("".equals(userPwd)) {
							return null;
						} else {
							// 否则认为其授权无效（账户输入错误？），抛出未授权异常
							throw new UnAuthorizedException();
						}
					}
				} else {
					// 授权信息的格式显然有错误
					throw new IllegalArgumentException();
				}
			} else {
				// 授权方式不为BASIC
				throw new IllegalArgumentException();
			}
		} else {
			throw new UnAuthorizedException();
		}
	}

	/**
	 * 
	 * <h2>告知客户端必须使用正确的HTTP BASIC授权</h2>
	 * <p>
	 * 该方法将会返回一个授权响应头，告知客户端需要在请求头中使用BASIC方式发送正确的授权信息。
	 * 客户端应在接下来的所有请求中添加“authorization”请求头，其内容格式应类似于“BASIC {使用Base64编码的授权信息}”，
	 * 其中，“{使用Base64编码的授权信息}”在使用Base64解码后，应能得到格式为“{userName}:{userPwd}”的文本信息， 从而便于利用
	 * String
	 * kohgylw.kiftd.server.webdav.KiftdWebDAVServlet.getAccountFromRequestHeader(HttpServletRequest
	 * req) throws UnAuthorizedException, IllegalArgumentException 方法解析出授权所需的账户名和密码。
	 * </p>
	 * 
	 * @throws IOException 响应对象发送信息失败
	 * @param resp 响应对象
	 * @author 青阳龙野(kohgylw)
	 */
	private void needAuthorizationByBasic(HttpServletResponse resp) throws IOException {
		StringBuilder value = new StringBuilder(16);
		value.append("Basic realm=\"");
		value.append(REALM_NAME);
		value.append('\"');
		resp.setHeader(AUTH_HEADER_NAME, value.toString());
		resp.setStatus(WebdavStatus.SC_UNAUTHORIZED);
	}

	/**
	 * 
	 * <h2>创建一个JAXP规范的DocumentBuilder对象方法</h2>
	 * <p>
	 * 该方法用于创建一个JAXP规范的DocumentBuilder对象，用于生成符合WebDAV协议要求的XML响应体。
	 * </p>
	 * 
	 * @author 代码源自于Apache Tomcat
	 * @return javax.xml.parsers.DocumentBuilder 一个JAXP规范的DocumentBuilder对象
	 */
	protected DocumentBuilder getDocumentBuilder() throws ServletException {
		DocumentBuilder documentBuilder = null;
		DocumentBuilderFactory documentBuilderFactory = null;
		try {
			documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			documentBuilderFactory.setExpandEntityReferences(false);
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			documentBuilder.setEntityResolver(new WebdavResolver());
		} catch (ParserConfigurationException e) {
			throw new ServletException();
		}
		return documentBuilder;
	}

	/**
	 * URL rewriter.
	 *
	 * @param path Path which has to be rewritten
	 * @return the rewritten path
	 */
	protected String rewriteUrl(String path) {
		return URL_ENCODER_XML.encode(path, StandardCharsets.UTF_8);
	}

	/**
	 * Override the MyDefaultServlet implementation and only use the PathInfo. If
	 * the ServletPath is non-null, it will be because the WebDAV servlet has been
	 * mapped to a url other than /* to configure editing at different url than
	 * normal viewing.
	 *
	 * @param request The servlet request we are processing
	 */
	protected String getRelativePath(HttpServletRequest request) {
		return getRelativePath(request, false);
	}

	protected String getRelativePath(HttpServletRequest request, boolean allowEmptyPath) {
		String pathInfo;
		if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
			pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
		} else {
			pathInfo = request.getPathInfo();
		}

		StringBuilder result = new StringBuilder();
		if (pathInfo != null) {
			result.append(pathInfo);
		}
		if (result.length() == 0) {
			result.append('/');
		}
		return result.toString();
	}

	/**
	 * 
	 * <h2>处理options请求</h2>
	 * <p>
	 * 该方法用于处理options请求，提供指定路径下允许的请求方法名称以及Microsoft客户端的认证机制，同时也用于WebDAV连通性测试。
	 * </p>
	 * 
	 * @author Apache Tomcat
	 * @param req  请求对象
	 * @param resp 响应对象
	 */
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.addHeader("DAV", "1,2");
		resp.addHeader("Allow", determineMethodsAllowed(req));
		resp.addHeader("MS-Author-Via", "DAV");// 告知MS客户端仅接受HTTP授权（不接受MS账户认证）
	}

	/**
	 * 
	 * <h2>处理propfind请求</h2>
	 * <p>
	 * 该方法用于处理propfind请求，提供指定路径下文件或文件夹的属性集合，并以JAXP方式将结果以XML格式返回给客户端。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象
	 * @param resp 响应对象
	 */
	protected void doPropfind(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 获取WebDAV请求资源的路径（例如“/”或“/test.txt”）
		String path = getRelativePath(req);
		// 获取客户端的授权信息并解析出账户
		String account;
		try {
			account = getAccountFromRequestHeader(req);
		} catch (IllegalArgumentException | UnAuthorizedException e) {
			// 授权信息不正确或未给出授权时，告知客户端提供正确的授权信息
			needAuthorizationByBasic(resp);
			return;
		}
		if (ConfigureReader.instance().mustLogin() && account == null) {
			// 在开启“必须登入”时，不允许匿名访问
			needAuthorizationByBasic(resp);
			return;
		}
		// 存放待返回属性名称的集合
		Vector<String> properties = null;
		// 查询深度
		int depth = maxDepth;
		// 查询类型（允许查询那些属性？）
		int type = FIND_ALL_PROP;
		// 从请求头中获取客户端要求的查询深度
		String depthStr = req.getHeader("Depth");
		if (depthStr == null) {
			// 如果没声明，就直接用最大查询深度
			depth = maxDepth;
		} else {
			// 如果声明了，则按照声明深度的进行设置（如果声明的是“infinity”，则视为最大查询深度值）
			if (depthStr.equals("0")) {
				depth = 0;
			} else if (depthStr.equals("1")) {
				depth = 1;
			} else if (depthStr.equals("infinity")) {
				depth = maxDepth;
			}
		}
		// 首先，获取客户端要求的属性查询方式（如果声明了）
		Node propNode = null;
		if (req.getContentLengthLong() > 0) {
			DocumentBuilder documentBuilder = getDocumentBuilder();
			try {
				// 从请求中以JAXP方式解析出XML文档的内容，并从中获取客户端想要查询的属性类型声明
				Document document = documentBuilder.parse(new InputSource(req.getInputStream()));
				Element rootElement = document.getDocumentElement();
				NodeList childList = rootElement.getChildNodes();
				for (int i = 0; i < childList.getLength(); i++) {
					Node currentNode = childList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						// 忽略文本节点（通常不应该存在）
						break;
					case Node.ELEMENT_NODE:
						if (currentNode.getNodeName().endsWith("prop")) {
							// 若指定了查询哪些属性，则后面需要按照这些属性返回，先将此信息节点暂存。
							type = FIND_BY_PROPERTY;
							propNode = currentNode;
						}
						if (currentNode.getNodeName().endsWith("propname")) {
							// 若指定了要查询的某种属性名称，则将返回方式设置为按名称查询。
							type = FIND_PROPERTY_NAMES;
						}
						if (currentNode.getNodeName().endsWith("allprop")) {
							// 若要求返回全部属性，则将返回方式设置为按全部属性查询。
							type = FIND_ALL_PROP;
						}
						break;
					}
					// 理论上只有上述两种可能
				}
			} catch (SAXException | IOException e) {
				// 若上述过程出现了异常，则将信息写入日志并返回代码400（此问题应该是由于客户端传递的数据不正确导致的）
				lu.writeException(e);
				resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
				return;
			}
		}
		// 之后，按照客户端声明的属性查询方式进行返回（如果声明了）或按照默认方式（按全部属性查询）返回。
		if (type == FIND_BY_PROPERTY) {
			properties = new Vector<>();
			// 获取要求查询的属性
			NodeList childList = propNode.getChildNodes();
			for (int i = 0; i < childList.getLength(); i++) {
				Node currentNode = childList.item(i);
				switch (currentNode.getNodeType()) {
				case Node.TEXT_NODE:
					// 依旧忽略文本节点
					break;
				case Node.ELEMENT_NODE:
					// 每个声明的节点中应包含“哪个文件或文件夹要查询哪个属性”的信息
					String nodeName = currentNode.getNodeName();// 节点名称
					String propertyName = null;// 其中声明的要查询属性的名称
					// 仅获取要查询的属性名称（这里借鉴Tomca 9的方式，不单独处理某个文件而是直接返回最大集合）
					if (nodeName.indexOf(':') != -1) {
						propertyName = nodeName.substring(nodeName.indexOf(':') + 1);
					} else {
						propertyName = nodeName;
					}
					// 将所有要求的属性名称存入待返回属性名称的集合
					properties.addElement(propertyName);
					break;
				// 理论上只有上述两种可能
				}
			}
		}
		// 根据路径获取具体的资源对象
		WebResource resource = resources.getResource(path);
		// 接下来，按照声明的路径将kiftd中存放的文件或文件夹的属性以JAXP方式返回给客户端
		if (!resource.exists()) {
			// 如果资源不存在且父路径已经被上锁，则认为此资源是个锁定的空资源，返回客户端信息
			String parentPath = HttpPathUtil.getParentPath(path);
			Vector<String> currentLockNullResources = lockNullResources.get(parentPath);
			if (currentLockNullResources != null) {
				Enumeration<String> lockNullResourcesList = currentLockNullResources.elements();
				while (lockNullResourcesList.hasMoreElements()) {
					String lockNullPath = lockNullResourcesList.nextElement();
					if (lockNullPath.equals(path)) {
						resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
						resp.setContentType("text/xml; charset=UTF-8");
						XMLWriter generatedXML = new XMLWriter(resp.getWriter());
						generatedXML.writeXMLHeader();
						generatedXML.writeElement("D", DEFAULT_NAMESPACE, "multistatus", XMLWriter.OPENING);
						parseLockNullProperties(req, generatedXML, lockNullPath, type, properties);
						generatedXML.writeElement("D", "multistatus", XMLWriter.CLOSING);
						generatedXML.sendData();
						return;
					}
				}
			}
		}
		// 如果资源不存在且父路径未被上锁，则返回404
		if (!resource.exists()) {
			resp.setStatus(WebdavStatus.SC_NOT_FOUND);
			return;
		}
		// 如果资源存在，则检查用户是否具备检索权限
		KiftdWebDAVResource kiftdWebDAVResource = (KiftdWebDAVResource) resource;
		if (resource.isDirectory()) {
			// 如果该资源对应一个文件夹，则检查访问者是否有权访问此文件夹
			Folder target = kiftdWebDAVResource.getFolder();
			if (!ConfigureReader.instance().accessFolder(target, account)) {
				// 无权访问，则返回授权信息无效
				needAuthorizationByBasic(resp);
				return;
			}
		} else {
			// 如果该资源对应的是一个文件，则检查访问者是否有权访问此文件所在的文件夹
			kohgylw.kiftd.server.model.Node node = kiftdWebDAVResource.getNode();
			if (node != null) {
				Folder parentFolder = fm.queryById(node.getFileParentFolder());
				if (!ConfigureReader.instance().accessFolder(parentFolder, account)) {
					// 无权访问，则返回授权信息无效
					needAuthorizationByBasic(resp);
					return;
				}
			} else {
				// 若是资源存在且不为文件夹，则理论上必然有一个非null的node，此处是为了增强健壮性进行二次试错
				resp.setStatus(WebdavStatus.SC_NOT_FOUND);
				return;
			}
		}
		// 至此权限检查完毕，则设置响应方式为207，并以WebDAV规定的XML格式返回资源属性
		resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
		resp.setContentType("text/xml; charset=UTF-8");
		XMLWriter generatedXML = new XMLWriter(resp.getWriter());
		generatedXML.writeXMLHeader();
		generatedXML.writeElement("D", DEFAULT_NAMESPACE, "multistatus", XMLWriter.OPENING);
		if (depth == 0) {
			// 如果检索深度为0，则直接返回当前资源的信息
			parseProperties(req, generatedXML, path, resource, type, properties);
		} else {
			// 否则，迭代检索该资源（文件夹）下的各层资源至检索深度
			Stack<String> stack = new Stack<>();
			stack.push(path);
			Stack<String> stackBelow = new Stack<>();
			while ((!stack.isEmpty()) && (depth >= 0)) {
				String currentPath = stack.pop();
				resource = resources.getResource(currentPath);
				parseProperties(req, generatedXML, currentPath, resource, type, properties);
				if (resource.isDirectory() && (depth > 0)) {
					String[] entries = resources.list(currentPath, account);
					for (String entry : entries) {
						String newPath = currentPath;
						if (!(newPath.endsWith("/"))) {
							newPath += "/";
						}
						newPath += entry;
						stackBelow.push(newPath);
					}
					// 如果其中含有被锁定的不存在资源，则要告知客户端这些资源的信息
					String lockPath = currentPath;
					Vector<String> currentLockNullResources = lockNullResources.get(lockPath);
					if (currentLockNullResources != null) {
						Enumeration<String> lockNullResourcesList = currentLockNullResources.elements();
						while (lockNullResourcesList.hasMoreElements()) {
							String lockNullPath = lockNullResourcesList.nextElement();
							parseLockNullProperties(req, generatedXML, lockNullPath, type, properties);
						}
					}
				}
				if (stack.isEmpty()) {
					depth--;
					stack = stackBelow;
					stackBelow = new Stack<>();
				}
				generatedXML.sendData();
			}
		}
		generatedXML.writeElement("D", "multistatus", XMLWriter.CLOSING);
		generatedXML.sendData();
	}

	/**
	 * 
	 * <h2>处理proppatch请求</h2>
	 * <p>
	 * 该方法用于处理proppatch请求，修改文件或文件夹的属性。
	 * 一些客户端（例如Windows的“资源管理器”）在进行文件上传操作时，会尝试通过此方法为新文件设置一些属性（例如“创建时间”）。
	 * 按照惯例，服务器端不应允许客户端直接修改文件属性，因为文件的属性应该是“只读”的，由服务器自动管理而不应修改，
	 * 因此，该方法不会执行任何修改，无论客户端发送何种请求，一律返回拒绝执行的回复信息。
	 * 特别地：Microsoft提供的开发文档中声明了此请求类型不应再被维护。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象
	 * @param resp 响应对象
	 */
	protected void doProppatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		// 检查资源是否被锁定
		if (isLocked(req)) {
			resp.sendError(WebdavStatus.SC_LOCKED);
			return;
		}
		// 此处省略账户和权限检查逻辑，因为所有操作都会被拒绝
		// 首先，获取客户端声明的属性更新内容
		if (req.getContentLengthLong() > 0) {
			// 检查请求内容（XML）并解析，解析逻辑与propfind类似
			/*
			 * 请求的内容类似于这种格式： <D:propertyupdate xmlns:D="DAV:"
			 * xmlns:Z="urn:schemas-microsoft-com:"> <D:set> <D:prop>
			 * <Z:Win32CreationTime>Fir, 27 May 2022 00:24:45 GMT</Z:Win32CreationTime>
			 * </D:prop> </D:set> </D:propertyupdate>
			 */
			DocumentBuilder documentBuilder = getDocumentBuilder();
			try {
				Document document = documentBuilder.parse(new InputSource(req.getInputStream()));
				Element rootElement = document.getDocumentElement();
				NodeList rootChildList = rootElement.getChildNodes();
				// 先找出root节点下的set或remove节点
				Node operationNode = null;
				sreachOperation: for (int i = 0; i < rootChildList.getLength(); i++) {
					Node currentNode = rootChildList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						break;
					case Node.ELEMENT_NODE:
						if (currentNode.getNodeName().endsWith(":set")) {
							operationNode = currentNode;
							break sreachOperation;
						}
						if (currentNode.getNodeName().endsWith(":remove")) {
							operationNode = currentNode;
							break sreachOperation;
						}
						break;
					}
				}
				// 再找出set或remove节点下的prop节点
				Node propNode = null;
				if (operationNode != null) {
					NodeList setChildList = operationNode.getChildNodes();
					sreachProp: for (int i = 0; i < setChildList.getLength(); i++) {
						Node currentNode = setChildList.item(i);
						switch (currentNode.getNodeType()) {
						case Node.TEXT_NODE:
							break;
						case Node.ELEMENT_NODE:
							if (currentNode.getNodeName().endsWith(":prop")) {
								propNode = currentNode;
								break sreachProp;
							}
							break;
						}
					}
				}
				// 最后，根据上述内容生成返回信息（XML）
				/*
				 * 返回的信息类似于这种格式： <D:multistatus xmlns:D="DAV:"
				 * xmlns:Z="urn:schemas-microsoft-com:"> <D:response>
				 * <D:href>/dav/foo.txt</D:href> <D:propstat> <D:prop>
				 * <Z:Win32CreationTime></Z:Win32CreationTime> </D:prop> <D:status>HTTP/1.1 403
				 * Forbidden</D:status> </D:propstat> </D:response> </D:multistatus>
				 */
				resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
				resp.setContentType("text/xml; charset=UTF-8");
				XMLWriter generatedXML = new XMLWriter(resp.getWriter());
				generatedXML.writeXMLHeader();
				// 开始根节点
				generatedXML.writeElement("D", DEFAULT_NAMESPACE, "multistatus", XMLWriter.OPENING);
				// 开始response节点
				generatedXML.writeElement("D", "response", XMLWriter.OPENING);
				// 开始href节点
				generatedXML.writeElement("D", "href", XMLWriter.OPENING);
				String path = getRelativePath(req);
				WebResource resource = resources.getResource(path);
				String href = req.getContextPath() + req.getServletPath();
				if ((href.endsWith("/")) && (path.startsWith("/"))) {
					href += path.substring(1);
				} else {
					href += path;
				}
				if (resource.isDirectory() && (!href.endsWith("/"))) {
					href += "/";
				}
				String rewrittenUrl = rewriteUrl(href);
				generatedXML.writeText(rewrittenUrl);
				// 关闭href节点
				generatedXML.writeElement("D", "href", XMLWriter.CLOSING);
				// 开始propstat节点
				generatedXML.writeElement("D", "propstat", XMLWriter.OPENING);
				// 开始prop节点
				generatedXML.writeElement("D", "prop", XMLWriter.OPENING);
				// 依次添加各种更新属性节点
				// 将prop节点下的全部节点名称计入要更新的属性集合中
				if (propNode != null) {
					NodeList propChildList = propNode.getChildNodes();
					for (int i = 0; i < propChildList.getLength(); i++) {
						Node currentNode = propChildList.item(i);
						switch (currentNode.getNodeType()) {
						case Node.TEXT_NODE:
							break;
						case Node.ELEMENT_NODE:
							String propName = currentNode.getNodeName();
							if (propName.indexOf(':') != -1) {
								propName = propName.substring(propName.indexOf(':') + 1);
							}
							generatedXML.writeElement(currentNode.getPrefix(), currentNode.getNamespaceURI(), propName,
									XMLWriter.NO_CONTENT);
							break;
						}
					}
				}
				// 关闭prop节点
				generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
				// 开始status节点
				generatedXML.writeElement("D", "status", XMLWriter.OPENING);
				StringBuffer status = new StringBuffer("HTTP/1.1 ");
				if (resource.exists()) {
					status.append(WebdavStatus.SC_FORBIDDEN);
					status.append(" Forbidden");
				} else {
					status.append(WebdavStatus.SC_NOT_FOUND);
					status.append(" Not Found");
				}
				generatedXML.writeText(status.toString());
				// 关闭status节点
				generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
				// 关闭propstat节点
				generatedXML.writeElement("D", "propstat", XMLWriter.CLOSING);
				// 关闭response节点
				generatedXML.writeElement("D", "response", XMLWriter.CLOSING);
				// 关闭根节点
				generatedXML.writeElement("D", "multistatus", XMLWriter.CLOSING);
				// 发送数据
				generatedXML.sendData();
			} catch (SAXException | IOException e) {
				// 若上述过程出现了异常，则将信息写入日志并返回代码400（此问题应该是由于客户端传递的数据不正确导致的）
				lu.writeException(e);
				resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
				return;
			}
		}
	}

	/**
	 * 
	 * <h2>处理get请求</h2>
	 * <p>
	 * 该方法用于处理get请求，获取一个文件。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request  请求对象
	 * @param response 响应对象
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// 获取请求的逻辑路径
		String path = getRelativePath(request);
		// 解析授权信息
		String account;
		try {
			account = getAccountFromRequestHeader(request);
		} catch (IllegalArgumentException | UnAuthorizedException e) {
			// 授权信息不正确时，告知客户端提供正确的授权
			needAuthorizationByBasic(response);
			return;
		}
		if (ConfigureReader.instance().mustLogin() && account == null) {
			// 如果开启了“必须登入”，则不允许匿名访问
			needAuthorizationByBasic(response);
			return;
		}
		// 根据逻辑路径获取资源
		WebResource resource = resources.getResource(path);
		if (!resource.exists()) {
			// 如果该资源不存在，返回状态码404
			response.setStatus(WebdavStatus.SC_NOT_FOUND);
			return;
		}
		if (!resource.canRead()) {
			// 如果资源不可读，则返回状态码403
			response.setStatus(WebdavStatus.SC_FORBIDDEN);
			return;
		}
		KiftdWebDAVResource kiftdWebDAVResource = ((KiftdWebDAVResource) resource);
		Folder accessFolder = kiftdWebDAVResource.isDirectory() ? kiftdWebDAVResource.getFolder()
				: fm.queryById(kiftdWebDAVResource.getNode().getFileParentFolder());
		if (!ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
				fu.getAllFoldersId(accessFolder.getFolderId()))
				|| !ConfigureReader.instance().accessFolder(accessFolder, account)) {
			// 如果访问者无权访问此资源，则返回授权无效信息
			needAuthorizationByBasic(response);
			return;
		}
		// 执行发送资源操作
		if (resource.isFile()) {
			// 对于文件资源，直接写回文件数据
			kohgylw.kiftd.server.model.Node n = ((KiftdWebDAVResource) resource).getNode();
			final File fo = this.fbu.getFileFromBlocks(n);
			final String ip = idg.getIpAddr(request);
			final String range = request.getHeader("Range");
			if (fo != null) {
				int status = RangeFileStreamWriter.writeRangeFileStream(request, response, fo, n.getFileName(),
						CONTENT_STREAM, ConfigureReader.instance().getDownloadMaxRate(account), fbu.getETag(fo), true);
				// 日志记录（仅针对一次下载）
				if (status == WebdavStatus.SC_OK || (range != null && range.startsWith("bytes=0-"))) {
					this.lu.writeDownloadFileEvent(account, ip, n);
				}
				// 一切成功，处理结束
				return;
			}
			// 若对应的文件块丢失，返回状态码500
			response.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
			return;
		} else {
			// 对于文件夹资源，则无需返回任何信息（WebDAV客户端不应使用Get或Post方式访问文件夹，而是使用propfind方式检索）
			sendNotAllowed(request, response);
			return;
		}
	}

	/**
	 * 
	 * <h2>处理head请求</h2>
	 * <p>
	 * 该方法用于处理head请求，获取一个文件（与Get或Post类似），但不返回具体内容，只返回响应头。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param request  请求对象
	 * @param response 响应对象
	 */
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		// 获取请求的逻辑路径
		String path = getRelativePath(request);
		// 解析授权信息
		String account;
		try {
			account = getAccountFromRequestHeader(request);
		} catch (IllegalArgumentException | UnAuthorizedException e) {
			// 授权信息不正确时，告知客户端提供正确的授权
			needAuthorizationByBasic(response);
			return;
		}
		if (ConfigureReader.instance().mustLogin() && account == null) {
			// 如果开启了“必须登入”，则不允许匿名访问
			needAuthorizationByBasic(response);
			return;
		}
		// 根据逻辑路径获取资源
		WebResource resource = resources.getResource(path);
		if (!resource.exists()) {
			// 如果该资源不存在，返回状态码404
			response.setStatus(WebdavStatus.SC_NOT_FOUND);
			return;
		}
		if (!resource.canRead()) {
			// 如果资源不可读，则返回状态码403
			response.setStatus(WebdavStatus.SC_FORBIDDEN);
			return;
		}
		KiftdWebDAVResource kiftdWebDAVResource = ((KiftdWebDAVResource) resource);
		Folder accessFolder = kiftdWebDAVResource.isDirectory() ? kiftdWebDAVResource.getFolder()
				: fm.queryById(kiftdWebDAVResource.getNode().getFileParentFolder());
		if (!ConfigureReader.instance().authorized(account, AccountAuth.DOWNLOAD_FILES,
				fu.getAllFoldersId(accessFolder.getFolderId()))
				|| !ConfigureReader.instance().accessFolder(accessFolder, account)) {
			// 如果访问者无权访问此资源，则返回授权无效信息
			needAuthorizationByBasic(response);
			return;
		}
		// 执行发送资源操作
		if (resource.isFile()) {
			// 对于文件资源，直接写回文件的响应头（但不发送具体内容）
			kohgylw.kiftd.server.model.Node n = ((KiftdWebDAVResource) resource).getNode();
			final File fo = this.fbu.getFileFromBlocks(n);
			if (fo != null) {
				RangeFileStreamWriter.writeRangeFileHead(request, response, fo, n.getFileName(), CONTENT_STREAM,
						fbu.getETag(fo), true);
				// 一切成功，处理结束
				return;
			}
			// 若对应的文件块丢失，返回状态码500
			response.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
			return;
		} else {
			// 对于文件夹资源，则无需返回任何信息（WebDAV客户端不应使用Get或Post方式访问文件夹，而是使用propfind方式检索）
			sendNotAllowed(request, response);
			return;
		}
	}

	/**
	 * 
	 * <h2>处理mkcol请求</h2>
	 * <p>
	 * 该方法用于处理mkcol请求，以默认规则在指定逻辑路径上创建一个新的文件夹。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象。
	 * @param resp 响应对象。
	 */
	protected void doMkcol(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = getRelativePath(req);
		WebResource resource = resources.getResource(path);
		// 如果指定逻辑路径下已经有了一个同名的文件夹，则创建失败。
		if (resource.exists() && resource.isDirectory()) {
			sendNotAllowed(req, resp);
			return;
		}
		// 如果资源已经被锁定，则创建失败。
		if (isLocked(req)) {
			resp.setStatus(WebdavStatus.SC_LOCKED);
			return;
		}
		// 不允许有请求体，若有，则根据请求体是否合法返回相应的错误代码。这里尝试进行解析是为了规范错误代码的类型。
		if (req.getContentLengthLong() > 0) {
			DocumentBuilder documentBuilder = getDocumentBuilder();
			try {
				documentBuilder.parse(new InputSource(req.getInputStream()));
				resp.setStatus(WebdavStatus.SC_NOT_IMPLEMENTED);
				return;
			} catch (SAXException saxe) {
				resp.setStatus(WebdavStatus.SC_UNSUPPORTED_MEDIA_TYPE);
				return;
			}
		}
		// 权限检查，确认用户是否具备创建文件夹权限
		String account;
		try {
			account = getAccountFromRequestHeader(req);
		} catch (IllegalArgumentException | UnAuthorizedException e) {
			// 如果授权信息有误，则告知客户端必须正确授权
			needAuthorizationByBasic(resp);
			return;
		}
		if (ConfigureReader.instance().mustLogin() && account == null) {
			// 如果设置了“必须登入”但客户端未提供任何授权信息，则告知其必须正确授权
			needAuthorizationByBasic(resp);
			return;
		}
		Folder parentFolder = resources.getFolderByPath(HttpPathUtil.getParentPath(path));
		if (!ConfigureReader.instance().accessFolder(parentFolder, account) || !ConfigureReader.instance()
				.authorized(account, AccountAuth.CREATE_NEW_FOLDER, fu.getAllFoldersId(parentFolder.getFolderId()))) {
			// 如果无权访问父文件夹或在父文件夹内无创建文件夹权限
			needAuthorizationByBasic(resp);
			return;
		}
		String folderName = HttpPathUtil.getResourceName(path);
		// 一切检查完毕，尝试创建文件夹。
		Folder newFolder = resources.mkdir(folderName, parentFolder, account);
		if (newFolder != null) {
			// 创建成功后，记录日志
			lu.writeCreateFolderEvent(account, idg.getIpAddr(req), newFolder);
			// 设置成功代码201
			resp.setStatus(WebdavStatus.SC_CREATED);
			// 将此路径从已锁定的不存在资源列表中移除
			lockNullResources.remove(path);
		} else {
			// 创建失败的话，则返回错误代码409
			resp.setStatus(WebdavStatus.SC_CONFLICT);
		}
	}

	/**
	 * 
	 * <h2>处理put请求</h2>
	 * <p>
	 * 该方法用于处理put请求，将客户端上传的文件存放至指定逻辑路径。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象。
	 * @param resp 响应对象。
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 如果资源已被锁定，则上传失败
		if (isLocked(req)) {
			resp.setStatus(WebdavStatus.SC_LOCKED);
			return;
		}
		// 得到上传的逻辑路径
		String path = getRelativePath(req);
		// 执行从请求中接收资源的相关处理，PUT服务功能的主要实现请参见此方法
		receiveResource(req, resp, path);
		// 上述处理完毕后，无论操作成功与否，都需要将请求的逻辑路径从锁定的空资源列表中移除
		lockNullResources.remove(path);
	}

	/**
	 * 
	 * <h2>从请求中接收资源</h2>
	 * <p>
	 * 该方法包含了执行接收资源操作的所有前置检查和相应的逻辑，用以实现PUT请求的功能。
	 * 将此方法独立出来是为了便于在操作结束后统一将请求的逻辑路径从锁定的空资源列表中移除。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象
	 * @param resp 响应对象
	 * @param path 请求的逻辑路径，应使用getRelativePath方法获得
	 */
	private void receiveResource(HttpServletRequest req, HttpServletResponse resp, String path)
			throws ServletException, IOException {
		// 得到账户信息
		String account;
		try {
			account = getAccountFromRequestHeader(req);
		} catch (IllegalArgumentException | UnAuthorizedException e) {
			// 如果授权信息有误或者无授权信息，则要求客户端给出授权信息
			needAuthorizationByBasic(resp);
			return;
		}
		// 检查上传路径是否合法并检查用户是否具备上传权限
		if (ConfigureReader.instance().mustLogin() && account == null) {
			needAuthorizationByBasic(resp);
			return;
		}
		Folder parentFolder = resources.getFolderByPath(HttpPathUtil.getParentPath(path));
		if (parentFolder == null) {
			resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return;
		}
		if (!ConfigureReader.instance().accessFolder(parentFolder, account)) {
			needAuthorizationByBasic(resp);
			return;
		}
		List<String> parentFoldersId = fu.getAllFoldersId(parentFolder.getFolderId());
		if (!ConfigureReader.instance().authorized(account, AccountAuth.UPLOAD_FILES, parentFoldersId)) {
			needAuthorizationByBasic(resp);
			return;
		}
		// 检查上传文件名称是否合法
		String pathFileName = HttpPathUtil.getResourceName(path);
		if (pathFileName.isEmpty()) {
			resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return;
		}
		// 如果对应路径下有一个文件原件且内容不为空，则执行此操作必定会导致覆盖（无论是部分修改还是全部重写），因此需要删除权限
		kohgylw.kiftd.server.model.Node originNode = resources.getNodeByPath(path);
		File originBlock = null;
		if (originNode != null) {
			originBlock = fbu.getFileFromBlocks(originNode);
			if (originBlock != null && originBlock.length() > 0 && !ConfigureReader.instance().authorized(account,
					AccountAuth.DELETE_FILE_OR_FOLDER, parentFoldersId)) {
				needAuthorizationByBasic(resp);
				return;
			}
		}
		// 一切检查完毕，接收上传的数据并存放在临时文件夹内
		File tempFile;
		long maxSize = ConfigureReader.instance().getUploadFileSize(account);
		// 检查请求中的Range声明以决定如何接收数据
		Range range = parseContentRange(req, resp);
		if (range == null) {
			// 如果Range声明有误，则无需继续处理，相应的错误代码已在parseContentRange方法中设置
			return;
		}
		if (range == IGNORE) {
			// 如果未声明Range请求，则按照全新的数据接收
			tempFile = saveToTempFile(req, null, range, maxSize);
		} else {
			// 如果是有Range的话，除非range.start=0，否则理论上应该有原件。
			if (originNode != null) {
				// 若确实有原件，则进一步检查是否正确获得了对应的文件块
				if (originBlock == null) {
					// 如果文件块获取失败，说明文件系统存在问题（理论上一个文件节点必定对应一个文件块），返回代码500并结束处理
					resp.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
					return;
				} else {
					// 如果文件块获取成功，则在原件文件块的基础上进行修改并生成临时文件
					tempFile = saveToTempFile(req, originBlock, range, maxSize);
				}
			} else {
				// 如果没有原件
				if (range.start == 0) {
					// 如果range.start=0，则也创建一个新的文件（可能是断点续传的第一个数据段？）
					tempFile = saveToTempFile(req, null, range, maxSize);
				} else {
					// 否则设置返回代码404并结束处理
					resp.setStatus(WebdavStatus.SC_NOT_FOUND);
					return;
				}
			}
		}
		// 如果文件超限，返回状态码413
		if (tempFile == null) {
			resp.setStatus(WebdavStatus.SC_REQUEST_TOO_LONG);
			return;
		}
		// 至此，上传的数据已经保存为一个临时文件，接下来进行处理
		if (originNode != null) {
			// 若是有原件，则先删除原件
			if (!fbu.deleteNode(originNode)) {
				// 若是原件删除失败，则终止上传并返回状态码409
				resp.setStatus(WebdavStatus.SC_CONFLICT);
				return;
			}
		}
		// 一切准备就绪，将上传的文件存入文件系统中
		final File block = this.fbu.saveToFileBlocks(tempFile);
		if (block == null) {
			// 如果存入失败，终止操作并告知客户端
			tempFile.delete();
			resp.setStatus(WebdavStatus.SC_CONFLICT);
			return;
		}
		kohgylw.kiftd.server.model.Node node = fbu.insertNewNode(pathFileName, account, block.getName(),
				fbu.getFileSize(block.length()), parentFolder.getFolderId());
		if (node == null) {
			// 如果节点插入失败，终止操作并告知客户端
			block.delete();
			resp.setStatus(WebdavStatus.SC_CONFLICT);
			return;
		} else {
			// 全部操作成功，返回代码201，并记录日志
			resp.setStatus(WebdavStatus.SC_CREATED);
			lu.writeUploadFileEvent(req, node, account);
			return;
		}
	}

	/**
	 * 
	 * <h2>将输入流中的数据存储在临时目录中</h2>
	 * <p>
	 * 该方法用于将输入流中的数据写为文件并存放在文件系统的临时目录中。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param in       输入流，用以读取要存放的数据。
	 * @param oldBlock 原始文件，该参数将基于range参数生成此文件的修改版，若传入null或者其不为文件，直接使用输入流生成新文件。
	 * @param range    range对象，用于指定修改原始文件中哪些位置的数据，仅当oldBlock不为null时此参数才会生效。
	 * @param maxSize  指定允许接收的文件大小，以byte为单位。当不限大小时，请传入负数。若传入0则将无法正常上传数据。
	 * @return java.io.File 存放好的临时文件，使用完毕后应删除以释放临时存储空间。如果数据超过maxSize指定的大小， 则返回null。
	 * @throws IOException 因文件读写问题导致的存放失败。
	 */
	public File saveToTempFile(HttpServletRequest req, File oldBlock, Range range, long maxSize) throws IOException {
		if (tempDir.isDirectory()) {
			// 在临时文件夹中生成一个临时文件
			File tempFile = new File(tempDir, "temp_" + UUID.randomUUID().toString().replace("-", "") + ".block");
			if (tempFile.createNewFile()) {
				int numBytesRead;
				long size = 0L;
				if (oldBlock != null && oldBlock.isFile()) {
					// 如果有文件原体，则先将文件原体拷贝至此临时文件中
					Files.copy(oldBlock.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					// 在原体拷贝版的基础上追加数据
					RandomAccessFile randAccessTempFile = new RandomAccessFile(tempFile, "rw");
					if (maxSize >= 0 && range.length > maxSize) {
						// 若是体积已经超限了，则终止继续写入
						randAccessTempFile.close();
						tempFile.delete();
						return null;
					}
					randAccessTempFile.setLength(range.length);
					randAccessTempFile.seek(range.start);
					byte[] transferBuffer = new byte[BUFFER_SIZE];
					ServletInputStream in = req.getInputStream();
					BufferedInputStream bufIn = new BufferedInputStream(in, BUFFER_SIZE);
					while ((numBytesRead = bufIn.read(transferBuffer)) != -1) {
						randAccessTempFile.write(transferBuffer, 0, numBytesRead);
						size += numBytesRead;
					}
					randAccessTempFile.close();
				} else {
					// 如果没有文件原体，则直接将输入流中的所有数据存入临时文件中
					FileOutputStream out = new FileOutputStream(tempFile);
					byte[] copyBuffer = new byte[BUFFER_SIZE];
					ServletInputStream in = req.getInputStream();
					BufferedInputStream bufIn = new BufferedInputStream(in, BUFFER_SIZE);
					while ((numBytesRead = bufIn.read(copyBuffer)) != -1) {
						out.write(copyBuffer, 0, numBytesRead);
						size += numBytesRead;
						if (maxSize >= 0 && size > maxSize) {
							// 同理，体积限制逻辑
							out.close();
							tempFile.delete();
							return null;
						}
					}
					bufIn.close();
					out.flush();
					out.close();
				}
				// 生成完毕后，返回此临时文件
				return tempFile;
			}
		}
		throw new IOException();
	}

	/**
	 * 
	 * <h2>从请求对象中解析Range标签</h2>
	 * <p>
	 * 该方法用于解析请求对象中的Content-Range请求头（若有），并将其参数封装为Range对象返回，具体规则请见返回说明。
	 * </p>
	 * 
	 * @param request  请求对象
	 * @param response 响应对象
	 * @author Apache Tomcat
	 * @return Range对象，其中包含了从Content-Range请求头中解析出来的Range信息（若有），
	 *         如果请求中未声明Content-Range请求头，则会返回特定的kohgylw.kiftd.server.webdav.WebdavServlet.IGNORE对象，
	 *         此时可以视作忽略Range功能。如果Content-Range请求头声明有误，则会返回null，并会在响应对象中设置相应的响应码。
	 * @throws IOException 响应对象的响应码发送失败。
	 */
	protected Range parseContentRange(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String contentRangeHeader = request.getHeader("Content-Range");
		if (contentRangeHeader == null) {
			return IGNORE;
		}
		ContentRange contentRange = ContentRange.parse(new StringReader(contentRangeHeader));
		if (contentRange == null) {
			response.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return null;
		}
		if (!contentRange.getUnits().equals("bytes")) {
			response.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return null;
		}
		Range range = new Range();
		range.start = contentRange.getStart();
		range.end = contentRange.getEnd();
		range.length = contentRange.getLength();
		if (!range.validate()) {
			response.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return null;
		}
		return range;
	}

	/**
	 * 
	 * <h2>处理delete请求</h2>
	 * <p>
	 * 该方法用于处理delete请求，删除指定逻辑路径上的文件或文件夹。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象。
	 * @param resp 响应对象。
	 */
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 判断资源是否已被锁定，若锁定则无法删除
		if (isLocked(req)) {
			resp.setStatus(WebdavStatus.SC_LOCKED);
			return;
		}
		String path = getRelativePath(req);
		String ifHeader = req.getHeader("If");
		if (ifHeader == null) {
			ifHeader = "";
		}
		String lockTokenHeader = req.getHeader("Lock-Token");
		if (lockTokenHeader == null) {
			lockTokenHeader = "";
		}
		if (isLocked(path, ifHeader + lockTokenHeader)) {
			resp.setStatus(WebdavStatus.SC_LOCKED);
			return;
		}
		// 检查深度标签是否正确，理论上只接受“Depth”请求头为“infinity”，若无此请求头，默认为“infinity”
		String depthStr = req.getHeader("Depth");
		if (depthStr != null && !depthStr.equals("infinity")) {
			resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return;
		}
		// 认证检查，解析账户信息
		String account;
		try {
			account = getAccountFromRequestHeader(req);
		} catch (IllegalArgumentException | UnAuthorizedException e) {
			// 认证失败
			needAuthorizationByBasic(resp);
			return;
		}
		if (ConfigureReader.instance().mustLogin() && account == null) {
			needAuthorizationByBasic(resp);
			return;
		}
		// 检查要删除的资源是否存在
		KiftdWebDAVResource resource = resources.getResource(path);
		if (!resource.exists()) {
			// 若资源不存在，则返回状态码404
			resp.setStatus(WebdavStatus.SC_NOT_FOUND);
			return;
		}
		if (resource.isFile()) {
			// 如果删除的资源是一个文件节点，则执行文件删除逻辑
			kohgylw.kiftd.server.model.Node node = resource.getNode();
			if (node != null) {
				// 权限检查……
				Folder parentFolder = fm.queryById(node.getFileParentFolder());
				if (!ConfigureReader.instance().accessFolder(parentFolder, account)
						|| !ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
								fu.getAllFoldersId(parentFolder.getFolderId()))) {
					needAuthorizationByBasic(resp);
					return;
				}
				// 删除文件节点
				if (this.fbu.deleteNode(node)) {
					// 删除成功，记录日志并返回状态码204
					this.lu.writeDeleteFileEvent(req, node);
					resp.setStatus(WebdavStatus.SC_NO_CONTENT);
					return;
				}
			}
			// 删除失败，返回状态码500
			resp.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
			return;
		} else {
			// 如果删除的资源是一个文件夹，则执行迭代删除文件夹逻辑
			Folder folder = resource.getFolder();
			if (folder != null) {
				// 权限检查……
				Folder parentFolder = fm.queryById(folder.getFolderParent());
				if (!ConfigureReader.instance().accessFolder(parentFolder, account)
						|| !ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
								fu.getAllFoldersId(parentFolder.getFolderId()))) {
					needAuthorizationByBasic(resp);
					return;
				}
				// 执行迭代删除
				final List<Folder> l = this.fu.getParentList(folder.getFolderId());
				if (this.fm.deleteById(folder.getFolderId()) > 0) {
					// 成功，尝试删除子文件夹并计入日志，返回代码204
					fu.deleteAllChildFolder(folder.getFolderId());
					this.lu.writeDeleteFolderEvent(req, folder, l);
					ServerInitListener.needCheck = true;
					resp.setStatus(WebdavStatus.SC_NO_CONTENT);
					return;
				}
			}
			// 失败，返回状态码500
			resp.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}

	/**
	 * 
	 * <h2>处理copy请求</h2>
	 * <p>
	 * 该方法用于处理copy请求，将指定逻辑路径上的文件或文件夹拷贝到“Destination”请求头指定的逻辑路径上。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象。
	 * @param resp 响应对象。
	 */
	protected void doCopy(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// 例行的资源锁定检查
		if (isLocked(req)) {
			resp.setStatus(WebdavStatus.SC_LOCKED);
			return;
		}
		// 执行复制操作
		doMoveOrCopy(req, resp, true);
	}

	/**
	 * 
	 * <h2>处理move请求</h2>
	 * <p>
	 * 该方法用于处理move请求，将指定逻辑路径上的文件或文件夹移动到“Destination”请求头指定的逻辑路径上。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象。
	 * @param resp 响应对象。
	 */
	protected void doMove(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// 例行的资源锁定检查
		if (isLocked(req)) {
			resp.setStatus(WebdavStatus.SC_LOCKED);
			return;
		}
		// 执行移动操作
		doMoveOrCopy(req, resp, false);
	}

	/**
	 * 
	 * <h2>执行移动或复制逻辑</h2>
	 * <p>
	 * 该方法实现了移动或复制逻辑，当设定为复制模式时，会在指定逻辑路径上创建一个原资源的副本； 当设定为移动模式时，则会将原资源移动到指定逻辑路径上。
	 * 其中，指定逻辑路径由“Destination”请求头声明。
	 * 当“Overwrite”请求头存在且为“T”时，会覆盖指定逻辑路径上冲突的资源，否则资源冲突会导致操作失败。
	 * 当“Depth”请求头存在时，若为拷贝模式，则当其值为“0”时，拷贝文件夹只会在指定逻辑路径上创建一个与原资源同名的空文件夹；
	 * 当其值为“infinity”时，拷贝文件夹会在指定逻辑路径上生成原资源的完整副本（包括其内容）。移动模式仅允许该值为“infinity”。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req    请求对象
	 * @param resp   响应对象
	 * @param isCopy 是否为拷贝模式，当传入true时执行拷贝，否则执行移动
	 */
	private void doMoveOrCopy(HttpServletRequest req, HttpServletResponse resp, boolean isCopy) throws IOException {
		// 检查深度标签是否正确，移动模式理只接受“Depth”请求头为“infinity”，若无此请求头，默认为“infinity”
		String depthStr = req.getHeader("Depth");
		boolean copyAll = true;// 标识是否为全部拷贝模式，若是，则拷贝完整的文件夹，否则仅创建空文件夹
		if (depthStr != null && !depthStr.equals("infinity")) {
			if (depthStr.equals("infinity")) {
				copyAll = true;
			} else if (depthStr.equals("0")) {
				if (isCopy) {
					copyAll = false;
				} else {
					// 移动模式不支持除“infinity”以外的值
					resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
					return;
				}
			} else {
				// 两种模式均不支持除“infinity”或“0”以外的值
				resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
				return;
			}
		}
		// 例行的授权检查
		String account;
		try {
			account = getAccountFromRequestHeader(req);
		} catch (IllegalArgumentException | UnAuthorizedException e) {
			needAuthorizationByBasic(resp);
			return;
		}
		if (ConfigureReader.instance().mustLogin() && account == null) {
			needAuthorizationByBasic(resp);
			return;
		}
		// 获取资源路径
		String path = getRelativePath(req);
		// 此资源存在？
		KiftdWebDAVResource resource = resources.getResource(path);
		if (!resource.exists()) {
			resp.setStatus(WebdavStatus.SC_NOT_FOUND);
			return;
		}
		// 存在，那么开始解析“Destination”请求头，获得目标路径
		String destinationPath = getDestinationPath(req);
		if (destinationPath == null) {
			// 如果解析失败，则返回状态码400
			resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return;
		}
		if (destinationPath.equals(path)) {
			// 如果目标逻辑路径和当前路径是一样的，那么该操作必然导致冲突，如果覆盖，则操作无意义，如果不覆盖，则无法执行
			resp.sendError(WebdavStatus.SC_FORBIDDEN);
			return;
		}
		// 接下来开始权限检查，检查客户端是否有权访问资源的父目录及目标目录
		Folder targetFolder = resources.getFolderByPath(HttpPathUtil.getParentPath(destinationPath));
		Folder parentFolder = null;
		if (resource.isDirectory()) {
			parentFolder = fm.queryById(resource.getFolder().getFolderParent());
		} else {
			parentFolder = fm.queryById(resource.getNode().getFileParentFolder());
		}
		if (!ConfigureReader.instance().accessFolder(parentFolder, account)
				|| !ConfigureReader.instance().accessFolder(targetFolder, account)) {
			needAuthorizationByBasic(resp);
			return;
		}
		// 如果改变了资源名称，那么需要“重命名”权限；如果改变了资源父目录，那么需要“移动”权限
		String newName = HttpPathUtil.getResourceName(destinationPath);
		if (newName.isEmpty()) {
			resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return;
		}
		if (!newName.equals(resource.getName()) && !ConfigureReader.instance().authorized(account,
				AccountAuth.RENAME_FILE_OR_FOLDER, fu.getAllFoldersId(targetFolder.getFolderId()))) {
			needAuthorizationByBasic(resp);
			return;
		}
		if (!parentFolder.getFolderId().equals(targetFolder.getFolderId()) && (!ConfigureReader.instance()
				.authorized(account, AccountAuth.MOVE_FILES, fu.getAllFoldersId(parentFolder.getFolderId()))
				|| !ConfigureReader.instance().authorized(account, AccountAuth.MOVE_FILES,
						fu.getAllFoldersId(targetFolder.getFolderId())))) {
			needAuthorizationByBasic(resp);
			return;
		}
		// 是否要求覆盖？如果是，则额外需要“删除”权限
		boolean overwrite = true;// 标记是否执行覆盖操作
		String overwriteHeader = req.getHeader("Overwrite");
		if (overwriteHeader != null) {
			if (overwriteHeader.equalsIgnoreCase("T")) {
				overwrite = true;
			} else {
				overwrite = false;
			}
		}
		if (overwrite && !ConfigureReader.instance().authorized(account, AccountAuth.DELETE_FILE_OR_FOLDER,
				fu.getAllFoldersId(targetFolder.getFolderId()))) {
			needAuthorizationByBasic(resp);
			return;
		}
		// 前置检查完毕，接下来判断原资源是文件还是文件夹？
		if (resource.isFile()) {
			// 如果是文件，先检查目标文件夹内是否有重名文件？
			kohgylw.kiftd.server.model.Node originNode = resource.getNode();
			String originPath = fbu.getNodePath(originNode);
			if (nm.queryByParentFolderId(targetFolder.getFolderId()).parallelStream()
					.anyMatch((e) -> e.getFileName().equals(newName))) {
				// 若有，是否执行覆盖？
				if (overwrite) {
					// 执行覆盖，获得冲突节点
					kohgylw.kiftd.server.model.Node conflictNode = nm.queryByParentFolderId(targetFolder.getFolderId())
							.parallelStream().filter((e) -> e.getFileName().equals(newName)).findFirst().get();
					if (conflictNode.getFileId().equals(originNode.getFileId())) {
						// 如果要覆盖的节点就是原节点本身，则直接认为操作成功，也无需记录日志（因为操作无效）
						resp.setStatus(WebdavStatus.SC_NO_CONTENT);
						return;
					}
					// 否则，删除冲突节点
					if (fbu.deleteNode(conflictNode)) {
						if (isCopy) {
							// 如果是拷贝模式，新建一个与原资源使用相同文件块的节点添加到目标路径下
							kohgylw.kiftd.server.model.Node copyNode = fbu.insertNewNode(newName, account,
									originNode.getFilePath(), originNode.getFileSize(), targetFolder.getFolderId());
							if (copyNode != null) {
								// 拷贝成功，记录日志
								lu.writeMoveFileEvent(account, idg.getIpAddr(req), originPath,
										fbu.getNodePath(copyNode), true);
								// 返回状态码204
								resp.setStatus(WebdavStatus.SC_NO_CONTENT);
								// 成功
								return;
							}
						} else {
							// 如果是移动模式，将原节点的名称改为新名称，同时将其父文件夹改为目标文件夹
							originNode.setFileName(newName);
							originNode.setFileParentFolder(targetFolder.getFolderId());
							if (nm.update(originNode) > 0) {
								// 移动成功，记录日志
								if (targetFolder.getFolderId().equals(parentFolder.getFolderId())) {
									// 如果未更改父路径（仅有名称变化），则记录为“重命名”日志
									lu.writeRenameFileEvent(account, idg.getIpAddr(req), targetFolder.getFolderId(),
											resource.getName(), newName);
								} else {
									// 如果更改了父路径，则记录为“移动”日志（名称变化也会体现出来）
									lu.writeMoveFileEvent(account, idg.getIpAddr(req), originPath,
											fbu.getNodePath(originNode), false);
								}
								// 返回状态码204
								resp.setStatus(WebdavStatus.SC_NO_CONTENT);
								// 成功
								return;
							}
						}
					}
					// 移动或复制操作失败，返回状态码500
					resp.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
					return;
				} else {
					// 资源冲突又不覆盖，则返回状态码412
					resp.setStatus(WebdavStatus.SC_PRECONDITION_FAILED);
					return;
				}
			} else {
				// 若无，直接执行复制或移动操作
				if (!parentFolder.getFolderId().equals(targetFolder.getFolderId()) && nm.countByParentFolderId(
						targetFolder.getFolderId()) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
					// 如果移动或复制后会导致目标文件夹超限，则拒绝执行此操作，返回状态码403
					resp.setStatus(WebdavStatus.SC_FORBIDDEN);
					return;
				}
				// 判断是移动模式还是复制模式
				if (isCopy) {
					// 如果是拷贝模式，则创建一个新节点并与原节点引用相同的文件块
					kohgylw.kiftd.server.model.Node newNode = fbu.insertNewNode(newName, account,
							originNode.getFilePath(), originNode.getFileSize(), targetFolder.getFolderId());
					if (newNode != null) {
						// 拷贝成功，记录日志
						lu.writeMoveFileEvent(account, idg.getIpAddr(req), originPath, fbu.getNodePath(newNode), true);
						// 返回状态码201
						resp.setStatus(WebdavStatus.SC_CREATED);
						lockNullResources.remove(destinationPath);
						// 成功
						return;
					}
				} else {
					// 如果是移动模式，将原节点的名称改为新名称，同时将其父文件夹改为目标文件夹
					originNode.setFileName(newName);
					originNode.setFileParentFolder(targetFolder.getFolderId());
					if (nm.update(originNode) > 0) {
						// 移动成功，记录日志
						if (targetFolder.getFolderId().equals(parentFolder.getFolderId())) {
							lu.writeRenameFileEvent(account, idg.getIpAddr(req), targetFolder.getFolderId(),
									resource.getName(), newName);
						} else {
							lu.writeMoveFileEvent(account, idg.getIpAddr(req), originPath, fbu.getNodePath(originNode),
									false);
						}
						// 返回状态码201
						resp.setStatus(WebdavStatus.SC_CREATED);
						lockNullResources.remove(destinationPath);
						// 成功
						return;
					}
				}
				// 操作失败，返回状态码500
				resp.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		} else {
			// 如果是文件夹，判断是否为移动模式
			Folder originFolder = resource.getFolder();
			String originPath = fu.getFolderPath(originFolder);
			if (!isCopy) {
				// 对于移动模式，还需要检查是否把文件夹移动到自己内部
				if (originFolder.getFolderId().equals(targetFolder.getFolderId())
						|| fu.getParentList(targetFolder.getFolderId()).parallelStream()
								.anyMatch((e) -> e.getFolderId().equals(originFolder.getFolderId()))) {
					resp.setStatus(WebdavStatus.SC_FORBIDDEN);
					return;
				}
			}
			// 再检查目标文件夹内是否有重名文件夹？
			if (fm.queryByParentId(targetFolder.getFolderId()).parallelStream()
					.anyMatch((e) -> e.getFolderName().equals(newName))) {
				// 若有，是否覆盖？
				if (overwrite) {
					// 与移动文件的规则类似
					Folder conflictFolder = fm.queryByParentId(targetFolder.getFolderId()).parallelStream()
							.filter((e) -> e.getFolderName().equals(newName)).findFirst().get();
					if (fm.deleteById(conflictFolder.getFolderId()) > 0) {
						if (isCopy) {
							// 拷贝模式，判断是否拷贝完整文件夹？
							if (copyAll) {
								// 如果是完整拷贝文件夹……
								Folder newFolder = fu.copyFolderByNewNameToPath(conflictFolder, account, targetFolder,
										newName);
								if (newFolder != null) {
									// 拷贝成功，记录日志
									lu.writeMoveFolderEvent(account, idg.getIpAddr(req), originPath,
											fu.getFolderPath(newFolder), true);
									resp.setStatus(WebdavStatus.SC_NO_CONTENT);
									lockNullResources.remove(destinationPath);
									return;
								}
							} else {
								// 如果仅创建一个同名的空文件夹……
								int constraint = originFolder.getFolderConstraint();
								if (originFolder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
									constraint = targetFolder.getFolderConstraint();
								}
								try {
									Folder newFolder = fu.createNewFolder(targetFolder.getFolderId(), account, newName,
											"" + constraint);
									if (newFolder != null && fu.isValidFolder(newFolder)) {
										// 创建成功，记录日志
										lu.writeCreateFolderEvent(account, idg.getIpAddr(req), newFolder);
										resp.setStatus(WebdavStatus.SC_NO_CONTENT);
										lockNullResources.remove(destinationPath);
										// 成功
										return;
									}
								} catch (FoldersTotalOutOfLimitException e1) {
									// 理论上不会引起此异常，无需单独处理
								}
							}
						} else {
							// 移动模式
							originFolder.setFolderParent(targetFolder.getFolderId());
							originFolder.setFolderName(newName);
							// 额外的，如果原文件夹的访问级别比目标文件夹小，则还需要将访问级别升高至与目标文件夹一致
							int originConstraint = originFolder.getFolderConstraint();
							boolean needChangeChildsConstranint = false;
							if (originFolder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
								originFolder.setFolderConstraint(targetFolder.getFolderConstraint());
								needChangeChildsConstranint = true;
							}
							if (fm.update(originFolder) > 0) {
								// 如果升高了文件夹的访问级别，那么子文件夹的访问级别也要一起升高
								if (needChangeChildsConstranint) {
									fu.changeChildFolderConstraint(originFolder.getFolderId(),
											targetFolder.getFolderConstraint());
								}
								// 成功后，记录日志
								if (parentFolder.getFolderId().equals(targetFolder.getFolderId())) {
									lu.writeRenameFolderEvent(account, idg.getIpAddr(req), originFolder.getFolderId(),
											resource.getName(), newName, "" + originConstraint,
											"" + originFolder.getFolderConstraint());
								} else {
									lu.writeMoveFolderEvent(account, idg.getIpAddr(req), originPath,
											fu.getFolderPath(originFolder), false);
								}
								resp.setStatus(WebdavStatus.SC_NO_CONTENT);
								lockNullResources.remove(destinationPath);
								return;
							}
						}
						ServerInitListener.needCheck = true;// 删除冲突文件夹可能导致特定文件夹权限设置失效，需重新检查
						// 清理冲突文件夹内的所有资源
						fu.deleteAllChildFolder(conflictFolder.getFolderId());
					}
					// 操作失败（冲突文件夹已无法还原，因此无需尝试再插入冲突文件夹）
					resp.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
					return;
				} else {
					// 资源冲突且不覆盖，返回状态码412
					resp.setStatus(WebdavStatus.SC_PRECONDITION_FAILED);
					return;
				}
			} else {
				// 若无，则直接执行移动或拷贝，与文件的规则类似
				if (!parentFolder.getFolderId().equals(targetFolder.getFolderId()) && fm
						.countByParentId(targetFolder.getFolderId()) >= FileNodeUtil.MAXIMUM_NUM_OF_SINGLE_FOLDER) {
					resp.setStatus(WebdavStatus.SC_FORBIDDEN);
					return;
				}
				if (isCopy) {
					Folder newFolder = fu.copyFolderByNewNameToPath(originFolder, account, parentFolder, newName);
					if (newFolder != null) {
						lu.writeMoveFolderEvent(account, idg.getIpAddr(req), originPath, fu.getFolderPath(newFolder),
								true);
						resp.setStatus(WebdavStatus.SC_NO_CONTENT);
						lockNullResources.remove(destinationPath);
						return;
					}
				} else {
					originFolder.setFolderParent(targetFolder.getFolderId());
					originFolder.setFolderName(newName);
					// 确保移入后访问级别不越界
					int originConstraint = originFolder.getFolderConstraint();
					boolean needChangeChildsConstranint = false;
					if (originFolder.getFolderConstraint() < targetFolder.getFolderConstraint()) {
						originFolder.setFolderConstraint(targetFolder.getFolderConstraint());
						needChangeChildsConstranint = true;
					}
					if (fm.update(originFolder) > 0) {
						// 操作成功
						if (needChangeChildsConstranint) {
							fu.changeChildFolderConstraint(originFolder.getFolderId(),
									targetFolder.getFolderConstraint());
						}
						if (parentFolder.getFolderId().equals(targetFolder.getFolderId())) {
							lu.writeRenameFolderEvent(account, idg.getIpAddr(req), originFolder.getFolderId(),
									resource.getName(), newName, "" + originConstraint,
									"" + originFolder.getFolderConstraint());
						} else {
							lu.writeMoveFolderEvent(account, idg.getIpAddr(req), originPath,
									fu.getFolderPath(originFolder), false);
						}
						resp.setStatus(WebdavStatus.SC_CREATED);
						lockNullResources.remove(destinationPath);
						return;
					}
				}
				// 操作失败
				resp.setStatus(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		}
	}

	/**
	 * 
	 * <h2>从请求对象中解析出“Destination”请求头</h2>
	 * <p>
	 * 该方法用于解析“Destination”请求头并完成必要的检查，最后返回其代表的资源路径。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req 请求对象
	 * @return java.lang.String 由“Destination”请求头解析出的逻辑路径，如果客户端发送了此请求头且格式正确，
	 *         则返回解析结果，否则返回null。该方法仅对请求头的格式进行检查，但不确保其对应的逻辑路径合法。
	 */
	private String getDestinationPath(HttpServletRequest req) {
		// 解析出“Destination”请求头，之后将其内容格式化
		String destinationHeader = req.getHeader("Destination");
		if (destinationHeader == null || destinationHeader.isEmpty()) {
			return null;
		}
		URI destinationUri;
		try {
			destinationUri = new URI(destinationHeader);
		} catch (URISyntaxException e) {
			return null;
		}
		String destinationPath = destinationUri.getPath();
		// 不支持相对路径声明
		if (!destinationPath.equals(RequestUtil.normalize(destinationPath))) {
			return null;
		}
		// 对于绝对路径，要进行路径合理性检查
		if (destinationUri.isAbsolute()) {
			// 检查目标路径的域名是否与服务器的端口号一致
			if (!req.getScheme().equals(destinationUri.getScheme())
					|| !req.getServerName().equals(destinationUri.getHost())) {
				return null;
			}
			// 检查目标路径的端口号是否与服务器的端口号一致
			if (req.getServerPort() != destinationUri.getPort()) {
				if (destinationUri.getPort() == -1 && ("http".equals(req.getScheme()) && req.getServerPort() == 80
						|| "https".equals(req.getScheme()) && req.getServerPort() == 443)) {
					// All good.
				} else {
					return null;
				}
			}
		}
		// 禁止跨上下文操作
		String reqContextPath = req.getContextPath();
		if (!destinationPath.startsWith(reqContextPath + "/")) {
			return null;
		}
		// 去掉上下文和Servlet路径前缀，仅保留逻辑路径部分
		destinationPath = destinationPath.substring(reqContextPath.length() + req.getServletPath().length());
		return destinationPath;
	}

	/**
	 * 
	 * <h2>处理lock请求</h2>
	 * <p>
	 * 该方法用于处理lock请求，将指定逻辑路径上的文件或文件夹锁定。对于Mac OS X系统中的“访达”而言，
	 * 这一功能同样是实现“上传”操作的必要功能之一（单独实现PUT是不够的）。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象。
	 * @param resp 响应对象。
	 */
	protected void doLock(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 已经锁定的资源不能再次被锁定
		if (isLocked(req)) {
			resp.setStatus(WebdavStatus.SC_LOCKED);
			return;
		}
		// 获取授权信息
		String account;
		try {
			account = getAccountFromRequestHeader(req);
		} catch (IllegalArgumentException | UnAuthorizedException e1) {
			// 授权无效
			needAuthorizationByBasic(resp);
			return;
		}
		// “必须登入”约束检查
		if (ConfigureReader.instance().mustLogin() && account == null) {
			needAuthorizationByBasic(resp);
			return;
		}
		// 权限检查，由于可能会锁定不存在的路径，因此需要先依次沿路径向上轮询直至找到一个存在的父文件夹，再据此判断权限
		String path = getRelativePath(req);
		String AuthFolderPath = path;
		Folder parentFolder = null;
		while (parentFolder == null && !AuthFolderPath.isEmpty()) {
			// 如果当前parentPath路径并不是实际存在的文件夹，则继续向上查询，直至找到一个存在的父文件夹或者一直找不到
			AuthFolderPath = HttpPathUtil.getParentPath(AuthFolderPath);
			parentFolder = resources.getFolderByPath(AuthFolderPath);
		}
		if (parentFolder == null) {
			// 如果逻辑路径上找不到一个存在的父文件夹，则请求路径必然有误，返回状态码400
			resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return;
		}
		if (!ConfigureReader.instance().accessFolder(parentFolder, account) || !ConfigureReader.instance()
				.authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(parentFolder.getFolderId()))) {
			// 无操作权限（上传）或不允许访问此文件夹
			needAuthorizationByBasic(resp);
			return;
		}
		// 生成LockInfo对象并确定检索深度
		LockInfo lock = new LockInfo(maxDepth);
		String depthStr = req.getHeader("Depth");
		if (depthStr == null) {
			lock.depth = maxDepth;
		} else {
			if (depthStr.equals("0")) {
				lock.depth = 0;
			} else {
				lock.depth = maxDepth;
			}
		}
		// 确定锁定的超时时间
		int lockDuration = DEFAULT_TIMEOUT;
		String lockDurationStr = req.getHeader("Timeout");
		if (lockDurationStr == null) {
			lockDuration = DEFAULT_TIMEOUT;
		} else {
			int commaPos = lockDurationStr.indexOf(',');
			if (commaPos != -1) {
				lockDurationStr = lockDurationStr.substring(0, commaPos);
			}
			if (lockDurationStr.startsWith("Second-")) {
				lockDuration = Integer.parseInt(lockDurationStr.substring(7));
			} else {
				if (lockDurationStr.equalsIgnoreCase("infinity")) {
					lockDuration = MAX_TIMEOUT;
				} else {
					try {
						lockDuration = Integer.parseInt(lockDurationStr);
					} catch (NumberFormatException e) {
						lockDuration = MAX_TIMEOUT;
					}
				}
			}
			if (lockDuration == 0) {
				lockDuration = DEFAULT_TIMEOUT;
			}
			if (lockDuration > MAX_TIMEOUT) {
				lockDuration = MAX_TIMEOUT;
			}
		}
		lock.expiresAt = System.currentTimeMillis() + (lockDuration * 1000);
		// 从请求中解析出客户端要求执行的锁定方式
		int lockRequestType = LOCK_CREATION;
		Node lockInfoNode = null;
		DocumentBuilder documentBuilder = getDocumentBuilder();
		try {
			Document document = documentBuilder.parse(new InputSource(req.getInputStream()));
			Element rootElement = document.getDocumentElement();
			lockInfoNode = rootElement;
		} catch (IOException | SAXException e) {
			lockRequestType = LOCK_REFRESH;
		}
		if (lockInfoNode != null) {
			NodeList childList = lockInfoNode.getChildNodes();
			StringWriter strWriter = null;
			DOMWriter domWriter = null;
			Node lockScopeNode = null;
			Node lockTypeNode = null;
			Node lockOwnerNode = null;
			for (int i = 0; i < childList.getLength(); i++) {
				Node currentNode = childList.item(i);
				switch (currentNode.getNodeType()) {
				case Node.TEXT_NODE:
					break;
				case Node.ELEMENT_NODE:
					String nodeName = currentNode.getNodeName();
					if (nodeName.endsWith("lockscope")) {
						lockScopeNode = currentNode;
					}
					if (nodeName.endsWith("locktype")) {
						lockTypeNode = currentNode;
					}
					if (nodeName.endsWith("owner")) {
						lockOwnerNode = currentNode;
					}
					break;
				}
			}
			if (lockScopeNode != null) {
				childList = lockScopeNode.getChildNodes();
				for (int i = 0; i < childList.getLength(); i++) {
					Node currentNode = childList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						break;
					case Node.ELEMENT_NODE:
						String tempScope = currentNode.getNodeName();
						if (tempScope.indexOf(':') != -1) {
							lock.scope = tempScope.substring(tempScope.indexOf(':') + 1);
						} else {
							lock.scope = tempScope;
						}
						break;
					}
				}
				if (lock.scope == null) {
					resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
				}
			} else {
				resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
			}
			if (lockTypeNode != null) {
				childList = lockTypeNode.getChildNodes();
				for (int i = 0; i < childList.getLength(); i++) {
					Node currentNode = childList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						break;
					case Node.ELEMENT_NODE:
						String tempType = currentNode.getNodeName();
						if (tempType.indexOf(':') != -1) {
							lock.type = tempType.substring(tempType.indexOf(':') + 1);
						} else {
							lock.type = tempType;
						}
						break;
					}
				}
				if (lock.type == null) {
					resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
				}
			} else {
				resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
			}
			if (lockOwnerNode != null) {
				childList = lockOwnerNode.getChildNodes();
				for (int i = 0; i < childList.getLength(); i++) {
					Node currentNode = childList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						lock.owner += currentNode.getNodeValue();
						break;
					case Node.ELEMENT_NODE:
						strWriter = new StringWriter();
						domWriter = new DOMWriter(strWriter);
						domWriter.print(currentNode);
						lock.owner += strWriter.toString();
						break;
					}
				}
				if (lock.owner == null) {
					resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
				}
			} else {
				lock.owner = "";
			}
		}
		// 准备工作进行完毕，接下来对要求锁定的资源进行确认
		WebResource resource = resources.getResource(path);
		lock.path = path;
		Enumeration<LockInfo> locksList = null;
		if (lockRequestType == LOCK_CREATION) {
			// 以“创建”为目的进行的锁定操作
			String lockTokenStr = req.getServletPath() + "-" + lock.type + "-" + lock.scope + "-" + account + "-"
					+ lock.depth + "-" + lock.owner + "-" + lock.tokens + "-" + lock.expiresAt + "-"
					+ System.currentTimeMillis() + "-" + secret;
			String lockToken = MD5Encoder
					.encode(ConcurrentMessageDigest.digestMD5(lockTokenStr.getBytes(StandardCharsets.ISO_8859_1)));
			if (resource.isDirectory() && lock.depth == maxDepth) {
				// 对文件夹及其子文件夹进行锁定，先检查是否有子文件夹已经被锁定了
				Vector<String> lockPaths = new Vector<>();
				locksList = collectionLocks.elements();
				while (locksList.hasMoreElements()) {
					LockInfo currentLock = locksList.nextElement();
					if (currentLock.hasExpired()) {
						resourceLocks.remove(currentLock.path);
						continue;
					}
					if ((currentLock.path.startsWith(lock.path))
							&& ((currentLock.isExclusive()) || (lock.isExclusive()))) {
						lockPaths.addElement(currentLock.path);
					}
				}
				locksList = resourceLocks.elements();
				while (locksList.hasMoreElements()) {
					LockInfo currentLock = locksList.nextElement();
					if (currentLock.hasExpired()) {
						resourceLocks.remove(currentLock.path);
						continue;
					}
					if ((currentLock.path.startsWith(lock.path))
							&& ((currentLock.isExclusive()) || (lock.isExclusive()))) {
						lockPaths.addElement(currentLock.path);
					}
				}
				if (!lockPaths.isEmpty()) {
					// 如果已经有子文件夹被锁定了，向客户端返回错误信息并设置响应码为409
					Enumeration<String> lockPathsList = lockPaths.elements();
					resp.setStatus(WebdavStatus.SC_CONFLICT);
					XMLWriter generatedXML = new XMLWriter();
					generatedXML.writeXMLHeader();
					generatedXML.writeElement("D", DEFAULT_NAMESPACE, "multistatus", XMLWriter.OPENING);
					while (lockPathsList.hasMoreElements()) {
						generatedXML.writeElement("D", "response", XMLWriter.OPENING);
						generatedXML.writeElement("D", "href", XMLWriter.OPENING);
						generatedXML.writeText(lockPathsList.nextElement());
						generatedXML.writeElement("D", "href", XMLWriter.CLOSING);
						generatedXML.writeElement("D", "status", XMLWriter.OPENING);
						generatedXML.writeText("HTTP/1.1 " + WebdavStatus.SC_LOCKED + " ");
						generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
						generatedXML.writeElement("D", "response", XMLWriter.CLOSING);
					}
					generatedXML.writeElement("D", "multistatus", XMLWriter.CLOSING);
					Writer writer = resp.getWriter();
					writer.write(generatedXML.toString());
					writer.close();
					return;
				}
				// 对文件夹路径加锁，如果它已经有一个功能各项的锁，则无需重复添加，否则添加一个新锁
				boolean addLock = true;// 判断是否添加新锁
				// 检查是否有共享锁
				locksList = collectionLocks.elements();
				while (locksList.hasMoreElements()) {
					LockInfo currentLock = locksList.nextElement();
					if (currentLock.path.equals(lock.path)) {
						// 如果此逻辑路径已经有一个锁了
						if (currentLock.isExclusive()) {
							// 且这个锁是一个独享锁，则返回响应码423，告知客户端此文件夹路径已经被锁
							resp.setStatus(WebdavStatus.SC_LOCKED);
							return;
						} else {
							// 如果已有的锁不是独享锁，但是客户端新申请的是一个独享锁，则也要返回响应码423
							if (lock.isExclusive()) {
								resp.setStatus(WebdavStatus.SC_LOCKED);
								return;
							}
						}
						// 如果客户端新申请的也是一个共享锁，则将其锁标识加入此锁以便判断
						currentLock.tokens.addElement(lockToken);
						lock = currentLock;
						addLock = false;// 在此情况下，无需加入新锁
					}
				}
				if (addLock) {
					// 若未找到共享锁，则要添加新锁
					lock.tokens.addElement(lockToken);
					collectionLocks.addElement(lock);
				}
			} else {
				// 锁定一个文件，先检查此文件是否已经被锁定了
				LockInfo presentLock = resourceLocks.get(lock.path);
				if (presentLock != null) {
					// 如果确实已经被锁定了
					if ((presentLock.isExclusive()) || (lock.isExclusive())) {
						// 与文件夹锁同理，如果已经有的锁是一个独享锁或者客户端新申请的是一个独享锁，则返回响应码412
						resp.setStatus(WebdavStatus.SC_PRECONDITION_FAILED);
						return;
					} else {
						// 否则，作为共享锁加入标识
						presentLock.tokens.addElement(lockToken);
						lock = presentLock;
					}
				} else {
					// 该文件尚未有锁，则加入新锁
					lock.tokens.addElement(lockToken);
					resourceLocks.put(lock.path, lock);
					if (!resource.exists()) {
						// 如果锁定的是一个不存在的资源，则将其路径加入到不存在的锁定资源列表
						int slash = lock.path.lastIndexOf('/');
						String parentPath = lock.path.substring(0, slash);
						Vector<String> lockNulls = lockNullResources.get(parentPath);
						if (lockNulls == null) {
							lockNulls = new Vector<>();
							lockNullResources.put(parentPath, lockNulls);
						}
						lockNulls.addElement(lock.path);
					}
					// 通过Lock-Token响应头告知客户端其申请的新锁标识（该操作仅在新锁被创建后才需要执行）
					// 客户端进行unlock操作时需要凭借此标识进行
					resp.addHeader("Lock-Token", "<opaquelocktoken:" + lockToken + ">");
				}
			}
		}
		if (lockRequestType == LOCK_REFRESH) {
			// 以“刷新”为目的进行的锁定操作
			String ifHeader = req.getHeader("If");
			if (ifHeader == null) {
				ifHeader = "";
			}
			// 如果是文件锁，那么此时应该已存在一个旧锁，尝试获取并更新它
			LockInfo toRenew = resourceLocks.get(path);
			Enumeration<String> tokenList = null;
			if (toRenew != null) {
				// 将旧锁的标识进行更新
				tokenList = toRenew.tokens.elements();
				while (tokenList.hasMoreElements()) {
					String token = tokenList.nextElement();
					if (ifHeader.contains(token)) {
						toRenew.expiresAt = lock.expiresAt;
						lock = toRenew;
					}
				}
			}
			// 如果是文件夹锁，同理
			Enumeration<LockInfo> collectionLocksList = collectionLocks.elements();
			while (collectionLocksList.hasMoreElements()) {
				toRenew = collectionLocksList.nextElement();
				if (path.equals(toRenew.path)) {
					tokenList = toRenew.tokens.elements();
					while (tokenList.hasMoreElements()) {
						String token = tokenList.nextElement();
						if (ifHeader.contains(token)) {
							toRenew.expiresAt = lock.expiresAt;
							lock = toRenew;
						}
					}
				}
			}
		}
		// 最后，向客户端返回锁定信息
		XMLWriter generatedXML = new XMLWriter();
		generatedXML.writeXMLHeader();
		generatedXML.writeElement("D", DEFAULT_NAMESPACE, "prop", XMLWriter.OPENING);
		generatedXML.writeElement("D", "lockdiscovery", XMLWriter.OPENING);
		lock.toXML(generatedXML);
		generatedXML.writeElement("D", "lockdiscovery", XMLWriter.CLOSING);
		generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
		resp.setStatus(WebdavStatus.SC_OK);
		resp.setContentType("text/xml; charset=UTF-8");
		Writer writer = resp.getWriter();
		writer.write(generatedXML.toString());
		writer.close();
	}

	/**
	 * 
	 * <h2>处理unlock请求</h2>
	 * <p>
	 * 该方法用于处理unlock请求，解除指定逻辑路径上的文件或文件夹的锁定。与lock请求配合使用。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req  请求对象。
	 * @param resp 响应对象。
	 */
	protected void doUnlock(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// 已经锁定（且客户端不能提供正确锁标识）的逻辑路径不允许解锁
		if (isLocked(req)) {
			resp.setStatus(WebdavStatus.SC_LOCKED);
			return;
		}
		// 授权检查，逻辑与lock过程类似（注意：锁标识并不能作为是否执行解锁的判断条件）
		String account;
		try {
			account = getAccountFromRequestHeader(req);
		} catch (IllegalArgumentException | UnAuthorizedException e1) {
			needAuthorizationByBasic(resp);
			return;
		}
		if (ConfigureReader.instance().mustLogin() && account == null) {
			needAuthorizationByBasic(resp);
			return;
		}
		String path = getRelativePath(req);
		String AuthFolderPath = path;
		Folder parentFolder = null;
		while (parentFolder == null && !AuthFolderPath.isEmpty()) {
			AuthFolderPath = HttpPathUtil.getParentPath(AuthFolderPath);
			parentFolder = resources.getFolderByPath(AuthFolderPath);
		}
		if (parentFolder == null) {
			resp.setStatus(WebdavStatus.SC_BAD_REQUEST);
			return;
		}
		if (!ConfigureReader.instance().accessFolder(parentFolder, account) || !ConfigureReader.instance()
				.authorized(account, AccountAuth.UPLOAD_FILES, fu.getAllFoldersId(parentFolder.getFolderId()))) {
			needAuthorizationByBasic(resp);
			return;
		}
		// 从请求头中获取锁标识
		String lockTokenHeader = req.getHeader("Lock-Token");
		if (lockTokenHeader == null) {
			lockTokenHeader = "";
		}
		// 对于文件锁，将其进行文件解锁操作
		LockInfo lock = resourceLocks.get(path);
		Enumeration<String> tokenList = null;
		if (lock != null) {
			// 找到此文件锁后，移除对应的标识
			tokenList = lock.tokens.elements();
			while (tokenList.hasMoreElements()) {
				String token = tokenList.nextElement();
				if (lockTokenHeader.contains(token)) {
					lock.tokens.removeElement(token);
				}
			}
			if (lock.tokens.isEmpty()) {
				// 如果删除此标识后该锁已经没有更多标识了（共享锁可能有多个标识），则删除此锁
				resourceLocks.remove(path);
				// 无论如何，将该路径从不存在的锁定路径列表中移除
				lockNullResources.remove(path);
			}
		}
		// 对于文件夹锁，同理
		Enumeration<LockInfo> collectionLocksList = collectionLocks.elements();
		while (collectionLocksList.hasMoreElements()) {
			lock = collectionLocksList.nextElement();
			if (path.equals(lock.path)) {
				tokenList = lock.tokens.elements();
				while (tokenList.hasMoreElements()) {
					String token = tokenList.nextElement();
					if (lockTokenHeader.contains(token)) {
						lock.tokens.removeElement(token);
						break;
					}
				}
				if (lock.tokens.isEmpty()) {
					collectionLocks.removeElement(lock);
					lockNullResources.remove(path);
				}
			}
		}
		// 解锁成功
		resp.setStatus(WebdavStatus.SC_NO_CONTENT);
	}

	/**
	 * Check to see if a resource is currently write locked. The method will look at
	 * the "If" header to make sure the client has give the appropriate lock tokens.
	 *
	 * @param req Servlet request
	 * @return <code>true</code> if the resource is locked (and no appropriate lock
	 *         token has been found for at least one of the non-shared locks which
	 *         are present on the resource).
	 */
	private boolean isLocked(HttpServletRequest req) {

		String path = getRelativePath(req);

		String ifHeader = req.getHeader("If");
		if (ifHeader == null) {
			ifHeader = "";
		}

		String lockTokenHeader = req.getHeader("Lock-Token");
		if (lockTokenHeader == null) {
			lockTokenHeader = "";
		}

		return isLocked(path, ifHeader + lockTokenHeader);

	}

	/**
	 * Check to see if a resource is currently write locked.
	 *
	 * @param path     Path of the resource
	 * @param ifHeader "If" HTTP header which was included in the request
	 * @return <code>true</code> if the resource is locked (and no appropriate lock
	 *         token has been found for at least one of the non-shared locks which
	 *         are present on the resource).
	 */
	private boolean isLocked(String path, String ifHeader) {

		// Checking resource locks

		LockInfo lock = resourceLocks.get(path);
		Enumeration<String> tokenList = null;
		if ((lock != null) && (lock.hasExpired())) {
			resourceLocks.remove(path);
		} else if (lock != null) {

			// At least one of the tokens of the locks must have been given

			tokenList = lock.tokens.elements();
			boolean tokenMatch = false;
			while (tokenList.hasMoreElements()) {
				String token = tokenList.nextElement();
				if (ifHeader.contains(token)) {
					tokenMatch = true;
					break;
				}
			}
			if (!tokenMatch) {
				return true;
			}

		}

		// Checking inheritable collection locks

		Enumeration<LockInfo> collectionLocksList = collectionLocks.elements();
		while (collectionLocksList.hasMoreElements()) {
			lock = collectionLocksList.nextElement();
			if (lock.hasExpired()) {
				collectionLocks.removeElement(lock);
			} else if (path.startsWith(lock.path)) {

				tokenList = lock.tokens.elements();
				boolean tokenMatch = false;
				while (tokenList.hasMoreElements()) {
					String token = tokenList.nextElement();
					if (ifHeader.contains(token)) {
						tokenMatch = true;
						break;
					}
				}
				if (!tokenMatch) {
					return true;
				}

			}
		}

		return false;

	}

	/**
	 * Propfind helper method.
	 *
	 * @param req              The servlet request
	 * @param generatedXML     XML response to the Propfind request
	 * @param path             Path of the current resource
	 * @param type             Propfind type
	 * @param propertiesVector If the propfind type is find properties by name, then
	 *                         this Vector contains those properties
	 */
	private void parseProperties(HttpServletRequest req, XMLWriter generatedXML, String path, WebResource resource,
			int type, Vector<String> propertiesVector) {
		if (!resource.exists()) {
			// 请求不存在的文件？理论上这应该不会发生。
			return;
		}
		String href = req.getContextPath() + req.getServletPath();
		if ((href.endsWith("/")) && (path.startsWith("/"))) {
			href += path.substring(1);
		} else {
			href += path;
		}
		if (resource.isDirectory() && (!href.endsWith("/"))) {
			href += "/";
		}
		String rewrittenUrl = rewriteUrl(href);
		generatePropFindResponse(generatedXML, rewrittenUrl, path, type, propertiesVector, resource.isFile(), false,
				resource.getCreation(), resource.getLastModified(), resource.getContentLength(),
				getServletContext().getMimeType(resource.getName()), generateETag(resource));
	}

	/**
	 * Propfind helper method. Displays the properties of a lock-null resource.
	 *
	 * @param req              The servlet request
	 * @param generatedXML     XML response to the Propfind request
	 * @param path             Path of the current resource
	 * @param type             Propfind type
	 * @param propertiesVector If the propfind type is find properties by name, then
	 *                         this Vector contains those properties
	 */
	private void parseLockNullProperties(HttpServletRequest req, XMLWriter generatedXML, String path, int type,
			Vector<String> propertiesVector) {

		// Retrieving the lock associated with the lock-null resource
		LockInfo lock = resourceLocks.get(path);

		if (lock == null) {
			return;
		}

		String absoluteUri = req.getRequestURI();
		String relativePath = getRelativePath(req);
		String toAppend = path.substring(relativePath.length());
		if (!toAppend.startsWith("/")) {
			toAppend = "/" + toAppend;
		}

		String rewrittenUrl = rewriteUrl(RequestUtil.normalize(absoluteUri + toAppend));

		generatePropFindResponse(generatedXML, rewrittenUrl, path, type, propertiesVector, true, true,
				lock.creationDate.getTime(), lock.creationDate.getTime(), 0, "", "");
	}

	private void generatePropFindResponse(XMLWriter generatedXML, String rewrittenUrl, String path, int propFindType,
			Vector<String> propertiesVector, boolean isFile, boolean isLockNull, long created, long lastModified,
			long contentLength, String contentType, String eTag) {

		generatedXML.writeElement("D", "response", XMLWriter.OPENING);
		String status = "HTTP/1.1 " + WebdavStatus.SC_OK + " ";

		// Generating href element
		generatedXML.writeElement("D", "href", XMLWriter.OPENING);
		generatedXML.writeText(rewrittenUrl);
		generatedXML.writeElement("D", "href", XMLWriter.CLOSING);

		String resourceName = path;
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash != -1) {
			resourceName = resourceName.substring(lastSlash + 1);
		}

		switch (propFindType) {

		case FIND_ALL_PROP:

			generatedXML.writeElement("D", "propstat", XMLWriter.OPENING);
			generatedXML.writeElement("D", "prop", XMLWriter.OPENING);

			generatedXML.writeProperty("D", "creationdate", getISOCreationDate(created));
			generatedXML.writeElement("D", "displayname", XMLWriter.OPENING);
			generatedXML.writeData(resourceName);
			generatedXML.writeElement("D", "displayname", XMLWriter.CLOSING);
			if (isFile) {
				generatedXML.writeProperty("D", "getlastmodified", FastHttpDateFormat.formatDate(lastModified));
				generatedXML.writeProperty("D", "getcontentlength", Long.toString(contentLength));
				if (contentType != null) {
					generatedXML.writeProperty("D", "getcontenttype", contentType);
				}
				generatedXML.writeProperty("D", "getetag", eTag);
				if (isLockNull) {
					generatedXML.writeElement("D", "resourcetype", XMLWriter.OPENING);
					generatedXML.writeElement("D", "lock-null", XMLWriter.NO_CONTENT);
					generatedXML.writeElement("D", "resourcetype", XMLWriter.CLOSING);
				} else {
					generatedXML.writeElement("D", "resourcetype", XMLWriter.NO_CONTENT);
				}
			} else {
				generatedXML.writeProperty("D", "getlastmodified", FastHttpDateFormat.formatDate(lastModified));
				generatedXML.writeElement("D", "resourcetype", XMLWriter.OPENING);
				generatedXML.writeElement("D", "collection", XMLWriter.NO_CONTENT);
				generatedXML.writeElement("D", "resourcetype", XMLWriter.CLOSING);
			}

			generatedXML.writeProperty("D", "source", "");

			String supportedLocks = "<D:lockentry>" + "<D:lockscope><D:exclusive/></D:lockscope>"
					+ "<D:locktype><D:write/></D:locktype>" + "</D:lockentry>" + "<D:lockentry>"
					+ "<D:lockscope><D:shared/></D:lockscope>" + "<D:locktype><D:write/></D:locktype>"
					+ "</D:lockentry>";
			generatedXML.writeElement("D", "supportedlock", XMLWriter.OPENING);
			generatedXML.writeText(supportedLocks);
			generatedXML.writeElement("D", "supportedlock", XMLWriter.CLOSING);

			generateLockDiscovery(path, generatedXML);

			generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
			generatedXML.writeElement("D", "status", XMLWriter.OPENING);
			generatedXML.writeText(status);
			generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
			generatedXML.writeElement("D", "propstat", XMLWriter.CLOSING);

			break;

		case FIND_PROPERTY_NAMES:

			generatedXML.writeElement("D", "propstat", XMLWriter.OPENING);
			generatedXML.writeElement("D", "prop", XMLWriter.OPENING);

			generatedXML.writeElement("D", "creationdate", XMLWriter.NO_CONTENT);
			generatedXML.writeElement("D", "displayname", XMLWriter.NO_CONTENT);
			if (isFile) {
				generatedXML.writeElement("D", "getcontentlanguage", XMLWriter.NO_CONTENT);
				generatedXML.writeElement("D", "getcontentlength", XMLWriter.NO_CONTENT);
				generatedXML.writeElement("D", "getcontenttype", XMLWriter.NO_CONTENT);
				generatedXML.writeElement("D", "getetag", XMLWriter.NO_CONTENT);
				generatedXML.writeElement("D", "getlastmodified", XMLWriter.NO_CONTENT);
			}
			generatedXML.writeElement("D", "resourcetype", XMLWriter.NO_CONTENT);
			generatedXML.writeElement("D", "source", XMLWriter.NO_CONTENT);
			generatedXML.writeElement("D", "lockdiscovery", XMLWriter.NO_CONTENT);

			generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
			generatedXML.writeElement("D", "status", XMLWriter.OPENING);
			generatedXML.writeText(status);
			generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
			generatedXML.writeElement("D", "propstat", XMLWriter.CLOSING);

			break;

		case FIND_BY_PROPERTY:

			Vector<String> propertiesNotFound = new Vector<>();

			// Parse the list of properties

			generatedXML.writeElement("D", "propstat", XMLWriter.OPENING);
			generatedXML.writeElement("D", "prop", XMLWriter.OPENING);

			Enumeration<String> properties = propertiesVector.elements();

			while (properties.hasMoreElements()) {

				String property = properties.nextElement();

				if (property.equals("creationdate")) {
					generatedXML.writeProperty("D", "creationdate", getISOCreationDate(created));
				} else if (property.equals("displayname")) {
					generatedXML.writeElement("D", "displayname", XMLWriter.OPENING);
					generatedXML.writeData(resourceName);
					generatedXML.writeElement("D", "displayname", XMLWriter.CLOSING);
				} else if (property.equals("getcontentlanguage")) {
					if (isFile) {
						generatedXML.writeElement("D", "getcontentlanguage", XMLWriter.NO_CONTENT);
					} else {
						propertiesNotFound.addElement(property);
					}
				} else if (property.equals("getcontentlength")) {
					if (isFile) {
						generatedXML.writeProperty("D", "getcontentlength", Long.toString(contentLength));
					} else {
						propertiesNotFound.addElement(property);
					}
				} else if (property.equals("getcontenttype")) {
					if (isFile) {
						generatedXML.writeProperty("D", "getcontenttype", contentType);
					} else {
						propertiesNotFound.addElement(property);
					}
				} else if (property.equals("getetag")) {
					if (isFile) {
						generatedXML.writeProperty("D", "getetag", eTag);
					} else {
						propertiesNotFound.addElement(property);
					}
				} else if (property.equals("getlastmodified")) {
					if (isFile) {
						generatedXML.writeProperty("D", "getlastmodified", FastHttpDateFormat.formatDate(lastModified));
					} else {
						propertiesNotFound.addElement(property);
					}
				} else if (property.equals("resourcetype")) {
					if (isFile) {
						if (isLockNull) {
							generatedXML.writeElement("D", "resourcetype", XMLWriter.OPENING);
							generatedXML.writeElement("D", "lock-null", XMLWriter.NO_CONTENT);
							generatedXML.writeElement("D", "resourcetype", XMLWriter.CLOSING);
						} else {
							generatedXML.writeElement("D", "resourcetype", XMLWriter.NO_CONTENT);
						}
					} else {
						generatedXML.writeElement("D", "resourcetype", XMLWriter.OPENING);
						generatedXML.writeElement("D", "collection", XMLWriter.NO_CONTENT);
						generatedXML.writeElement("D", "resourcetype", XMLWriter.CLOSING);
					}
				} else if (property.equals("source")) {
					generatedXML.writeProperty("D", "source", "");
				} else if (property.equals("supportedlock")) {
					supportedLocks = "<D:lockentry>" + "<D:lockscope><D:exclusive/></D:lockscope>"
							+ "<D:locktype><D:write/></D:locktype>" + "</D:lockentry>" + "<D:lockentry>"
							+ "<D:lockscope><D:shared/></D:lockscope>" + "<D:locktype><D:write/></D:locktype>"
							+ "</D:lockentry>";
					generatedXML.writeElement("D", "supportedlock", XMLWriter.OPENING);
					generatedXML.writeText(supportedLocks);
					generatedXML.writeElement("D", "supportedlock", XMLWriter.CLOSING);
				} else if (property.equals("lockdiscovery")) {
					if (!generateLockDiscovery(path, generatedXML)) {
						propertiesNotFound.addElement(property);
					}
				} else {
					propertiesNotFound.addElement(property);
				}

			}

			generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
			generatedXML.writeElement("D", "status", XMLWriter.OPENING);
			generatedXML.writeText(status);
			generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
			generatedXML.writeElement("D", "propstat", XMLWriter.CLOSING);

			Enumeration<String> propertiesNotFoundList = propertiesNotFound.elements();

			if (propertiesNotFoundList.hasMoreElements()) {

				status = "HTTP/1.1 " + WebdavStatus.SC_NOT_FOUND + " ";

				generatedXML.writeElement("D", "propstat", XMLWriter.OPENING);
				generatedXML.writeElement("D", "prop", XMLWriter.OPENING);

				while (propertiesNotFoundList.hasMoreElements()) {
					generatedXML.writeElement("D", propertiesNotFoundList.nextElement(), XMLWriter.NO_CONTENT);
				}

				generatedXML.writeElement("D", "prop", XMLWriter.CLOSING);
				generatedXML.writeElement("D", "status", XMLWriter.OPENING);
				generatedXML.writeText(status);
				generatedXML.writeElement("D", "status", XMLWriter.CLOSING);
				generatedXML.writeElement("D", "propstat", XMLWriter.CLOSING);

			}

			break;

		}

		generatedXML.writeElement("D", "response", XMLWriter.CLOSING);
	}

	/**
	 * Print the lock discovery information associated with a path.
	 *
	 * @param path         Path
	 * @param generatedXML XML data to which the locks info will be appended
	 * @return <code>true</code> if at least one lock was displayed
	 */
	private boolean generateLockDiscovery(String path, XMLWriter generatedXML) {

		LockInfo resourceLock = resourceLocks.get(path);
		Enumeration<LockInfo> collectionLocksList = collectionLocks.elements();

		boolean wroteStart = false;

		if (resourceLock != null) {
			wroteStart = true;
			generatedXML.writeElement("D", "lockdiscovery", XMLWriter.OPENING);
			resourceLock.toXML(generatedXML);
		}

		while (collectionLocksList.hasMoreElements()) {
			LockInfo currentLock = collectionLocksList.nextElement();
			if (path.startsWith(currentLock.path)) {
				if (!wroteStart) {
					wroteStart = true;
					generatedXML.writeElement("D", "lockdiscovery", XMLWriter.OPENING);
				}
				currentLock.toXML(generatedXML);
			}
		}

		if (wroteStart) {
			generatedXML.writeElement("D", "lockdiscovery", XMLWriter.CLOSING);
		} else {
			return false;
		}

		return true;

	}

	/**
	 * Get creation date in ISO format.
	 * 
	 * @return the formatted creation date
	 */
	private String getISOCreationDate(long creationDate) {
		return creationDateFormat.format(new Date(creationDate));
	}

	/**
	 * 
	 * <h2>根据请求的逻辑路径判断允许哪些类型的请求</h2>
	 * <p>
	 * 该方法用于辅助生成Allow响应头的内容，提供指定逻辑路径下允许的请求方法名称。
	 * 注意：该方法并不会对权限或逻辑路径的正确性进行判断，仅会进行简单的可能性判断，
	 * 因此此方法提供的允许请求类型并不一定能被具体的处理逻辑所“允许”（例如客户端可能没有操作权限）。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param req 请求对象
	 * @return java.lang.String 根据请求逻辑路径生成的符合Allow响应头规范的请求类型声明字符串，例如“OPTIONS,
	 *         DELETE, LOCK, UNLOCK, PROPPATCH, COPY, MOVE, PROPFIND”。该方法不会返回null。
	 */
	protected String determineMethodsAllowed(HttpServletRequest req) {
		WebResource resource = resources.getResource(getRelativePath(req));
		// 对于任何资源都允许的方法
		StringBuilder methodsAllowed = new StringBuilder(
				"OPTIONS, DELETE, LOCK, UNLOCK, PROPPATCH, COPY, MOVE, PROPFIND");
		// 对于文件资源允许的方法
		if (!resource.isDirectory()) {
			methodsAllowed.append(", PUT, GET, POST, HEAD");
		}
		// 如果要求跟踪，额外允许TRACE方法
		if (req instanceof RequestFacade && ((RequestFacade) req).getAllowTrace()) {
			methodsAllowed.append(", TRACE");
		}
		// 如果请求资源不存在，额外允许MKCOL方法
		if (!resource.exists()) {
			methodsAllowed.append(", MKCOL");
		}
		return methodsAllowed.toString();
	}

	// -------------------------------------------------- LockInfo Inner Class

	/**
	 * Holds a lock information.
	 */
	private static class LockInfo implements Serializable {

		private static final long serialVersionUID = 1L;

		public LockInfo(int maxDepth) {
			this.maxDepth = maxDepth;
		}

		// ------------------------------------------------- Instance Variables

		private final int maxDepth;

		String path = "/";
		String type = "write";
		String scope = "exclusive";
		int depth = 0;
		String owner = "";
		Vector<String> tokens = new Vector<>();
		long expiresAt = 0;
		Date creationDate = new Date();

		// ----------------------------------------------------- Public Methods

		/**
		 * Get a String representation of this lock token.
		 */
		@Override
		public String toString() {

			StringBuilder result = new StringBuilder("Type:");
			result.append(type);
			result.append("\nScope:");
			result.append(scope);
			result.append("\nDepth:");
			result.append(depth);
			result.append("\nOwner:");
			result.append(owner);
			result.append("\nExpiration:");
			result.append(FastHttpDateFormat.formatDate(expiresAt));
			Enumeration<String> tokensList = tokens.elements();
			while (tokensList.hasMoreElements()) {
				result.append("\nToken:");
				result.append(tokensList.nextElement());
			}
			result.append("\n");
			return result.toString();
		}

		/**
		 * @return true if the lock has expired.
		 */
		public boolean hasExpired() {
			return System.currentTimeMillis() > expiresAt;
		}

		/**
		 * @return true if the lock is exclusive.
		 */
		public boolean isExclusive() {
			return scope.equals("exclusive");
		}

		/**
		 * Get an XML representation of this lock token.
		 *
		 * @param generatedXML The XML write to which the fragment will be appended
		 */
		public void toXML(XMLWriter generatedXML) {

			generatedXML.writeElement("D", "activelock", XMLWriter.OPENING);

			generatedXML.writeElement("D", "locktype", XMLWriter.OPENING);
			generatedXML.writeElement("D", type, XMLWriter.NO_CONTENT);
			generatedXML.writeElement("D", "locktype", XMLWriter.CLOSING);

			generatedXML.writeElement("D", "lockscope", XMLWriter.OPENING);
			generatedXML.writeElement("D", scope, XMLWriter.NO_CONTENT);
			generatedXML.writeElement("D", "lockscope", XMLWriter.CLOSING);

			generatedXML.writeElement("D", "depth", XMLWriter.OPENING);
			if (depth == maxDepth) {
				generatedXML.writeText("Infinity");
			} else {
				generatedXML.writeText("0");
			}
			generatedXML.writeElement("D", "depth", XMLWriter.CLOSING);

			generatedXML.writeElement("D", "owner", XMLWriter.OPENING);
			generatedXML.writeText(owner);
			generatedXML.writeElement("D", "owner", XMLWriter.CLOSING);

			generatedXML.writeElement("D", "timeout", XMLWriter.OPENING);
			long timeout = (expiresAt - System.currentTimeMillis()) / 1000;
			generatedXML.writeText("Second-" + timeout);
			generatedXML.writeElement("D", "timeout", XMLWriter.CLOSING);

			generatedXML.writeElement("D", "locktoken", XMLWriter.OPENING);
			Enumeration<String> tokensList = tokens.elements();
			while (tokensList.hasMoreElements()) {
				generatedXML.writeElement("D", "href", XMLWriter.OPENING);
				generatedXML.writeText("opaquelocktoken:" + tokensList.nextElement());
				generatedXML.writeElement("D", "href", XMLWriter.CLOSING);
			}
			generatedXML.writeElement("D", "locktoken", XMLWriter.CLOSING);

			generatedXML.writeElement("D", "activelock", XMLWriter.CLOSING);
		}
	}

	// --------------------------------------------- WebdavResolver Inner Class
	/**
	 * Work around for XML parsers that don't fully respect
	 * {@link DocumentBuilderFactory#setExpandEntityReferences(boolean)} when called
	 * with <code>false</code>. External references are filtered out for security
	 * reasons. See CVE-2007-5461.
	 */
	private static class WebdavResolver implements EntityResolver {

		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
			return new InputSource(new StringReader("Ignored external entity"));
		}
	}

	/**
	 * 
	 * <h2>设置响应对象的状态码为405并向客户端返回Allow响应头以声明允许的请求类型</h2>
	 * <p>
	 * 该方法可用于告知客户端当前的请求类型未被允许，并向其提供允许的请求类型列表。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @throws IOException 响应对象发送信息失败
	 * @param req  请求对象
	 * @param resp 响应对象
	 */
	protected void sendNotAllowed(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.addHeader("Allow", determineMethodsAllowed(req));
		resp.setStatus(WebdavStatus.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * Provides the entity tag (the ETag header) for the given resource. Intended to
	 * be over-ridden by custom MyDefaultServlet implementations that wish to use an
	 * alternative format for the entity tag.
	 *
	 * @param resource The resource for which an entity tag is required.
	 *
	 * @return The result of calling {@link WebResource#getETag()} on the given
	 *         resource
	 */
	protected String generateETag(WebResource resource) {
		return resource.getETag();
	}

	protected static class Range {

		public long start;
		public long end;
		public long length;

		/**
		 * Validate range.
		 *
		 * @return true if the range is valid, otherwise false
		 */
		public boolean validate() {
			if (end >= length) {
				end = length - 1;
			}
			return (start >= 0) && (end >= 0) && (start <= end) && (length > 0);
		}
	}
}

// --------------------------------------------------------  WebdavStatus Class

/**
 * Wraps the HttpServletResponse class to abstract the specific protocol used.
 * To support other protocols we would only need to modify this class and the
 * WebDavRetCode classes.
 *
 * @author Marc Eaddy
 * @version 1.0, 16 Nov 1997
 */
class WebdavStatus {

	// ------------------------------------------------------ HTTP Status Codes

	/**
	 * Status code (200) indicating the request succeeded normally.
	 */
	public static final int SC_OK = HttpServletResponse.SC_OK;

	/**
	 * Status code (201) indicating the request succeeded and created a new resource
	 * on the server.
	 */
	public static final int SC_CREATED = HttpServletResponse.SC_CREATED;

	/**
	 * Status code (202) indicating that a request was accepted for processing, but
	 * was not completed.
	 */
	public static final int SC_ACCEPTED = HttpServletResponse.SC_ACCEPTED;

	/**
	 * Status code (204) indicating that the request succeeded but that there was no
	 * new information to return.
	 */
	public static final int SC_NO_CONTENT = HttpServletResponse.SC_NO_CONTENT;

	/**
	 * Status code (301) indicating that the resource has permanently moved to a new
	 * location, and that future references should use a new URI with their
	 * requests.
	 */
	public static final int SC_MOVED_PERMANENTLY = HttpServletResponse.SC_MOVED_PERMANENTLY;

	/**
	 * Status code (302) indicating that the resource has temporarily moved to
	 * another location, but that future references should still use the original
	 * URI to access the resource.
	 */
	public static final int SC_MOVED_TEMPORARILY = HttpServletResponse.SC_MOVED_TEMPORARILY;

	/**
	 * Status code (304) indicating that a conditional GET operation found that the
	 * resource was available and not modified.
	 */
	public static final int SC_NOT_MODIFIED = HttpServletResponse.SC_NOT_MODIFIED;

	/**
	 * Status code (400) indicating the request sent by the client was syntactically
	 * incorrect.
	 */
	public static final int SC_BAD_REQUEST = HttpServletResponse.SC_BAD_REQUEST;

	/**
	 * Status code (401) indicating that the request requires HTTP authentication.
	 */
	public static final int SC_UNAUTHORIZED = HttpServletResponse.SC_UNAUTHORIZED;

	/**
	 * Status code (403) indicating the server understood the request but refused to
	 * fulfill it.
	 */
	public static final int SC_FORBIDDEN = HttpServletResponse.SC_FORBIDDEN;

	/**
	 * Status code (404) indicating that the requested resource is not available.
	 */
	public static final int SC_NOT_FOUND = HttpServletResponse.SC_NOT_FOUND;

	/**
	 * Status code (500) indicating an error inside the HTTP service which prevented
	 * it from fulfilling the request.
	 */
	public static final int SC_INTERNAL_SERVER_ERROR = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

	/**
	 * Status code (501) indicating the HTTP service does not support the
	 * functionality needed to fulfill the request.
	 */
	public static final int SC_NOT_IMPLEMENTED = HttpServletResponse.SC_NOT_IMPLEMENTED;

	/**
	 * Status code (502) indicating that the HTTP server received an invalid
	 * response from a server it consulted when acting as a proxy or gateway.
	 */
	public static final int SC_BAD_GATEWAY = HttpServletResponse.SC_BAD_GATEWAY;

	/**
	 * Status code (503) indicating that the HTTP service is temporarily overloaded,
	 * and unable to handle the request.
	 */
	public static final int SC_SERVICE_UNAVAILABLE = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

	/**
	 * Status code (100) indicating the client may continue with its request. This
	 * interim response is used to inform the client that the initial part of the
	 * request has been received and has not yet been rejected by the server.
	 */
	public static final int SC_CONTINUE = 100;

	/**
	 * Status code (405) indicating the method specified is not allowed for the
	 * resource.
	 */
	public static final int SC_METHOD_NOT_ALLOWED = 405;

	/**
	 * Status code (409) indicating that the request could not be completed due to a
	 * conflict with the current state of the resource.
	 */
	public static final int SC_CONFLICT = 409;

	/**
	 * Status code (412) indicating the precondition given in one or more of the
	 * request-header fields evaluated to false when it was tested on the server.
	 */
	public static final int SC_PRECONDITION_FAILED = 412;

	/**
	 * Status code (413) indicating the server is refusing to process a request
	 * because the request entity is larger than the server is willing or able to
	 * process.
	 */
	public static final int SC_REQUEST_TOO_LONG = 413;

	/**
	 * Status code (415) indicating the server is refusing to service the request
	 * because the entity of the request is in a format not supported by the
	 * requested resource for the requested method.
	 */
	public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;

	// -------------------------------------------- Extended WebDav status code

	/**
	 * Status code (207) indicating that the response requires providing status for
	 * multiple independent operations.
	 */
	public static final int SC_MULTI_STATUS = 207;
	// This one collides with HTTP 1.1
	// "207 Partial Update OK"

	/**
	 * Status code (418) indicating the entity body submitted with the PATCH method
	 * was not understood by the resource.
	 */
	public static final int SC_UNPROCESSABLE_ENTITY = 418;
	// This one collides with HTTP 1.1
	// "418 Reauthentication Required"

	/**
	 * Status code (419) indicating that the resource does not have sufficient space
	 * to record the state of the resource after the execution of this method.
	 */
	public static final int SC_INSUFFICIENT_SPACE_ON_RESOURCE = 419;
	// This one collides with HTTP 1.1
	// "419 Proxy Reauthentication Required"

	/**
	 * Status code (420) indicating the method was not executed on a particular
	 * resource within its scope because some part of the method's execution failed
	 * causing the entire method to be aborted.
	 */
	public static final int SC_METHOD_FAILURE = 420;

	/**
	 * Status code (423) indicating the destination resource of a method is locked,
	 * and either the request did not contain a valid Lock-Info header, or the
	 * Lock-Info header identifies a lock held by another principal.
	 */
	public static final int SC_LOCKED = 423;

}
