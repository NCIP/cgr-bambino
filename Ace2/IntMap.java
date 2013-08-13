package Ace2;
// subset of Map for int[], hopefully faster than HashMap.put(autobox(int))?
// mne 3/2010
// FIX ME: TYPING (how to do that??)

public abstract class IntMap {
  protected int MAX_VALUE, FREED_THROUGH;
  protected int PAGE_SIZE = 65536;

  public IntMap (int max_value) {
    FREED_THROUGH = 0;
    MAX_VALUE = max_value;
  }

  public IntMap (int max_value, int page_size) {
    FREED_THROUGH = 0;
    MAX_VALUE = max_value;
    PAGE_SIZE = page_size;
  }

  public abstract void put (int key, Object value);
  public abstract Object get (int key);
  public abstract void remove (int key);

  public void free_through (int key) {
    // intended for use when iterating through a window of data,
    // when completely free of a region.
    //    System.err.println("free_through: deleting from " + FREED_THROUGH + " => " + key);  // debug
    //    new Exception().printStackTrace();
    for (int i = FREED_THROUGH; i <= key; i++) {
      remove(i);
    }
    FREED_THROUGH = key;
  }

}
