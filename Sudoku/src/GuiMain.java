import javax.swing.*;

public class GuiMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            SudokuModel model = new SudokuModel();
            model.loadPuzzle();

            SudokuView view = new SudokuView(model);
            new SudokuController(model, view);
            view.setVisible(true);
        });
    }
}
