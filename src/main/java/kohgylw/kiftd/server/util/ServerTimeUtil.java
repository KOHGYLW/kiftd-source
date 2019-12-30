package kohgylw.kiftd.server.util;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
}
