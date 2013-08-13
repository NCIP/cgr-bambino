package Ace2;

import java.util.*;

public class IntMapBenchmark {
  
  public static void main (String[] argv) {
    int positions = 1000000;
    int reads = 1000;
    int step = 1;

    if (argv.length == 0) {
      System.err.println("specify type");  // debug
      System.exit(1);
    }

    String type = argv[0];
    Base b1 = Base.valueOf('A');
    Base b2 = Base.valueOf('C');

    Funk.Timer t = new Funk.Timer(type);

    int cpos;
    if (type.equals("tree") || type.equals("hash")) {
      AbstractMap<Integer,BaseCounter3> map;
      if (type.equals("tree")) {
	map = new TreeMap<Integer,BaseCounter3>();
      } else {
	map = new HashMap<Integer,BaseCounter3>();
      }

      for (cpos = 1; cpos < positions; cpos += step) {
	BaseCounter3 bc = map.get(cpos);
	if (bc == null) map.put(cpos, bc = new BaseCounter3());
	bc.add_base(b1, null, true);
	bc.add_base(b2, null, true);
      }
    } else if (type.equals("simpleint") || type.equals("pagedint")) {
      IntMap map;
      if (type.equals("simpleint")) {
	map = new SimpleIntMap(positions);
      } else {
	map = new PagedIntMap(positions);
      }
      for (cpos = 1; cpos < positions; cpos += step) {
	BaseCounter3 bc = (BaseCounter3) map.get(cpos);
	if (bc == null) map.put(cpos, bc = new BaseCounter3());
	bc.add_base(b1, null, true);
	bc.add_base(b2, null, true);
      }
     } else {
      System.err.println("unknown type");  // debug
    }
    
    t.finish();

  }
  
}