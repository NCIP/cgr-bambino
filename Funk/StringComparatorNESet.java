package Funk;

import java.util.*;

public class StringComparatorNESet implements LogicalComparator {
  private HashSet<String> unacceptable_values;

  public StringComparatorNESet (String[] values) {
    unacceptable_values = new HashSet<String>();
    for (int i=0; i < values.length; i++) {
      unacceptable_values.add(values[i]);
    }
    //    System.err.println("set is: " + acceptable_values);  // debug
  };

  public boolean compare (Object raw) {
    return !unacceptable_values.contains(raw.toString());
  };

}
