package TCGA;

import java.util.*;

public class GenomicSampleReformatter {
  
  private int[] header_map;
  // map of child header indices to use to remap data to parent's format
  HashMap<String,Integer> child_indices;

  public GenomicSampleReformatter (GenomicMeasurement parent, GenomicMeasurement child) {
    String[] child_headers = child.get_headers();
    child_indices = new HashMap<String,Integer>();
    for (int i = 0; i < child_headers.length; i++) {
      child_indices.put(child_headers[i], i);
    }
    // index headers in child dataset

    String[] parent_headers = parent.get_headers();
    header_map = new int[parent_headers.length];
    
    for (int i = 0; i < parent_headers.length; i++) {
      Integer index = child_indices.get(parent_headers[i]);
      //      System.err.println(i + " " + parent_headers[i] + " => " + index);  // debug
      if (index == null) {
	header_map[i] = -1;
      } else {
	header_map[i] = index;
      }
    }
  }

  public byte[] remap (GenomicSample child) {
    // re-order data from a child record to the ordering of the parent record
    byte[] data = new byte[header_map.length];
    int idx;
    for (int i = 0; i < header_map.length; i++) {
      if (header_map[i] == -1) {
	data[i] = GenomicSample.NULL_VALUE;
      } else {
	data[i] = child.copynum_data[header_map[i]];
      }
    }
    return data;
  }

  public byte get_mapped (GenomicSample child, int parent_header_index) {
    return header_map[parent_header_index] == -1 ? 
      GenomicSample.NULL_VALUE : child.copynum_data[header_map[parent_header_index]];
  }

  

}
