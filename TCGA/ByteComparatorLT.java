package TCGA;

import java.util.*;

public class ByteComparatorLT extends ByteComparator {

  public ByteComparatorLT(byte value) {
    super(value);
  };

  public boolean compare(byte x) {
    return x < comparator_value;
  };

}