package TCGA;

public abstract class ByteComparator {
  // wrap java operators (for use in expression evaluation / data filtering)
  protected byte comparator_value;

  public static final String LABEL_NE = "!=";
  public static final String LABEL_GT = ">";
  public static final String LABEL_GE = ">=";
  public static final String LABEL_LT = "<";
  public static final String LABEL_LE = "<=";
  public static final String LABEL_EQ = "=";

  public ByteComparator(byte value) {
    comparator_value = value;
  };

  public abstract boolean compare(byte x);

  public static ByteComparator get_comparator (String logic, byte value) {
    ByteComparator result = null;
    if (logic.equals(LABEL_NE)) {
      result = new ByteComparatorNE(value);
    } else if (logic.equals(LABEL_GT)) {
      result = new ByteComparatorGT(value);
    } else if (logic.equals(LABEL_GE)) {
      result = new ByteComparatorGE(value);
    } else if (logic.equals(LABEL_LT)) {
      result = new ByteComparatorLT(value);
    } else if (logic.equals(LABEL_LE)) {
      result = new ByteComparatorLE(value);
    } else if (logic.equals(LABEL_EQ)) {
      result = new ByteComparatorEQ(value);
    } else {
      System.err.println("ERROR: can't init for " + logic);  // debug
    }
    return result;
  }
  
}
