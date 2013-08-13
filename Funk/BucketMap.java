package Funk;

import java.util.*;

public class BucketMap<V> {
  //
  // crude integer-based hash index to store objects APPROXIMATELY
  // linked to a particular integer region.
  // results require further (manual) parsing to determine which 
  // objects actually overlap.
  // Much faster lookup than iterating through arrays though.
  //

  HashMap<Integer,ArrayList<V>> map;
  private int hash_size = 10000;

  public BucketMap(int hash_size) {
    map = new HashMap<Integer,ArrayList<V>>();
    this.hash_size = hash_size;
  }

  public void add (V value, int pos) {
    int key = pos / hash_size;
    bucket_add(value, key);
  }

  public void add (V value, int start, int end) {
    int start_key = start / hash_size;
    int end_key = end / hash_size;
    for (int i = start_key; i <= end_key; i++) {
      bucket_add(value, i);
    }
  }

  private void bucket_add (V value, Integer key) {
    ArrayList<V> bucket = map.get(key);
    if (bucket == null) map.put(key, bucket = new ArrayList<V>());
    bucket.add(value);
  }

  public ArrayList<V> find_by_position (int pos) {
    ArrayList<V> results = new ArrayList<V>();
    int key = pos / hash_size;
    ArrayList<V> bucket = map.get(key);
    if (bucket != null) results.addAll(bucket);
    return results;
  }

  public ArrayList<V> find_by_position (int start, int end) {
    // return objects overlapping region
    ArrayList<V> results = new ArrayList<V>();
    int start_key = start / hash_size;
    int end_key = end / hash_size;
    for (int i = start_key; i <= end_key; i++) {
      ArrayList<V> bucket = map.get(i);
      if (bucket != null) results.addAll(bucket);
    }
    return results;
  }

  public static void main (String[] argv) {
    BucketMap<String> bm = new BucketMap<String>(10000);
    
    String test_border = "border";
    bm.add(test_border, 9000, 11000);

    ArrayList<String> hits = bm.find_by_position(80000);
    System.err.println("hits="+hits);  // debug
  }

}