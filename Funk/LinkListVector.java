package Funk;

import java.util.*;

// pseudo-linked-list methods to control position of a vector.

public class LinkListVector extends Vector {
  private int index;

  public LinkListVector () {
    super();
    start();
  }
  
  public void start () {
    index = 0;
  }

  public void end () {
    index = size() - 1;
  }

  public boolean previous() {
    if (--index < 0) {
      index = 0;
      return(false);
    } else {
      return(true);
    }
  }

  public boolean next() {
    if (++index >= size()) {
      index = size() - 1;
      return(false);
    } else {
      return(true);
    }
  }
  
  public Object current () {
    // fix me: if index is bogus...
    if (index > (size() - 1)) {
      // yikes!
      return(null);
    } else {
      return elementAt(index);
    }
  }

  public Object get_next () {
    // return "next" object without moving index.  yuk.
    int i = index + 1;
    if (i >= size()) {
      return null;
    } else {
      return elementAt(i);
    }
  }
}
