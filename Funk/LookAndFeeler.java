package Funk;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.event.*;
import java.util.HashMap;

import Funk.MenuBuilder;

public class LookAndFeeler implements ActionListener {
  private MenuBuilder mb;
  private JFrame jf;
  private LookAndFeelInfo[] lif;
  private ButtonGroup bg;
  HashMap<String,JRadioButtonMenuItem> class2button = new HashMap<String,JRadioButtonMenuItem>();

  public LookAndFeeler (JFrame jf, MenuBuilder mb) {
    this.jf = jf;
    this.mb = mb;
    setup();
  }

  private void setup () {
    lif = UIManager.getInstalledLookAndFeels();
    bg = new ButtonGroup();
    LookAndFeel current_laf = UIManager.getLookAndFeel();
    String current_class = (current_laf == null) ? "" : current_laf.getClass().getName();
    mb.start_submenu("Look and feel", KeyEvent.VK_L);
    mb.push_listener(this);

    for (int i = 0; i < lif.length; i++) {
      String classname = lif[i].getClassName();
      JRadioButtonMenuItem jrb = new JRadioButtonMenuItem(lif[i].getName(),
							  classname.equals(current_class)
							  );
      class2button.put(classname,jrb);
      bg.add(jrb);
      mb.add(jrb);
    }
    mb.add(new JSeparator());
    mb.add("Native", KeyEvent.VK_N);
    mb.add("Cross-platform", KeyEvent.VK_C);

    mb.pop_listener();
    mb.end_submenu();
  }

  public static void main(String[] argv) {
    JFrame jf = new JFrame();
    MenuBuilder mb = new MenuBuilder(jf, null);
    mb.start_menu("File");
    new LookAndFeeler(jf, mb);
    jf.pack();
    jf.setVisible(true);
  }

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    String target = null;

    if (cmd.equals("Native")) {
      target = UIManager.getSystemLookAndFeelClassName();
    } else if (cmd.equals("Cross-platform")) {
      target = UIManager.getCrossPlatformLookAndFeelClassName();
    } else {
      for (int i = 0; i < lif.length; i++) {
	if (cmd.equals(lif[i].getName())) {
	  try {
	    UIManager.setLookAndFeel(lif[i].getClassName());
	    SwingUtilities.updateComponentTreeUI(jf);
	    jf.pack();
	  } catch (Exception ex) {
	    System.err.println(ex);  // debug
	  }
	  break;
	}
      }
    }

    if (target != null) {
      JRadioButtonMenuItem which = class2button.get(target);
      if (which != null) which.doClick();
    }

  }
  // end ActionListener stub

  public static void set_native_lookandfeel() {
    try {
      String name = UIManager.getSystemLookAndFeelClassName();
      if (name != null) 
	UIManager.setLookAndFeel(name);
    } catch (Exception e) {
      System.err.println("error setting native look and feel: " + e);  // debug
    }
  }


}
