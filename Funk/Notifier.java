// adds a periodic update feature to Observable.
// usually used for occasional notification of file loading progress.

package Funk;

public class Notifier extends java.util.Observable {
  private int chunk_size;
  private int next_notify;

  private static final int DEFAULT_CHUNK_SIZE = 16384;

  public Notifier () {
    next_notify = this.chunk_size = DEFAULT_CHUNK_SIZE;
  }

  public Notifier (int chunk_size) {
    this.chunk_size = next_notify = chunk_size;
  }

  public boolean notify_check (int i) {
    if (i > next_notify) {
      next_notify = i + chunk_size;
      setChanged();
      notifyObservers(new Integer(i));
      try {
	Thread.sleep(0);
      } catch (InterruptedException e) {}
      return true;
    } else {
      return false;
    }
  }
}
