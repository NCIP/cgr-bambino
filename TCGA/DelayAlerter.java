package TCGA;
import java.util.*;
import javax.swing.JOptionPane;

public class DelayAlerter implements Observer,Runnable {

  private int delay_ms;
  private String message;
  private long start_time;
  private boolean finished;
  
  public DelayAlerter (int delay_ms, String message) {
    start_time = System.currentTimeMillis();
    this.delay_ms = delay_ms;
    this.message = message;
    finished = false;
    new Thread(this).start();
  }

  public void run() {
    try {
      Thread.sleep(delay_ms);
      if (!finished) {
	JOptionPane.showMessageDialog(null, message);
      }
    } catch (Exception e) {
    }
  }

  public void update (Observable o, Object arg) {
    finished = true;
  }

}