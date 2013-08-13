package Ace2;

import java.util.*;
import Trace.*;

public class TraceViewerCache {
  HashMap<String,TraceViewer> cache = new HashMap<String,TraceViewer>();

  static int MAX_OPEN_VIEWERS = 10;

  public TraceViewer get_traceviewer (String fn, boolean rc) {
    TraceViewer tv = cache.get(fn);

    if (false) {
      System.err.println("DEBUG: cache disabled!");  // debug
      tv = null;
    }

    if (tv == null) {

      //      System.err.println("size="+cache.size());  // debug
      if (cache.size() >= MAX_OPEN_VIEWERS) {
	//	System.err.println("CLEANUP");  // debug
	int remove = (cache.size() + 1) - MAX_OPEN_VIEWERS;
	//	System.err.println("remove " + remove);  // debug
	ArrayList<String> list = new ArrayList<String>(cache.keySet());
	for (int i = 0; i < remove; i++) {
	  String id = list.get(i);
	  TraceViewer t = cache.get(id);
	  t.setVisible(false);
	  t.dispose();
	  cache.remove(id);
	}
      }

      cache.put(fn, tv = new TraceViewer(fn, rc));
    }
    return tv;
  }

}