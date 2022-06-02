package kohgylw.kiftd.server.webdav.pojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.cert.Certificate;
import java.util.jar.Manifest;

import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;

import kohgylw.kiftd.server.model.Folder;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.util.ServerTimeUtil;
import kohgylw.kiftd.server.webdav.date.FastHttpDateFormat;
import kohgylw.kiftd.server.webdav.url.HttpPathUtil;

/**
 * 
 * <h2>封装WebDAV请求的文件或文件夹的资源类</h2>
 * <p>
 * 该类用于将kiftd中的文件或文件夹封装为一个抽象的“资源”以便进行统一处理，避免文件和文件夹区分处理带来的代码混乱。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class KiftdWebDAVResource implements WebResource {

	private String path;// 该资源的路径
	private boolean isExists;// 该资源是否存在？
	private boolean isDirectory;// 该资源是一个文件夹？
	private long creation;// 创建日期
	private String name;// 资源名称
	private String mimeType;// Mime Type
	private volatile String weakETag;// 存储ETag声明

	private Node node;// 如果资源是个文件，那么该属性对应文件节点对象
	private File fileBlock;// 如果资源是个文件，那么该属性对应文件块对象
	private Folder folder;// 如果资源是个文件夹，那么该属性对应文件夹对象

	/**
	 * 
	 * <h2>构造一个文件夹资源</h2>
	 * <p>
	 * 该方法将构造一个文件夹对应的资源。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path   资源路径，必须输入完整的路径名称，以“/”开头，例如“/foo”或“/foo/bar”
	 * @param folder 文件夹对象，如果文件夹不存在则可传入null
	 */
	public KiftdWebDAVResource(String path, Folder folder) {
		this.path = path;
		this.folder = folder;
		if (this.path != null && this.folder != null) {
			if (!path.endsWith("/")) {
				// 规范化路径名，文件夹路径统一以“/”结尾以符合URL标准
				this.path = path + "/";
			}
			isExists = true;
			isDirectory = true;
			name = folder.getFolderName();
			creation = ServerTimeUtil.getTimeFromDateAccurateToDay(this.folder.getFolderCreationDate());
		} else {
			this.name = HttpPathUtil.getResourceName(this.path);
		}
	}

	/**
	 * 
	 * <h2>构造一个文件资源</h2>
	 * <p>
	 * 该方法将构造一个文件对应的资源。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path  资源路径，必须输入完整的路径名称，以“/”开头，并以“/”结尾。例如“/foo/”
	 * @param node  文件节点对象，如果文件节点不存在则可传入null
	 * @param block 文件块对象，如果文件节点不存在则也应传入null（但不会作为文件节点存在与否的判断条件）
	 */
	public KiftdWebDAVResource(String path, Node node, File block) {
		this.path = path;
		this.node = node;
		this.fileBlock = block;
		if (this.path != null && this.node != null && this.fileBlock != null) {
			isExists = true;
			name = this.node.getFileName();
			try {
				BasicFileAttributes attrs = Files.readAttributes(fileBlock.toPath(), BasicFileAttributes.class);
				creation = attrs.creationTime().toMillis();
			} catch (IOException e) {
			}
		} else {
			this.name = HttpPathUtil.getResourceName(this.path);
		}
	}

	@Override
	public long getLastModified() {
		return creation;
	}

	@Override
	public String getLastModifiedHttp() {
		return FastHttpDateFormat.formatDate(getLastModified());
	}

	@Override
	public boolean exists() {
		return isExists;
	}

	@Override
	public boolean isVirtual() {
		return false;
	}

	@Override
	public boolean isDirectory() {
		return isDirectory;
	}

	@Override
	public boolean isFile() {
		return isExists && !isDirectory;
	}

	@Override
	public boolean delete() {
		if (isExists) {
			if (isDirectory) {
				return true;
			}
			return fileBlock.delete();
		} else {
			return false;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public long getContentLength() {
		if (isExists) {
			if (isDirectory) {
				return -1;
			}
			return fileBlock.length();
		} else {
			return 0;
		}
	}

	@Override
	public String getCanonicalPath() {
		return path;
	}

	@Override
	public boolean canRead() {
		if (isExists) {
			if (isDirectory) {
				return true;
			}
			return fileBlock.canRead();
		} else {
			return false;
		}
	}

	@Override
	public String getWebappPath() {
		return path;
	}

	@Override
	public String getETag() {
		// 使用懒加载模式，如果第一次调用时该值尚未初始化，则临时初始化其值
		if (weakETag == null) {
			synchronized (this) {
				if (weakETag == null) {
					long contentLength = getContentLength();
					long lastModified = getLastModified();
					if ((contentLength >= 0) || (lastModified >= 0)) {
						weakETag = "W/\"" + contentLength + "-" + lastModified + "\"";
					}
				}
			}
		}
		// 已经初始化过了，则直接返回
		return weakETag;
	}

	@Override
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	@Override
	public String getMimeType() {
		return mimeType;
	}

	@Override
	public InputStream getInputStream() {
		if (isDirectory) {
			// 文件夹没有输入流
			return null;
		}
		try {
			return new FileInputStream(fileBlock);
		} catch (IOException | NullPointerException e) {
			// 文件不存在
			return null;
		}
	}

	@Override
	public byte[] getContent() {
		long len = getContentLength();
		if (len > Integer.MAX_VALUE) {
			// 文件内容过大
			throw new ArrayIndexOutOfBoundsException();
		}
		if (isDirectory || len < 0) {
			// 如果是文件夹或不存在的文件，则无内容返回
			return null;
		}
		int size = (int) len;
		byte[] result = new byte[size];
		int pos = 0;
		try (InputStream is = new FileInputStream(fileBlock)) {
			while (pos < size) {
				int n = is.read(result, pos, size - pos);
				if (n < 0) {
					break;
				}
				pos += n;
			}
		} catch (IOException | NullPointerException e) {
			return null;
		}
		return result;
	}

	@Override
	public long getCreation() {
		return creation;
	}

	@Override
	public URL getURL() {
		if (isExists) {
			try {
				return new URI("file:" + path).toURL();
			} catch (MalformedURLException | URISyntaxException e) {
				// 路径格式有错时，也返回null
			}
		}
		return null;
	}

	@Override
	public URL getCodeBase() {
		// 不存在解析代码问题，因此全部视为普通文件
		return getURL();
	}

	@Override
	public WebResourceRoot getWebResourceRoot() {
		// 由于使用的是虚拟文件系统，因此不基于Servlet容器进行文件操作
		return null;
	}

	@Override
	public Certificate[] getCertificates() {
		// 不提供签名验证服务
		return null;
	}

	@Override
	public Manifest getManifest() {
		// 不提供资源关联服务
		return null;
	}

	/**
	 * 
	 * <h2>获取该资源对应的文件节点</h2>
	 * <p>
	 * 当该资源对应的是一个文件时，返回文件节点对象。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return kohgylw.kiftd.server.model.Node 该资源对应的文件节点对象，
	 *         当该资源不存在或者该资源对应的是一个文件夹时，返回null
	 */
	public Node getNode() {
		return node;
	}

	/**
	 * 
	 * <h2>获取该资源对应的文件夹</h2>
	 * <p>
	 * 当该资源对应的是一个文件夹时，返回文件夹对象。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return kohgylw.kiftd.server.model.Folder 该资源对应的文件夹对象，
	 *         当该资源不存在或者该资源对应的是一个文件时，返回null
	 */
	public Folder getFolder() {
		return folder;
	}
}
