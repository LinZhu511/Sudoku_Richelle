import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("deprecation")
public class SudokuView extends JFrame implements Observer {
    private final SudokuModel model;
    private final JTextField[][] cells = new JTextField[9][9];
    private boolean syncingFromModel;

    public SudokuView(SudokuModel model) {
        this.model = model;
        model.addObserver(this);

        setTitle("数独");
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
                                if (!syncingFromModel) {
                                    commitCell(fr, fc);
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

        JButton loadBtn = new JButton("新题目");
        loadBtn.addActionListener(e -> model.loadPuzzle());

        JButton undoBtn = new JButton("撤销");
        undoBtn.addActionListener(e -> model.undo());

        JButton hintBtn = new JButton("提示");
        hintBtn.addActionListener(e -> {
            model.hint();
            maybeShowWin();
        });

        JCheckBox errBox = new JCheckBox("标出错误");
        errBox.setSelected(model.isHighlightErrors());
        errBox.addActionListener(e -> model.setHighlightErrors(errBox.isSelected()));

        JCheckBox hintBox = new JCheckBox("允许提示");
        hintBox.setSelected(model.isHintsEnabled());
        hintBox.addActionListener(e -> model.setHintsEnabled(hintBox.isSelected()));

        JCheckBox randBox = new JCheckBox("随机题目");
        randBox.setSelected(model.isRandomPuzzle());
        randBox.addActionListener(e -> model.setRandomPuzzle(randBox.isSelected()));

        bar.add(loadBtn);
        bar.add(undoBtn);
        bar.add(hintBtn);
        bar.add(errBox);
        bar.add(hintBox);
        bar.add(randBox);
        return bar;
    }

    private void commitCell(int row, int col) {
        if (model.isFixedCell(row, col)) {
            return;
        }

        String t = cells[row][col].getText().trim();
        int value = 0;
        if (t.length() == 1) {
            char ch = t.charAt(0);
            if (ch >= '1' && ch <= '9') {
                value = ch - '0';
            }
        }

        if (value == 0 && !t.isEmpty()) {
            refreshFromModel();
            return;
        }

        int current = model.getValue(row, col);
        if (value != current) {
            model.setDigit(row, col, value);
        }
        maybeShowWin();
    }

    private void maybeShowWin() {
        if (model.isSolved()) {
            JOptionPane.showMessageDialog(this, "恭喜，数独完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void refreshFromModel() {
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
                    } else if (model.isHighlightErrors() && v != 0 && !model.isValid(r, c, v)) {
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
}
