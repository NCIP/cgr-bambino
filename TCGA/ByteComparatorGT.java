package TCGA;

import java.util.*;

public class ByteComparatorGT extends ByteComparator {

  public ByteComparatorGT(byte value) {
    super(value);
  };

  public boolean compare(byte x) {
    return x > comparator_value;
  };

  public static void main(String[] argv) {
    try {
      //      GenomicMeasurement gm = new GenomicMeasurement("p53.txt", false);
      GenomicMeasurement gm = new GenomicMeasurement("gene_copynumber_liver_cancer_normal.txt", false);
      ArrayList<GenomicSample> rows = gm.get_rows();
      GenomicSample gs = rows.get(0);
      ByteComparator bc = new ByteComparatorGT((byte) 0);
      for (int i=0; i < gs.copynum_data.length; i++) {
	//	System.err.println(i + ": " + bc.compare(gs.copynum_data[i]));  // debug
	bc.compare(gs.copynum_data[i]);
      }
    } catch (Exception e) {
      System.err.println("error:"+e);  // debug
    }
    
  }

}