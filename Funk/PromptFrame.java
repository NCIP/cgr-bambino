// prompt for something in a new toplevel window.

package Funk;
import java.awt.*;
import java.util.*;
import java.awt.event.*;

public class PromptFrame extends Frame implements WindowListener,KeyListener,ActionListener {
  private Font myfont = new Font("TimesRoman", Font.PLAIN, 16);
  private Button b_ok, b_cancel;
  private Observer observer;
  private String message, title;
  private TextField tf;
  private DoWhatISayObservable dwis = new DoWhatISayObservable();

  private static final String L_OK = "OK";
  private static final String L_CANCEL = "Cancel";

  public PromptFrame (Observer observer, String message) {
    this.observer = observer;
    this.message = message;
    setup();
  }

  public PromptFrame (Observer observer, String message, String title) {
    this.observer = observer;
    this.message = message;
    this.title = title;
    setup();
  }

  private void setup () {
    dwis.addObserver(observer);
    addWindowListener(this);
    setTitle(title == null ? "Prompt" : title);
    setLayout(new BorderLayout());

    Panel p = new Panel();
    p.add(b_ok = new Button(L_OK));
    p.add(b_cancel = new Button(L_CANCEL));

    b_ok.addActionListener(this);
    b_cancel.addActionListener(this);
    add("South", p);
    
    p = new Panel();
    p.add(new Label(message + ":"));
    tf = new TextField(20);
    tf.addKeyListener(this);
    p.add(tf);
    add("North", p);

    pack();
    setSize(getPreferredSize());
    setResizable(false);
    setVisible(true);

    new Funk.FocusHack(tf);
  }

  private void quit () {
    setVisible(false);
    dispose();
  }

  private void notifyObservers () {
    dwis.setChanged();
    dwis.notifyObservers(this);
    quit();
  }

  public static void main (String []argv) {
    new PromptFrame(null, "Hi there", "Test");
  }

  public String get_value () {
    return tf.getText();
  }

  public void windowActivated(WindowEvent we) {}
  public void windowClosed(WindowEvent we) {}
  public void windowDeactivated(WindowEvent we) {}
  public void windowDeiconified(WindowEvent we) {}
  public void windowIconified(WindowEvent we) {}
  public void windowOpened(WindowEvent we) {}
  public void windowClosing(WindowEvent we) {
    quit();
  }

  // begin KeyListener stubs 
  public void keyPressed(KeyEvent ke) {
    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
      notifyObservers();
    }
  }

  public void keyReleased(KeyEvent ke) {}
  public void keyTyped(KeyEvent ke) {}
  // end KeyListener stubs 

  // begin ActionListener stubs 
  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (cmd.equals(L_OK)) {
      notifyObservers();
    } else if (cmd.equals(L_CANCEL)) {
      quit();
    } else {
      System.out.println("WTF?");  // debug
    }
  }
  // end ActionListener stubs 
       
}

