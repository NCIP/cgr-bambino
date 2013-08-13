package TCGA;

import java.util.*;

public class HashMapArrayList extends HashMap {
  // 
  //  use a HashMap to bucket multiple values per key into an ArrayList.
  //  (i.e. 1:many relationship between keys and values).
  //
  public Object put (Object key, Object value) {
    // FIX ME: generics definition???
    ArrayList bucket = get(key);
    ArrayList last = bucket;
    if (bucket == null) super.put(key, bucket = new ArrayList());
    bucket.add(value);
    return last;
  }

  public ArrayList get(Object key) {
    return (ArrayList) super.get(key);
  }

  public void addAll(Object key, ArrayList values) {
    for (Object value : values) {
      put(key, value);
    }
  }

  public static void main (String[] argv) {
    HashMapArrayList hma = new HashMapArrayList();
    hma.put("key", "value1");
    hma.put("key", "value2");
    
    ArrayList bucket = hma.get("key");
    System.err.println("bucket="+bucket);  // debug

  }
}
