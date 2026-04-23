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
    private int[][] board = new int[9][9];          
    private int[][] initialBoard = new int[9][9];
    private int[][] answer = new int[9][9];         
    private boolean[][] isFixed = new boolean[9][9]; 
    private Stack<Move> undoStack = new Stack<>();  


    private boolean highlightErrors = false;
    private boolean hintsEnabled = true;
    private boolean isRandom = true;
    private final Random random = new Random();
    private String lastErrorMessage;

 
    private int lastEditedRow = -1;
    private int lastEditedCol = -1;

    // Constructor: initialize game state at startup
    public SudokuModel() {
        // Start with an empty board before loading puzzles
        clearBoard();
    }

    private void clearBoard() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                board[r][c] = 0;
                initialBoard[r][c] = 0;
                answer[r][c] = 0;
                isFixed[r][c] = false;
            }
        }
        lastEditedRow = -1;
        lastEditedCol = -1;
    }


    public void setDigit(int row, int col, int value) {
    
        assert row >= 0 && row < 9 : "Row index out of range";
        assert col >= 0 && col < 9 : "Column index out of range";
        assert value >= 0 && value <= 9 : "Value must be between 0 and 9";

        
        if (!isFixed[row][col]) {
        
            undoStack.push(new Move(row, col, board[row][col], value));

            board[row][col] = value;
            lastEditedRow = row;
            lastEditedCol = col;

            setChanged();
            notifyObservers();
        }
    }

    public boolean isValid(int row, int col, int val) {
        if (val == 0) return true; 

        for (int i = 0; i < 9; i++) {
            if (i != col && board[row][i] == val) {
                return false;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i != row && board[i][col] == val) {
                return false;
            }
        }

        int boxRowStart = (row / 3) * 3;
        int boxColStart = (col / 3) * 3;

        for (int r = boxRowStart; r < boxRowStart + 3; r++) {
            for (int c = boxColStart; c < boxColStart + 3; c++) {
                if ((r != row || c != col) && board[r][c] == val) {
                    return false;
                }
            }
        }

        return true;
    }

    // ========= Backtracking solver used by hint generation =========

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
                    return false; 
                }
            }
        }
        return true; 
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
     * Solve from the current board state. Returns null if the current
     * board is already contradictory and therefore unsolvable.
     */
    private int[][] solveFromCurrentBoard() {
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                copy[r][c] = board[r][c];
                int v = copy[r][c];
                if (v != 0 && !isValidInGrid(copy, r, c, v)) {
                    return null; // Existing conflict in current board input
                }
            }
        }

        boolean ok = solveGrid(copy);
        return ok ? copy : null;
    }


    public boolean hint() {
        if (!hintsEnabled) return false;

        int[][] solution = solveFromCurrentBoard();
        if (solution == null) {
            lastErrorMessage = "The current board is unsolvable.";
            return false;
        }

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0 && solution[r][c] != 0) {
                    setDigit(r, c, solution[r][c]); 
                    return true;
                }
            }
        }
        return false;
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Move lastMove = undoStack.pop(); 
            board[lastMove.getRow()][lastMove.getCol()] = lastMove.getOldValue(); 

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
                lastErrorMessage = "No valid puzzle found in puzzles file.";
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
                    initialBoard[row][col] = val;
                    isFixed[row][col] = true;
                }
            }

    
            int[][] solved = copyGrid(board);
            if (solveGrid(solved)) {
                answer = solved;
            }

            setChanged();
            notifyObservers();

        } catch (IOException e) {
            lastErrorMessage = "Unable to load puzzle file: " + e.getMessage();
        }
    }

    public void reset() {
        undoStack.clear();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                board[r][c] = initialBoard[r][c];
            }
        }
        lastEditedRow = -1;
        lastEditedCol = -1;
        setChanged();
        notifyObservers();
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

    /** Whether an editable cell differs from the solved answer. */
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

    public String consumeLastErrorMessage() {
        String msg = lastErrorMessage;
        lastErrorMessage = null;
        return msg;
    }

    /** True when board is full and every cell is valid (win condition). */
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