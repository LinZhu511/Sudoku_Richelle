public class CliMain {
    public static void main(String[] args) {
        SudokuModel model = new SudokuModel();
        new SudokuCLI(model).run();
    }
}
