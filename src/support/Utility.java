package support;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import main.Paths;

public class Utility {

	public static <T> T[] combineArrays(T[]... arrays){
		int totalLeng = 0;
		for(T[] arr : arrays){
			totalLeng += arr.length;
		}
		
		@SuppressWarnings("unchecked")
		T[] result = (T[]) new Object[totalLeng];
		int index = 0;
		for(T[] arr:arrays){
			for(int i=0;i<arr.length;i++){
				result[index] = arr[i];
				index += 1;
			}
		}
		return result;
	}
	
	public static <E> ArrayList<E> createList(E... es){
		ArrayList<E> result = new ArrayList<E>();
		for(E e: es){
			result.add(e);
		}
		return result;
	}
	
	public static String path = Paths.appDataDir;
	
	public static boolean deleteObject(String name){
		File f = new File(path+name);
		if(f.exists()){
			return f.delete();
		}
		return true;
	}
	
	public static boolean writeObject(String name, Object o){
		try {
			File f = new File(Paths.appDataDir);
			if(!f.exists()){
				boolean created = f.mkdir();
				if(created == false) System.out.println("folder creation fails");
			}
			System.out.println("writing to "+path+name);
			FileOutputStream fileOut = new FileOutputStream(path+name);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(o);
	        out.close();
	        fileOut.close();
			
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public static Object readObject(String name){
		try {
			FileInputStream fileIn = new FileInputStream(path+name);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			Object result;
			result = in.readObject();
			in.close();
			fileIn.close();
			return result;
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
			System.out.println("Reading objects from disk fails"); 
		}
        return null;
	}
	
	public static void showTree(TreeNode root){
		JTree tree = new JTree(root); 
		JScrollPane treeView = new JScrollPane(tree);
		Dimension minimumSize = new Dimension(100, 50);
        treeView.setMinimumSize(minimumSize);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(treeView);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
	}
		
	public static void showTwoPanel(JComponent panel1, JComponent panel2){
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(panel1);
		splitPane.setRightComponent(panel2);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(splitPane);

        splitPane.setDividerSize(3);
//        splitPane.setDividerLocation(300);
        
        splitPane.setResizeWeight(0.5);
//        splitPane.setDividerLocation(100); 
        splitPane.setPreferredSize(new Dimension(600, 400));
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);
	}
	
}
