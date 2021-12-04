package leaderboards;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BoardContent {
    private final UUID boardId;
    private final List<BoardContent.BoardLine> content;

    public BoardContent clear() {
        this.content.clear();
        return this;
    }

    public BoardContent addLine(UUID player, String... columnValues) {
        this.content.add(new BoardContent.BoardLine(player, columnValues));
        return this;
    }

    public BoardContent(UUID boardId, List<BoardContent.BoardLine> content) {
        this.boardId = boardId;
        this.content = content;
    }

    public UUID getBoardId() {
        return this.boardId;
    }

    public List<BoardContent.BoardLine> getContent() {
        return this.content;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof BoardContent)) {
            return false;
        } else {
            BoardContent other;
            if (!(other = (BoardContent)o).canEqual(this)) {
                return false;
            } else {
                Object this$boardId = this.getBoardId();
                Object other$boardId = other.getBoardId();
                if (this$boardId == null) {
                    if (other$boardId != null) {
                        return false;
                    }
                } else if (!this$boardId.equals(other$boardId)) {
                    return false;
                }

                Object this$content = this.getContent();
                Object other$content = other.getContent();
                if (this$content == null) {
                    if (other$content != null) {
                        return false;
                    }
                } else if (!this$content.equals(other$content)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof BoardContent;
    }

    public int hashCode() {
        Object $boardId = this.getBoardId();
        int result = 59 + ($boardId == null ? 43 : $boardId.hashCode());
        Object $content = this.getContent();
        return result * 59 + ($content == null ? 43 : $content.hashCode());
    }

    public String toString() {
        return "BoardContent(boardId=" + this.getBoardId() + ", content=" + this.getContent() + ")";
    }

    public static class BoardLine {
        private final UUID player;
        private final String[] columns;

        public BoardLine(UUID player, String[] columns) {
            this.player = player;
            this.columns = columns;
        }

        public UUID getPlayer() {
            return this.player;
        }

        public String[] getColumns() {
            return this.columns;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof BoardContent.BoardLine)) {
                return false;
            } else {
                BoardContent.BoardLine other;
                if (!(other = (BoardContent.BoardLine)o).canEqual(this)) {
                    return false;
                } else {
                    Object this$player = this.getPlayer();
                    Object other$player = other.getPlayer();
                    if (this$player == null) {
                        if (other$player == null) {
                            return Arrays.deepEquals(this.getColumns(), other.getColumns());
                        }
                    } else if (this$player.equals(other$player)) {
                        return Arrays.deepEquals(this.getColumns(), other.getColumns());
                    }

                    return false;
                }
            }
        }

        protected boolean canEqual(Object other) {
            return other instanceof BoardContent.BoardLine;
        }

        public int hashCode() {
            Object $player = this.getPlayer();
            int result1;
            return (result1 = 59 + ($player == null ? 43 : $player.hashCode())) * 59 + Arrays.deepHashCode(this.getColumns());
        }

        public String toString() {
            return "BoardContent.BoardLine(player=" + this.getPlayer() + ", columns=" + Arrays.deepToString(this.getColumns()) + ")";
        }
    }
}
