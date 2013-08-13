package TCGA;

import java.util.*;

public class ByteComparatorLE extends ByteComparator {

  public ByteComparatorLE(byte value) {
    super(value);
  };

  public boolean compare(byte x) {
    return x <= comparator_value;
  };

}