package kohgylw.kiftd.mc;

public class MC
{
    public static void main(final String[] args) {
        if (args == null || args.length == 0) {
            UIRunner.build();
        }
        else {
            ConsoleRunner.build(args);
        }
    }
}
