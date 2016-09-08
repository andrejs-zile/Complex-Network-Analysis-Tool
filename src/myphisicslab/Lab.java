/*
  File: Lab.java

  Part of the www.MyPhysicsLab.com physics simulation applet.
  Copyright (c) 2001  Erik Neumann

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

  Contact Erik Neumann at erikn@MyPhysicsLab.com or
  610 N. 65th St. Seattle WA 98103

  Comments by Andrejs Zile:

  // There have been slight modifications to the code to allow to run Network2D class simulations.
  // Also the list of simulations has been reduced for simplicity, but the structure of this file
  // has not changed that significantly to not be able to put those classes back.

  IMPORTANT
  When first building this class, make sure to mark it as Application and not Applet.
  Applet will cause error.

*/
package myphisicslab;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/* Lab creates the window and simulation and animation thread.
  When run as an application (not in a browser) there is a pop-up menu for
  selecting which simulation to run.
  When run as an application main() is the first procedure to be called; we
  create a window (Frame) and instantiate the Lab applet.
  When run as an applet, the browser creates the window and instantiates
  the applet.
*/
public class Lab extends JApplet implements ItemListener, ActionListener {

  private SimThread timer = null;
  private Simulation sim = null;
  private JComboBox simMenu = null;  // JComboBox is equiv of awt.Choice
  private boolean browserMode = true;  // assume we are running under a browser
  private boolean gameMode = false;
  private int currentSimMenuItem = -1;  // -1 means no sim selected
  private static JFrame frame;
  
  public static void main(String[] args) {
		// see Graphic Java Mastering the JFC Volume II: Swing, p. 38 
		// about "Applet/Application Combinations"... their code is a bit different to this!
    //JFrame frame = new SimFrame(new Lab(false));
		JApplet applet = new Lab(false);
		frame = new SimFrame(applet);



    //frame.pack(); // size the frame
		frame.setContentPane(applet.getContentPane());
        frame.setVisible(true);


		applet.init();
  }

  public Lab() {
    // do nothing in the constructor, because the applet
    // is not 'loaded' yet by the browser... no window, etc.
		// try to detect old MSIE java, display a message in that case...
		//Utility.println("java.version = "+System.getProperty("java.version"));
		//Utility.println("java.vendor = "+System.getProperty("java.vendor"));
		//Utility.println("java.vm.vendor = "+System.getProperty("java.vm.vendor"));
		//Utility.println("java.vm.name = "+System.getProperty("java.vm.name"));

    //Build the first menu.
    //Source: https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html


  }

  public Lab(boolean browserMode) {
		this();
    this.browserMode = browserMode;
  }

  /** init is called by the browser or applet viewer to inform this applet
  that it has been loaded into the system. It is always called before
  the first time that the start method is called.
  */
  public void init() {
		String s;
    System.out.println("base MyPhysicsLab 0.9.1_04");
    System.out.println("starting Complex-Network-Analysis-Tool 1.0");

    //getContentPane().setBackground(Color.white);
    // if app mode, create menu for selecting sim
    if (!browserMode)
      createMenu();
      createTopMenu();
		// The "game" parameter is used for initializing the Thruster5 simulation.
//    if ((s = getParameter("game")) != null) {
//      gameMode = s.equalsIgnoreCase("true");
//		} else
			gameMode = false;
    // read parameter telling what sim to run
    // see class SimFrame, which answers these questions for the stand-alone app.
    s = getParameter("simulation");
    System.out.println(s);
    if (s != null) {
      for (int i=0; i<sims.length; i++)
        if (s.equalsIgnoreCase(sims[i])) {
          startSim(i);
          break;
        }
    }
    if (sim == null)
      throw new IllegalStateException ("unable to create simulation "+s);
    else
      Utility.println("Simulation "+s+" created.");
    s = getParameter("showControls");
    if (s!=null) sim.showControls(s.equalsIgnoreCase("true"));
    s = getParameter("showGraph");
    if (s!=null) sim.showGraph(s.equalsIgnoreCase("true"));
    if (sim.getGraph() != null) {
      s = getParameter("graphXVar");
      if (s!=null) sim.getGraph().setXVar(Integer.parseInt(s));
      s = getParameter("graphYVar");
      if (s!=null) sim.getGraph().setYVar(Integer.parseInt(s));
      s = getParameter("graphMode");
      if (s!=null) {
        if (s.equalsIgnoreCase("dots"))
          sim.getGraph().setDrawMode(Graph.DOTS);
        else if (s.equalsIgnoreCase("lines"))
          sim.getGraph().setDrawMode(Graph.LINES);
        else
          Utility.println("Could not set graphMode to "+s);
      }
    }
    // Get initial setting simulation variables
    for (int i=0; i<sim.numVariables(); i++) {
      s = getParameter("variable"+i);
      if (s!=null)
        sim.setVariable(i, (new Double(s)).doubleValue());
    }
    // Get simulation parameters (like gravity, mass, spring stiffness, etc.)
    String[] params = sim.getParameterNames();
    for (int i=0; i<params.length; i++) {
      s = getParameter(params[i]);
      if (s!=null)
        sim.setParameter(params[i], (new Double(s)).doubleValue());
    }
  }

  private void createMenu() {
    simMenu = new JComboBox();
    for (int i=0; i<sims.length; i++)
      simMenu.addItem(sims[i]);
    simMenu.addItemListener(this);
    getContentPane().add(simMenu);
    //This is for user not to see the menu
    simMenu.setVisible(false);
  }

  /**
   * This method creates the menu to load the files into the simulator.
   *
   * Reference:
   * "How to Use Menus (The Java™ Tutorials > Creating a GUI With JFC/Swing > Using Swing Components)",
   * Docs.oracle.com, 2016. [Online].
   * Available: https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html.
   * [Accessed: 08- Sep- 2016].
   *
   * Implemented by Andrejs Zile
   */
  private void createTopMenu() {
    JMenu menu = new JMenu("File");
    menu.setMnemonic(KeyEvent.VK_A);
    menu.getAccessibleContext().setAccessibleDescription(
            "The only menu in this program that has menu items");
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(menu);

    //a group of JMenuItems
    JMenuItem menuItem = new JMenuItem("Import for visualisation",
            KeyEvent.VK_T);
    menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_1, ActionEvent.ALT_MASK));
    menuItem.getAccessibleContext().setAccessibleDescription(
            "To import file");
    menuItem.setActionCommand("import");
    menuItem.addActionListener(this);

    menu.add(menuItem);

    JMenuItem menuItem2 = new JMenuItem("Import for simulation",
            KeyEvent.VK_T);
    menuItem2.getAccessibleContext().setAccessibleDescription(
            "To import file NRT");
    menuItem2.setActionCommand("import-nrt");
    menuItem2.addActionListener(this);

    menu.add(menuItem2);

    frame.setJMenuBar(menuBar);
            //add(simMenu);
    menu.setVisible(true);
    menu.repaint();
    frame.repaint();
    frame.invalidate();
    frame.validate();
  }

  public String[] getSimNames() {
    // return a clone of the array to ensure no one can change our array
    return (String[])sims.clone();
  }

  //Only two simulations have been left.
  private String[] sims = {
    "Complex Network with Forced Vibrations",
    "Complex Network with Forced Vibrations NRT"
    };

  private void startSim(int simIndex) {
    Utility.println("startSim("+simIndex+")");

    if (sim != null) {
      Utility.println("stopping and shutting down "+sim);
      stop();
      sim.shutDown();  // hides all components of this sim
      sim = null;
      this.currentSimMenuItem = -1;
    }
    // if no sim is selected, then simIndex should be -1
    if (simIndex<0)
      return;
    System.out.println("starting simulation "+sims[simIndex]);
    //Switch between simulation, one is the simulation with visualisation
    //The second one is simulation for testing with visualisation (the false one)
    switch (simIndex) {
      case 0: sim = new Network2DSim(getContentPane(),8, true); break;
      case 1: sim = new Network2DSim(getContentPane(),8, false); break;
      default: sim = null;
    }
    this.currentSimMenuItem = simIndex; // remember which menu item is running
    Utility.println("created simulation "+sim);
    Utility.println(" with rootpane.isDoubleBuffered="+getRootPane().isDoubleBuffered());
    sim.setupControls();
    sim.setupGraph();
    getContentPane().setLayout(sim.makeLayoutManager());
    //getContentPane().setLayout(new FlowLayout());
    /* The validate method is used to cause a container to lay out its
    subcomponents again. It should be invoked when this container's
    subcomponents are modified (added to or removed from the container, or
    layout-related information changed) after the container has been displayed.
    */
    getContentPane().invalidate();
    getContentPane().validate();
    getContentPane().repaint();
    start();
    // get the menu to adjust... (perhaps menu should be a listener to this?)
    if (simMenu != null && simMenu.getSelectedIndex()!=simIndex)
      simMenu.setSelectedIndex(simIndex);
  }

  private void startSim(int simIndex, int numberOfNodes, Network network) {
    Utility.println("startSim("+simIndex+")");
    if (sim != null) {
      Utility.println("stopping and shutting down "+sim);
      stop();
      sim.shutDown();  // hides all components of this sim
      sim = null;
      this.currentSimMenuItem = -1;
    }
    // if no sim is selected, then simIndex should be -1
    if (simIndex<0)
      return;
    System.out.println("starting simulation "+sims[simIndex]);
    switch (simIndex) {
      case 0: sim = new Network2DSim(getContentPane(),numberOfNodes, network, true); break; //-- true stands for simulation with visualisation
      case 1: sim = new Network2DSim(getContentPane(),numberOfNodes, network, false); break; //-- false stands for simulation with visualisation for testing
      default: sim = null;
    }
    this.currentSimMenuItem = simIndex; // remember which menu item is running
    Utility.println("created simulation "+sim);
    Utility.println(" with rootpane.isDoubleBuffered="+getRootPane().isDoubleBuffered());
    sim.setupControls();
    sim.setupGraph();
    getContentPane().setLayout(sim.makeLayoutManager());
    //getContentPane().setLayout(new FlowLayout());
    /* The validate method is used to cause a container to lay out its
    subcomponents again. It should be invoked when this container's
    subcomponents are modified (added to or removed from the container, or
    layout-related information changed) after the container has been displayed.
    */
    getContentPane().invalidate();
    getContentPane().validate();
    getContentPane().repaint();
    start();
    // get the menu to adjust... (perhaps menu should be a listener to this?)
    if (simMenu != null && simMenu.getSelectedIndex()!=simIndex)
      simMenu.setSelectedIndex(simIndex);
  }

  public void itemStateChanged(ItemEvent event) {
    int i = simMenu.getSelectedIndex();
    // currentSimMenuItem should be the menu item index of the currently running sim.
    // only switch to a different simulation if that one is not yet running
    if (i!=this.currentSimMenuItem)
      startSim(simMenu.getSelectedIndex());
  }

  // called when user returns to browser page containing applet
  public void start() {
    Utility.println("Lab.start ");
    if (timer == null && sim != null) {
      timer = new SimThread(sim, 10);  // was 33
      timer.start();
    }
  }

  // called when user leaves browser page containing applet
  public void stop() {
    Utility.println("Lab.stop ");
    if (timer != null) {
      timer.interrupt();
      timer = null;  // destroys the thread
    }
  }

  /* printing EXPERIMENTAL August 2004
  public void printAll(Graphics g) {
    System.out.println("printAll called");
    //g.drawRect(10, 10, 100, 100);
    Component cnvs = getComponent(0);  // this should be the canvas
    int y = cnvs.getSize().height;
    g.clearRect(0,y,getSize().width,getSize().height-y); // clear area of controls
    cnvs.print(g);
  }*/

  /* Override update to NOT erase the background before painting, except for
    the area of the controls.
    We only want to clear the area where the controls are, not where
    the graph or simulation is (to avoid flicker, and avoid destroying graph). 
  public void update(Graphics g) {
    Component cnvs = getComponent(0);  // this should be the canvas
    int y = cnvs.getSize().height;
    g.clearRect(0,y,getSize().width,getSize().height-y); // clear area of controls
    //super.paint(g);
    paint(g);
  }
*/

  /* The following methods are designed to be called from JavaScript
     when running in a web browser. */
  public void setVar(int n, String value) {
    sim.setVariable(n, (new Double(value)).doubleValue());
  }

  public void setGraphXVar(int n) {
    Graph g = sim.getGraph();
    g.setXVar(n);
  }

  public void setGraphYVar(int n) {
    Graph g = sim.getGraph();
    g.setYVar(n);
  }

  public void setParameter(String name, String value) {
    sim.setParameter(name, (new Double(value)).doubleValue());
  }

  public void graphRepaint() {
    sim.getGraph().reset();
  }
  
  public void setSize(int width, int height) {
    Utility.println("Lab.setSize "+width+" "+height);
    super.setSize(width, height);
  }
  public void setSize(Dimension d) {
    Utility.println("Lab.setSize "+d);
    super.setSize(d);
  }

  /**
   * This method simply provides facility to import files into simulator.
   *
   * First one is to import the file for simulation with visualisation.
   * Second one is to import the file for simulation with visualisation for testing.
   *
   * Implemented by Andrejs Zile
   *
   * @param e
   */
  @Override
  public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand() == "import") {
        try {
          PajekFileLoader fileLoader = new PajekFileLoader();
          Network network = fileLoader.loadFile();
          int number = network.getNodeList().size();
          startSim(0,number, network);
        } catch (Exception e1) {

        }
      } else if (e.getActionCommand() == "import-nrt") {
        try {
          PajekFileLoader fileLoader = new PajekFileLoader();
          Network network = fileLoader.loadFile();
          int number = network.getNodeList().size();
          startSim(1,number, network);
        } catch (Exception e1) {

        }
      }
  }
}





