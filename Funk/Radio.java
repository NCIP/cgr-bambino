package Funk;

import java.awt.*;

public class Radio {
  //
  // simple "radio buttons"
  //

  private CheckboxGroup cbg;
  private Checkbox b1, b2;

  public Radio (Container c, String label,
		String b1_label, String b2_label, boolean initial_state) {
    c.add(new Label(label));
    cbg = new CheckboxGroup();
    c.add(b1 = new Checkbox(b1_label, cbg, initial_state));
    c.add(b2 = new Checkbox(b2_label, cbg, !initial_state));
  }

  public boolean getState () {
    // return true if the first button specified is selected.
    return b1.getState();
  }
  
}
