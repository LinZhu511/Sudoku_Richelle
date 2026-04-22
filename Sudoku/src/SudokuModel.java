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
    private int[][] answer = new int[9][9];         // 开局时算出的标准答案
    private boolean[][] isFixed = new boolean[9][9]; // 哪些是题目给的，不能改
    private Stack<Move> undoStack = new Stack<>();  // 存放“小纸条”的抽屉

    // 孙子老师要求的三个布尔标志
    private boolean highlightErrors = false;
    private boolean hintsEnabled = true;
    private boolean isRandom = true;
    private final Random random = new Random();

    // 最近一次被玩家（或提示）改动的格子，用于“是谁出的错”的高亮
    private int lastEditedRow = -1;
    private int lastEditedCol = -1;

    // 构造函数：奶奶一打开游戏，它就先初始化
    public SudokuModel() {
        // 暂时先清空棋盘，后面我们会写加载文件的代码
        clearBoard();
    }

    private void clearBoard() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                board[r][c] = 0;
                answer[r][c] = 0;
                isFixed[r][c] = false;
            }
        }
        lastEditedRow = -1;
        lastEditedCol = -1;
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
            lastEditedRow = row;
            lastEditedCol = col;

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

    // ========= 下面是“真正解数独”的求解器，用来给提示提供正确答案 =========

    private boolean isValidInGrid(int[][] grid, int row, int col, int val) {
        if (val == 0) return true;

        for (int i = 0; i < 9; i++) {
            if (i != col && grid[row][i] == val) return false;
            if (i != row && grid[i][col] == val) return false;
        }

        int boxRowStart = (row / 3) * 3;
        int boxColStart = (col / 3) * 3;
        for (int r = boxRowStart; r < boxRowStart + 3; r++) {
            for (int c = boxColStart; c < boxColStart + 3; c++) {
                if ((r != row || c != col) && grid[r][c] == val) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean solveGrid(int[][] grid) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (grid[r][c] == 0) {
                    for (int val = 1; val <= 9; val++) {
                        if (isValidInGrid(grid, r, c, val)) {
                            grid[r][c] = val;
                            if (solveGrid(grid)) {
                                return true;
                            }
                            grid[r][c] = 0;
                        }
                    }
                    return false; // 这个空格无论填什么都不行，回溯
                }
            }
        }
        return true; // 没有空格了，解出来了
    }

    private int[][] copyGrid(int[][] src) {
        int[][] dst = new int[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                dst[r][c] = src[r][c];
            }
        }
        return dst;
    }

    /**
     * 从当前棋盘出发，尝试算出一个完整解；若当前盘面已经自相矛盾，则返回 null。
     */
    private int[][] solveFromCurrentBoard() {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                copy[r][c] = board[r][c];
                int v = copy[r][c];
                if (v != 0 && !isValidInGrid(copy, r, c, v)) {
                    return null; // 已经冲突，说明玩家之前的输入有问题
                }
            }
        }

        boolean ok = solveGrid(copy);
        return ok ? copy : null;
    }


    public void hint() {
        if (!hintsEnabled) return; // 如果提示开关没开，就别干活

        int[][] solution = solveFromCurrentBoard();
        if (solution == null) {
            // 当前盘面已经无解：说明有格子填错了，这时不给“瞎提示”
            System.out.println("当前局面已经无解，请先检查红色错误格子。");
            return;
        }

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0 && solution[r][c] != 0) {
                    setDigit(r, c, solution[r][c]); // 填入真正解里的数字
                    return; // 只提示一个格子
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

            lastEditedRow = lastMove.getRow();
            lastEditedCol = lastMove.getCol();
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
            lastEditedRow = -1;
            lastEditedCol = -1;

            for (int i = 0; i < 81; i++) {
                int row = i / 9;
                int col = i % 9;
                int val = Character.getNumericValue(chosen.charAt(i));

                if (val >= 1 && val <= 9) {
                    board[row][col] = val;
                    isFixed[row][col] = true;
                }
            }

            // 题目加载后先求一遍完整解，后续用于“答案驱动判题”
            int[][] solved = copyGrid(board);
            if (solveGrid(solved)) {
                answer = solved;
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

    public boolean isLastEditedCell(int row, int col) {
        return row == lastEditedRow && col == lastEditedCol;
    }

    /** 是否与开局时求出的标准答案不一致（仅玩家可编辑格参与判定） */
    public boolean isWrongByAnswer(int row, int col) {
        if (isFixed[row][col]) {
            return false;
        }
        int v = board[row][col];
        return v != 0 && answer[row][col] != 0 && v != answer[row][col];
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