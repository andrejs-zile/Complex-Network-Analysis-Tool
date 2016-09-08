/*
  File: Network2DSim.java

  This Class is to be used solely for simulation without animation. To achieve non real time calculations.

  Part of the www.MyPhysicsLab.com physics simulation applet.
  Copyright (c) 2001  Erik Neumann


  Modification of code by Andrejs Zile
  There are many changes involved:
   - added more methods for each evaluation
   - split existing evaluations, specifically evaluate method to make it more readable
   - added forced oscillations
   - changed variables, instead of matrix, list of edges and nodes are used
   - implemented JFreeChart to display results
   - created control panel to control simulations
   - many more variables have been added
   - methods that have been created by Andrejs Zile
     are added in comments, by each method
   - methods that are modified are marked with modified
   - For debugging uncomment the System.out.println lines.

*/

package myphisicslab;

import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtilities;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


/////////////////////////////////////////////////////////////////////////////////
public class Network2DSim extends Simulation implements ActionListener
{
  private ArrayList<Node> allNodesList;
  private ArrayList<Edge> allEdgesList;
  private double gravity = 0.0, damping=20.0, time=0.0, amplitude=0.0, frequency=0.0;
  private double minDispY[], maxDispY[];
  private double minDispY1 =0.0, maxDispY1 = 0.0, timeSplit = 0.0, timeSplitStart = 0.0;
  private JButton button_stop, button_frequency, button_import;
  private boolean simulationStart = false;
  private boolean simulationPositions = false;

  JButton destinationFolder, simulationStartBtn, simulationStop;
  JTextField simPasses, forceAmplitude, frequencyMax, timeMultiplier, timeStep, frequencyStep, dampingForce;
  JTextField sourceFilePath, destinationFilePath;
  JLabel passTracker, timeElapsed;
  String sourceNetworkPath = "";

  private static final String   DAMPING="damping",
                                GRAVITY="gravity",
                                AMPLITUDE="amplitude",
                                FREQUENCY="frequency";

  //Additions for molecule
  private double m_Left, m_Right, m_Top, m_Bottom;
  private CRect m_Walls;

  //this is used to calculate driven oscillation
  private long timer;
  // important that the params list of strings remains private, so can't
  // be overridden
  private String[] params = {DAMPING, GRAVITY, AMPLITUDE, FREQUENCY};

  private XYSeries objSeriesAverage, objSeriesCurrent;
  private double edgeEnergy = 0;
  private int passCount = 1;
  private int passLimit = 3; //always +1
  private double passTime = 10.0;
  private List<List> allSimulationValues = new ArrayList<>();
  public double frequencyLimit = 2.0;
  private NRTControlPanel controlPanel;
  private double incrementFrequency = 0.0125;
  private File destFilePath; //Destination file path
  private String sourceFileName;
  private JFreeChart objChart;
  private boolean nodesPositionedDebug = false;

  private long simulationStartTime;

  /**
   * Original constructor.
   * Modified by Andrejs Zile.
   *
   * @param container
   * @param nm
   * @param realTimeSet
     */
  public Network2DSim(Container container, int nm, boolean realTimeSet) {
    super(container, nm*4); //this needs to be taken into account when importing values
    super.realTime = realTimeSet;

    createControls();
    populateVars(nm);

    setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -6, 6, -6, 6,
        CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));
    double xx = 0, yy = -2, w = 0.5;

    //-------------------------------------------------------------------------------
    //--------This addition is to have dynamic number of masses and springs----------
    //-------------------------------------------------------------------------------
    //Dynamic option added which will have custom numbers
    allNodesList = new ArrayList<Node>(nm);
    for (int i=0; i<nm; i++) {
      allNodesList.add(i, new Node(xx-w/2, 0, w, w, CElement.MODE_CIRCLE, i));
      double posY = 1*i;
      allNodesList.get(i).m_Mass=5 * (i+1); //TEMPORARY TO RANDOMISE arbitrary mass for node
      allNodesList.get(i).setX1(1);
      allNodesList.get(i).setY1(posY);

      //-- This is to see how far atom has moved from center of molecule;
      //-- Initializing variables
      if (i%2 == 0) {
        allNodesList.get(i).setChargePositive();
        allNodesList.get(i).m_Color = Color.blue;
      }

      //if (super.realTime) //remove to enable visualisation
        cvs.addElement(allNodesList.get(i));
    }

    for (int i=0; i<allNodesList.size(); i++) {
      double posY = allNodesList.get(i).getY();
      double centerOfMolecule = getCenterOfMoleculeY();
      if (centerOfMolecule < (posY)) {
        allNodesList.get(i).minDispY = (posY) - centerOfMolecule;
        allNodesList.get(i).maxDispY = (posY) - centerOfMolecule;
      } else {
        allNodesList.get(i).minDispY = centerOfMolecule - (posY);
        allNodesList.get(i).maxDispY = centerOfMolecule - (posY);
      }
    }

    allEdgesList = new ArrayList<Edge>(nm-1);
    for (int i=0; i<nm-1; i++) {
      allEdgesList.add(i, new Edge(xx, yy, 1.0 * (i+1)/2, 0.3, i));
      allEdgesList.get(i).setX2(xx);
      allEdgesList.get(i).m_SpringConst=6 * (i+1); //TEMPORARY TO RANDOMISE
      allEdgesList.get(i).setWeight(0.5);
      allEdgesList.get(i).setNodeFrom(allNodesList.get(i));
      allEdgesList.get(i).setNodeTo(allNodesList.get(i+1));


      //if (super.realTime) //remove to enable visualisation
        cvs.addElement(allEdgesList.get(i));
    }

    initializeWalls();

    stopMotion();  // get to quiet state

    //timer to calculate time elapsed from start of run
    timer = (long)getTime();

    modifyObjects();

  }

  /**
   * Constructor for Network in 2D with Non Real Time calculations.
   *
   * Modified by Andrejs Zile.
   *
   * @param container - where panel of Graphical representation of network is placed.
   * @param nm - number of Nodes in the Network
   * @param network - Network that is imported through Pajek format data file
   * @param realTimeSet - if true then network is simulated as real time, false means that simulation is speeded up.
     */
  public Network2DSim(Container container, int nm, Network network, boolean realTimeSet) {
    //-- nm is number of items, and is multiplied by 4 to create an array.
    //-- which contains velocity and position of each node in both x and y direction.
    super(container, nm*4);
    //-- This sets that this is non real time calculation, which means that in Simulation.java
    //-- Appropriate filters are triggered, so that calculations are done quicker
    super.realTime = realTimeSet;

    //This deactivates animation of nodes
    if (!super.realTime)
      super.simulationActive = false;

    //-- Need to add to CVS to display some values that could be useful on display while running simulation without animation.

    sourceNetworkPath = network.getFilePath();
    sourceFileName = network.getFileName();

    createControls();

    //-- This is to populate list of names of each variable in vars array.
    populateVars(nm);

    setCoordMap(new CoordMap(CoordMap.INCREASE_DOWN, -6, 6, -6, 6,
            CoordMap.ALIGN_MIDDLE, CoordMap.ALIGN_MIDDLE));

    //-- Loads the network into variables from the network passed through constructor.
    initializeNetwork(network);

    //-- Box addition to canvas to have boundaries for animation
    initializeWalls();

    //-- Get to quiet state before simulation starts
    stopMotion();

    //-- Timer to set current time that will be used in calculations
    timer = (long)getTime();

    //-- Calculate initial positions and velocities of Nodes and Edges
    modifyObjects();

  }

  /**
   * UI for control panel.
   * This method mainly creates the new window for Control Panel.
   * This includes all buttons and fields.
   *
   *
   * Created by Andrejs Zile.
   */
  public void createControls() {
    Container mainContainer = cvs.getParent().getParent();
    //System.out.println(mainContainer.getComponentCount());

    JFrame controlsFrame = new JFrame("Simulation Control");
    controlsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    controlsFrame.setMinimumSize(new Dimension(500,500));

    if (mainContainer.getComponentCount()>1) {
      boolean found = false;
      for (Component component : mainContainer.getComponents()) {
        //System.out.println(component.getClass());
        //System.out.println(NRTControlPanel.class);
        if (component.getClass() == NRTControlPanel.class) {
          NRTControlPanel controlPanelOld = (NRTControlPanel)component;
          controlPanelOld.removeAll();
          controlPanel = controlPanelOld;
          controlPanel.revalidate();
          found = true;
        };
      }
      if (!found) controlPanel = new NRTControlPanel();
      //mainContainer.getComponent();
    }

    if (!super.realTime) {

      Dimension dimCont = mainContainer.getSize();
      dimCont = new Dimension((int)dimCont.getWidth()+50, (int)dimCont.getHeight()-100);
      //cvs.removeAll(); //remove to enable visualisation
      //System.out.println(mainContainer.getSize());

      sourceFilePath = new JTextField(sourceNetworkPath);
      sourceFilePath.setEditable(false);
      destinationFilePath = new JTextField("");
      destinationFilePath.setEnabled(false);

      forceAmplitude = new JTextField("5");
      frequencyMax = new JTextField("2.0");
      timeMultiplier = new JTextField("1.0");
      timeStep = new JTextField("10.0");
      frequencyStep = new JTextField("0.0125");
      simPasses = new JTextField("3");
      dampingForce = new JTextField("20.0");

      //Labels for seeing how far the simulation has gone
      timeElapsed = new JLabel("0 ms");
      passTracker = new JLabel("");

      destinationFolder = new JButton("Change Folder");
      simulationStartBtn = new JButton("Start");
      simulationStop = new JButton("Stop");

      destinationFolder.addActionListener(this);
      simulationStartBtn.addActionListener(this);
      simulationStop.addActionListener(this);

      controlPanel.removeAll();
      mainContainer.setVisible(false);
      controlPanel.setLocation(0,0);
      controlPanel.setSize(dimCont);
      controlPanel.setMinimumSize(new Dimension(dimCont));

      controlPanel.setLayout(new MigLayout(
              "fill",                           //Layout constraints
              "10[][][][][][][]10",                 //Column constraints - 6 cols + 20 from each side
              "10[][][][][][][][][][][][]10"        //Row constraints - 10 rows + 20 from each side
      ));

      controlPanel.add(new JLabel("Source file:"), "cell 1 1, span 2 1");
      controlPanel.add(sourceFilePath, "cell 1 2, span 5 1, growx");

      controlPanel.add(new JSeparator(JSeparator.HORIZONTAL), "cell 1 3, span 5 1, growx");

      controlPanel.add(new JLabel("Destination folder:"), "cell 1 4, span 2 1");
      controlPanel.add(destinationFolder, "cell 3 4, span 3 1, right");
      controlPanel.add(destinationFilePath, "cell 1 5, span 5 1, growx");

      controlPanel.add(new JSeparator(JSeparator.HORIZONTAL), "cell 1 6, span 5 1, growx");


      controlPanel.add(new JLabel("Amplitude"), "cell 1 7");
      controlPanel.add(new JLabel("Frequency (max)"), "cell 2 7");
      controlPanel.add(new JLabel("Time multiplier"), "cell 3 7");
      controlPanel.add(new JLabel("Time step"), "cell 4 7");
      controlPanel.add(new JLabel("Frequency Step"), "cell 5 7");

      controlPanel.add(forceAmplitude, "cell 1 8, growx");
      controlPanel.add(frequencyMax, "cell 2 8, growx");
      controlPanel.add(timeMultiplier, "cell 3 8, growx");
      controlPanel.add(timeStep, "cell 4 8, growx");
      controlPanel.add(frequencyStep, "cell 5 8, growx");

      controlPanel.add(new JLabel("Pass count:"), "cell 1 9");
      controlPanel.add(new JLabel("Damping force:"), "cell 2 9");


      controlPanel.add(simPasses, "cell 1 10, growx");
      controlPanel.add(dampingForce, "cell 2 10, growx");

      //labels that do get updated during simulation
      controlPanel.add(passTracker, "cell 1 11, growx");
      controlPanel.add(timeElapsed, "cell 4 11, growx");

      controlPanel.add(simulationStartBtn, "cell 1 12, span 2 1, grow");
      controlPanel.add(simulationStop, "cell 4 12, span 2 1, grow");


      controlsFrame.add(controlPanel);

      controlPanel.setVisible(true);
      mainContainer.setVisible(true);
      controlsFrame.setVisible(true);

    }
  }

  /**
   * Sets simulation variables before starting the simulation.
   *
   * Created by Andrejs Zile.
   */
  public void setSimulationVariables() {
    //resets the elapsed time to current time in the Simulation class
    super.timeNowReset = true;

    super.simulationActive = true;
    simulationStartTime = System.currentTimeMillis();

    super.timeMultiplier = Double.parseDouble(timeMultiplier.getText());
    timeMultiplier.setEditable(false);

    amplitude = Double.parseDouble(forceAmplitude.getText());
    forceAmplitude.setEditable(false);

    incrementFrequency = Double.parseDouble(frequencyStep.getText());
    frequencyStep.setEditable(false);

    passLimit = Integer.parseInt(simPasses.getText());
    simPasses.setEditable(false);

    frequencyLimit = Double.parseDouble(frequencyMax.getText());
    frequencyMax.setEditable(false);

    passTime = Double.parseDouble(timeStep.getText());
    timeStep.setEditable(false);

    passTracker.setText("Current: 1/" + passLimit + " pass");
    passTracker.repaint();

    damping = Double.parseDouble(dampingForce.getText());
    dampingForce.setEditable(false);

    simulationStartBtn.setEnabled(false);
    simulationStop.setEnabled(true);

    objSeriesAverage.clear();
    objSeriesCurrent.clear();

  }

  /**
   * Validates simulation variables and outputs warnings or errors.
   * Prevents from executing simulation if false.
   *
   * Created by Andrejs Zile.
   *
   * @return boolean
   */
  public boolean validateSimulationVariables() {
    boolean validValues = false;
    double valTimeMultiplier = Double.parseDouble(timeMultiplier.getText());

    if (valTimeMultiplier>=1.0 && valTimeMultiplier<=16.0) {
      validValues = true;
    } else {
      JOptionPane.showMessageDialog(null, "Time multiplier is invalid. Has to be in range between 1 and 16.");
      validValues = false;
    }

    double valAmplitude = Double.parseDouble(forceAmplitude.getText());
    if (valAmplitude < 0) {
      JOptionPane.showMessageDialog(null, "Do set only positive amplitude.");
      validValues = false;
    }

    double valIncrementFrequency = Double.parseDouble(frequencyStep.getText());
    if (valIncrementFrequency <= 0) {
      JOptionPane.showMessageDialog(null, "Set frequency step value. Must be positive.");
      validValues = false;
    }

    int valPassLimit = Integer.parseInt(simPasses.getText());
    if (valPassLimit < 1) {
      JOptionPane.showMessageDialog(null, "Pass count must be whole number and at least 1.");
      validValues = false;
    }

    double valFrequencyLimit = Double.parseDouble(frequencyMax.getText());
    if (valFrequencyLimit < valIncrementFrequency || valFrequencyLimit <= 0) {
      JOptionPane.showMessageDialog(null, "Maximum frequency value must be higher than zero.");
      validValues = false;
    }

    double valPassTime = Double.parseDouble(timeStep.getText());
    if (valPassTime <= 0) {
      JOptionPane.showMessageDialog(null, "Pass time must be higher than 0.");
      validValues = false;
    }

    double valDamping = Double.parseDouble(dampingForce.getText());
    if (valDamping >= 0 && valDamping < 10) {
      JOptionPane.showMessageDialog(null, "It is recommended to set damping value higher than 10.0");
    } else if (valDamping < 0) {
      JOptionPane.showMessageDialog(null, "Damping value must be higher than 0.");
      validValues = false;
    }

    //Check destination folder
    if (destinationFilePath.getText().length() == 0) {
      JOptionPane.showMessageDialog(null, "Destination folder empty. Please set destination folder.");
      validValues=false;
    }

    return validValues;
  }

  /**
   * Populates variables used for plotting graph for positions and velocities of each node.
   *
   * Original implementation, not modified.
   *
   * @param nm
   */
  public void populateVars(int nm) {
    var_names = new String[nm*4];
    for (int i=0; i<var_names.length; i++){
      if (i%4 == 0) {
        var_names[i] = "x" + i/4 + " position";
      }
      if (i%4 == 1) {
        var_names[i] = "y" + i/4 + " position";
      }
      if (i%4 == 2) {
        var_names[i] = "x" + i/4 + " velocity";
      }
      if (i%4 == 3) {
        var_names[i] = "y" + i/4 + " velocity";
      }
    }
  }


  /**
   * Initializes walls, that constrain where the node can be dragged to.
   *
   * Original implementation, not modified.
   */
  public void initializeWalls() {
    double w = 0.5;
    DoubleRect box = cvs.getSimBounds();
    m_Walls = new CRect(box);
    m_Left = box.getXMin() + w/2;
    m_Right = box.getXMax() - w/2;
    m_Bottom = box.getYMin() + w/2;
    m_Top = box.getYMax() - w/2;
    //if (realTime) //remove to enable visualisation
      cvs.addElement(m_Walls);
  }

  /**
   * Loads all necessary variables from network that is supplied via File in Lab.java
   *
   * This addition is to have dynamic number of Nodes and Edges
   *
   * Created by Andrejs Zile.
   * @param network
   */
  public void initializeNetwork(Network network) {
    allNodesList = network.getNodeList();
    allEdgesList = network.getEdgeList();
    //System.out.println("Network loading containing -- " + (allNodesList.size()) + " -- nodes.");

    //-- If the realTime variable is set to True, then elements (nodes and edges) will be added onto canvas,
    //-- to display all simulations.
    for (int i=0; i<allNodesList.size(); i++) {
      //if (super.realTime) //remove to enable visualisation
        cvs.addElement(allNodesList.get(i));
    }
    for (int i=0; i<allEdgesList.size(); i++) {
      //if (super.realTime) //remove to enable visualisation
        cvs.addElement(allEdgesList.get(i));
    }
  }

  /**
   * This resize the window for graphical animation of Nodes and Edges.
   *
   * Original implementation, not modified.
   * @param o
   */
  public void objectChanged(Object o) {
    if (o == cvs) {
      DoubleRect box = cvs.getSimBounds();
      m_Walls.setBounds(box);
      double w = allNodesList.get(0).m_Width;
      m_Left = box.getXMin() + w/2;
      m_Right = box.getXMax() - w/2;
      m_Bottom = box.getYMin() + w/2;
      m_Top = box.getYMax() - w/2;
    }
  }

  /**
   * This sets up controls for the window to have buttons and values.
   *
   * Original implementation, partially modified.
   * Added start sim button.
   *
   * Disabled to show only dropdown.
   *
   * TODO hide the bar
   */
  public void setupControls() {
    boolean disabled = true;


    if (!disabled) {
      super.setupControls();
//      addControl(button_stop = new JButton("reset"));
//      button_stop.addActionListener(this);

      // DoubleField params:  subject, name, fraction digits
      for (int i=0; i<params.length; i++)
        addObserverControl(new DoubleField(this, params[i], 2));

      //-- This button allows to start simulation that increases frequency step by step
      //-- and logs it onto a graph and further to a file.
//      addControl(button_frequency = new JButton("start sim"));
//      button_frequency.addActionListener(this);
      showControls(true);
    }


  }

  /**
   * This method sets up graph to draw current and max total energy after each
   * run is complete. Method does not return any value.
   *
   * Followed tutorial:
   * Reference:
   * "Using JFreechart to draw XY line chart with XYDataset", Codejava.net, 2016. [Online].
   * Available: http://www.codejava.net/java-se/graphics/using-jfreechart-to-draw-xy-line-chart-with-xydataset.
   * [Accessed: 07- Sep- 2016].
   *
   * Modified by Andrejs Zile.
   */
  public void setupGraph() {
    //-- Creates two XYSeries one for current run and the other for total average.
    //-- Potentially will add Median as well.
    objSeriesAverage = new XYSeries("Total Average Energy");
    objSeriesCurrent = new XYSeries("Current Max Energy");

    //-- Add series to object collection to display them on the graph.
    XYSeriesCollection objCollection = new XYSeriesCollection();
    objCollection.addSeries(objSeriesAverage);
    objCollection.addSeries(objSeriesCurrent);

    //-- Create a chart object to load it with series for Average and Current data stream.
    //-- Here all labels are specified and data collection as well.
    objChart = ChartFactory.createXYLineChart (
            "Spectra",
            "Frequency",
            "Energy average",
            objCollection
    );


    //-- Create a frame to show the graph and set it to visible.
    ChartFrame frame = new ChartFrame("Spectra of Network", objChart);
    frame.pack();
    if (!super.realTime) {
      frame.setVisible(true);
    }
  }

  /* This method is designed to be overriden, just be sure to
    call the super method also to deal with the super class's parameters. */
  protected boolean trySetParameter(String name, double value) {
    if (name.equalsIgnoreCase(DAMPING))
      {damping = value; return true;}
    else if (name.equalsIgnoreCase(GRAVITY))
      {gravity = value; return true;}
    else if (name.equalsIgnoreCase(AMPLITUDE))
      {amplitude = value; return true;}
    else if (name.equalsIgnoreCase(FREQUENCY))
      {frequency = value; return true;}
    return super.trySetParameter(name, value);
  }

  /* When overriding this method, be sure to call the super class
     method at the end of the procedure, to deal with other
     parameters and exceptions. */
  public double getParameter(String name) {
    if (name.equalsIgnoreCase(DAMPING))
      return damping;
    else if (name.equalsIgnoreCase(GRAVITY))
      return gravity;
    else if (name.equalsIgnoreCase(AMPLITUDE))
      return amplitude;
    else if (name.equalsIgnoreCase(FREQUENCY))
      return frequency;
    return super.getParameter(name);
  }

  /* When overriding this method, you need to call the super class
     to get its parameters, and add them on to the array. */
  public String[] getParameterNames() {
    return params;
  }

  /**
   * This method stops motion of all Nodes.
   * Sets velocity to 0 and sets positions back to initial positions.
   * Also it resets the maximum energy of each Edge in the Network.
   *
   * Modified by Andrejs Zile.
   */
  private void stopMotion() {
    //-- Loop through all Nodes to set X and Y position of each node.
    //System.out.println("Stopping motion");
    //System.out.println("Damping: " + damping);
    for (int i=0; i<allNodesList.size(); i++) {
      double pos = 0.5 * i;
      int numberOfNodes = allNodesList.size();
      double angleIncrement = (2*Math.PI)/numberOfNodes;
      double angle = angleIncrement * i;
      Node currentNode = allNodesList.get(i);
      if (!simulationPositions) {
        //System.out.println("First time set, no positions yet set");
        double radius = 4;
        damping = 20.0;
        double xPos = radius * Math.cos(angle);
        double yPos = radius * Math.sin(angle);
        currentNode.setInitPosX(xPos);
        currentNode.setInitPosY(yPos);
        currentNode.setX1(currentNode.getInitPosX());
        currentNode.setY1(currentNode.getInitPosY());
        vars[0 + 4 * i] = currentNode.getInitPosX();
        vars[1 + 4 * i] = currentNode.getInitPosY();
      } else {
        //System.out.println("Positions are reset");
        currentNode.setX1(currentNode.getInitPosX());
        currentNode.setY1(currentNode.getInitPosY());
        vars[0 + 4 * i] = currentNode.getInitPosX();
        vars[1 + 4 * i] = currentNode.getInitPosY();
      }
      //System.out.println("Current Node id: " + currentNode.getId() + " x: " + currentNode.getX() + " y: " + currentNode.getX());
    }


    //-- Set velocity to 0
    for (int i=0; i<vars.length; i++) {
      if (i%4 == 2 || i%4 == 3) {
        vars[i] = 0;
      }
    }

    //-- Set Maximum Energy of each node to be 0.
    for (int e=0; e<allEdgesList.size(); e++) {
      Edge currentEdge = allEdgesList.get(e);
      currentEdge.setMaxEnergy(0);
    }
  }

  /**
   * Stops the simulation and resets all values.
   * Also allows user to edit values and then start the simulation again.
   *
   * Created by Andrejs Zile.
   */
  public void stopSimulation() {
    stopMotion();

    simulationStop.setEnabled(false);
    simulationStartBtn.setEnabled(true);

    timeMultiplier.setEditable(true);
    forceAmplitude.setEditable(true);
    frequencyStep.setEditable(true);
    simPasses.setEditable(true);
    frequencyMax.setEditable(true);
    timeStep.setEditable(true);
    dampingForce.setEditable(true);


    simulationStart = false;
    simulationPositions = true; //do not need to evaluate positions again for next run

    timeSplit = 0.0;
    frequency = 0.0;
    passCount = 1;
    damping = 20; //TODO change this

    allSimulationValues.clear();

    super.simulationActive = false;
  }

  /**
   * Controls Stop and Start sim button
   * Start sim triggers loop that increases frequency and logs changes on the graph.
   *
   * Modified by Andrejs Zile.
   * @param e - Event
   */
  public void actionPerformed (ActionEvent e) {
    if(e.getSource() == button_stop) {
      stopMotion();
    } else if (e.getSource() == button_frequency) { //This is the RT simulation start button
      //super.timeNowReset = true;
      simulationStartTime = System.currentTimeMillis();
      simulationStart = true;
      timeSplitStart = getTime();
    } else if (e.getSource() == simulationStartBtn) { //This is the NRT simulation start button
      if (validateSimulationVariables()) {
        setSimulationVariables();
        simulationStart = true;
        timeSplitStart = getTime();
      };
    } else if (e.getSource() == simulationStop) {
      stopSimulation();
    } else if (e.getSource() == destinationFolder) {
      changeDirectory();
    }
  }

  /**
   * Causes motion of objects by updating each of its variables.
   *
   * Modified by Andrejs Zile.
   */
  public void modifyObjects() {
    //super.stepperAltTime += 2;
    // assume all masses are same width & height
    double w = allNodesList.get(0).m_Width/2;

    for (int i=0; i<allNodesList.size(); i++) {
      Node currentNode = allNodesList.get(i);
      currentNode.setX1(vars[4*i] - w);
      currentNode.setY1(vars[1 + 4*i] - w);
      //System.out.println("Node id: " + currentNode.getId() + ", x: " + currentNode.getX() + ", y: " + currentNode.getY());
    }

    //for each edge id set position of current node
    for (int i=0; i<allEdgesList.size(); i++) {
      allEdgesList.get(i).setX1(allEdgesList.get(i).getNodeFrom().m_X1 + w);
      allEdgesList.get(i).setY1(allEdgesList.get(i).getNodeFrom().m_Y1 + w);
      allEdgesList.get(i).setX2(allEdgesList.get(i).getNodeTo().m_X1 + w);
      allEdgesList.get(i).setY2(allEdgesList.get(i).getNodeTo().m_Y1 + w);
    }
  }

  /**
   * This method allows to drag Nodes on the screen.
   *
   * Original implementation, modified.
   * @param e
   */
  public void startDrag(Dragable e) {
    if (super.realTime) {
      for (int i=0; i<allNodesList.size(); i++)
        if (e==allNodesList.get(i))
          for (int j=0; j<4; j++)
            calc[j + 4*i] = false;
    }
  }

  /**
   * Original implementation, modified.
   * @param e
   * @param x
   * @param y
     */
  public void constrainedSet(Dragable e, double x, double y) {
    // objects other than mass are not allowed to be dragged
    // assume all masses are same width & height
    double w = allNodesList.get(0).m_Width/2;
    x += w;
    y += w;

    // disallow drag outside of window
    if (x < m_Left)
      x = m_Left + 0.0001;
    if (x > m_Right)
      x = m_Right - 0.0001;
    if (y < m_Bottom)
      y = m_Bottom + 0.0001;
    if (y > m_Top)
      y = m_Top - 0.0001;

    for (int i=0; i<allNodesList.size(); i++)
      if (e==allNodesList.get(i)) {
        vars[4*i] = x;
        vars[1 + 4*i] = y;
        vars[2 + 4*i] = 0;
        vars[3 + 4*i] = 0;
      }
  }

  /**
   * Evaluate is mostly implementation done originally by myphisicslab.com
   * Although there have been significant changes to the original code
   * to make it work with network setup.
   *
   * Evaluate part evaluates forces that are affecting nodes position including
   * motion and deformations of edges.
   *
   * @param x
   * @param change
     */
  public void evaluate(double[] x, double[] change)
  {
    double DIST_TOL = 0.02;
    double timeStep = 0.03;  // assume timeStep is this length or longer

    double now = getTime();
    timeSplit = (now - timeSplitStart);

    //Work out initial positions (this is run before simulation starts)
    if (!simulationPositions && simulationStart) {
      evaluatePositions();
    }

    // i = index of variable whose derivative we want to calc
    for (int i=0; i<vars.length; i++) {
      int j = i%4;  // % is mod, so j tells what derivative is wanted:
      // 0=Ux, 1=Uy, 2=Vx, 3=Vy
      int obj = i/4;  // obj is the 'particle number', from 0 to 5
      if ((j==0)||(j==1))  // requested derivative for Ux or Uy
        change[i] = x[i+2]; // derivative of position U is velocity V
      else  {
        // requested derivative is Vx or Vy for particle number 'obj'
        double r = 0;  // result net force
        double mass = allNodesList.get(obj).m_Mass;  // mass of our object

        //this runs constantly, even when simulation is not started
        r = evaluateSpringForces(r, x, mass, obj, j);


        if (damping != 0)
          r -= (damping/mass)*x[i];

        //-----------------------------------------------------
        //--------------- Forced Oscillations -----------------
        //-----------------------------------------------------


        //Driven oscillations
        double amplitudeDriven = amplitude; //Force applied
        double frequencyDriven = frequency; //Omega
        double relativePosition = allNodesList.get(obj).getY();


        if (simulationStart) {
          DecimalFormat f = new DecimalFormat("############.#");
          double elapsedTime = (System.currentTimeMillis() - simulationStartTime)/1000.0;
          if (!super.realTime) {
            timeElapsed.setText(f.format(elapsedTime)+"");
            //FOR DEBUG
            //timeElapsed.setText(Math.round(getTime()) + "");
            timeElapsed.repaint();
          }
        }

        //Forced component activated (this is run once simulation is started and positions have been worked out)
        if (simulationPositions && simulationStart) {
          r = evaluateForcedOscillations(amplitudeDriven, j, obj, relativePosition, r);
        }

        //When time expires, trigger next run
        if (simulationStart && simulationPositions && (timeSplit > passTime) && frequency <= frequencyLimit) {
          nextRun(frequencyDriven, relativePosition);
        }

        change[i] = r;
      }
    }
  }

  /**
   * Evaluates force of the spring that connects two masses together.
   * @param r - is the resultant force
   * @param x - is an array of variables
   * @param mass - is the mass of the node
   * @param obj - is the number of node
   * @param j - is the number of element (vx, vy, ux, uy)
   * @return r - resultant force
   *
   * Spring forces equations mainly are used from existing package,
   * but modifications have been made with use of allEdgeList and allNodeList
   * instead of matrix that was used originally.
   */
  public double evaluateSpringForces(double r, double[] x, double mass, int obj, int j) {
    // for each spring, get force from spring,
    // look at LHS (left hand side) of msm matrix
    for (int k=0; k<allEdgesList.size(); k++) {
      // this spring is connected to our object
      if (allEdgesList.get(k).getNodeFrom().getId() == obj) {
        // the object on other end of the spring
        int obj2 = allEdgesList.get(k).getNodeTo().getId();
        // x distance between objects
        double xx = x[4 * obj2] - x[4 * obj];
        // y distance betw objects
        double yy = x[1 + 4 * obj2] - x[1 + 4 * obj];
        // total distance betw objects
        double len = Math.sqrt(xx * xx + yy * yy);
        Edge spr = allEdgesList.get(k);
        // spring constant for this spring
        double sc = spr.m_SpringConst;
        // see earlier comments for more on the force equations.
        // Fx = (sc/m)*(len - R)*xx/len or
        // Fy = (sc/m)*(len - R)*yy/len - g
        double f = (sc / mass) * (len - spr.m_RestLength) / len;
        r += (j == 2) ? f * xx : -gravity + f * yy;
      }
    }

    // same deal, but look at RHS (right hand side) of the msm matrix
    for (int k=0; k<allEdgesList.size(); k++) {
      // this spring is connected to our object
      if (allEdgesList.get(k).getNodeTo().getId() == obj)  {
        // the object on other end of the spring
        int obj2 = allEdgesList.get(k).getNodeFrom().getId();
        // x distance between objects
        double xx = x[4 * obj2] - x[4 * obj];
        // y distance betw objects
        double yy = x[1 + 4 * obj2] - x[1 + 4 * obj];
        // total distance betw objects
        double len = Math.sqrt(xx * xx + yy * yy);
        Edge spr = allEdgesList.get(k);
        // spring constant for this spring
        double sc = spr.m_SpringConst;
        // see earlier comments for more on the force equations.
        // Fx = (sc/m)*(len - R)*xx/len or
        // Fy = (sc/m)*(len - R)*yy/len - g
        double f = (sc / mass) * (len - spr.m_RestLength) / len;
        r += (j == 2) ? f * xx : -gravity + f * yy;
      }
    }
    return r;
  }

  /**
   * Evaluate forces of forced oscillations.
   * @param amplitudeDriven - amplitude, which determines maximum strength of force
   * @param j - number of element in the array (vx, vy, ux, uy)
   * @param obj - number of element in the array of Nodes
   * @param relativePosition - relative position of node to the center of the molecule
   * @param r - input force
   * @return r - resultant force
   *
   * Created by Andrejs Zile.
   */
  public double evaluateForcedOscillations(double amplitudeDriven, int j, int obj, double relativePosition, double r) {

    Node currentNode = allNodesList.get(obj);
    //System.out.println("Amplitude driven: " + amplitudeDriven);

    if (amplitudeDriven>0 && j == 3) {
        //------------------------------------------------------
        //--Assuming electric field produces positive charge----
        //------------------------------------------------------
        //System.out.println("node id: " + currentNode.getId());
        //System.out.println("x: " + currentNode.getX() + ",y: "+ currentNode.getY() + "");
        if (currentNode.getChargeIfPositive()) {
          r += getElectroForce(currentNode.getX());
          //r += amplitudeDriven * Math.cos(frequencyDriven * (timeElapsed + offset));
        } else {
          r -= getElectroForce(currentNode.getX());
          //r -= amplitudeDriven * Math.cos(frequencyDriven * (timeElapsed + offset));
        }
      }
      //-----------------------------------------------------
      //-----------------------------------------------------
      //-----------------------------------------------------
      relativePosition = currentNode.getY();
      double centerOfMoleculeY = getCenterOfMoleculeY();

      if (relativePosition > centerOfMoleculeY) {
        relativePosition = relativePosition - centerOfMoleculeY;
      } else {
        relativePosition = centerOfMoleculeY - relativePosition;
      }

      //--------- Record Displacement of Node ---------------
      if (currentNode.minDispY > relativePosition) {
        currentNode.minDispY = relativePosition;
      }

      if (currentNode.maxDispY < relativePosition) {
        currentNode.maxDispY = relativePosition;
      }

      for (int e=0; e<allEdgesList.size(); e++) {
        Edge currentEdge = allEdgesList.get(e);
        if (currentEdge.getMaxEnergy() < currentEdge.getEnergy()) {
          currentEdge.setMaxEnergy(currentEdge.getEnergy());
        }
      }

    return r;
  }

  /**
   * Evaluates initial position of nodes in the first run of simulation.
   * This run is not recorded onto the results. Simply used to record the position
   * of node once it is almost still in the network.
   *
   * Created by Andrejs Zile.
   */
  public void evaluatePositions() {
    damping = 20.0;
    if (timeSplit > passTime-1) {
      //record positions
      for ( Node node : allNodesList) {
        node.setInitPosX(node.getX());
        node.setInitPosY(node.getY());
        //System.out.println("Node initial positions: " + node.getId() +" x: " + node.getX() + " y: " +node.getY());
      }
      simulationPositions = true;
    }
  }

  /**
   * This method starts next pass of simulation.
   * @param frequencyDriven - frequency at which external force is applied.
   * @param relativePosition - relative position of node to the cetner of the network.
   *
   * Created by Andrejs Zile.
   */
  public void nextRun(double frequencyDriven, double relativePosition) {
    timeSplit = 0;
    timeSplitStart = (long)getTime();

    double averageEnergy = 0.0;
    for (int e=0; e<allEdgesList.size(); e++) {
      Edge currentEdge = allEdgesList.get(e);
      averageEnergy += currentEdge.getMaxEnergy();
    }
    averageEnergy = averageEnergy/(allEdgesList.size());


    objSeriesCurrent.add(frequencyDriven, averageEnergy);
    stopMotion();

    for (int el = 1; el<4; el++) {
      allNodesList.get(el).maxDispY = relativePosition;
      allNodesList.get(el).minDispY = relativePosition;
    }

    frequencyDriven += incrementFrequency;
    frequency = frequencyDriven;

    //If running as simulation for faster and multiple times
    if (!realTime) {
      if (frequency >= frequencyLimit && passCount < passLimit) {
        frequency = 0;
        passCount++;
        passTracker.setText("Current: "+ passCount +"/" + passLimit + " pass");
        passTracker.repaint();

        List<XYDataItem> allItems = new ArrayList<XYDataItem>(objSeriesCurrent.getItems());
        allSimulationValues.add(allItems);
        objSeriesCurrent.clear();
        //System.out.println("Pass: " + passCount + " / " + passLimit);
        //System.out.println("Size of all sim values: " + allSimulationValues.size());
      } else if (frequency >= frequencyLimit && passCount == passLimit) {

        //Final part to export all values and stop the simulation
        //Once the pass count has reached maximum
        List<XYDataItem> allItems = new ArrayList<XYDataItem>(objSeriesCurrent.getItems());
        allSimulationValues.add(allItems);
        objSeriesCurrent.clear();
        stopMotion();
        List<XYDataItem> averageList = calculateAverageEnergy();
        for (int a=0; a<averageList.size(); a++) {
          objSeriesAverage.add(averageList.get(a));
        }
        //export to file
        exportToFile(allSimulationValues, averageList, destFilePath);

        //Stops the simulation from running
        stopSimulation();
      }
    }
  }

  /**
   * This method returns the average position of all Nodes in the Network.
   * It is used to calculate Maximum displacement relative to the center of the network.
   * In direction of X.
   *
   * Created by Andrejs Zile.
   * @return
   */
  public double getCenterOfMoleculeX() {
    double x = 0;
    for (int i=0; i< allNodesList.size(); i++) {
      x += allNodesList.get(i).getX();
    }
    if (allNodesList.size()>0) x=x/allNodesList.size();
    return x;
  }

  /**
   * This method returns the average position of all Nodes in the Network.
   * It is used to calculate Maximum displacement relative to the center of the network.
   * In direction of Y.
   *
   * Created by Andrejs Zile.
   * @return
   */
  public double getCenterOfMoleculeY() {
    double y = 0;
    for (int i=0; i< allNodesList.size(); i++) {
      y += allNodesList.get(i).getY();
    }
    if (allNodesList.size()>0) y=y/allNodesList.size();
    return y;
  }

  /**
   * This method calculates Electro Magnetic force that is applied to the network in Y direction.
   *
   * @param x - position of Node relative to x
   * @return electroMagneticForce - amount of force at specific time and location of Node
   *
   * Created by Andrejs Zile.
   */
  public double getElectroForce(double x) {
    double electroMagneticForce = 0;

    //y = A sin (kx - wt)
    //where k = (2pi / lambda) - lambda is wave length
    //and w = (2pi / period)

    // frequency = 1/period
    double k = 2 * Math.PI / 0.25;
    double period = 1/frequency;
    double w = 2 * Math.PI / period;

    //double now = getTime(); now - gettime
    double t = getTime();

    electroMagneticForce = amplitude * Math.sin(k*x - w*t);
    //System.out.println("Electro force: " + electroMagneticForce + " x: " + x + " time: " + t);

    return  electroMagneticForce;
  }

  /**
   * This method calculates average value on Y axis and returns a list
   * that can be used to plot a graph with X and Y data values.
   *
   * @return list - of XYDataItem items
   *
   * Created by Andrejs Zile.
   */
  public List<XYDataItem> calculateAverageEnergy() {
    List<XYDataItem> averageList = new ArrayList<>();
    XYDataItem averageItem;
    //for each item at the same position in different array
    double runSize = allSimulationValues.size();
    if (allSimulationValues.size()>0) {
      double size = allSimulationValues.get(0).size();
      for (int i=0; i<size; i++) {
        double xValue = 0.0;
        double yValue = 0.0;

        for (int j=0; j<allSimulationValues.size(); j++) {
          XYDataItem oneItem = (XYDataItem)allSimulationValues.get(j).get(i);
          if (xValue == 0.0) xValue = oneItem.getXValue();
          //System.out.println("yValue = " + oneItem.getYValue());
          yValue += oneItem.getYValue();
        }
        yValue = yValue / (runSize); //-----------------removed +1 here
        averageItem = new XYDataItem(xValue, yValue);
        averageList.add(averageItem);
        //System.out.println("Values: x: " + xValue + ", y: " + yValue);
      }
    }
    return averageList;
  }

  /**
   * This method handles the export of all values to specified file.
   *
   * Includes export of an average image.
   *
   * @param allValues - all values generated from simulation
   * @param averageValues - all average values from simulation
   * @param filePath - file path for destination
   *
   * Created by Andrejs Zile.
   */
  public void exportToFile(List allValues, List<XYDataItem> averageValues, File filePath) {

    String fileName = getCurrentTimeStamp() + "-analysis-results";

    String actualFilePath = filePath.getAbsolutePath() + "/" + fileName + ".csv";
    String imageFilePath = filePath.getAbsolutePath() + "/" + fileName + "-avg.png";

    File imageFile = new File(imageFilePath);

    try {
      ChartUtilities.saveChartAsPNG(imageFile,objChart,1400,800);
    } catch (IOException e) {
      e.printStackTrace();
    }

    //System.out.println("Actual file path: " + actualFilePath);
    try (PrintWriter writer = new PrintWriter(actualFilePath, "UTF-8")) {
      for (int i=0; i<allValues.size(); i++) {
        List<XYDataItem> currentListItem = (List<XYDataItem>)allValues.get(i);
        if (i==0) {
          for (int j=0; j<currentListItem.size(); j++) {
            writer.print(currentListItem.get(j).getX());
            if (j<(currentListItem.size()-1))
              writer.print(",");
          }
          writer.println();
        }
        for (int j=0; j<currentListItem.size(); j++) {
          writer.print(currentListItem.get(j).getY());
          if (j<(currentListItem.size()-1))
            writer.print(",");
        }

        writer.println();
      }
      writer.println();
      writer.println("-----,-----,-----,-----,-----,-----,-----,-----");
      double timeElapsedInSeconds = (simulationStartTime - System.currentTimeMillis()) / 1000.0;
      writer.println("Network file name: " + sourceFileName
              + ",Time multiplier: " + super.timeMultiplier
              + ",Force Amplitude Set: " + amplitude
              + ",Passes: " + passLimit + ",Maximum frequency: " + frequencyLimit
              + ",Damping: " + damping + ",Frequency step increment: " + incrementFrequency
              + ",Step duration: " + passTime
              + ",Time elapsed in seconds: " + timeElapsedInSeconds);
      writer.close();

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }

  /**
   * Method to change destination directory.
   *
   * Created by Andrejs Zile.
   *
   * Source of tutorial: https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html
   */
  public void changeDirectory() {
    final JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fc.setAcceptAllFileFilterUsed(false);

    int returnVal = fc.showOpenDialog(null);

    if (returnVal == JFileChooser.APPROVE_OPTION) {

      destFilePath = fc.getSelectedFile();
      destinationFilePath.setText(destFilePath + "");
      destinationFilePath.repaint();
      //System.out.println("Saving to: " + destFilePath.getPath() + ".");
    } else {
      destFilePath = null;
      //System.out.println("Open command cancelled by user." );
    }
  }

  /**
   * Returns string of time stamp for export function for file name.
   * @return timeStamp
   *
   * Modified by Andrejs Zile.
   * Source: https://stackoverflow.com/questions/10361955/java-system-time
   */
  public static String getCurrentTimeStamp() {
    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    Date now = new Date();
    String strDate = sdfDate.format(now);
    return strDate;
  }
}