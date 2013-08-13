package Ace2;

public class PagedIntMap extends IntMap {
  private Object[][] pages;

  public PagedIntMap (int max_value) {
    super(max_value);
    setup();
  }

  public PagedIntMap (int max_value, int page_size) {
    super(max_value, page_size);
    setup();
  }
  
  private void setup() {
    int max_page_index = MAX_VALUE / PAGE_SIZE;
    pages = new Object[max_page_index + 1][];
    //    System.err.println("pageslen="+pages.length);  // debug
  }

  public void put (int key, Object value) {
    int page_index = key / PAGE_SIZE;
    Object[] bucket = pages[page_index];
    if (bucket == null) pages[page_index] = bucket = new Object[PAGE_SIZE];
    bucket[key % PAGE_SIZE] = value;
  }

  public Object get (int key) {
    //    System.err.println("index for " + key + ": page=" + (key / PAGE_SIZE) + " index=" + (key % PAGE_SIZE));  // debug
    Object[] bucket = pages[key / PAGE_SIZE];
    return bucket == null ? null : bucket[key % PAGE_SIZE];
  }

  public void remove (int key) {
    Object[] bucket = pages[key / PAGE_SIZE];
    if (bucket != null) bucket[key % PAGE_SIZE] = null;
  }

  public void free_through (int key) {
    int first_freeable_page = FREED_THROUGH / PAGE_SIZE;
    super.free_through(key);
    int last_used_page_index = key / PAGE_SIZE;
    
    for (int pi = first_freeable_page; pi < last_used_page_index; pi++) {
      //      System.err.println("free page index " + pi);  // debug
      pages[pi] = null;
    }
  }

  public void bucket_debug() {
    for (int i = 0; i < pages.length; i++) {
      System.err.println("page " + i + ": " + pages[i]);  // debug
    }
  }

  public static void main (String[] argv) {
    int max_count = 250000000;
    int page_size = 65536;

    PagedIntMap map = new PagedIntMap(max_count, page_size);

    int free_ptr = -10000;
    for (int i = 0; i < max_count; i++, free_ptr++) {
      if (i % 250000 == 0) System.err.println("at " + i);  // debug
      map.put(i, Integer.valueOf(i));
      if (free_ptr >= 0) {
	map.free_through(free_ptr);
      }
    }

//     map.bucket_debug();
//     map.free_through(20);
//     map.bucket_debug();
//     map.free_through(33);

try {
System.out.println("killing time...");
Thread.sleep(50000);
} catch (InterruptedException e) {}



  }

}
