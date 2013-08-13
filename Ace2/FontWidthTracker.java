package Ace2;

import java.awt.*;
import java.util.*;

public class FontWidthTracker implements Runnable {
  private FontMetrics fm;
  private Assembly assembly;
  private int max_width;
  private boolean finished;

  static HashSet<String> tracked_strings = new HashSet<String>();

  public FontWidthTracker (FontMetrics fm, Assembly assembly) {
    this.fm = fm;
    this.assembly = assembly;
    if (assembly.alignment_is_built()) {
      //      new Thread(this).start();
      run();
      // sync hassles, fast enough not worth the trouble
    } else {
      System.err.println("ERROR: asm must be built first");  // debug
    }

  }

  public void run() {
    finished = false;
    //    Funk.Timer t = new Funk.Timer("font_width_track");
    max_width = 0;
    int w;
    for (AssemblySequence as : assembly.get_sequences()) {
      w = fm.stringWidth(as.get_name());
      if (w > max_width) max_width = w;
    }
    for (String s : tracked_strings) {
      //      System.err.println("tracked: " + s);  // debug
      w = fm.stringWidth(s);
      if (w > max_width) max_width = w;
    }
    //    System.err.println("max_width = " + max_width);  // debug
    //    t.finish();
    finished = true;
  }

  public int get_max_width () {
    if (finished) {
      return max_width;
    } else {
      return assembly.get_max_id_length() * fm.stringWidth("W");
    }
  }

  public static void add_tracked_string (String s) {
    tracked_strings.add(s);
  }


}