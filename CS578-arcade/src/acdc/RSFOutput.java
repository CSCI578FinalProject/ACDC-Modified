package acdc;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

/**
* This class has one method which creates an RSF file.
* 
* The string representation of the output is of the format: 
*
* contain parent_node node
*
*/
public class RSFOutput implements OutputHandler 
{
	public void writeOutput(String outputName, DefaultMutableTreeNode root) 
	{
		PrintWriter out = null;
		try 
		{
			out = new PrintWriter(new BufferedWriter(new FileWriter(outputName)));
		} 
		catch (IOException e) 
		{
			System.err.println(e.getMessage());
		}

		Node ncurr, nj, ni, np;
		DefaultMutableTreeNode curr, i, j, pi;

		Enumeration allNodes = root.breadthFirstEnumeration();

		// Avoid output for the root node
		i = (DefaultMutableTreeNode) allNodes.nextElement();
		
		String lastName = null;
		Map<String, Integer> map = new HashMap<String, Integer>(); 

		while (allNodes.hasMoreElements()) 
		{
			i = (DefaultMutableTreeNode) allNodes.nextElement();

			ni = (Node) i.getUserObject();

			pi = (DefaultMutableTreeNode) i.getParent();

			np = (Node) pi.getUserObject();
			
			String cleanNpName = np.getName();
			if (np.getName().startsWith("\"") && !np.getName().endsWith("\"")) {
				cleanNpName = np.getName().substring(1, np.getName().length());
			}
			
			if (!map.containsKey(cleanNpName)) {
				map.put(cleanNpName, 0);
			}
			
			if (lastName == null || !lastName.equals(cleanNpName)) {
				map.put(cleanNpName, map.get(cleanNpName) + 1);
			}
			
			lastName = cleanNpName;
			
			
			

			if (pi != root) out.println("contain " + cleanNpName + map.get(cleanNpName) + " " + ni.getName());
		}
		out.close();
	}
}