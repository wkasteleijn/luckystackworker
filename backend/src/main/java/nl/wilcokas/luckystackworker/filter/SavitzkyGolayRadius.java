package nl.wilcokas.luckystackworker.filter;

public enum SavitzkyGolayRadius {
    OFF(0), RADIUS_25(2), RADIUS_49(3), RADIUS_81(4);

    private int halfWidth;

    private SavitzkyGolayRadius(int halfWidth) {
        this.halfWidth = halfWidth;
    }

    public int getHalfWidth() {
        return halfWidth;
    }

    public static SavitzkyGolayRadius valueOf(int halfWidth) {
        return switch (halfWidth) {
        case 2 ->  RADIUS_25;
        case 3 ->  RADIUS_49;
        case 4 ->  RADIUS_81;
        default -> OFF;
        };
    }
}
