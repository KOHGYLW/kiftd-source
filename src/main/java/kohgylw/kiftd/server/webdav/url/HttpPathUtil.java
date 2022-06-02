package kohgylw.kiftd.server.webdav.url;

/**
 * 
 * <h2>URL路径处理工具类</h2>
 * <p>
 * 该工具类用于提供各种URL路径的处理方法，所有方法皆为静态，因此无需实例化本类，直接使用静态方式调用即可。 各个方法的具体功能详见方法注释。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class HttpPathUtil {

	/**
	 * 
	 * <h2>获取URL路径中的资源名称</h2>
	 * <p>
	 * 该方法将解析出URL路径中包含的逻辑资源名称，例如“/foo”的逻辑资源名称为“foo”，“/foo/bar/”的逻辑资源名称为“bar”。以此类推。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path URL路径，该路径应为以“/”起始的字符串。
	 * @return java.lang.String 逻辑资源名称。当路径格式不正确时（例如不以“/”起始），则返回空字符串（""）
	 */
	public static String getResourceName(String path) {
		if (!isAvailablePath(path)) {
			return "";
		} else {
			if (path.equals("/")) {
				// 如果是根目录，直接返回根目录名称
				return path;
			} else {
				String currentPath = path;
				if (currentPath.endsWith("/")) {
					// 如果是文件夹（以“/”结尾），则先去掉结束的“/”
					currentPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
				}
				// 取最后一个“/”后面的名称
				int indexOfSep = currentPath.lastIndexOf('/');
				return currentPath.substring(indexOfSep + 1);
			}
		}
	}

	/**
	 * 
	 * <h2>获取URL路径中的父路径</h2>
	 * <p>
	 * 该方法将解析出URL路径中所包含的逻辑父路径，例如“/foo”的逻辑父路径为“/”，“/foo/bar/”的逻辑父路径为“/foo/”，以此类推。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path URL路径，该路径应为以“/”起始的字符串。
	 * @return java.lang.String 逻辑父路径，必定以“/”结尾。
	 *         当路径格式不正确时（例如不以“/”起始）或者路径为“/”时，则返回空字符串（""）
	 */
	public static String getParentPath(String path) {
		if (!isAvailablePath(path) || "/".equals(path)) {
			return "";
		} else {
			String currentPath = path.endsWith("/") ? path.substring(0, path.lastIndexOf('/')) : path;
			int index = currentPath.lastIndexOf('/');
			return currentPath.substring(0, index + 1);
		}
	}

	/**
	 * 
	 * <h2>判断是否为合法路径</h2>
	 * <p>
	 * 该方法用于判断一个URL路径是否符合：1，不为null；2，不为空串；3，以“/”起始。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param path URL路径
	 * @return boolean 判断结果，若满足条件，则返回true。
	 */
	private static boolean isAvailablePath(String path) {
		return (path != null && !path.isEmpty() && path.startsWith("/"));
	}

}
