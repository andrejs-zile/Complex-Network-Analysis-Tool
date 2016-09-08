package myphisicslab;

import java.util.ArrayList;

/**
 * Network Class
 *
 * This class is used for simplicity and provides capability of adding all nodes and edges
 * of one network to a single class object.
 * It contains only two lists and filePath and fileName
 *
 * This class is created by Andrejs Zile.
 */
public class Network {
    private String filePath = "";
    private String fileName = "";
    private ArrayList<Node> nodeList;
    private ArrayList<Edge> edgeList;

    public Network() {
    }

    /**
     * Sets node list of the Network
     * @param nodeList
     */
    public void setNodeList (ArrayList<Node> nodeList) {
        this.nodeList = nodeList;
    }

    /**
     * Sets edge list of the Network
     * @param edgeList
     */
    public void setEdgeList (ArrayList<Edge> edgeList) {
        this.edgeList = edgeList;
    }

    /**
     * Returns Node List of the Network
     * @return ArrayList<Node>
     */
    public ArrayList<Node> getNodeList() {
        return this.nodeList;
    }

    /**
     * Returns Edge List of the Network
     * @return ArrayList<Edge>
     */
    public ArrayList<Edge> getEdgeList() {
        return this.edgeList;
    }

    /**
     * Returns file path of the source network file.
     * @return String
     */
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * Sets file path of the network source file.
     * @param filePath
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Returns file name of the network source file.
     * @return String
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Sets file name of the network file.
     * @param fileName
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
