//
// >i
// You are carrying:
//   A purple scroll
//   A spell book
// 
// >examine purple scroll
// The scroll reads "filfre spell: create gratuitous fireworks".
// 
// >filfre
// As you cast the spell, the purple scroll vanishes!
// In a blinding burst of pyrotechnics, the air lights up with fireworks and dazzling explosions of
// multicolored fire! In sizzling sparks and roiling smoke is written:
// 
//   Enchanter
//      by
// Dave Lebling
//      and
//  Marc Blank
// 
// Copyright 1983, by Infocom, Inc.
// 
// After a while, the smoke dissipates and the lights dim. You remain slightly dazzled for a while,
// but fortunately, this wears off.
//
// ----------------------------------------------------------------------
//
// visit http://edmonson.paunix.org/rezrov/
// -MNE
//
package Ace2;

import java.util.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Filfre {
  HashMap<String,String> messages;
  int max_len;
  String buffer;
  Component component;

  public Filfre (Component c) {
    component = c;
    buffer = "";
    
    messages = new HashMap<String,String>();
    messages.put("credits", "written by Michael Edmonson <edmonson@nih.gov> <mnedmonson@gmail.com>");
    messages.put("xyzzy", "A hollow voice says \"Fool.\"");
    messages.put("spicy", "Hi fofa! -VN");
    
    c.addKeyListener(new KeyListener() {
	// begin KeyListener stubs 
	public void keyPressed(KeyEvent ke) {}
	public void keyReleased(KeyEvent ke) {}
	public void keyTyped(KeyEvent ke) {
	  buffer = buffer.concat("" + Character.toLowerCase(ke.getKeyChar()));

	  if (buffer.length() > max_len) {
	    buffer = buffer.substring(buffer.length() - max_len);
	  }

	  for (String search : messages.keySet()) {
	    if (buffer.indexOf(search) > -1) {
	      if (search.equals("credits")) {
		bambino_credits();
	      } else {
		String msg = messages.get(search);
		JOptionPane.showMessageDialog(component,
					      msg,
					      "Message",
					      JOptionPane.INFORMATION_MESSAGE);
	      }
	      buffer = "";
	    }
	  }

	}
      });

    max_len = 0;
    for (String key : messages.keySet()) {
      if (key.length() > max_len) max_len = key.length();
    }

  }

  private void bambino_credits() {
    ClassLoader cl = this.getClass().getClassLoader();
    String resource = "Ace2/BambinoMuse.obt";
    URL load_url = cl == null ?
      ClassLoader.getSystemResource(resource) : cl.getResource(resource);

    JFrame f = new JFrame("Credits");
    JPanel jp_main = new JPanel();
    jp_main.setLayout(new BoxLayout(jp_main, BoxLayout.PAGE_AXIS));
    JPanel jp = new JPanel();
    jp.add(new javax.swing.JLabel(new javax.swing.ImageIcon(load_url)));
    jp_main.add(jp);

    jp = new JPanel();
    jp.add(new JLabel("<html><b>Bambino</b></html>"));
    jp_main.add(jp);

    jp = new JPanel();
    jp.add(new JLabel("Written by Michael Edmonson (edmonson@nih.gov / mnedmonson@gmail.com)"));
    jp_main.add(jp);

    jp = new JPanel();
    JButton jb = new JButton("OK");
    jb.addActionListener(
			 new ActionListener() {
			   public void actionPerformed(ActionEvent e) {
			     JButton jb = (JButton) e.getSource();
			     Frame f = Funk.Gr.getFrame(jb);
			     if (f != null) {
			       f.setVisible(false);
			       f.dispose();
			     }
			   }
			   // end ActionListener stub
			 }
			 );

    jp.add(jb);
    jp_main.add(jp);

    f.getContentPane().add(jp_main);

    f.pack();

    f.setVisible(true);
  }

}