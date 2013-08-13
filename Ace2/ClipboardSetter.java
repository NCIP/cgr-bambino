package Ace2;
// adapted from http://www.javapractices.com/topic/TopicAction.do?Id=82

import java.awt.datatransfer.*;
import java.awt.Toolkit;
import java.io.*;

public class ClipboardSetter implements ClipboardOwner {
  public static void main (String...  argv) {
    ClipboardSetter textTransfer = new ClipboardSetter();

    //display what is currently on the clipboard
    System.out.println("Clipboard contains:" + textTransfer.getClipboardContents() );

    //change the contents and then re-display
    textTransfer.setClipboardContents("blah, blah, blah");
    System.out.println("Clipboard contains:" + textTransfer.getClipboardContents() );
  }

  // begin ClipboardOwner stub
  public void lostOwnership(Clipboard clipboard, Transferable contents) {}
  // end ClipboardOwner stub


  /**
  * Place a String on the clipboard, and make this class the
  * owner of the Clipboard's contents.
  */
  public void setClipboardContents( String aString ){
    StringSelection stringSelection = new StringSelection( aString );
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents( stringSelection, this );
  }

  public void setClipboardContents (int v) {
    setClipboardContents(Integer.toString(v));
  }

  /**
  * Get the String residing on the clipboard.
  *
  * @return any text found on the Clipboard; if none found, return an
  * empty String.
  */
  public String getClipboardContents() {
    String result = "";
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    //odd: the Object param of getContents is not currently used
    Transferable contents = clipboard.getContents(null);
    boolean hasTransferableText =
      (contents != null) &&
      contents.isDataFlavorSupported(DataFlavor.stringFlavor)
    ;
    if ( hasTransferableText ) {
      try {
        result = (String)contents.getTransferData(DataFlavor.stringFlavor);
      }
      catch (UnsupportedFlavorException ex){
        //highly unlikely since we are using a standard DataFlavor
        System.out.println(ex);
        ex.printStackTrace();
      }
      catch (IOException ex) {
        System.out.println(ex);
        ex.printStackTrace();
      }
    }
    return result;
  }
} 
