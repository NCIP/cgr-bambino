package TCGA;

import javax.swing.*;
import java.io.*;
import java.awt.BorderLayout;
import java.awt.event.*;

public class ErrorReporter extends JFrame implements ActionListener {

  public static final String MAINTAINER = "Michael Edmonson <edmonson@nih.gov>";
  private JButton jb_yikes;

  public ErrorReporter (Throwable e) {
    setTitle("Error");

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.println("Sorry, a serious error has occurred.");
    pw.println("Please contact " + MAINTAINER + " with the following details, which will help diagnose the problem.");
    
    pw.println();

    if (e instanceof OutOfMemoryError) {
      pw.println("This appears to be an out-of-memory error.  If running from the command line, try specifying \"-Xmx256m\" as the first parameter, i.e.:");
      pw.println("   \"java -Xmx256m -jar heatmap.jar ...\"");
      pw.println("You can specify larger values to allow more memory usage (512, 1024, etc.)");
      
      pw.println();
      pw.println("You might also try the \"-index\" command-line option to use less memory-intensive images:");
      pw.println("   \"java -jar heatmap.jar -index -gm [file]...\"");
      pw.println();

      pw.println("If running as an applet, try using Java Web Start mode from the launch page.");

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
