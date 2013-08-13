package Ace2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import layout.SpringUtilities;

public class SearchWidget2 {
  private JDialog jd;
  private JFrame parent_frame;
  private JComboBox jcb_searchtype;
  private JTextField jtf_search;

  private final static String AUTO = "Automatic";
  private final static String ID = "Sequence ID";
  private final static String NUKE = "Nucleotide";

  private boolean wants_find = false;

  public SearchWidget2() {
    // debug only
    parent_frame = null;
    setup();
  }

  public SearchWidget2(JFrame parent_frame) {
    this.parent_frame = parent_frame;
    setup();
  }

  private void setup() {
    if (parent_frame == null) {
      jd = new JDialog();
    } else {
      jd = new JDialog(parent_frame, true);
    }

    JPanel jp_main = new JPanel();
    jp_main.setLayout(new BoxLayout(jp_main, BoxLayout.PAGE_AXIS));

    JPanel jp_controls = new JPanel();
    jp_controls.setLayout(new SpringLayout());
    jp_main.add(jp_controls);
    
    int rows = 0;
    int columns = 2;
    
    jp_controls.add(new JLabel("Search for: ", SwingConstants.RIGHT));
    jp_controls.add(jtf_search = new JTextField(20));
    jtf_search.addKeyListener(
			      new KeyListener() {// begin KeyListener stubs 
				public void keyPressed(KeyEvent ke) {
				  int code = ke.getKeyCode();
				  if (code == KeyEvent.VK_ESCAPE) {
				    decide(false);
				  } else if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
				    decide(true);
				  }
				}
				public void keyReleased(KeyEvent ke) {}
				public void keyTyped(KeyEvent ke) {}
				// end KeyListener stubs 
			      }
			      );
    rows++;

    Vector items = new Vector();
    items.add(AUTO);
    items.add(ID);
    items.add(NUKE);
    jp_controls.add(new JLabel("Search type: ", SwingConstants.RIGHT));
    
    jcb_searchtype = new JComboBox(items);
    jp_controls.add(jcb_searchtype);
    rows++;

    int border_pad = 2;
    SpringUtilities.makeCompactGrid(jp_controls,
 				    rows, columns,
 				    // rows, columns
				    border_pad, border_pad, border_pad, border_pad
				    );

    JButton jb_find = new JButton("Find");
    jb_find.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  decide(true);
	}
      });

    JButton jb_close = new JButton("Cancel");
    jb_close.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  decide(false);
	}
      });

    JPanel jp_buttons = new JPanel();
    jp_buttons.add(jb_find);
    jp_buttons.add(jb_close);
    jp_main.add(jp_buttons);

    jd.getContentPane().add(new JScrollPane(jp_main));

    jd.pack();
    jd.setTitle("Find");
    jd.setVisible(true);
  }

  private void decide (boolean wants_find) {
    this.wants_find = wants_find;
    jd.setVisible(false);
    jd.dispose();
  }

  public static void main (String[] argv) {
    Funk.LookAndFeeler.set_native_lookandfeel();
    new SearchWidget2();
  }

  public boolean is_id_search () {
    // is this a sequence ID search (as opposed to a nucleotide search)?
    String thing = (String) jcb_searchtype.getSelectedItem();
    boolean result = true;
    if (thing.equals(ID)) {
      result = true;
    } else if (thing.equals(NUKE)) {
      result = false;
    } else {
      // auto; depends on content
      String value = get_value();
      boolean all_acgt = true;
      for (int i=0; i < value.length(); i++) {
	char c = value.charAt(i);
	if (!(c == 'a' || c == 'A' ||
	      c == 'c' || c == 'C' ||
	      c == 'g' || c == 'G' ||
	      c == 't' || c == 'T')) {
	  // not ACGT, assume an ID search
	  all_acgt = false;
	  break;
	}
      }
      result = !all_acgt;
      // if all ACGT, assume nucleotide search
      // (if any not ACGT, assume ID search)
    }
    return result;
  }

  public boolean wants_find() {
    String text = get_value();
    return wants_find && text.length() > 0;
  }

  public String get_value() {
    return jtf_search.getText();
  }


}