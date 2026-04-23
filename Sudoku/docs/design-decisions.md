# Sudoku Design Notes

## Key Non-Functional Design Decisions

1. The project provides two separate entry points:
   - `GuiMain.main()` for the Swing application.
   - `CliMain.main()` for the command-line application.
2. The GUI follows MVC:
   - `SudokuModel` stores game state and rules.
   - `SudokuView` renders UI and exposes interaction callbacks.
   - `SudokuController` translates user actions into Model operations.
3. The CLI reuses `SudokuModel` directly and does not define dedicated View/Controller classes.
4. The Model centralizes Sudoku rule enforcement, undo stack handling, hint solving, reset, and puzzle loading.
5. Unit tests focus on `SudokuModel` behavior, including both valid and invalid scenarios.

## Notes on Invariants and Robustness

- Board values are constrained to `0..9`.
- Fixed cells are not editable through `setDigit`.
- Invalid text input in GUI/CLI is handled gracefully without process failure.
- Assertions in `setDigit` check coordinate and value ranges for defensive validation.
