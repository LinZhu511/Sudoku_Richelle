import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class SudokuModelTest {
    private SudokuModel model;

    @BeforeEach
    void setUp() {
        model = new SudokuModel();
    }

    /**
     * Scenario: the same number appears twice in a row.
     * Purpose: verify row/column/sub-grid conflict validation in Model.
     */
    @Test
    void shouldRejectInvalidMoveWhenRowHasDuplicate() throws Exception {
        int[][] board = new int[9][9];
        board[0][0] = 5;
        board[0][1] = 5; // duplicate in row
        setPrivateField("board", board);

        assertFalse(model.isValid(0, 1, 5), "Duplicate value in the same row must be invalid.");
    }

    /**
     * Scenario: user edits a non-fixed cell and then undoes the move.
     * Purpose: verify stack-based undo correctly restores previous value.
     */
    @Test
    void shouldRestorePreviousValueAfterUndo() {
        model.setDigit(0, 0, 7);
        assertEquals(7, model.getValue(0, 0), "Value should be updated before undo.");

        model.undo();
        assertEquals(0, model.getValue(0, 0), "Undo should restore the old value.");
    }

    /**
     * Scenario: user changes board state, then resets.
     * Purpose: verify reset restores the initial puzzle state reproducibly.
     */
    @Test
    void shouldResetBoardToInitialPuzzleState() throws Exception {
        int[][] initial = new int[9][9];
        initial[0][0] = 8;
        initial[1][1] = 3;

        int[][] board = deepCopy(initial);
        boolean[][] fixed = new boolean[9][9];
        fixed[0][0] = true;
        fixed[1][1] = true;

        setPrivateField("initialBoard", deepCopy(initial));
        setPrivateField("board", board);
        setPrivateField("isFixed", fixed);

        model.setDigit(0, 1, 6);
        assertEquals(6, model.getValue(0, 1), "Editable cell should change before reset.");

        model.reset();
        assertEquals(8, model.getValue(0, 0), "Fixed initial value must remain after reset.");
        assertEquals(3, model.getValue(1, 1), "Fixed initial value must remain after reset.");
        assertEquals(0, model.getValue(0, 1), "User-entered value must be cleared by reset.");
    }

    /**
     * Scenario: puzzle is almost solved with one empty cell.
     * Purpose: verify hint uses solver/backtracking to fill a correct value.
     */
    @Test
    void shouldFillOneCellWhenHintIsCalled() throws Exception {
        int[][] solved = {
                {5, 3, 4, 6, 7, 8, 9, 1, 2},
                {6, 7, 2, 1, 9, 5, 3, 4, 8},
                {1, 9, 8, 3, 4, 2, 5, 6, 7},
                {8, 5, 9, 7, 6, 1, 4, 2, 3},
                {4, 2, 6, 8, 5, 3, 7, 9, 1},
                {7, 1, 3, 9, 2, 4, 8, 5, 6},
                {9, 6, 1, 5, 3, 7, 2, 8, 4},
                {2, 8, 7, 4, 1, 9, 6, 3, 5},
                {3, 4, 5, 2, 8, 6, 1, 7, 9}
        };

        int[][] board = deepCopy(solved);
        board[0][0] = 0; // one missing cell
        boolean[][] fixed = new boolean[9][9];
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                fixed[r][c] = !(r == 0 && c == 0);
            }
        }

        setPrivateField("board", board);
        setPrivateField("answer", deepCopy(solved));
        setPrivateField("isFixed", fixed);

        boolean applied = model.hint();
        assertTrue(applied, "Hint should be applied on a solvable board with empty cells.");
        assertEquals(5, model.getValue(0, 0), "Hint should fill the missing cell with correct value.");
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = SudokuModel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(model, value);
    }

    private int[][] deepCopy(int[][] src) {
        int[][] copy = new int[src.length][src[0].length];
        for (int r = 0; r < src.length; r++) {
            System.arraycopy(src[r], 0, copy[r], 0, src[r].length);
        }
        return copy;
    }
}
