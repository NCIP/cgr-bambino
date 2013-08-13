package TCGA;

import java.util.*;

public class BooleanComparatorOr extends BooleanComparator {

  public boolean compare(boolean x, boolean y) {
    return x || y;
  };

}