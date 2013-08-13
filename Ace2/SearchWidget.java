package Ace2;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import Funk.VerticalLayout;
import Funk.DoWhatISayObservable;

public class SearchWidget extends Funk.CloseFrame implements ActionListener,KeyListener {
  private Choice choice;
  private Button b_ok, b_cancel;
  private DoWhatISayObservable dwis;
  private TextField tf;
  private Observer observer;

  private final static String AUTO = "Automatic";
  private final static String ID = "Sequence ID";
  private final static String NUKE = "Nucleotide";

  public SearchWidget (Observer observer) {
    dwis = new DoWhatISayObservable();
    dwis.addObserver(observer);
    setup();
  }

  public boolean is_id_search () {
    // is this a sequence ID search (as opposed to a nucleotide search)?
    String thing = choice.getSelectedItem();
    if (thing.equals(ID)) {
      return true;
    } else if (thing.equals(NUKE)) {
      return false;
    } else {
      // auto; depends on content
      String value = get_value();
      for (int i=0; i < value.length(); i++) {
	char c = value.charAt(i);
	if (!(c == 'a' || c == 'A' ||
	      c == 'c' || c == 'C' ||
	      c == 'g' || c == 'G' ||
	      c == 't' || c == 'T')) {
	  // not ACGT, assume an ID search
	  return true;
	}
      }
      return false;
      // all ACGT; assume nucleotide search
    }
  }

  public String get_value () {
    return tf.getText();
  }

  private void setup () {
    setTitle("Find");
    VerticalLayout vl = new Funk.VerticalLayout(this);

    Panel p = new Panel();
    p.add(new Label("Search for:"));
    p.add(tf = new TextField(40));
    tf.addKeyListener(this);
    vl.add_panel(p);

    p = new Panel();
    p.add(new Label("Search type:"));
    choice = new Choice();
    choice.addItem(AUTO);
    choice.addItem(NUKE);
    choice.addItem(ID);
    p.add(choice);

    vl.add_panel(p);

    p = new Panel();
    p.add(b_ok = new Button("OK"));
    p.add(b_cancel = new Button("Cancel"));
    b_ok.addActionListener(this);
    b_cancel.addActionListener(this);
    vl.add_panel(p);

    pack();
    setVisible(true);
    new Funk.FocusHack(tf);
  }

  public static void main (String [] argv) {
    new SearchWidget(null);
  }

//   public boolean handleEvent (Event e) {
//     switch (e.id) {
//     case Event.ACTION_EVENT:
//       if (e.target == b_cancel) {
// 	quit();
// 	return true;
//       } else if (e.target == b_ok) {
// 	notifyObservers();
//       }
//       break;
//     case Event.KEY_PRESS:
//       if (e.target == tf && (e.key == 10 || e.key == 13)) {
// 	notifyObservers();
//       }
//       break;
//     }
//     return false;
//   }

  private void quit () {
    setVisible(false);
    dispose();
  }

  private void notifyObservers () {
    dwis.setChanged();
    dwis.notifyObservers(this);
    quit();
  }

  // begin ActionListener stubs 
  public void actionPerformed(ActionEvent e) {
    if (e.getSource().equals(b_ok)) {
      notifyObservers();
    } else {
      quit();
    }
  }
  // end ActionListener stubs 

  // begin KeyListener stubs 
  public void keyPressed(KeyEvent ke) {
    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
      notifyObservers();
    }
  }
  public void keyReleased(KeyEvent ke) {}
  public void keyTyped(KeyEvent ke) {}
  // end KeyListener stubs 





}
