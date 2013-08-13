package TCGA;

import java.util.*;

public class DoubleHashMap extends HashMap {
  // 
  //  use 2 levels of HashMap to bucket values.
  //  Similar to a 2d array, but backed by hashes.
  //
  public Object put (Object key1, Object key2, Object value) {
    // FIX ME: generics definition???
    HashMap bucket = (HashMap) get(key1);
    if (bucket == null) super.put(key1, bucket = new HashMap());
    return bucket.put(key2, value);
  }

  public Object get(Object key1, Object key2) {
    HashMap bucket = (HashMap) get(key1);
    //    System.err.println("bucket="+bucket + " key2=" +key2);  // debug
    return bucket == null ? null : bucket.get(key2);
  }

  public static void main (String[] argv) {
    DoubleHashMap dhm = new DoubleHashMap();
    dhm.put("key1", "value1", new Integer(1));
    dhm.put("key1", "value2", new Integer(2));

    System.err.println(dhm.get("key1", "value2"));  // debug
  }

  public void removeAll (Object key) {
    // remove toplevel and secondary references to the specified key
    remove(key);
    remove_all_secondary(key);
  }
  
  public void remove_all_secondary (Object secondary_key) {
    for (Object key1 : keySet()) {
      HashMap bucket = (HashMap) get(key1);
      //      if (bucket.containsKey(secondary_key)) System.err.println("cleanout!");  // debug
      bucket.remove(secondary_key);
    }
  }

}
