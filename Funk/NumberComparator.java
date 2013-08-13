package Funk;

public abstract class NumberComparator implements LogicalComparator {
  // wrap java operators (for use in expression evaluation / data filtering)
  protected double comparator_value;

  public static final String LABEL_NE = "!=";
  public static final String LABEL_GT = ">";
  public static final String LABEL_GE = ">=";
  public static final String LABEL_LT = "<";
  public static final String LABEL_LE = "<=";
  public static final String LABEL_EQ = "=";

  public NumberComparator(Number value) {
    comparator_value = value.doubleValue();
  };

  protected double get_value (Object raw) throws NumberFormatException {
    double result = 0;
    if (raw instanceof Number) {
      result = ((Number) raw).doubleValue();
    } else if (raw instanceof String) {
      Double d = Double.parseDouble((String) raw);
      result = d.doubleValue();
    } else {
      throw new NumberFormatException();
    }
    return result;
  }

  public static NumberComparator get_comparator (String logic, Number value) {
    NumberComparator result = null;
    if (logic.equals(LABEL_NE)) {
      result = new NumberComparatorNE(value);
    } else if (logic.equals(LABEL_GT)) {
      result = new NumberComparatorGT(value);
    } else if (logic.equals(LABEL_GE)) {
      result = new NumberComparatorGE(value);
    } else if (logic.equals(LABEL_LT)) {
      result = new NumberComparatorLT(value);
    } else if (logic.equals(LABEL_LE)) {
      result = new NumberComparatorLE(value);
    } else if (logic.equals(LABEL_EQ)) {
      result = new NumberComparatorEQ(value);
    } else {
      System.err.println("ERROR: can't init for " + logic);  // debug
    }
    return result;
  }


  public static void main (String[] argv) {
    NumberComparator nc;

    String s_0 = "0";
    String s_1 = "1";
    String s_2 = "2";
    
    Integer i_0 = Integer.valueOf(0);
    Integer i_1 = Integer.valueOf(1);
    Integer i_2 = Integer.valueOf(2);

    nc = NumberComparator.get_comparator(">", 1);
    System.err.println(nc + " > 1");  // debug
    System.err.println("  " + nc.compare(i_0));  // debug
    System.err.println("  " + nc.compare(i_1));  // debug
    System.err.println("  " + nc.compare(i_2));  // debug
    System.err.println("");  // debug

    System.err.println("< 1");  // debug
    nc = NumberComparator.get_comparator("<", 1);
    System.err.println(nc + " " + nc.compare("0"));  // debug
    System.err.println(nc + " " + nc.compare("1"));  // debug
    System.err.println(nc + " " + nc.compare("2"));  // debug
    System.err.println("");  // debug

    System.err.println("= 1");  // debug
    nc = NumberComparator.get_comparator("=", 1);
    System.err.println(nc + " " + nc.compare("0"));  // debug
    System.err.println(nc + " " + nc.compare("1"));  // debug
    System.err.println(nc + " " + nc.compare("2"));  // debug
    System.err.println("");  // debug

    System.err.println(">= 1");  // debug
    nc = NumberComparator.get_comparator(">=", 1);
    System.err.println(nc + " " + nc.compare("0"));  // debug
    System.err.println(nc + " " + nc.compare("1"));  // debug
    System.err.println(nc + " " + nc.compare("2"));  // debug
    System.err.println("");  // debug

    System.err.println("<= 1");  // debug
    nc = NumberComparator.get_comparator("<=", 1);
    System.err.println(nc + " " + nc.compare("0"));  // debug
    System.err.println(nc + " " + nc.compare("1"));  // debug
    System.err.println(nc + " " + nc.compare("2"));  // debug
    System.err.println("");  // debug

    System.err.println("!= 1");  // debug
    nc = NumberComparator.get_comparator("!=", 1);
    System.err.println(nc + " " + nc.compare("0"));  // debug
    System.err.println(nc + " " + nc.compare("1"));  // debug
    System.err.println(nc + " " + nc.compare("2"));  // debug
    System.err.println("");  // debug


  }
  
}
