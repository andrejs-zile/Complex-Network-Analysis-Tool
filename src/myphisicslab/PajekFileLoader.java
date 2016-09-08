package myphisicslab;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * PajekFileLoader class provides functionality to parse simple Pajek format data file
 * and return Network class object.
 *
 * Created by Andrejs Zile
 */
public class PajekFileLoader {
    private Network network;

    public PajekFileLoader() {
    }

    /**
     * Returns Network class object from Pajek format file.
     *
     * Parses only weighted undirected network data.
     *
     * @return Network class object
     * @throws IOException
     *
     * Source of tutorial:
     * Reference:
     * "How to Use File Choosers (The Java™ Tutorials > Creating a GUI With JFC/Swing > Using Swing Components)",
     * Docs.oracle.com, 2016. [Online]. Available: https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html.
     * [Accessed: 07- Sep- 2016].
     */
    public Network loadFile() throws IOException {
        //This needs to feed back to model
        final JFileChooser fc = new JFileChooser();

        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            File file = fc.getSelectedFile();
            //System.out.println("Opening: " + file.getName() + ".");

            network = parseFile(file);
            network.setFilePath(file.getPath());
            network.setFileName(file.getName());

            return network;
        } else {
            //System.out.println("Open command cancelled by user." );
            return null;
        }
    }

    /**
     * Parses the Pajek file
     * @param file - the file that is opened via JFileChooser
     * @return Network class object
     *
     */
    public Network parseFile(File file) {
        try {
            //System.out.println("Scanner: ");
            Scanner sc = new Scanner(file);
            int numberOfNodes = 0;
            int numberOfEdges = 0;
            int i=0;
            int e=0;
            boolean edgeStart = false;
            boolean validEdges = false;
            boolean validNodes = false;
            boolean test = false; //to debug the file parser


            ArrayList<Node> arrayOfNodes = new ArrayList<Node>();
            ArrayList<Edge> arrayOfEdges = new ArrayList<Edge>();

            while (sc.hasNextLine()) {
                String str = sc.nextLine();

                if (str.contains("*Vertices")) {
                    numberOfNodes = Integer.parseInt(str.substring(10));
                }

                //If contains vertice data
                if (str.matches("\\s*?\\d+\\s\".*\".*")) {
                    if (test) System.out.println("Match: " + str);
                    int nodeId = 0;
                    String nodeLabel = null;

                    //match pattern
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(str);
                    if (matcher.find())
                    {
                        if (test) System.out.println("Matcher found id.");
                        if (test) System.out.println(matcher.group(0));
                        nodeId = (Integer.parseInt(matcher.group(0)));
                        if (test) System.out.println(nodeId);
                    }
                    pattern = Pattern.compile("\".*\"");
                    matcher = pattern.matcher(str);
                    if (matcher.find())
                    {
                        nodeLabel = (matcher.group(0));
                    }
                    Node newNode = new Node(nodeId-1, nodeLabel);
                    newNode.setX1(0);
                    newNode.setY1(0);
                    if (nodeId % 2 == 0) {
                        newNode.setChargeNegative();
                    } else {
                        newNode.setChargePositive();
                    }
                    newNode.m_Mass = newNode.weight;
                    arrayOfNodes.add(i, newNode);
                    if (test) System.out.println(arrayOfNodes.get(i).toString());
                    i++;
                }

                //If contains edge data
                if (str.contains("*edges") || str.contains("*Edges")) {
                    if (test) System.out.println("Edges loading");
                    edgeStart = true;
                }

                boolean weightedDouble = str.matches("\\s*\\d+\\s*\\d+\\s*\\d+\\.\\d+");
                boolean weightedInt = str.matches("\\s*\\d+\\s*\\d+\\s*\\d+");

                if ((weightedDouble || weightedInt) && edgeStart) {
                    if (test) System.out.println("Match: " + str);
                    int nodeFrom = 0;
                    int nodeTo = 0;
                    double weight = 0.0;

                    //match pattern
                    Pattern pattern = Pattern.compile("\\d+");
                    Matcher matcher = pattern.matcher(str);
                    if (matcher.find())
                        nodeFrom = (Integer.parseInt(matcher.group(0)));
                    if (matcher.find())
                        nodeTo = (Integer.parseInt(matcher.group(0)));
                    if (weightedInt && matcher.find())
                        weight = Double.parseDouble(matcher.group(0));

                    pattern = Pattern.compile("\\d+\\.\\d+");
                    matcher = pattern.matcher(str);
                    if (weightedDouble && matcher.find())
                    {
                        weight = Double.parseDouble(matcher.group(0));
                    }

                    Edge newEdge = new Edge(e, weight);
                    for (int n=0; n<arrayOfNodes.size(); n++) {
                        Node currentNode = arrayOfNodes.get(n);
                        //System.out.println("Current Node: " + currentNode.getId());
                        //System.out.println("Node From: " + nodeFrom);
                        //System.out.println("Node To: " + nodeTo);
                        if (currentNode.getId() == nodeFrom-1) {
                            //System.out.println("Node From Match");
                            newEdge.setNodeFrom(currentNode);
                        } else if (currentNode.getId() == nodeTo-1) {
                            //System.out.println("Node To Match");
                            newEdge.setNodeTo(currentNode);
                        }

                        newEdge.m_SpringConst=weight *100;
                        newEdge.m_RestLength = 2;
                        newEdge.setWeight(weight);
                    }
                    arrayOfEdges.add(e, newEdge);
                    if (test) System.out.println(arrayOfEdges.get(e));
                    e++;
                }

            }
            Network assembledNetwork = new Network();

            assembledNetwork.setEdgeList(arrayOfEdges);
            assembledNetwork.setNodeList(arrayOfNodes);

            if (assembledNetwork.getEdgeList().size() == 0) {
                validEdges = false;
            } else {
                validEdges = true;
            }

            if (assembledNetwork.getNodeList().size() == 0) {
                validNodes = false;
            } else {
                validNodes = true;
            }

            if (!validEdges || !validNodes) {
                JOptionPane.showMessageDialog(null, "The import file is invalid. Please load another file.");
            }

            if (test) System.out.println("Network contains " + numberOfNodes + " nodes");
            sc.close();
            return assembledNetwork;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

    }

}
