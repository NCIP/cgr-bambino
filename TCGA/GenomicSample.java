package TCGA;
import java.util.*;

public class GenomicSample implements Cloneable {
  public static final int NULL_VALUE = -99;
  // FIX ME: byte???

  public String sample_id;
  // raw identifier as parsed from source file
  // may contain subset identifier as well

  public String patient_id;
  // if sample ID is some flavor of TCGA barcode, 
  // then the 3-field TCGA patient ID (TCGA-XX-YYYY).
  // if another format, just the sample ID, but stripped of any subset information.

  public String subset_id;
  // subset ID (split out from raw sample ID field)

  public byte[] copynum_data;
  // data starts at index 0 (i.e. first "reporter" column removed)
  // FIX ME: RENAME THIS
  public boolean all_null = false;
  public boolean all_empty = false;

  public boolean visible_in_display = true;
  public boolean visible_to_sorting = true;

  public byte[] rle_values = null;
  public int[] rle_lengths = null;
  // DELETE ME: will break if data reordered...
  
  public void reorder_data (ArrayList<Integer> new_order) {
    if (rle_values != null) {
      System.err.println("FATAL ERROR: reorder attempt w/RLE data");  // debug
      System.exit(1);
    } else if (new_order.size() != copynum_data.length) {
      System.err.println("FATAL ERROR: invalid length in reorder");  // debug
      System.exit(1);
    }

    byte[] data_new = new byte[copynum_data.length];
    int ti=0;
    for (Integer si : new_order) {
      data_new[ti++] = copynum_data[si];
    }
    copynum_data = data_new;
  }

  public GenomicSample clone () {
    GenomicSample gs_new=null;
    try {
      gs_new = (GenomicSample) super.clone();
      // create shallow copy
    } catch (Exception e) {
      System.err.println("clone error!:"+e);
    }
    return gs_new;
  }

  public static HashSet get_patient_ids (ArrayList<GenomicSample> list) {
    HashSet<String> results = new HashSet<String>();
    for (GenomicSample gs : list) {
      results.add(gs.patient_id);
    }
    return results;
  }

  public static ArrayList<String> get_patient_id_list (ArrayList<GenomicSample> list) {
    // return unique, ordered list of patient IDs from a given list of samples
    ArrayList<String> results = new ArrayList<String>();
    HashSet<String> saw = new HashSet<String>();
    for (GenomicSample gs : list) {
      if (!saw.contains(gs.patient_id)) results.add(gs.patient_id);
      saw.add(gs.patient_id);
    }
    return results;
  }

  public void set_null_flags() {
    all_null = true;
    all_empty = true;

    int i;
    for (i=0; i < copynum_data.length; i++) {
      // check whether all datapoints for the sample are null
      if (copynum_data[i] != NULL_VALUE) {
	all_null = false;
      }
      if (copynum_data[i] != 0) {
	all_empty = false;
      }
      
      if (all_null == false && all_empty == false) break;
    }

    //    System.err.println("null flag for " + sample_id + "=" + all_null);  // debug

  }


}
