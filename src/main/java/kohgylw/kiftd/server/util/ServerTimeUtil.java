package kohgylw.kiftd.server.util;

import java.util.*;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalQueries;

/**
 * 
 * <h2>时间相关处理工具</h2>
 * <p>
 * 该工具提供获取服务器时间等与时间相关的操作方法，全部为静态的，本身无需实例化。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
public class ServerTimeUtil {
	public static String accurateToSecond() {
		LocalDateTime ldt = LocalDateTime.now();
		DateTimeFormatter dtfDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
		return dtfDateTimeFormatter.format(ldt);
	}

	public static String accurateToMinute() {
		LocalDateTime ldt = LocalDateTime.now();
		DateTimeFormatter dtfDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
		return dtfDateTimeFormatter.format(ldt);
	}

	public static String accurateToDay() {
		LocalDateTime ldt = LocalDateTime.now();
		DateTimeFormatter dtfDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return dtfDateTimeFormatter.format(ldt);
	}

	public static String accurateToLogName() {
		LocalDateTime ldt = LocalDateTime.now();
		DateTimeFormatter dtfDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
		return dtfDateTimeFormatter.format(ldt);
	}

	public static Date getServerTime() {
		return new Date();
	}

	/**
	 * 
	 * <h2>从文件块生成“最后修改时间”标签</h2>
	 * <p>
	 * 该方法用于生成符合HTTP规范的Last-Modified格式时间截，用于判定资源的最后修改日期。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param block java.io.File 要生成的文件块对象，应该是文件，但也支持文件夹，或者null
	 * @return java.lang.String 记录最后修改日期的时间截，格式类似于“Wed, 29 Apr 2020 08:18:43
	 *         GMT”，若传入文件不存在或为null，则返回当前时间
	 */
	public static String getLastModifiedFormBlock(File block) {
		ZonedDateTime longToTime;
		if (block != null && block.exists()) {
			longToTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(block.lastModified()), ZoneId.of("GMT"));
		} else {
			longToTime = ZonedDateTime.now(ZoneId.of("GMT"));
		}
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
				.withZone(ZoneId.of("GMT"));
		return longToTime.format(dtf);
	}

	/**
	 * 
	 * <h2>将日期字符串（精确到日）转化为时间值</h2>
	 * <p>
	 * 该方法用于将本工具生成的日期字符串（精确到日）转化成以毫秒为单位的long型时间，
	 * 该时间从1970-01-01T00:00:00Z开始计数。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param date 要转化的日期字符串（精确到日），该字符串应由本类中的String accurateToDay()方法生成。
	 * @return long 时间，以毫秒为单位，从1970-01-01T00:00:00Z开始计数。如果无法转化（例如传入“--”），则返回0。
	 */
	public static long getTimeFromDateAccurateToDay(String date) {
		try {
			DateTimeFormatter dtfDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
			LocalDate ld = dtfDateTimeFormatter.parse(date, TemporalQueries.localDate());
			Instant instant = ld.atTime(LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault()).toInstant();
			return instant.toEpochMilli();
		} catch (DateTimeParseException e) {
			return 0L;
		}
	}
}
