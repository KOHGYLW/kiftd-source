package kohgylw.kiftd.printer;

import kohgylw.kiftd.ui.module.*;
import kohgylw.kiftd.server.util.*;

public class Printer
{
    public static Printer instance;
    private static boolean isUIModel;
    private static ServerUIModule sum;
    
    public static void init(final boolean isUIModel) {
        Printer.instance = new Printer();
        if (isUIModel) {
            Printer.sum = ServerUIModule.getInsatnce();
        }
        Printer.isUIModel = isUIModel;
    }
    
    public void print(final String context) {
        if (Printer.instance != null) {
            if (Printer.isUIModel) {
                Printer.sum.printMessage(context);
            }
            else {
                System.out.println("[" + new String(ServerTimeUtil.accurateToSecond().getBytes()) + "]" + new String(context.getBytes()) + "\r\n");
            }
        }
    }
}
