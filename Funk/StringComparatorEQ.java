package Funk;

import java.util.*;

public class StringComparatorEQ implements LogicalComparator {
  private String comparator_value;

  public StringComparatorEQ (String value) {
    comparator_value = value;
  };

  public boolean compare (Object raw) {
    return raw.toString().equals(comparator_value);
  };

}
