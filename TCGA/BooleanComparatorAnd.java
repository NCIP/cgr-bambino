package TCGA;

import java.util.*;

public class BooleanComparatorAnd extends BooleanComparator {

  public boolean compare(boolean x, boolean y) {
    return x && y;
  };

}