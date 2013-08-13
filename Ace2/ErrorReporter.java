package Ace2;

import javax.swing.*;
import java.io.*;
import java.awt.BorderLayout;
import java.awt.event.*;

public class ErrorReporter extends JFrame implements ActionListener,Runnable {

  //  public static final String MAINTAINER = "Michael Edmonson <edmonson@nih.gov>";
  public static final String MAINTAINER = "Michael Edmonson <Michael.Edmonson@stjude.org>";
  private JButton jb_yikes;
  private Throwable e;

  public ErrorReporter (Throwable e) {
    this.e = e;
    e.printStackTrace();
    new Thread(this).start();
  }

  public void run() {
    setTitle("Error");

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.println("Sorry, a serious error has occurred.");
    pw.println("Please contact " + MAINTAINER + " with the following details, which will help diagnose the problem.");
    pw.println("If possible, please include the URL and any steps required to reproduce the bug.");
    
    pw.println();

    if (e instanceof OutOfMemoryError) {
      pw.println("This appears to be an out-of-memory error.  The application may be trying to process more");
      pw.println("reads than will fit in memory; please try the viewer on a smaller region and/or sample set.");

      pw.println("If running from the command line, try specifying \"-Xmx1024m\" or higher as the first parameter, i.e.:");

      pw.println("   \"java -Xmx1024m -jar av.jar ...\"");
      pw.println("You can specify larger values to allow additional RAM usage (1024, 2048, etc.)");
      pw.println("It may also be possible to set these values in your system's Java control panel.");
      pw.println();
    }


    pw.println("Stack trace:");
    e.printStackTrace(pw);

    //    System.err.println(sw.getBuffer().toString());  // debug

    //    setLayout(new BorderLayout());
    add("Center", new JScrollPane(new JTextArea(sw.getBuffer().toString())));
    JPanel controls = new JPanel();

    String label = Math.random() < 0.5 ? "Oh, the humanity" : "Yikes";
    jb_yikes = new JButton(label);

    jb_yikes.addActionListener(this);
    controls.add(jb_yikes);
    add("South", controls);
    pack();
    setVisible(true);
  }

  // begin ActionListener stub
  public void actionPerformed(ActionEvent e) {
    setVisible(false);
    System.exit(1);
  }
  // end ActionListener stub



  public static void main (String[] argv) {
    booey();
  }

  private static void booey() {
    Error e = new OutOfMemoryError();
    //    e.printStackTrace();
    new ErrorReporter(e);
  }


}
