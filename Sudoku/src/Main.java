import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            SudokuModel model = new SudokuModel();
            model.loadPuzzle();

            SudokuView view = new SudokuView(model);
            view.setVisible(true);
        });
    }
}
