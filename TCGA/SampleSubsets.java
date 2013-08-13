package TCGA;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;

public class SampleSubsets implements ActionListener {
  private GenomicMeasurement gm;

  private String[] subsets;
  private HashSet<String> all_subsets;
  // coded into data file

  private ArrayList<String> visible_subsets;
  // currently visible subsets: may be initialized from data file
  private HashMap<String,JCheckBoxMenuItem> checkboxes;

  public SampleSubsets (GenomicMeasurement gm) {
    this.gm = gm;
    setup();
  }

  public SampleSubsets (GenomicMeasurement gm, SampleSubsets parent) {
    // essentially acts as clone() 
    this.gm = gm;
    setup();

    visible_subsets = new ArrayList<String>(parent.get_visible_subsets());
    // deep copy visible subset array
  }

  private void setup() {
    checkboxes = new HashMap<String,JCheckBoxMenuItem>();
    CommentOptions gm_options = gm.get_options();
    //    System.err.println("GMO for " + gm + " = " + gm_options);  // debug

    String subs = gm_options.get("sample_subsets");
    //    System.err.println("subs for " + gm + "="+subs);  // debug

    String visible_subs = gm_options.get("visible_sample_subsets");
    visible_subsets = new ArrayList<String>();
    all_subsets = new HashSet<String>();
    if (subs != null) {
      subsets = subs.split(",");

      HashSet<String> vs = new HashSet<String>();
      if (visible_subs == null) {
	// no entry: everything visible
	for (int i = 0; i < subsets.length; i++) {
	  vs.add(subsets[i]);
	}
      } else {
	String[] vsub = visible_subs.split(",");
	for (int i=0; i < vsub.length; i++) {
	  vs.add(vsub[i]);
	}
      }
      for (int i = 0; i < subsets.length; i++) {
	all_subsets.add(subsets[i]);
	if (vs.contains(subsets[i])) visible_subsets.add(subsets[i]);
      }

      for (GenomicSample gs : gm.get_rows()) {
	// assign subset ID to each sample
	for (int i=0; i < subsets.length; i++) {
	  if (gs.sample_id.indexOf(subsets[i]) >= 0) {
	    gs.subset_id = subsets[i];
	    break;
	  }
	}

      }
    }
  }

  public ArrayList<JCheckBoxMenuItem> get_checkboxes() {
    ArrayList<JCheckBoxMenuItem> results = new ArrayList<JCheckBoxMenuItem>();
    results.addAll(checkboxes.values());
    return results;
  }

  public boolean isEmpty() {
    return gm.get_options().get("sample_subsets") == null;
  }

  public String[] get_subsets() {
    return subsets;
  }

  public ArrayList<String> get_subsets_arraylist() {
    ArrayList<String> results = new ArrayList<String>();
    if (subsets == null) {
      System.err.println("WTF: no subsets initialized");  // debug
    } else {
      for (int i = 0; i < subsets.length; i++) {
	results.add(subsets[i]);
      }
    }
    return results;
  }

  public JCheckBoxMenuItem get_checkbox_menuitem (String s) {
    JCheckBoxMenuItem cb = checkboxes.get(s);
    if (cb == null) {
      cb = new JCheckBoxMenuItem(s, visible_subsets.contains(s));
      checkboxes.put(s, cb);
      cb.addActionListener(this);
    }
    return cb;
  }

  public ArrayList<String> get_visible_subsets() {
    return visible_subsets;
  }
  
  public void actionPerformed(ActionEvent e) {
    //
    // a checkbox has been changed, rebuild visible set
    //
    visible_subsets = new ArrayList<String>();
    for (int i = 0; i < subsets.length; i++) {
      // rebuild list of visible subsets
      if (checkboxes.get(subsets[i]).getState()) visible_subsets.add(subsets[i]);
    }
    set_visibility_flags();
    gm.set_rows(gm.get_rows(), false);
  }

  public void set_visibility_flags() {
    //
    // set sample visibility flags based on currently visible subsets
    //
    ArrayList<GenomicSample> rows = gm.get_rows();
    boolean visible;
    for (GenomicSample gs : rows) {
      visible = false;
      for (String subset : visible_subsets) {
	if (gs.sample_id.indexOf(subset) >= 0) {
	  visible = true;
	  break;
	}
      }
      gs.visible_in_display = visible;
    }
  }

  public String strip_id (String id) {
    //
    // attempt to guess "patient ID" by stripping out subset names from sample ID field
    //
    String[] chunks = id.split("\\s+");
    int[] ok = new int[chunks.length];
    Arrays.fill(ok, 1);
    
    for (int i = 0; i < chunks.length; i++) {
      if (all_subsets.contains(chunks[i])) {
	ok[i] = 0;
      }
    }

    String result = id;
    for (int i = 0; i < chunks.length; i++) {
      if (ok[i] == 1) {
	result = chunks[i];
	break;
      }
    }
    
    return result;
  }

}
