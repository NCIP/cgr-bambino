package Ace2;
// MNE 5/2012
//
// do some work in a (presumably temporary) directory then
// copy results back to target directory.  Can be used to
// perform work in a scratch filesystem, for example.
//
// Would be handier if Java supported the concept of a current working directory.
//   :/
//
// limitations:
//   - all files in working directory are moved to final directory
//     (even pre-existing ones)
//   - subdirectories not supported in working directory
//     (will not be copied to final directory)
//

import java.io.*;

public class WorkingDirectory {
  private File working_dir, final_dir;
  boolean created_working_dir;
  boolean use_mv;

  private static final long MAX_NIO_COPY_LENGTH = 2000000000;
  // 6/2012: NIO file copy operations seem to truncate where file sizes
  // approach signed int maximum, even though API seems to use longs.
  // WTF?

  public WorkingDirectory(File working_dir) throws IOException {
    this.working_dir = working_dir;
    setup(new File("."));
  }

  public WorkingDirectory(File working_dir, File final_dir) throws IOException {
    this.working_dir = working_dir;
    setup(final_dir);
  }

  private void setup (File final_dir) throws IOException {
    use_mv = false;
    this.final_dir = final_dir;

    if (working_dir.equals(final_dir)) {
      throw new IOException("working directory must be different from current directory");
    } else {
      dir_setup(final_dir);
      created_working_dir = dir_setup(working_dir);
    }
  }

  private boolean dir_setup (File dir) throws IOException {
    boolean created_dir = false;
    if (dir.exists()) {
      if (!dir.isDirectory()) throw new IOException(dir + " exists but is not a directory!");
    } else {
      if (dir.mkdir()) {
	created_dir = true;
      } else {
	throw new IOException("directory " + dir + " doesn't exist and can't create");
      }
    }
    return created_dir;
  }

  public void finish() throws IOException {
    File[] files = working_dir.listFiles();
    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
	if (files[i].isDirectory()) {
	  throw new IOException ("WorkingDirectory ERROR: subdirectories not supported: " + files[i]);  // debug
	} else {
	  File f_final = new File(final_dir, files[i].getName());

	  boolean used_mv = false;
	  if (use_mv || files[i].length() > MAX_NIO_COPY_LENGTH) {
	    String[] cmd = new String[4];
	    cmd[0] = "env";
	    cmd[1] = "mv";
	    cmd[2] = files[i].getCanonicalPath();
	    cmd[3] = f_final.getCanonicalPath();
	    int exit = Funk.Sys.exec_command(cmd, true);
	    //	    System.err.println("exit="+exit);  // debug
	    used_mv = true;
	  } else {
	    NIOUtils.copyFile(files[i], f_final);
	  }

	  if (!f_final.exists()) {
	    throw new IOException(f_final + " doesn't exist!");
	  } else if (!used_mv) {
	    if (f_final.length() != files[i].length()) {
	      throw new IOException("ERROR: output file size mismatch!!");
	    } else {
	      // OK
	      files[i].delete();
	      // delete temp file after successful copy to final
	    }
	  }
	}
      }
    }

    if (created_working_dir) working_dir.delete();
  }

  public void set_use_mv (boolean v) {
    use_mv = v;
  }

  public File get_file (String bn) {
    return new File(working_dir, bn);
  }

  public static void main (String[] argv) {
    try {
      WorkingDirectory wd = new WorkingDirectory(new File("tmp"), new File("elsewhere"));
      wd.set_use_mv(true);

      File f1 = wd.get_file("file1");
      OutputStream os = new BufferedOutputStream(new FileOutputStream(f1));
      PrintStream ps = new PrintStream(os);
      ps.println("f1 line1");
      ps.println("f1 line2");
      ps.close();

      File f2 = wd.get_file("file2");
      os = new BufferedOutputStream(new FileOutputStream(f2));
      ps = new PrintStream(os);
      ps.println("f2 line1");
      ps.println("f2 line2");
      ps.close();

      wd.finish();
    } catch (Exception e) {
      System.err.println("ERROR: " + e);  // debug
    }
  }

}
