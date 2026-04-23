import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class SudokuView extends JFrame implements Observer {
    public interface CellInputListener {
        void onCellInput(int row, int col, String text);
    }

    private final SudokuModel model;
    private final JTextField[][] cells = new JTextField[9][9];
    private boolean syncingFromModel;
    private CellInputListener cellInputListener;
    private Runnable onNewGame;
    private Runnable onUndo;
    private Runnable onHint;
    private Runnable onReset;
    private Consumer<Boolean> onValidationToggle;
    private Consumer<Boolean> onHintToggle;
    private Consumer<Boolean> onPuzzleSelectionToggle;

    public SudokuView(SudokuModel model) {
        this.model = model;
        model.addObserver(this);

        setTitle("Sudoku");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildBoardPanel(), BorderLayout.CENTER);
        add(buildToolbar(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        refreshFromModel();
    }

    private JPanel buildBoardPanel() {
        JPanel outer = new JPanel(new GridLayout(3, 3, 3, 3));
        outer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (int br = 0; br < 3; br++) {
            for (int bc = 0; bc < 3; bc++) {
                JPanel box = new JPanel(new GridLayout(3, 3, 1, 1));
                box.setBorder(new LineBorder(Color.DARK_GRAY, 2));

                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        int row = br * 3 + r;
                        int col = bc * 3 + c;
                        JTextField tf = new JTextField(1);
                        tf.setHorizontalAlignment(JTextField.CENTER);
                        tf.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
                        tf.setPreferredSize(new Dimension(44, 44));

                        final int fr = row;
                        final int fc = col;
                        tf.addFocusListener(new FocusAdapter() {
                            @Override
                            public void focusLost(FocusEvent e) {
                                if (!syncingFromModel && cellInputListener != null) {
                                    cellInputListener.onCellInput(fr, fc, cells[fr][fc].getText());
                                }
                            }
                        });

                        cells[row][col] = tf;
                        box.add(tf);
                    }
                }
                outer.add(box);
            }
        }
        return outer;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));

        JButton loadBtn = new JButton("New Game");
        loadBtn.addActionListener(e -> {
            if (onNewGame != null) onNewGame.run();
        });

        JButton undoBtn = new JButton("Undo");
        undoBtn.addActionListener(e -> {
            if (onUndo != null) onUndo.run();
        });

        JButton hintBtn = new JButton("Hint");
        hintBtn.addActionListener(e -> {
            if (onHint != null) onHint.run();
        });

        JButton resetBtn = new JButton("Reset");
        resetBtn.addActionListener(e -> {
            if (onReset != null) onReset.run();
        });

        JCheckBox errBox = new JCheckBox("Highlight Errors");
        errBox.setSelected(model.isHighlightErrors());
        errBox.addActionListener(e -> {
            if (onValidationToggle != null) onValidationToggle.accept(errBox.isSelected());
        });

        JCheckBox hintBox = new JCheckBox("Enable Hints");
        hintBox.setSelected(model.isHintsEnabled());
        hintBox.addActionListener(e -> {
            if (onHintToggle != null) onHintToggle.accept(hintBox.isSelected());
        });

        JCheckBox randBox = new JCheckBox("Random Puzzle");
        randBox.setSelected(model.isRandomPuzzle());
        randBox.addActionListener(e -> {
            if (onPuzzleSelectionToggle != null) onPuzzleSelectionToggle.accept(randBox.isSelected());
        });

        bar.add(loadBtn);
        bar.add(undoBtn);
        bar.add(hintBtn);
        bar.add(resetBtn);
        bar.add(errBox);
        bar.add(hintBox);
        bar.add(randBox);
        return bar;
    }

    public void maybeShowWin() {
        if (model.isSolved()) {
            JOptionPane.showMessageDialog(this, "Congratulations! Sudoku completed!", "Completed", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void refreshFromModel() {
        syncingFromModel = true;
        try {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    JTextField tf = cells[r][c];
                    int v = model.getValue(r, c);
                    tf.setText(v == 0 ? "" : String.valueOf(v));

                    boolean fixed = model.isFixedCell(r, c);
                    tf.setEditable(!fixed);

                    Color bg;
                    if (fixed) {
                        bg = new Color(235, 235, 235);
                    } else if (model.isHighlightErrors()
                            && v != 0
                            && (!model.isValid(r, c, v) || model.isWrongByAnswer(r, c))
                            && model.isLastEditedCell(r, c)) {
                        // Mark both rule conflicts and mismatches against solution
                        bg = new Color(255, 200, 200);
                    } else {
                        bg = Color.WHITE;
                    }
                    tf.setBackground(bg);
                }
            }
        } finally {
            syncingFromModel = false;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(this::refreshFromModel);
    }

    public void showErrorMessage(String message) {
        if (message != null && !message.isEmpty()) {
            JOptionPane.showMessageDialog(this, message, "Notice", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void setCellInputListener(CellInputListener cellInputListener) {
        this.cellInputListener = cellInputListener;
    }

    public void setOnNewGame(Runnable onNewGame) {
        this.onNewGame = onNewGame;
    }

    public void setOnUndo(Runnable onUndo) {
        this.onUndo = onUndo;
    }

    public void setOnHint(Runnable onHint) {
        this.onHint = onHint;
    }

    public void setOnReset(Runnable onReset) {
        this.onReset = onReset;
    }

    public void setOnValidationToggle(Consumer<Boolean> onValidationToggle) {
        this.onValidationToggle = onValidationToggle;
    }

    public void setOnHintToggle(Consumer<Boolean> onHintToggle) {
        this.onHintToggle = onHintToggle;
    }

    public void setOnPuzzleSelectionToggle(Consumer<Boolean> onPuzzleSelectionToggle) {
        this.onPuzzleSelectionToggle = onPuzzleSelectionToggle;
    }
}
