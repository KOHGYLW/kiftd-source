package kohgylw.kiftd.server.util;

import java.util.regex.*;

public class TextFormateUtil
{
    private static TextFormateUtil tfu;
    
    public static TextFormateUtil instance() {
        return TextFormateUtil.tfu;
    }
    
    public boolean matcherFolderName(final String folderName) {
        final Pattern p = Pattern.compile("^[0-9a-zA-Z_\u4e00-\u9fff]+$");
        final Matcher m = p.matcher(folderName);
        return m.matches();
    }
    
    public boolean matcherFileName(final String fileName) {
        final Pattern p = Pattern.compile("[|\\/*<> \"]+");
        final Matcher m = p.matcher(fileName);
        return !m.find();
    }
    
    static {
        TextFormateUtil.tfu = new TextFormateUtil();
    }
}
