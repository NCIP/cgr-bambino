package Ace2;

import java.sql.*;
import java.util.prefs.*;
import java.util.*;
import java.security.MessageDigest;

public class JDBCCache {
  private String DB_SERVER = "my_server";
  private String DB_DATABASE = "my_database";
  private String DB_USER = "my_user";
  private String DB_PASSWORD = "my_password";

  public static String UCSC_DB_SERVER = "genome-mysql.cse.ucsc.edu";
  public static String UCSC_DB_DATABASE = "hg18";
  public static String UCSC_DB_USERNAME = "genome";
  public static String UCSC_DB_PASSWORD = "";
  public static String UCSC_SNP_TABLE = "snp129";

  private Preferences cache;
  private ArrayList<HashMap<String,String>> results;
  private static final String KEY_DELIM = "\t";
  //  private static final String NULL_VALUE = "";
  private static final String KEY_TABLE_COLS = "__cols";

  private static final String VALUE_NULL_SET = "___NULL_SET___";

  private Connection connection = null;

  public JDBCCache() {
    setup();
  }

  public static JDBCCache get_ucsc_genome_client() {
    //
    // UCSC public server (http://genome.ucsc.edu/FAQ/FAQdownloads#download29)
    //
    JDBCCache c = new JDBCCache();
    c.set_db_server(UCSC_DB_SERVER);
    c.set_db_database(UCSC_DB_DATABASE);
    c.set_db_user(UCSC_DB_USERNAME);
    c.set_db_password(UCSC_DB_PASSWORD);
    return c;
  }

  public void set_db_server (String s) {
    DB_SERVER = s;
  }
  public void set_db_database (String s) {
    DB_DATABASE = s;
  }
  public void set_db_user (String s) {
    DB_USER = s;
  }
  public void set_db_password (String s) {
    DB_PASSWORD = s;
  }
  
  private void setup() {
    cache = Preferences.userNodeForPackage(this.getClass());
  }

  private Connection get_connection() throws SQLException {
    if (connection == null) {
      String cs = "jdbc:mysql://" + DB_SERVER + "/" + DB_DATABASE + "?user=" + DB_USER;
      if (DB_PASSWORD != null && DB_PASSWORD.length() > 0) {
	cs = cs.concat("&password=" + DB_PASSWORD);
      }
      System.err.println("cs="+cs);  // debug
      connection = DriverManager.getConnection(cs);
    }
    return connection;
  }

  public byte[] string2ba (String ins) {
    char[] input = ins.toCharArray();
    byte[] output = new byte[input.length];
    for (int i=0; i < input.length; i++) {
      output[i] = (byte) input[i];
    }
    return output;
  }

  public synchronized ArrayList<HashMap<String,String>> query (String query) throws Exception {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.reset();
      md5.update(string2ba(DB_SERVER));
      md5.update(string2ba(DB_DATABASE));
      md5.update(string2ba(query));
      String key_base = new String(md5.digest());
      
      results = new ArrayList<HashMap<String,String>>();
      int ri = 0;
      boolean have_cache = false;

      ArrayList<String> columns = null;
      while (true) {
	String key = key_base + KEY_DELIM + ri++;
	String value = cache.get(key, "");
	//	System.err.println("key="+key + " value="+value + " hit=" + (value == null ? "n" : "y"));  // debug
	if (value.length() == 0) {
	  // no more data
	  break;
	} else if (value.equals(VALUE_NULL_SET)) {
	  // original query returned a null value, treat as empty result
	  // (otherwise we will continually re-query, never getting results and slowing application)
	  System.err.println("JDBC cache: null result set!");  // debug
	  have_cache = true;
	  break;
	} else {
	  //
	  // cache hit
	  //
	  have_cache = true;
	  String[] f = value.split(KEY_DELIM);
	  if (columns == null) {
	    // first entry: column names
	    columns = new ArrayList<String>();
	    for (int i=0; i < f.length; i++) {
	      columns.add(f[i]);
	    }
	  } else {
	    // rows
	    HashMap<String,String> row = new HashMap<String,String>();
	    //	  System.err.println("f size="+f.length);  // debug
	    for (int i=0; i < f.length; i++) {
	      row.put(columns.get(i), new String(f[i]));
	    }
	    results.add(row);
	  }
	}
      }

      if (!have_cache) {
	//
	// need to query database
	//

	System.err.println("database query needed");  // debug
	Connection c = get_connection();
	Statement st = c.createStatement();
	if (st.execute(query)) {
	  ResultSet rs = st.getResultSet();

	  //
	  //  column setup:
	  //
	  ResultSetMetaData meta = rs.getMetaData();
	  int cc = meta.getColumnCount();
	  columns = new ArrayList<String>();
	  for (int i=1; i <= cc; i++) {
	    columns.add(meta.getColumnName(i));
	  }

	  boolean need_headers = true;
	  //
	  //  fetch database results:
	  //
	  int row_count = 0;
	  while (rs.next()) {
	    row_count++;
	    HashMap<String,String> row = new HashMap<String,String>();
	    String value;
	    ArrayList<String> raw = new ArrayList<String>();

	    if (true) {
	      for (int i=1; i <= cc; i++) {
		value = new String(rs.getString(i));
		raw.add(value);
		row.put(columns.get(i - 1), value);
	      }
	    } else {
	      // old method: fetch data by column name.
	      // this catches fire, explodes, and falls over when running a 
	      // "show tables" command.
	      for (String col : columns) {
		value = new String(rs.getString(col));
		raw.add(value);
		row.put(col, value);
	      }
	    }
	    
	    if (need_headers) {
	      ri = 0;
	      cache.put(key_base + KEY_DELIM + ri++, Funk.Str.join(KEY_DELIM, columns));
	      need_headers = false;
	    }

	    cache.put(key_base + KEY_DELIM + ri++, Funk.Str.join(KEY_DELIM, raw));

	    results.add(row);
	  }

	  if (row_count == 0) {
	    // null set returned
	    ri = 0;
	    cache.put(key_base + KEY_DELIM + ri++, VALUE_NULL_SET);
	  }

	  cache.remove(key_base + KEY_DELIM + ri++);
	  // ensure next cache entry is empty so parsing from cache will stop
	  // (prevent stale partially overwritten results from being loaded)

	  //	  System.err.println("row_count="+row_count);  // debug

	} else {
	  System.err.println("no results!");  // debug
	}
      }
//     } catch (Exception e) {
//       exception = e;
//       System.err.println("ERROR: " + e);  // debug
//       e.printStackTrace();
//    }
    return results;
  }

  public int get_result_count() {
    return results.size();
  }
  
  public ArrayList<HashMap<String,String>> get_results() {
    return results;
  }

  public void flush_cache() {
    // flush all cached entries
    try {
      String[] keys = cache.keys();
      for (int i=0; i < keys.length; i++) {
	cache.remove(keys[i]);
      }
    } catch (BackingStoreException e) {
    }
  }

  public static void main (String[] argv) {
    JDBCCache ucsc = JDBCCache.get_ucsc_genome_client();
    if (argv.length == 1 && argv[0].equals("-flush")){ 
      System.err.println("flushing cache");  // debug
      ucsc.flush_cache();
    } else {
      try {
	ArrayList<HashMap<String,String>> results = ucsc.query("select * from refGene where name2=\"tp53\"");
	for (HashMap<String,String> row : results) {
	  System.err.println(row.get("name2"));  // debug
	}
      
	System.err.println("count=" + ucsc.get_result_count());  // debug
      } catch (Exception e) {
	System.err.println("ERROR: "+ e);  // debug
	e.printStackTrace();
      }
    }

  }


}