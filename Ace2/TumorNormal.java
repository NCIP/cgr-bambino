package Ace2;

public enum TumorNormal {
  TUMOR, NORMAL, UNKNOWN;

  public static final byte TUMOR_BYTE = 'T';
  public static final byte NORMAL_BYTE = 'N';

  public boolean isValid() {
    return this.equals(TUMOR) || this.equals(NORMAL);
  }

  public static TumorNormal valueOfString (String s) {
    TumorNormal result = null;
    if (s.equalsIgnoreCase("N")) {
      result = NORMAL;
    } else if (s.equalsIgnoreCase("T")) {
      result = TUMOR;
    }
    return result;
  }
}