// Decompiled with: FernFlower
// Class Version: 6
package leaderboards;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BoardStructure {
    private final UUID uuid;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private String name;
    private int linesDisplayed;
    private final List<BoardColumn> columns;

    public BoardStructure(UUID uuid) {
        this.columns = new ArrayList();
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public String getName() {
        return this.name;
    }

    public int getLinesDisplayed() {
        return this.linesDisplayed;
    }

    public List<BoardColumn> getColumns() {
        return this.columns;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLinesDisplayed(int linesDisplayed) {
        this.linesDisplayed = linesDisplayed;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof BoardStructure)) {
            return false;
        } else {
            BoardStructure other;
            if (!(other = (BoardStructure)o).canEqual(this)) {
                return false;
            } else if (Double.compare(this.getX(), other.getX()) != 0) {
                return false;
            } else if (Double.compare(this.getY(), other.getY()) != 0) {
                return false;
            } else if (Double.compare(this.getZ(), other.getZ()) != 0) {
                return false;
            } else if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
                return false;
            } else if (this.getLinesDisplayed() != other.getLinesDisplayed()) {
                return false;
            } else {
                Object this$uuid = this.getUuid();
                Object other$uuid = other.getUuid();
                if (this$uuid == null) {
                    if (other$uuid != null) {
                        return false;
                    }
                } else if (!this$uuid.equals(other$uuid)) {
                    return false;
                }

                Object this$name = this.getName();
                Object other$name = other.getName();
                if (this$name == null) {
                    if (other$name != null) {
                        return false;
                    }
                } else if (!this$name.equals(other$name)) {
                    return false;
                }

                Object this$columns = this.getColumns();
                Object other$columns = other.getColumns();
                if (this$columns == null) {
                    if (other$columns == null) {
                        return true;
                    }
                } else if (this$columns.equals(other$columns)) {
                    return true;
                }

                return false;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof BoardStructure;
    }

    public int hashCode() {
        long $x = Double.doubleToLongBits(this.getX());
        int result = 59 + (int)($x >>> 32 ^ $x);
        long $y = Double.doubleToLongBits(this.getY());
        result = result * 59 + (int)($y >>> 32 ^ $y);
        long $z = Double.doubleToLongBits(this.getZ());
        result = ((result * 59 + (int)($z >>> 32 ^ $z)) * 59 + Float.floatToIntBits(this.getYaw())) * 59 + this.getLinesDisplayed();
        Object $uuid = this.getUuid();
        result = result * 59 + ($uuid == null ? 43 : $uuid.hashCode());
        Object $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        Object $columns = this.getColumns();
        return result * 59 + ($columns == null ? 43 : $columns.hashCode());
    }

    public String toString() {
        return "BoardStructure(uuid=" + this.getUuid() + ", x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ() + ", yaw=" + this.getYaw() + ", name=" + this.getName() + ", linesDisplayed=" + this.getLinesDisplayed() + ", columns=" + this.getColumns() + ")";
    }
}