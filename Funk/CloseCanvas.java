package Funk;

import java.awt.*;
import java.awt.event.*;

public class CloseCanvas extends Canvas implements MouseListener {
  // watch for mouse clicks in upper-right corner, close parent frame
  // if seen.  Nice for window managers without a window close button
  // (fvwm 1.x)

  //  int x,y;

  public CloseCanvas () {
    addMouseListener(this);
  }

  final static int X_W = 10;
  final static int X_H = 10;

//   public void paint (Graphics g) {
//     update(g);
//   }

//   public void update (Graphics g) {
//     Dimension d = size();
//     if (x > d.width - X_W && y < X_H) {
//       g.setColor(Color.black);
//       g.fillRect(d.width - X_W,0,d.width,X_H);
//       g.setColor(Color.white);
//       g.drawLine(d.width - X_W, 0, d.width, X_H);
//       g.drawLine(d.width, 0, d.width - X_W, X_H);
//       System.out.println("yow");  // debug
//     }
//   }


  // begin MouseListener stubs
  public void mouseDragged(MouseEvent e) {}
  public void mousePressed(MouseEvent e) {}
  public void mouseMoved(MouseEvent e) {};
  
  public void mouseClicked(MouseEvent e) {
    if (e.getX() > getSize().width - X_W &&
	e.getY() < X_H) {
      // click in far upper-right corner = close
      Frame f = Funk.Gr.getFrame(this);
      f.setVisible(false);
      f.dispose();
    }
  };
  
  public void mouseReleased(MouseEvent e) {};
  public void mouseEntered(MouseEvent e) {};
  public void mouseExited(MouseEvent e) {};
  // end MouseListener stubs

//   public static void main (String argv[]) {
//     // debug
//     Frame f = new Frame();
//     f.add(new CloseCanvas());
//     f.pack();
//     f.show();
//     f.setSize(800,600);
//   }

}
