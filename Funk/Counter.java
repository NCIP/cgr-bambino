package Funk;

import java.util.Collection;

public class Counter {

  private int total,done;

  public Counter (int total) {
    this.total = total;
    done = 0;
  }

  public Counter (Collection c) {
    this.total = c.size();
    done = 0;
  }

  public void next () {
    done++;
  }

  public boolean complete () {
    return done >= total;
  }
  
  public int get_percent_complete () {
    return 100 - (int) (((total - done) * 100) / total);
  }


}
