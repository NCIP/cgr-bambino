package Funk;

import java.util.Comparator;
// import java.util.*;

public class NumberComparatorEQ extends NumberComparator {

  public NumberComparatorEQ (Number value) {
    super(value);
  };

  public boolean compare (Object raw) {
    try {
      return (get_value(raw) == comparator_value);
    } catch (NumberFormatException ex) {
      return false;
    }
  };


}
