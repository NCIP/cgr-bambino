package TCGA;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ProgressFrame extends Observable implements VisibilityToggle {
  private JFrame jf;
  private JProgressBar jpb;
  private String title,label;
  private int max_value;
  private boolean wants_cancel = false;

  public ProgressFrame(String title, String label, int max_value, boolean wants_cancel) {
    this.title = title;
    this.label = label;
    this.max_value = max_value;
    this.wants_cancel = wants_cancel;
    setup();
  }

  private JPanel create_padded_panel() {
    JPanel jp = new JPanel();
    jp.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
    return jp;
  }

  private void setup() {
    jf = new JFrame();
    jf.setTitle(title);
    jf.getContentPane().setLayout(new BorderLayout());

    JPanel jp = create_padded_panel();

    jpb = new JProgressBar(0, max_value);
    jpb.setValue(0);
    jpb.setStringPainted(true);
    jp.setLayout(new BorderLayout());
    jp.add("Center", jpb);

    jf.getContentPane().add("Center", jp);

    jp = create_padded_panel();
    JLabel jl = new JLabel(label, JLabel.CENTER);

    Dimension dl = jl.getPreferredSize();
    Dimension dp = jpb.getPreferredSize();
    int min_w = dl.width * 2;
    if (dp.width < min_w) {
      dp.width = min_w;
      jpb.setPreferredSize(dp);
    }

    jp.add(jl);
    jf.getContentPane().add("North", jp);

    if (wants_cancel) {
      jp = create_padded_panel();
      JButton jb = new JButton("Cancel");
      jb.addActionListener(
			       new ActionListener() {
				 public void actionPerformed(ActionEvent e) {
				   setChanged();
				   notifyObservers();
				   // cancel
				 }
			       }
			   );

      jp.add(jb);
      jf.getContentPane().add("South", jp);
    }

    jf.pack();
    jf.setVisible(true);
  }

  public void setVisible (boolean v) {
    jf.setVisible(v);
  }

  public static void main (String[] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    new ProgressFrame("Working...", "progress test", 100, true);
  }

  public void setValue (int n) {
    jpb.setValue(n);
  }
  
}
