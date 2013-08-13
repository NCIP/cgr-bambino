package TCGA;

import java.util.*;

public class ByteComparatorEQ extends ByteComparator {

  public ByteComparatorEQ(byte value) {
    super(value);
  };

  public boolean compare(byte x) {
    return x == comparator_value;
  };

}