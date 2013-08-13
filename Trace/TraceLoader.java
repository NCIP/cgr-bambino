package Trace;

import java.util.*;

public class TraceLoader extends Observable implements Observer {
  // supervisor / worker
  
  public static boolean LOCAL_MODE = false;

  private static int MAX_SIMULTANEOUS_REQUESTS = 3;

  private int slots_available = MAX_SIMULTANEOUS_REQUESTS;
  private boolean finished = false;
  private Iterator<String> iterator;
  private int total_job_size, jobs_left;
  private TraceServerClient tsc;

  public TraceLoader (Collection<String> traces) {
    fetch(traces);
  }

  public TraceLoader (Collection<String> traces, Observer o) {
    addObserver(o);
    fetch(traces);
  }

  public void fetch (Collection<String> traces) {
    tsc = new TraceServerClient();
    total_job_size = jobs_left = traces.size();
    iterator = traces.iterator();
    fill_slots();
  }

  private void fill_slots () {
    while (slots_available > 0 && iterator.hasNext()) {
      String trace = iterator.next();
      //      System.err.println(trace);
      //      System.err.println("LOCAL?:" + LOCAL_MODE);  // debug

      if (LOCAL_MODE) {
        // trace on local filesystem
        TraceFile t = new TraceFile(trace, this);
      } else {
        // trace on trace server
        tsc.get_trace(Integer.parseInt(trace), this);
      }
      slots_available--;
    }
  }

  public static void main (String [] argv) {
    StreamDelegator.set_local(true);
    Vector<String> v = new Vector<String>();
    v.add("ye50d01.s1");
    v.add("ye50d01.r1");
    new TraceLoader(v);
  }

  public boolean is_finished () {
    return jobs_left == 0;
  }

  public int get_percent_finished () {
    return (int) (((total_job_size - jobs_left) * 100) / total_job_size);
  }

  private void receive_trace (TraceFile t) {
    jobs_left--;
    //      System.err.println("finished?: " + is_finished());  // debug

    setChanged();
    notifyObservers(t);

    if (false) {
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
      }
    }

    slots_available++;
    fill_slots();
  }

  public void update (Observable o, Object arg) {
    //    System.err.println("UPDATE:" + o + " " + arg);  // debug

    if (o instanceof TraceFile && arg == null) {
      // trace has finished loading
      TraceFile t = (TraceFile) o;
      //      System.err.println("completed loading: " + t.name);  // debug
      receive_trace(t);
    }
  }


}
