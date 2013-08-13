package Funk;
// helper class for constructing simple menus.
//
// mne 8/2006

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.Component;

import java.util.ArrayList;

public class MenuBuilder {
  private JMenuBar jmb;
  private ArrayList<JMenu> jmenu_stack;
  // stack of current menu and any submenus
  private ArrayList<ActionListener> listener_stack;
  
  public MenuBuilder (JFrame jf, ActionListener al) {
    // automatically create a new menu bar for the given frame
    jf.setJMenuBar(jmb = new JMenuBar());
    setup(al);
  }

  public MenuBuilder (JMenuBar jmb, ActionListener al) {
    // pre-existing menu bar
    this.jmb = jmb;
    setup(al);
  }

  private void setup (ActionListener al) {
    listener_stack = new ArrayList<ActionListener>();
    push_listener(al);
  }

  public void push_listener (ActionListener al) {
    listener_stack.add(al);
  }

  public void pop_listener () {
    int size = listener_stack.size();
    if (size > 0) listener_stack.remove(size - 1);
  }

  public ActionListener get_current_listener() {
    int size = listener_stack.size();
    return size == 0 ? null : listener_stack.get(size - 1);
  }

  public void start_menu(JMenu jm) {
    jmenu_stack = new ArrayList<JMenu>();
    // clear stack
    jmenu_stack.add(jm);
    jmb.add(jm);
  }

  public void start_menu(String label) {
    start_menu(new JMenu(label));
  }

  public void start_menu(String label, int mnemonic) {
    JMenu jm = new JMenu(label);
    jm.setMnemonic(mnemonic);
    start_menu(jm);
  }
  
  public void start_submenu(String label) {
    JMenu jm = new JMenu(label);
    get_current_jmenu().add(jm);
    jmenu_stack.add(jm);
  }

  public void start_submenu(String label, int mnemonic) {
    JMenu jm = new JMenu(label);
    jm.setMnemonic(mnemonic);
    get_current_jmenu().add(jm);
    jmenu_stack.add(jm);
  }
  
  public void end_submenu() {
    jmenu_stack.remove(jmenu_stack.size() - 1);
  }

  public void add(Component c) {
    get_current_jmenu().add(c);
    ActionListener al = get_current_listener();
    if (c instanceof JMenuItem && al != null)
      ((JMenuItem) c).addActionListener(al);
    // AbstractButton instead??
    // only add listener for actual menu items (as opposed to
    // JSeparators, etc.)
  }

  public void add(AbstractButton ab, int mnemonic) {
    ab.setMnemonic(mnemonic);
    add(ab);
  }

  public void add(String label) {
    add(new JMenuItem(label));
  }

  public void add(String label, int mnemonic) {
    add(new JMenuItem(label, mnemonic));
  }
  
  private JMenu get_current_jmenu() {
    return jmenu_stack.get(jmenu_stack.size() - 1);
  }

}
