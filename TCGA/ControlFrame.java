package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ControlFrame extends JFrame {
  //
  // an outer frame which handles events for generic OK, Cancel, and Apply button events,
  // and optionally generates basic control buttons/panels
  //

  private static final String LABEL_OK = "OK";
  private static final String LABEL_CANCEL = "Cancel";
  private static final String LABEL_APPLY = "Apply";

  public static final int PANEL_OK_APPLY_CANCEL = 1;
  public static final int PANEL_OK_CANCEL = 2;

  private ControlFrameListener cfl;

  public ControlFrame (ControlFrameListener cfl) {
    this.cfl = cfl;
  }

  public JPanel generate_panel (int type) {
    JPanel jp = new JPanel();
    if (type == PANEL_OK_APPLY_CANCEL) {
      jp.add(generate_ok_button());
      jp.add(generate_apply_button());
      jp.add(new JLabel("      ")); //hack
      jp.add(generate_cancel_button());
    } else if (type == PANEL_OK_APPLY_CANCEL) {
      jp.add(generate_ok_button());
      jp.add(generate_cancel_button());
    }
    return jp;
  }

  public JButton generate_ok_button() {
    JButton jb = new JButton(LABEL_OK);
    jb.addActionListener(new ActionListener() {
	// begin ActionListener stub
	public void actionPerformed(ActionEvent e) {
	  cfl.apply_changes();
	  setVisible(false);
	}
	// end ActionListener stub
      });
    return jb;
  }

  public JButton generate_apply_button() {
    JButton jb = new JButton(LABEL_APPLY);
    jb.addActionListener(new ActionListener() {
	// begin ActionListener stub
	public void actionPerformed(ActionEvent e) {
	  cfl.apply_changes();
	}
	// end ActionListener stub
      });
    return jb;
  }

  public JButton generate_cancel_button() {
    JButton jb = new JButton(LABEL_CANCEL);
    jb.addActionListener(new ActionListener() {
	// begin ActionListener stub
	public void actionPerformed(ActionEvent e) {
	  setVisible(false);
	}
	// end ActionListener stub
      });
    return jb;
  }

}
