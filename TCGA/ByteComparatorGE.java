package TCGA;

import java.util.*;

public class ByteComparatorGE extends ByteComparator {

  public ByteComparatorGE(byte value) {
    super(value);
  };

  public boolean compare(byte x) {
    return x >= comparator_value;
  };

}