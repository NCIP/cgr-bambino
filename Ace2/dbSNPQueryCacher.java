package Ace2;

import java.util.*;

public class dbSNPQueryCacher implements dbSNPQuery {
  // caching layer for dbSNPQuery implementations.
  // use if underlying implementation is very expensive (database queries, etc.)
  private dbSNPQuery query;
  private HashMap<String,Boolean> cache;
  int query_count;
  
  private static final int CACHE_RESET_INTERVAL = 1000;

  public dbSNPQueryCacher (dbSNPQuery query) {
    this.query = query;
    reset_cache();
  }

  private void reset_cache () {
    //    System.err.println("cache reset");  // debug
    query_count = 0;
    cache = new HashMap<String,Boolean>();
  }

  public boolean snp_matches (int base_number, Base b1, Base b2) {
    if (++query_count % CACHE_RESET_INTERVAL == 0) reset_cache();
    String key = base_number + b1.toString() + b2.toString();
    boolean result = false;
    Boolean hit = cache.get(key);
    if (hit != null) {
      //      System.err.println("cache hit");  // debug
      result = hit;
    } else {
      result = query.snp_matches(base_number, b1, b2);
      cache.put(key, Boolean.valueOf(result));
    }

    return result;
  }

}
