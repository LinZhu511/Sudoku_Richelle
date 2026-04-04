import java.util.Observable;
import java.util.Stack;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;

public class SudokuModel extends Observable {
    private int[][] board = new int[9][9];          // 当前棋盘
    private boolean[][] isFixed = new boolean[9][9]; // 哪些是题目给的，不能改
    private Stack<Move> undoStack = new Stack<>();  // 存放“小纸条”的抽屉

    // 孙子老师要求的三个布尔标志
    private boolean highlightErrors = false;
    private boolean hintsEnabled = true;
    private boolean isRandom = true;
    private final Random random = new Random();

    // 构造函数：奶奶一打开游戏，它就先初始化
    public SudokuModel() {
        // 暂时先清空棋盘，后面我们会写加载文件的代码
        clearBoard();
    }

    private void clearBoard() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                board[r][c] = 0;
                isFixed[r][c] = false;
            }
        }
    }


    public void setDigit(int row, int col, int value) {
        // 孙子老师要看的 Assert：确保坐标在 0-8 之间，数字在 0-9 之间
        assert row >= 0 && row < 9 : "行坐标越界啦";
        assert col >= 0 && col < 9 : "列坐标越界啦";
        assert value >= 0 && value <= 9 : "数字必须是0-9";

        // 只有不是原始题目的格子，才允许修改
        if (!isFixed[row][col]) {
            // 在改数字之前，先写张“小纸条”存进抽屉，方便以后撤销
            undoStack.push(new Move(row, col, board[row][col], value));

            board[row][col] = value;

            // 关键：告诉所有 View（GUI 和 CLI），奶奶改数字了，快刷新！
            setChanged();
            notifyObservers();
        }
    }



    public boolean isValid(int row, int col, int val) {
        if (val == 0) return true; // 0 代表清空格子，总是合法的

        // 1. 检查行：数数这一行里有没有重复的数字
        for (int i = 0; i < 9; i++) {
            if (i != col && board[row][i] == val) {
                return false; // 哎呀，行里重复了！
            }
        }

        // 2. 检查列：数数这一列里有没有重复的数字
        for (int i = 0; i < 9; i++) {
            if (i != row && board[i][col] == val) {
                return false; // 哎呀，列里重复了！
            }
        }

        // 3. 检查 3x3 小方格：这块稍微有点绕，我们要先找到小方格的“左上角”
        int boxRowStart = (row / 3) * 3;
        int boxColStart = (col / 3) * 3;

        for (int r = boxRowStart; r < boxRowStart + 3; r++) {
            for (int c = boxColStart; c < boxColStart + 3; c++) {
                if ((r != row || c != col) && board[r][c] == val) {
                    return false; // 哎呀，小方格里重复了！
                }
            }
        }

        return true; // 三关都过了，它是合法的！
    }


    public void hint() {
        if (!hintsEnabled) return; // 如果提示开关没开，就别干活

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) { // 找一个还没填数字的空格
                    for (int val = 1; val <= 9; val++) {
                        if (isValid(r, c, val)) { // 试出一个合法的数字
                            setDigit(r, c, val); // 帮奶奶填上
                            return; // 填一个就够了，收工！
                        }
                    }
                }
            }
        }
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Move lastMove = undoStack.pop(); // 取出最后一张纸条
            board[lastMove.getRow()][lastMove.getCol()] = lastMove.getOldValue(); // 改回旧值

            setChanged();
            notifyObservers();
        }
    }

    public void loadPuzzle() {
        File file = new File("res/puzzles.txt");

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            List<String> lines = new ArrayList<>();
            String raw;
            while ((raw = br.readLine()) != null) {
                String line = raw.trim();
                if (line.length() >= 81) {
                    lines.add(line.substring(0, 81));
                }
            }

            if (lines.isEmpty()) {
                return;
            }

            String chosen = isRandom ? lines.get(random.nextInt(lines.size())) : lines.get(0);

            clearBoard();
            undoStack.clear();

            for (int i = 0; i < 81; i++) {
                int row = i / 9;
                int col = i % 9;
                int val = Character.getNumericValue(chosen.charAt(i));

                if (val >= 1 && val <= 9) {
                    board[row][col] = val;
                    isFixed[row][col] = true;
                }
            }

            setChanged();
            notifyObservers();

        } catch (IOException e) {
            System.out.println("找不到谜题文件： " + e.getMessage());
        }
    }

    public int getValue(int row, int col) {
        return board[row][col];
    }

    public boolean isFixedCell(int row, int col) {
        return isFixed[row][col];
    }

    public boolean isHighlightErrors() {
        return highlightErrors;
    }

    public void setHighlightErrors(boolean highlightErrors) {
        this.highlightErrors = highlightErrors;
        setChanged();
        notifyObservers();
    }

    public boolean isHintsEnabled() {
        return hintsEnabled;
    }

    public void setHintsEnabled(boolean hintsEnabled) {
        this.hintsEnabled = hintsEnabled;
    }

    public boolean isRandomPuzzle() {
        return isRandom;
    }

    public void setRandomPuzzle(boolean randomPuzzle) {
        this.isRandom = randomPuzzle;
    }

    /** 棋盘是否已全部填满且每格合法（用于“胜利”判断） */
    public boolean isSolved() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int v = board[r][c];
                if (v == 0 || !isValid(r, c, v)) {
                    return false;
                }
            }
        }
        return true;
    }
}