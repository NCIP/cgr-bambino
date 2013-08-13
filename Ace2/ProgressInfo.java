package Ace2;

public class ProgressInfo {
  int processed = 0;
  int total_processed = 0;
  int total = 0;

  public void add_processed (int amount) {
    processed = amount;
    total_processed += amount;
  }

  public int get_percent() {
    int result = 0;
    if (total > 0) {
      float percent = (float) total_processed / total;
      result = (int) (percent * 100);
    }
    return result;
  }
  
}