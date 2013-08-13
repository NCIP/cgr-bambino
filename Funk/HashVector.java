package Funk;

import java.util.*;

public class HashVector extends java.util.Dictionary {

  private Hashtable hash;

  public HashVector () {
    hash = new Hashtable();
  }
  
  public Object put(Object  key, Object  value) {
    // this is the only Dictionary method that's different from straight hash...
    Vector v = (Vector) hash.get(key);
    if (v == null) {
      v = new Vector();
      hash.put(key, v);
    }

    v.addElement(value);
    return null;
    // technically doesn't meet spec...
  }

  public Object get (Object key) {
    return hash.get(key);
  }

  public Object remove (Object key) {
    return hash.remove(key);
  }

  public int size () {
    return hash.size();
  }

  public boolean isEmpty() {
    return hash.isEmpty();
  }

  public Enumeration keys () {
    return hash.keys();
  }

  public Enumeration elements () {
    return hash.elements();
  }

  public Enumeration elements_for (Object key) {
    Vector v = (Vector) hash.get(key);
    return v == null ? null : v.elements();
  }

}
