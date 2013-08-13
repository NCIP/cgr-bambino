package Ace2;

public class MemoryMonitor {
  private float STRESSED_FREE_MEMORY_FRACTION = 0.005f;
  //  private int SUSTAINED_STRESS_TIME = 3000;
  private int SUSTAINED_STRESS_TIME = 1500;
  private Runtime rt = Runtime.getRuntime();
  private long max_memory = rt.maxMemory();
  private long start_stress_time = 0;

  public MemoryMonitor() {
  }
  
  public boolean is_stressed() {
    //
    // does the runtime appear to be running very low on memory?
    //
    // max RAM available to java (totalMemory() is total on machine)
    long free_memory = rt.freeMemory();
    //      long total_memory = rt.totalMemory();

    float free_fraction = (float) free_memory / max_memory;
      
    boolean stressed = free_fraction < STRESSED_FREE_MEMORY_FRACTION;

    //      System.err.println("monitor: max=" + max_memory + " free="+free_memory + " free_pct="+free_fraction + " stress="+stressed);
    return stressed;
  }

  public boolean is_sustained_stressed () {
    boolean result = false;
    boolean stressed = is_stressed();
    long now = System.currentTimeMillis();
    if (stressed) {
      if (start_stress_time == 0) {
	start_stress_time = now;
      } else {
	result = (now - start_stress_time) >= SUSTAINED_STRESS_TIME;
      }
    } else {
      start_stress_time = 0;
    }

    //    System.err.println("stress:" + stressed + " sustained:" + result + " time:" + now);  // debug
    return result;
  }

  public void set_sustained_stress_time (int ms) {
    SUSTAINED_STRESS_TIME = ms;
  }

  

}