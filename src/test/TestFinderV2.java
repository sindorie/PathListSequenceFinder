package test;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import Component.NodeContent;
import Component.WrappedSummary;
import support.Utility;
import support.arithmetic.*;
import support.solver.YicesProcessInterface;
import version2.PathListSequenceFinder; 

public class TestFinderV2 {

	//change this
	static String path = "/home/zhenxu/Tools/Solver/yices-2.2.2/bin/yices";
	
	static int basicTestCase = 0;
	public static void main(String[] args) throws Exception {
		serializedTest(false);
	}
	
//	static void testLoadLibrary(){
//		String path = "/home/zhenxu/workspace/EventGenerator/libs/libyices.so";
//		System.load(path);
//	}
	
	public static void basicTest() throws IOException{
		List<WrappedSummary> list = null;
		
		switch(basicTestCase){
		case 0:list = TestSummary.basicSummary1(); break;
		case 1:list = TestSummary.basicSummary2(); break;
		case 2:list = TestSummary.basicSummary3(); break;
		}
		
		for(WrappedSummary sum : list){
			System.out.println(sum.toStringDetail());
		}
		
		YicesProcessInterface solver = new YicesProcessInterface(path);
		PathListSequenceFinder plf = new PathListSequenceFinder(solver);
		Set<WrappedSummary> set = new HashSet<WrappedSummary>();
		set.addAll(list);
		DefaultMutableTreeNode node = plf.findSequence(set, list.get(0));
		
		showInformaion(node);
	}
	
	public static void serializedTest(boolean force) throws IOException{
		
		String name = "sumlist"+basicTestCase;
		if(force)Utility.deleteObject(name);
		
		Object o = Utility.readObject(name);
		DefaultMutableTreeNode node;
		if(o == null){
			List<WrappedSummary> list = TestSummary.basicSummary1();
			switch(basicTestCase){
				case 0:list = TestSummary.basicSummary1(); break;
				case 1:list = TestSummary.basicSummary2(); break;
				case 2:list = TestSummary.basicSummary3(); break;
			}
			
			YicesProcessInterface solver = new YicesProcessInterface(path);
			PathListSequenceFinder plf = new PathListSequenceFinder(solver);
			Set<WrappedSummary> set = new HashSet<WrappedSummary>();
			set.addAll(list);
			node = plf.findSequence(set, list.get(0));
			Utility.writeObject(name, node);
		}else{
			node = (DefaultMutableTreeNode)o;
		}
		
		System.out.println("Complete sequence");
		DefaultMutableTreeNode current = node.getFirstLeaf();
		while(current != null){
			if(((NodeContent)current.getUserObject()).summary.isEntry){
				System.out.println(Arrays.toString(current.getPath()));
			}
			current = current.getNextLeaf();
		}
		
		
		showInformaion(node);
	}
	
	public static void showInformaion(DefaultMutableTreeNode node){
		final JEditorPane edit = new JEditorPane();
		final JTree tree = new JTree(node);
		edit.setEditable(false);
		
		TreeSelectionListener listener = new TreeSelectionListener(){ 
			@Override
			public void valueChanged(TreeSelectionEvent e) { 
		        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                        tree.getLastSelectedPathComponent();
		        
		       NodeContent content = (NodeContent) node.getUserObject();
		       StringBuilder sb = new StringBuilder();
		       
		       BLOCK1:{//Cumulative constraints
			       sb.append("-------Cumulative constraints-------\n");
			       if(content.cumulativeConstraint.isEmpty()){
			    	   sb.append("None\n\n");
			       }else{
			    	   for(Formula f:content.cumulativeConstraint){
				    	   sb.append(f.toYicesStatement()+"\n");
				       }
				       sb.append("\n");
			       
				       Set<Variable> vars = Expression.getUnqiueVarSet(content.cumulativeConstraint, PathListSequenceFinder.variablePattern);
				       Variable[] varArr = vars.toArray(new Variable[0]);
				       for(int i=0;i<varArr.length;i++){
				    	   sb.append(varArr[i].toVariableDefStatement());
				    	   if(i%3 == 0) sb.append("\n");
				       }
				       sb.append("\n");
			       }
		       }
		       
		       BLOCK2:{//Summary constraints-
			       sb.append("---------Summary constraints---------\n");
			       if(content.summary.conditions.isEmpty()){
			    	   sb.append("None\n\n");
			       }else{
			    	   for(Formula f: content.summary.conditions){
				    	   sb.append(f.toYicesStatement()+"\n");
				       }
				       sb.append("\n");
				       
				       Set<Variable> vars = Expression.getUnqiueVarSet(content.summary.conditions, PathListSequenceFinder.variablePattern);
				       Variable[] varArr = vars.toArray(new Variable[0]);
				       for(int i=0;i<varArr.length;i++){
				    	   sb.append(varArr[i].toVariableDefStatement());
				    	   if(i%3 == 0) sb.append("\n");
				       }
				       sb.append("\n");
			       }
		       }
		       
		       BLOCK3:{//Summary symbolic states
			       sb.append("-------Summary symbolic states-------\n");
			       if(content.summary.symbolic.isEmpty()){
			    	   sb.append("None\n\n");
			       }else{
			    	   for(Assignment f: content.summary.symbolic){
				    	   sb.append(f.toYicesStatement());
				       }
				       sb.append("\n");
				       Set<Variable> vars = Expression.getUnqiueVarSet(content.summary.symbolic, PathListSequenceFinder.variablePattern);
				       Variable[] varArr = vars.toArray(new Variable[0]);
				       for(int i=0;i<varArr.length;i++){
				    	   sb.append(varArr[i].toVariableDefStatement());
				    	   if(i%3 == 0) sb.append("\n");
				       }
				       sb.append("\n");
			       }
		       }
		       
		       edit.setText(sb.toString());
			} 
		};
		
		tree.addTreeSelectionListener(listener);
		JScrollPane treeView = new JScrollPane(tree);
		
		treeView.setMinimumSize(new Dimension(300,200));
		edit.setMaximumSize(new Dimension(300,-1));
		Utility.showTwoPanel(treeView, edit);
	}
	
	

		
	


}
