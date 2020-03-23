package kohgylw.kiftd.server.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsPSMDetector;
import org.springframework.stereotype.Component;

/**
 * 
 * <h2>文本编码集判断器</h2>
 * <p>
 * 该工具类用于判断一个文本输入流最可能是哪种编码格式，详见getTxtCharset方法。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class TxtCharsetGetter {

	/**
	 * 
	 * <h2>获取文本输入流的编码集</h2>
	 * <p>
	 * 该方法用于获取一个文本输入流最可能的编码集，并将其名称返回。注意：由于该方法执行完毕后会关闭输入流，因此请传入一个新的输入流用于操作。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param in
	 *            java.io.InputStream 包含判断文本的输入流
	 * @return java.lang.String 可能性最高的编码集名称
	 */
	public String getTxtCharset(InputStream in) throws Exception {
		int lang = nsPSMDetector.CHINESE;
		nsDetector det = new nsDetector(lang);
		CharsetDetectionObserverImpl cdoi = new CharsetDetectionObserverImpl();
		det.Init(cdoi);
		BufferedInputStream imp = new BufferedInputStream(in);
		byte[] buf = new byte[1024];
		int len;
		boolean isAscii = true;
		while ((len = imp.read(buf, 0, buf.length)) != -1) {
			if (isAscii) {
				isAscii = det.isAscii(buf, len);
			}
			if (!isAscii) {
				if (det.DoIt(buf, len, false)) {
					break;
				}
			}
		}
		imp.close();
		in.close();
		det.DataEnd();
		if (isAscii) {
			return "ASCII";
		} else if (cdoi.getCharset() != null) {
			return cdoi.getCharset();
		} else {
			String[] prob = det.getProbableCharsets();
			if (prob != null && prob.length > 0) {
				return prob[0];
			}
			return "GBK";
		}
	}

	/**
	 * 
	 * <h2>获取文本输入流的编码集</h2>
	 * <p>
	 * 该方法用于获取一个字符串byte数组最可能的编码集，并将其名称返回。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param buf
	 *            待检验byte数组
	 * @param offset
	 *            检验位起始位置
	 * @param length
	 *            检验位长度
	 * @return java.lang.String 可能性最高的编码集名称
	 */
	public String getTxtCharset(byte[] buf, int offset, int length) throws Exception {
		int lang = nsPSMDetector.CHINESE;
		nsDetector det = new nsDetector(lang);
		CharsetDetectionObserverImpl cdoi = new CharsetDetectionObserverImpl();
		det.Init(cdoi);
		boolean isAscii = true;
		byte[] array = Arrays.copyOfRange(buf, offset, (offset + length));
		if (isAscii) {
			isAscii = det.isAscii(array, length);
		}
		if (!isAscii) {
			det.DoIt(array, length, false);
		}
		det.DataEnd();
		if (isAscii) {
			return "ASCII";
		} else if (cdoi.getCharset() != null) {
			return cdoi.getCharset();
		} else {
			String[] prob = det.getProbableCharsets();
			if (prob != null && prob.length > 0) {
				return prob[0];
			}
			return "GBK";
		}
	}

}
