// bring up a message in a new toplevel window.

package Funk;
import java.awt.*;
import java.awt.event.*;

public class Message extends CloseFrame implements ActionListener {
  private String message;
  private String button_label = "OK";

  public Message (String message) {
    this.message = message;
    setup();
  }

  public Message (String message, String button_label) {
    this.message = message;
    this.button_label = button_label;
    setup();
  }

  private void setup () {
    setTitle("Message");
    setLayout(new BorderLayout());
    Button close_b = new Button(button_label);
    close_b.addActionListener(this);
    Panel p = new Panel();
    p.add("Center", close_b);
    add("South", p);

    Panel mp = new Panel();
    mp.add(new Label(message));
    add("Center", mp);
    pack();
    setResizable(false);
    setVisible(true);
  }

  private void quit () {
    setVisible(false);
    dispose();
  }

  // begin ActionListener stubs 
  public void actionPerformed(ActionEvent e) {
    quit();
  }
  // end 

  public static void main (String []argv) {
    if (argv.length == 1) {
      new Message(argv[0]);
    } else if (argv.length == 2) {
      new Message(argv[0], argv[1]);
    } else {
      System.err.println("specify message");  // debug
    }
  }
}
