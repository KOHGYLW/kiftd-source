package kohgylw.kiftd.server.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * <h2>Kiftd特用版Properties类</h2>
 * <p>
 * 该类用于替代标准的Properties类作为kiftd各项设置参数的数据存储工具。
 * 相较于原始的Properties类，该特用版在查询性能上相近（或稍快），但在读取性能上更慢。
 * 同时能够确保在写入文件时保留原始的文本结构（包括顺序及注释内容）。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class KiftdProperties {

	private List<LineContext> contexts = new ArrayList<>();// 保存载入的整个文本信息
	private Map<String, String> properties = new HashMap<>();// 仅保存配置信息，用于提高查询效率

	// 用于存储每一行文本信息的包装类
	private class LineContext {
		private String key;// 键
		private String value;// 值
		private String text;// 文本

		private LineContext(String key, String value, String text) {
			this.key = key;
			this.value = value;
			this.text = text;
		}

		@Override
		public boolean equals(Object obj) {
			if (key == null) {
				return false;
			}
			return key.equals(obj);
		}
	}

	/**
	 * 
	 * <h2>获取参数</h2>
	 * <p>
	 * 该功能用于获取指定键对应的配置参数。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param key
	 *            java.lang.String 键名
	 * @return 查询到的值，若传入的键无对应值或键为null则返回null
	 */
	public String getProperty(String key) {
		if (key != null) {
			return properties.get(key);
		}
		return null;// 否则返回null
	}

	/**
	 * 
	 * <h2>获取参数</h2>
	 * <p>
	 * 该功能用于获取指定键对应的配置参数。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param key
	 *            java.lang.String 键名
	 * @param defaultValue
	 *            java.lang.String 若无该配置时返回的替代参数
	 * @return 查询到的值，若传入的键无对应值或键为null则返回null
	 */
	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		return value == null ? defaultValue : value;
	}

	/**
	 * 
	 * <h2>新增一个配置或修改已有的配置</h2>
	 * <p>
	 * 当传入的key已经存在时，修改该key对应的配置值，否则新增一个配置。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param key
	 *            java.lang.String 键名
	 * @param value
	 *            java.lang.String 新配置值
	 */
	public void setProperty(String key, String value) {
		if (key != null) {
			properties.put(key, value);
			for (LineContext lc : contexts) {
				if (key.equals(lc.key)) {
					lc.value = value;
					return;
				}
			}
			contexts.add(new LineContext(key, value, null));
		}
	}

	/**
	 * 
	 * <h2>从文本文件中载入配置项</h2>
	 * <p>
	 * 该功能用于清空旧的配置项并从文本流中载入新的配置项。文本中每项配置均应独占一行，
	 * 且使用“=”或“:”作为键值对的分隔符，当存在多个分隔符时，以第一个为准。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param in
	 *            java.io.InputStream 输入流，必须为文本输入流
	 */
	public void load(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "8859_1"));
		String lineStr = null;
		// 按行读取文本
		clear();
		while ((lineStr = reader.readLine()) != null && contexts.size() < Integer.MAX_VALUE) {
			if (lineStr.startsWith("#")) {
				contexts.add(new LineContext(null, null, lineStr));// 保存为注释
			} else {
				int delimit0 = lineStr.indexOf("=");
				int delimit1 = lineStr.indexOf(":");// 兼容Properties的“:”分割规则，但保存时将统一改为“=”
				int delimitIndex = -1;// 判断第一个出现的分隔符的位置
				if (delimit0 >= 0) {
					delimitIndex = delimit0;
				}
				if (delimit1 >= 0 && delimit1 < delimit0) {
					delimitIndex = delimit1;
				}
				if (delimitIndex >= 0) {
					setProperty(lineStr.substring(0, delimitIndex), lineStr.substring(delimitIndex + 1));// 保存为键值对
				} else {
					contexts.add(new LineContext(null, null, lineStr));// 保存为其他文本
				}
			}
		}
		reader.close();
	}

	/**
	 * 
	 * <h2>覆盖并保存配置</h2>
	 * <p>
	 * 将全部配置以文本流的形式写出，若写处至文件则会覆盖原有的内容。当添加标题头时，将会在文本流的开头处增加标题头文字及日期。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param out
	 *            java.io.OutputStream 输出流，能够接收文本流
	 * @param header
	 *            java.lang.String 标题头，若传入null则不添加此项
	 */
	public void store(OutputStream out, String header) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
		if (header != null) {
			writer.write("#" + header);
			writer.newLine();
			writer.write("#" + new Date().toString());
			writer.newLine();
		}
		for (LineContext line : contexts) {
			if (line.key != null) {
				writer.write(line.key + "=" + line.value);
				writer.newLine();
			} else {
				writer.write(line.text);
				writer.newLine();
			}
		}
		writer.close();
	}

	/**
	 * 
	 * <h2>获得所有配置项</h2>
	 * <p>
	 * 该方法用于获得目前存在的所有配置项，并以List的形式返回。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.util.List<java.lang.String> 所有的配置项
	 */
	public Set<String> stringPropertieNames() {
		return properties.keySet();
	}

	/**
	 * 
	 * <h2>清除某项配置</h2>
	 * <p>
	 * 根据传入的键名清除一项配置，若无该键名对应的配置或键名为null则不执行任何操作。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param key
	 *            java.lang.String 键名
	 */
	public void removeProperty(String key) {
		if (key != null) {
			properties.remove(key);
			for(Iterator<LineContext> itor=contexts.iterator();itor.hasNext();) {
				if(key.equals(itor.next().key)) {
					itor.remove();
				}
			}
		}
	}

	/**
	 * 
	 * <h2>清空所有配置项</h2>
	 * <p>
	 * 该功能用于清空所有的配置项。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void clear() {
		contexts.clear();
		properties.clear();
	}

}
