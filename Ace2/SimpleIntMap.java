package Ace2;

public class SimpleIntMap extends IntMap {
  private Object[] array;

  public SimpleIntMap (int max_value) {
    super(max_value);
    setup();
  }

  public SimpleIntMap (int max_value, int page_size) {
    super(max_value, page_size);
    setup();
  }
  
  private void setup() {
    array = new Object[MAX_VALUE + 1];
  }

  public void put (int key, Object value) {
    array[key] = value;
  }

  public Object get (int key) {
    return array[key];
  }

  public void remove (int key) {
    array[key] = null;
  }


}
