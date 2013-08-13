package Funk;

import java.util.*;

public class StringComparatorEQSet implements LogicalComparator {
  private HashSet<String> acceptable_values;

  public StringComparatorEQSet (String[] values) {
    acceptable_values = new HashSet<String>();
    for (int i=0; i < values.length; i++) {
      acceptable_values.add(values[i]);
    }
    //    System.err.println("set is: " + acceptable_values);  // debug
  };

  public boolean compare (Object raw) {
    return acceptable_values.contains(raw.toString());
  };

}
