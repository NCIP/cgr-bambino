package Ace2;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;

public class SequenceViewLock {
  private JScrollBar jsb_v;
  private HashSet<String> ids;
  private boolean adj_needed = false;

  public SequenceViewLock (JScrollBar jsb_h, JScrollBar jsb_v) {
    this.jsb_v = jsb_v;
    ids = new HashSet<String>();

    jsb_h.addAdjustmentListener(new AdjustmentListener() {
	  // begin AdjustmentListener stub
	public void adjustmentValueChanged(AdjustmentEvent e) {
	  // whenever the horizontal scrollbar is adjusted,
	  // we may need to adjust the vertical scrollbar to center
	  // on the selected ID
	  adj_needed = true;
	}
	// end AdjustmentListener stub
      });
  }

  public void toggle_id (String id) {
    if (ids.contains(id)) {
      // already active, unset
      ids.remove(id);
    } else {
      // add ID to locked set
      // (just one for now)
      ids.clear();
      ids.add(id);
    }
  }

  public boolean contains (String id) {
    // is the specified ID in the locked ID set?
    return ids.contains(id);
  }

  public boolean auto_lock (ArrayList<AssemblySequence> aligned_seqs) {
    boolean adjusted = false;
    if (adj_needed) {
      int i_position = 0;
      for (AssemblySequence as : aligned_seqs) {
	if (ids.contains(as.get_name())) {
	  int extent = jsb_v.getVisibleAmount();
	  //	  int pos = i_position - ((extent / 2) + 1);
	  int pos = i_position - (extent / 2) + 1;
	  if (pos < 0) pos = 0;
	  adj_needed = false;
	  jsb_v.setValue(pos);
	  adjusted = true;
	  break;
	}
	i_position++;
      }
      
    } else {
      //      System.err.println("no action needed");  // debug
    }
    return adjusted;
  }
  
}
