package kohgylw.kiftd.server.util;

import java.util.*;
import java.text.*;

public class ServerTimeUtil
{
    public static String accurateToSecond() {
        final Date d = new Date();
        final DateFormat df = new SimpleDateFormat("YYYY\u5e74MM\u6708dd\u65e5 HH:mm:ss");
        return df.format(d);
    }
    
    public static String accurateToMinute() {
        final Date d = new Date();
        final DateFormat df = new SimpleDateFormat("YYYY\u5e74MM\u6708dd\u65e5 HH:mm");
        return df.format(d);
    }
    
    public static String accurateToDay() {
        final Date d = new Date();
        final DateFormat df = new SimpleDateFormat("YYYY\u5e74MM\u6708dd\u65e5");
        return df.format(d);
    }
    
    public static String accurateToLogName() {
        final Date d = new Date();
        final DateFormat df = new SimpleDateFormat("YYYY_MM_dd");
        return df.format(d);
    }
    
    public static Date getServerTime() {
        return new Date();
    }
}
