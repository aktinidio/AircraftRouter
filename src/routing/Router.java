package routing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class Router {

	private final List<Edge> edges = new ArrayList<Edge>();
	private final List<Vertex> nodes = new ArrayList<Vertex>();
	private QPPTWarrivalFix qpptw;
	private String[][] gates;

 	public Router() {
		try {
			initialiseZurichGraph();
			//initialiseArlandaGraph(5, "Arlanda - cor.xls", 667);
		} catch (BiffException | IOException e) {}	
	}
 	
	public int[] exectue(int source, int target, int[][] nodesIDs) {

		if (source == -1)
			return new int[] { 0 };
		return qpptw.runSimpleQpptw(source, target, nodesIDs);

	}
	
	private void initialiseZurichGraph() throws BiffException, IOException {
		Workbook w = Workbook.getWorkbook(new File("Zurich.xls"));
		Sheet s0 = w.getSheet(0);

		for (int i = 0; i < 424; i++)
			nodes.add(new Vertex(i));

		for (int i = 1; i < s0.getRows(); i++)
			edges.add(new Edge(nodes.get(Integer.parseInt(s0.getCell(0, i).getContents())), nodes.get(Integer.parseInt(s0.getCell(1, i).getContents())), Double.parseDouble(s0.getCell(2, i).getContents())));
		
		qpptw = new QPPTWarrivalFix(edges, nodes, null);
	}
	
	@SuppressWarnings("unused")
	private void initialiseArlandaGraph(int day, String name, int noOfNodes) throws BiffException, IOException {
		
		Workbook w = Workbook.getWorkbook(new File(name));

		Sheet s0 = w.getSheet(0), s1 = w.getSheet(day);

		// Read nodes
		for (int i = 0; i < noOfNodes; i++)
			nodes.add(new Vertex(i));

		// Read distance of edges
		for (int i = 1; i < s0.getRows(); i++)
			edges.add(new Edge(nodes.get(Integer.parseInt(s0.getCell(0, i).getContents())), nodes.get(Integer.parseInt(s0.getCell(1, i).getContents())), Double.parseDouble(s0.getCell(2, i).getContents())));

		// Read stand codes
		gates = new String[2][s1.getRows() - 1];
		for (int i = 1; i < s1.getRows(); i++) {
			gates[0][i - 1] = s1.getCell(0, i).getContents();
			gates[1][i - 1] = s1.getCell(1, i).getContents();
		}
		
		qpptw = new QPPTWarrivalFix(edges, nodes, null);
	}
}