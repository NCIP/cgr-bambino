package TCGA;

public abstract class BooleanComparator {
  // wrap java logical operators (for use in expression evaluation / data filtering)
  public abstract boolean compare(boolean x, boolean y);

  public static final String LABEL_AND = "and";
  public static final String LABEL_OR = "or";

  public static BooleanComparator get_comparator (String label) {
    BooleanComparator result = null;
    if (label.equals(LABEL_AND)) {
      result = new BooleanComparatorAnd();
    } else if (label.equals(LABEL_OR)) {
      result = new BooleanComparatorOr();
    } else {
      System.err.println("ERROR: can't init comparator for " + label);  // debug
    }
    return result;
  }

}
