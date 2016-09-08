package myphisicslab;
/**
 * Edge Class
 *
 * Edge class is extended from CSpring class, which represents physical model of spring.
 * Here the addition is that Edge has a two Nodes the node at one end of spring and the node on the other end.
 * Also this class includes maximum energy calculation that is used when graphing a graph.
 *
 * This class is created by Andrejs Zile.
 */
public class Edge extends CSpring {
    private int id;
    private double weight;
    private Node nodeFrom, nodeTo;
    private double maxEnergy = 0;

    //-------- ------------ ---------
    //-------- Constructors ---------
    //-------- ------------ ---------
    public Edge(int id) {
        this.id = id;
        this.weight = 0;
        this.nodeFrom = null;
        this.nodeTo = null;
    }

    public Edge(int id, double weight) {
        this.id = id;
        this.weight = weight;
        this.nodeFrom = null;
        this.nodeTo = null;
    }

    public Edge(double X1, double Y1, double restLen, double thick) {
        super(X1, Y1, restLen, thick);
        id = 0;
        weight = 1;
        nodeFrom = null;
        nodeTo = null;
    }

    public Edge(double X1, double Y1, double restLen, double thick, int edgeId) {
        super(X1, Y1, restLen, thick);
        this.id = edgeId;
        weight = 1;
        nodeFrom = null;
        nodeTo = null;
    }

    public Edge(double X1, double Y1, double restLen, double thick, double weight,
                Node from, Node to, int edgeId) {
        super(X1, Y1, restLen, thick);
        this.weight = weight;
        nodeFrom = from;
        nodeTo = to;
        this.id = edgeId;
    }

    //-------- ------- ---------
    //-------- Methods ---------
    //-------- ------- ---------

    /**
     * Returns id of an Edge
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets id of an Edge
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns weight of an Edge
     * @return weight
     */
    public double getWeight() {
        return this.weight;
    }

    /**
     * Sets weight of an Edge
     * @param weight
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Sets links to Nodes for selected Edge.
     * Parameters include the node from and node to.
     * @param from - From Node
     * @param to - To Node
     */
    public void setNodeLinks(Node from, Node to) {
        this.nodeFrom = from;
        this.nodeTo = to;
    }

    /**
     * Calculates and returns potential energy of an Edge (Spring)
     * @return energy
     */
    public double getEnergy() {
        double k = this.m_SpringConst;
        double x = this.getStretch();
        double energy = 0.5 * k * (x * x); //1/2kx^2
        return energy;
    }

    /**
     * Sets variable maxEnergy to a value passed via method.
     * @param energy
     */
    public void setMaxEnergy(double energy) {
        this.maxEnergy = energy;
    }

    /**
     * Returns current maximum potential energy of an Edge
     * @return maxEnergy
     */
    public double getMaxEnergy() {
        return maxEnergy;
    }

    /**
     * Returns the Node on the from side of the Edge
     * @return Node nodeFrom
     */
    public Node getNodeFrom() {
        return this.nodeFrom;
    }

    /**
     * Returns the Node on the to side of the Edge
     * @return Node nodeTo
     */
    public Node getNodeTo() {
        return this.nodeTo;
    }

    /**
     * Sets Node from as a nodeTo for the Edge, and gets this Nodes position and
     * applies it to the position of the from side of an Edge.
     * @param from - Node
     */
    public void setNodeFrom(Node from) {
        setX1(from.getX());
        setY1(from.getY());
        from.addEdgeId(this.id);
        this.nodeFrom = from;
    }

    /**
     * Sets Node to as a nodeTo for the Edge, and gets this Nodes position and
     * applies it to the position of the to side of an Edge.
     * @param to - Node
     */
    public void setNodeTo(Node to) {
        setX2(to.getX());
        setY2(to.getY());
        to.addEdgeId(this.id);
        this.nodeTo = to;
    }

    @Override
    public String toString() {
        return "Edge id: " + this.id + " weight: " + this.weight + " Node From: "
                + this.nodeFrom + " Node To: " + this.nodeTo;
    }
}
