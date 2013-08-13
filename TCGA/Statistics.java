package TCGA;

import java.util.*;

public class Statistics {
  
  public static void main(String[] argv) {
    //    byte[] array1 = {1,2,3};
    //    byte[] array2 = {2,5,6};
    //    byte[] array1 = {1,1,1};
    //    byte[] array2 = {1,1,1};
    byte[] array1 = {2,2,2};
    byte[] array2 = {2,2,2};

    //    byte[] array1 = {0,0,-99,0,0};
    //    byte[] array2 = {0,0,0,-99,0};

    //    byte[] array1 = {1,1,1};
    //    byte[] array2 = {1,0,1};

    //    byte[] array1 = {1,2,3};
    // byte[] array2 = {4,5,6};

    //    byte[] array2 = {1,2,3};
    //    byte[] array1 = {-1,-2,-3};

    //byte[] array1 = {0,3,4,5};
    //byte[] array2 = {7,6,3,-1};
    // euclidean test

    if (false) {
      System.err.println("ED w/no limit=" + euclidean_distance(array1, array2, true, (byte) -99, (byte) 0));

      System.err.println("ED w/limit 2=" + euclidean_distance(array1, array2, true, (byte) -99, (byte) 2));
    } else {
      try {
	double r = pearson_r(array1, array2, true, (byte) -99,
			     //			     true
			     false
			     );
	System.err.println("r="+r);  // debug
      } catch (Exception e) {
	System.err.println("error:"+e);  // debug
      }
    }
  }

  public static double euclidean_distance (byte[] x, byte[] y, boolean invalid_filter,
					   byte invalid_value, byte limiter) throws ArithmeticException {
    // http://people.revoledu.com/kardi/tutorial/similarity/EuclideanDistance.html
    int usable = 0;
    int total = 0;
    int i;
    byte xv,yv;

    if (limiter > 0) {
      // limit maximum allowed positive and negative distances.
      // used e.g. for copy numbers -- differences between copy number of 2 and 6
      // are less interesting than between -2 and +2
      // (same absolute difference of 4, but the latter is much more interesting
      // than the former)
      for (i=0; i < x.length; i++) {
	if (invalid_filter ? x[i] != invalid_value && y[i] != invalid_value : true) {
	  xv = x[i];
	  yv = y[i];
	  if (xv > limiter) {
	    xv = limiter;
	  } else if (xv < - limiter) {
	    xv = (byte) (- limiter);
	  }
	  if (yv > limiter) {
	    yv = limiter;
	  } else if (yv < - limiter) {
	    yv = (byte) (- limiter);
	  }

	  total += Math.pow(xv - yv, 2);
	  usable++;
	}
      }
    } else {
      for (i=0; i < x.length; i++) {
	if (invalid_filter ? x[i] != invalid_value && y[i] != invalid_value : true) {
	  total += Math.pow(x[i] - y[i], 2);
	  usable++;
	}
      }
    }
    if (usable == 0) {
      throw new ArithmeticException("no computable values");
    } else {
      return Math.sqrt(total);
    }
  }


  public static double pearson_r (byte[] x, byte[] y) throws ArithmeticException {
    return pearson_r(x, y, false, (byte) 0, false);
  }

  public static double pearson_r (byte[] x, byte[] y,
				  boolean invalid_filter, byte invalid_value,
				  boolean nan_hack) throws ArithmeticException {
    // http://davidmlane.com/hyperstat/A56626.html

    if (x.length != y.length) throw new ArithmeticException("pearson_r: array length mismatch");
    
    double Ex = 0;
    double Ey = 0;
    double Exy = 0;
    double Ex2 = 0;
    double Ey2 = 0;
    int n = 0;
    int i;
    double xv,yv;
    for (i=0; i < x.length; i++) {
      if (invalid_filter ? x[i] != invalid_value && y[i] != invalid_value : true) {
	xv = x[i];
	yv = y[i];
	if (false) {
	  // maybe hack zero-value cases to give them a valid r distance??
	  // ...Bad Idea
	  if (xv == 0) xv = 0.000001;
	  if (yv == 0) yv = 0.000001;
	}

	Ex += xv;
	Ey += yv;
	Exy += xv * yv;
	Ex2 += xv * xv;
	Ey2 += yv * yv;
	n++;
      }
    }

    if (n == 0) throw new ArithmeticException("pearson_r: no valid values");

//      double num = (Exy - (Ex * Ey) / n);
//      double x_part = Ex2 - (Ex * Ex) / n;
//      double y_part = Ey2 - (Ey * Ey) / n;
//      System.err.println("Exy="+Exy);  // debug
//      System.err.println("Ex="+Ex);  // debug
//      System.err.println("Ex2="+Ex2);  // debug
//      System.err.println("Ey="+Ey);  // debug
//      System.err.println("Ey2="+Ey2);  // debug
//      System.err.println("N="+n);  // debug
//      System.err.println("num="+num);  // debug
//      System.err.println("x_part="+x_part);  // debug
//      System.err.println("y_part="+y_part);  // debug
    
    double r = (Exy - (Ex * Ey) / n) / Math.sqrt(
						 (Ex2 - (Ex * Ex) / n) *
						 (Ey2 - (Ey * Ey) / n)
						 );

    if ((new Double(r)).equals(new Double(0.0/0.0))) {
      if (nan_hack) {
	//
	// hack 
	//
	boolean identical = true;
	if (invalid_filter) {
	  for (i=0; i < x.length; i++) {
	    if (invalid_filter ? x[i] != invalid_value && y[i] != invalid_value : true) {
	      if (x[i] != y[i]) {
		identical = false;
		break;
	      }
	    }
	  }
	} else {
	  identical = Arrays.equals(x,y);
	}

	if (identical) {
	  r = 1;
	} else {
	  throw new ArithmeticException("pearson_r: NaN result");
	}
      } else {
	throw new ArithmeticException("pearson_r: NaN result");
      }
    }

    //    System.err.println("result="+r);  // debug
    return r;
  }
  

}
