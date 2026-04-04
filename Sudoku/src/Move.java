/**
 * Move 类就像一张小纸条，记录了玩家的一次动作。
 * 它是为了实现“撤销”功能而准备的。
 */
public class Move {
    // 属性（这些都是私有的，保护数据）
    private int row;
    private int col;
    private int oldValue;
    private int newValue;

    // 构造方法：就像是给小纸条填内容的模具
    public Move(int row, int col, int oldValue, int newValue) {
        this.row = row;
        this.col = col;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    // 获取数据的方法（Getter）
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getOldValue() { return oldValue; }
    public int getNewValue() { return newValue; }
}