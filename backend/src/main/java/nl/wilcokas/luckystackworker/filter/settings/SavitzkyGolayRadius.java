package nl.wilcokas.luckystackworker.filter.settings;

public enum SavitzkyGolayRadius {
  OFF(0),
  RADIUS_25(2),
  RADIUS_49(3),
  RADIUS_81(4),
  RADIUS_121(5),
  RADIUS_169(6);

  private int halfWidth;

  private SavitzkyGolayRadius(int halfWidth) {
    this.halfWidth = halfWidth;
  }

  public int getHalfWidth() {
    return halfWidth;
  }

  public static SavitzkyGolayRadius valueOf(int halfWidth) {
    return switch (halfWidth) {
      case 2 -> RADIUS_25;
      case 3 -> RADIUS_49;
      case 4 -> RADIUS_81;
      case 5 -> RADIUS_121;
      case 6 -> RADIUS_169;
      default -> OFF;
    };
  }
}
