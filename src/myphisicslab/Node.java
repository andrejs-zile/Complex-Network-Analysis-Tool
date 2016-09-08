package myphisicslab;

import java.awt.*;
/**
 * Node Class
 *
 * Node class is extended from CMass class, which represents physical model of mass.
 * The additions here are the record of positive or negative charge, weight of a node, label
 * , id and initial positions x and y, that are used for each simulation.
 *
 * This class is created by Andrejs Zile.
 */
public class Node extends CMass {
    private int id;
    private String label;
    public double weight;
    private int[] edgeIds;
    private int edgeCount = 0;
    private boolean chargePositive = false;
    protected double minDispY = 0;
    protected double maxDispY = 0;
    private double initPosX = 0.0, initPosY = 0.0;

    public Node(int id) {
        super(0, 0, 0.5, 0.5, CElement.MODE_CIRCLE);
        this.id = id;
        this.weight = 1;
        super.m_Mass = weight;
        this.edgeCount = 0;
        edgeIds = new int[500];
    }

    public Node(int id, String label) {
        super(0, 0, 0.5, 0.5, CElement.MODE_CIRCLE);
        this.id = id;
        this.weight = 1;
        super.m_Mass = weight;
        this.label = label;
        this.edgeCount = 0;
        edgeIds = new int[500];
    }

    public Node (double X1, double Y1, double width, double height, int drawMode) {
        this.id = 0;
        this.weight = 1;
        super.m_Mass = weight;
        edgeIds = new int[500];
    }

    public Node (double X1, double Y1, double width, double height, int drawMode, int nodeId) {
        super(X1, Y1, width, height, drawMode);
        this.id = nodeId;
        this.weight = 1;
        super.m_Mass = weight;
        edgeIds = new int[500];
    }

    /**
     * Get node id
     * @return id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Set node id
     * @param id - integer
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Set label of the node.
     * @param label - String
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Get the label of the node
     * @return String
     */
    public String getLabel() { return this.label; }

    /**
     * Set initial position of x of the current node
     * @param x - double
     */
    public void setInitPosX(double x) {
        this.initPosX = x;
    }

    /**
     * Set initial position of y of the current node
     * @param y - double
     */
    public void setInitPosY(double y) {
        this.initPosY = y;
    }

    /**
     * Get initial position of node in x direction
     * @return double
     */
    public double getInitPosX() {
        return this.initPosX;
    }

    /**
     * Get initial position of node in y direction
     * @return double
     */
    public double getInitPosY() {
        return this.initPosY;
    }


    /**
     * Returns an array of all edge ids
     * @return array of int
     */
    public int[] getEdgeIds() {
        return this.edgeIds;
    }

    /**
     * Add edge id to the array
     * @param id
     */
    public void addEdgeId(int id) {
        edgeIds[edgeCount] = id;
        edgeCount++;
        if (edgeCount>0) weight = weight * edgeCount;
        super.m_Mass = weight;
    }

    /**
     * Set the charge of node to be positive.
     * Also change the color of the node to blue.
     */
    public void setChargePositive() {
        this.chargePositive = true;
        this.m_Color = Color.blue;
    }

    /**
     * Set the charge of node to be negative.
     * Also make the color of the node red.
     */
    public void setChargeNegative() {
        this.chargePositive = false;
        this.m_Color = Color.red;
    }

    /**
     * Return if the charge of node is positive
     * @return boolean
     */
    public boolean getChargeIfPositive() {
        return this.chargePositive;
    }

    @Override
    public String toString() {
        return "Node id: " + this.id + " Node label: " + this.label;
    }


}
