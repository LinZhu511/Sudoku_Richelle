public class SudokuController {
    private final SudokuModel model;
    private final SudokuView view;

    public SudokuController(SudokuModel model, SudokuView view) {
        this.model = model;
        this.view = view;
        bindViewEvents();
    }

    private void bindViewEvents() {
        view.setCellInputListener(this::handleCellInput);
        view.setOnNewGame(model::loadPuzzle);
        view.setOnUndo(model::undo);
        view.setOnHint(this::handleHint);
        view.setOnReset(model::reset);
        view.setOnValidationToggle(model::setHighlightErrors);
        view.setOnHintToggle(model::setHintsEnabled);
        view.setOnPuzzleSelectionToggle(model::setRandomPuzzle);
    }

    private void handleCellInput(int row, int col, String rawText) {
        if (model.isFixedCell(row, col)) {
            view.refreshFromModel();
            return;
        }

        String text = rawText == null ? "" : rawText.trim();
        int value = 0;
        if (text.length() == 1) {
            char ch = text.charAt(0);
            if (ch >= '1' && ch <= '9') {
                value = ch - '0';
            }
        }

        // Non-empty invalid input is ignored and UI is reverted.
        if (value == 0 && !text.isEmpty()) {
            view.refreshFromModel();
            return;
        }

        if (value != model.getValue(row, col)) {
            model.setDigit(row, col, value);
        }
        view.maybeShowWin();
    }

    private void handleHint() {
        boolean applied = model.hint();
        if (!applied) {
            String error = model.consumeLastErrorMessage();
            if (error != null) {
                view.showErrorMessage(error);
            }
        }
        view.maybeShowWin();
    }
}
