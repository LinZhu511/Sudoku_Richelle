import java.util.Scanner;

public class SudokuCLI {
    private final SudokuModel model;

    public SudokuCLI(SudokuModel model) {
        this.model = model;
    }

    public void run() {
        model.loadPuzzle();
        System.out.println("Sudoku CLI started. Type 'help' for commands.");
        printBoard();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (!handleCommand(line)) {
                    break;
                }
            }
        }
    }

    private boolean handleCommand(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "set":
                handleSet(parts);
                printBoard();
                return true;
            case "clear":
                handleClear(parts);
                printBoard();
                return true;
            case "undo":
                model.undo();
                printBoard();
                return true;
            case "hint":
                if (!model.hint()) {
                    printLastErrorOrDefault("No hint could be applied.");
                }
                printBoard();
                return true;
            case "reset":
                model.reset();
                printBoard();
                return true;
            case "new":
            case "newgame":
                model.loadPuzzle();
                printLastErrorOrDefault(null);
                printBoard();
                return true;
            case "help":
                printHelp();
                return true;
            case "quit":
            case "exit":
                System.out.println("Bye.");
                return false;
            default:
                System.out.println("Unknown command. Type 'help'.");
                return true;
        }
    }

    private void handleSet(String[] parts) {
        if (parts.length != 4) {
            System.out.println("Usage: set <row 1-9> <col 1-9> <value 1-9>");
            return;
        }
        Integer row = parseIndex(parts[1]);
        Integer col = parseIndex(parts[2]);
        Integer val = parseDigit(parts[3]);
        if (row == null || col == null || val == null) {
            return;
        }
        if (model.isFixedCell(row, col)) {
            System.out.println("That cell is fixed and cannot be changed.");
            return;
        }
        model.setDigit(row, col, val);
        if (model.isSolved()) {
            System.out.println("Congratulations, puzzle solved!");
        }
    }

    private void handleClear(String[] parts) {
        if (parts.length != 3) {
            System.out.println("Usage: clear <row 1-9> <col 1-9>");
            return;
        }
        Integer row = parseIndex(parts[1]);
        Integer col = parseIndex(parts[2]);
        if (row == null || col == null) {
            return;
        }
        if (model.isFixedCell(row, col)) {
            System.out.println("That cell is fixed and cannot be cleared.");
            return;
        }
        model.setDigit(row, col, 0);
    }

    private Integer parseIndex(String token) {
        try {
            int oneBased = Integer.parseInt(token);
            if (oneBased < 1 || oneBased > 9) {
                System.out.println("Row/col must be in 1-9.");
                return null;
            }
            return oneBased - 1;
        } catch (NumberFormatException e) {
            System.out.println("Row/col must be an integer.");
            return null;
        }
    }

    private Integer parseDigit(String token) {
        try {
            int value = Integer.parseInt(token);
            if (value < 1 || value > 9) {
                System.out.println("Value must be in 1-9.");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("Value must be an integer.");
            return null;
        }
    }

    private void printLastErrorOrDefault(String fallback) {
        String error = model.consumeLastErrorMessage();
        if (error != null && !error.isEmpty()) {
            System.out.println(error);
        } else if (fallback != null) {
            System.out.println(fallback);
        }
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  set <r> <c> <v>   Set cell (1-9,1-9) to value 1-9");
        System.out.println("  clear <r> <c>     Clear cell (set to 0)");
        System.out.println("  undo              Undo one move");
        System.out.println("  hint              Fill one hint cell");
        System.out.println("  reset             Reset current puzzle");
        System.out.println("  new|newgame       Load a new puzzle");
        System.out.println("  help              Show this help");
        System.out.println("  quit|exit         Exit CLI");
    }

    private void printBoard() {
        System.out.println();
        for (int r = 0; r < 9; r++) {
            if (r > 0 && r % 3 == 0) {
                System.out.println("------+-------+------");
            }
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < 9; c++) {
                if (c > 0 && c % 3 == 0) {
                    line.append("| ");
                }
                int v = model.getValue(r, c);
                line.append(v == 0 ? "." : v).append(' ');
            }
            System.out.println(line.toString().trim());
        }
        System.out.println();
    }
}
