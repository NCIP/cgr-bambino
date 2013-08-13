package Funk;

import java.util.*;

public class StringComparatorNE implements LogicalComparator {
  private String comparator_value;

  public StringComparatorNE (String value) {
    comparator_value = value;
  };

  public boolean compare (Object raw) {
    return !raw.toString().equals(comparator_value);
  };

}
