public class Main {
    public static void main(String[] args) {
        if (args != null && args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
            CliMain.main(new String[0]);
            return;
        }
        GuiMain.main(new String[0]);
    }
}
