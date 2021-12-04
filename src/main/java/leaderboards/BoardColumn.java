// Decompiled with: FernFlower
// Class Version: 6
package leaderboards;

public class BoardColumn {
    private final String header;
    private final double width;

    public BoardColumn(String header, double width) {
        this.header = header;
        this.width = width;
    }

    public String getHeader() {
        return this.header;
    }

    public double getWidth() {
        return this.width;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof BoardColumn)) {
            return false;
        } else {
            BoardColumn other;
            if (!(other = (BoardColumn)o).canEqual(this)) {
                return false;
            } else if (Double.compare(this.getWidth(), other.getWidth()) != 0) {
                return false;
            } else {
                Object this$header = this.getHeader();
                Object other$header = other.getHeader();
                if (this$header == null) {
                    if (other$header != null) {
                        return false;
                    }
                } else if (!this$header.equals(other$header)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof BoardColumn;
    }

    public int hashCode() {
        long $width = Double.doubleToLongBits(this.getWidth());
        int result = 59 + (int)($width >>> 32 ^ $width);
        Object $header = this.getHeader();
        return result * 59 + ($header == null ? 43 : $header.hashCode());
    }

    public String toString() {
        return "BoardColumn(header=" + this.getHeader() + ", width=" + this.getWidth() + ")";
    }
}