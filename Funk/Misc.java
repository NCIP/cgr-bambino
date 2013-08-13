package Funk;

import java.util.*;
import javax.swing.ButtonGroup;

public class Misc {

  public static boolean contains(ButtonGroup bg, Object o) {
    return contains(bg.getElements(), o);
  }

  public static boolean contains(Enumeration e, Object o) {
    return Collections.list(e).contains(o);
  }

}
