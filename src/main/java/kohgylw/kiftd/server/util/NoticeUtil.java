package kohgylw.kiftd.server.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.annotation.Resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

import kohgylw.kiftd.printer.Printer;

/**
 * 
 * <h2>简述</h2>
 * <p>
 * 详细功能
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class NoticeUtil {

	private String md5;// 公告信息的md5值，如未生成则返回null
	private MutableDataHolder options;// markdown解析器的参数设置
	/**
	 * 程序主目录中的公告文件名称
	 */
	public static final String NOTICE_FILE_NAME = "notice.md";

	public static final String NOTICE_OUTPUT_NAME = "notice.html";

	@Resource
	private LogUtil lu;
	@Resource
	private TxtCharsetGetter tcg;

	public NoticeUtil() {
		options = new MutableDataSet();
		options.setFrom(ParserEmulationProfile.MARKDOWN);
	}

	/**
	 * 
	 * <h2>载入公告文件</h2>
	 * <p>
	 * 该方法将寻找并尝试载入位于主目录内的“notice.md”文件，如果该文件存在，
	 * 则会将该文件以markdown格式转化为HTML文档并存入临时目录中，并命名为“notice.html”，
	 * 同时计算原文件的md5值以便通过该对象的md5字段获取。如已生成旧的“notice.html”文件， 则执行该方法将会覆盖它。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 */
	public void loadNotice() {
		File noticeMD = new File(ConfigureReader.instance().getPath(), NOTICE_FILE_NAME);
		File noticeHTML = new File(ConfigureReader.instance().getTemporaryfilePath(), NOTICE_OUTPUT_NAME);// 转化后的输出位置
		if (noticeMD.isFile() && noticeMD.canRead()) {
			Printer.instance.print("正在载入公告信息...");
			try {
				// 先判断公告信息文件的编码格式
				String inputFileEncode = tcg.getTxtCharset(new FileInputStream(noticeMD));
				// 将其转化为HTML格式并保存
				Parser parser = Parser.builder(options).build();
				HtmlRenderer renderer = HtmlRenderer.builder(options).build();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(new FileInputStream(noticeMD), inputFileEncode));
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(noticeHTML), "UTF-8"));
				String line = null;
				while ((line = reader.readLine()) != null) {
					String html = renderer.render(parser.parse(line));
					writer.write(html);
					writer.newLine();
				}
				reader.close();
				writer.flush();
				writer.close();
				// 计算md5并保存
				md5 = DigestUtils.md5Hex(new FileInputStream(noticeMD));
				Printer.instance.print("公告信息载入完成。");
				return;
			} catch (Exception e) {
				Printer.instance.print("错误：公告文件载入失败，服务器将无法为用户显示公告内容。");
			}
		}
		md5 = null;
	}

	/**
	 * 
	 * <h2>获取公告信息的md5标识</h2>
	 * <p>
	 * 该方法将返回公告的md5标识，从而帮助前端判定是否需要显示公告。如果没有公告，那么该方法会返回null。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @return java.lang.String md5字符串或null。
	 */
	public String getMd5() {
		return md5;
	}
}
