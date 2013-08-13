package Ace2;

import java.util.*;

public class HashList<K,E> {
  private HashMap<K,ArrayList<E>> map;

  public HashList() {
    map = new HashMap<K,ArrayList<E>>();
  }

  public void add (K key, E value) {
    ArrayList<E> bucket = map.get(key);
    if (bucket == null) map.put(key, bucket = new ArrayList<E>());
    bucket.add(value);
  }

  public ArrayList<E> get (K key) {
    return map.get(key);
  }

  public Set<K> keySet() {
    return map.keySet();
  }

  public static void main (String[] argv) {
    HashList<String,String> hl = new HashList<String,String>();
    hl.add("fruit", "apple");
    hl.add("fruit", "orange");
    hl.add("candy", "starburst");
    hl.add("candy", "goldenberg's peanut chews");

    for (String s : hl.get("fruit")) {
      System.err.println(s);  // debug
    }

    HashList<Integer,String> h2 = new HashList<Integer,String>();
    h2.add(1, "one");
    h2.add(1, "uno");

    for (String s : h2.get(1)) {
      System.err.println(s);  // debug
    }

  }


}
