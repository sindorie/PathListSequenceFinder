package support;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class BracaketBlockSeparator {
	public static final String NODE_STRING = "block";
	public static void main(String[] args){
		show("(X + (Y + D) + Z)");
	}
	
	public static void show(String input){
		DefaultMutableTreeNode top = buildBracketsTree(input);
		JTree tree = new JTree(top); 
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
	
	
	public static DefaultMutableTreeNode buildBracketsTree(String input, PhaseProcessor processor){
		input = input.trim();
		input = "(" + input + ")";
		
		
		Stack<Object> stack = new Stack<Object>();
		StringBuilder buffer = new StringBuilder();
		for(int i =0; i<input.length() ; i++){
			char c = input.charAt(i);
			switch(c){
			case '\n': throw new AssertionError();
			case '\t': 
			case ' ':{ //separators
				if(buffer.length() > 0){
					String phase = buffer.toString().trim();
					if(processor != null){
						phase = processor.check(phase);
					}
					stack.push(new DefaultMutableTreeNode(phase));
					buffer = new StringBuilder();
				}
			}break;
			case '(':{ //push existing string onto the stack
				if(buffer.length() > 0){
					String phase = buffer.toString().trim();
					if(processor != null){
						phase = processor.check(phase);
					}
					stack.push(new DefaultMutableTreeNode(phase));
					buffer = new StringBuilder();
				}
				stack.push("(");
			}break;
			case ')':{ //pop elements from stack until ')' is encountered
				List<DefaultMutableTreeNode> popedList = new ArrayList<DefaultMutableTreeNode>();
				do{
					Object poped = stack.pop();
					if(poped.toString().equals("(")){ break;
					}else{ popedList.add((DefaultMutableTreeNode)poped); }
				}while(true);
				
				if(buffer.length() > 0){
					String phase = buffer.toString().trim();
					if(processor != null){
						phase = processor.check(phase);
					}
					popedList.add(0, new DefaultMutableTreeNode(phase));
					buffer = new StringBuilder();
				}
				
				
				switch(popedList.size()){
				case 0: throw new AssertionError("empty brackets content"); 
				case 1:{ //{X} --> put X back to stack
					stack.push(popedList.get(0));
				}break;
				default:{ //{X Y Z} --> create a parent node and push onto stack
					DefaultMutableTreeNode parent = new DefaultMutableTreeNode(NODE_STRING);
					for(int index = popedList.size() -1 ; index>=0;index--){
						parent.add(popedList.get(index));
					}
					stack.push(parent);
				}
				}
			}break;
			default:{ // a char
				buffer.append(c);
			}
			}
		}
		
		//"( X Y Z" 
		//"( X Y ( Z"
//		if(stack.size() > 1){
//			//eliminate "("
//			//no necessary at this point
//		}
		
		return (DefaultMutableTreeNode) stack.pop();
	}
	
	
	public static DefaultMutableTreeNode buildBracketsTree(String input){
		input = input.trim();
		input = "(" + input + ")";
		
		
		Stack<Object> stack = new Stack<Object>();
		StringBuilder buffer = new StringBuilder();
		for(int i =0; i<input.length() ; i++){
			char c = input.charAt(i);
			switch(c){
			case '\n': throw new AssertionError();
			case '\t': 
			case ' ':{ //separators
				if(buffer.length() > 0){
					stack.push(new DefaultMutableTreeNode(buffer.toString().trim()));
					buffer = new StringBuilder();
				}
			}break;
			case '(':{ //push existing string onto the stack
				if(buffer.length() > 0){
					stack.push(new DefaultMutableTreeNode(buffer.toString().trim()));
					buffer = new StringBuilder();
				}
				stack.push("(");
			}break;
			case ')':{ //pop elements from stack until ')' is encountered
				List<DefaultMutableTreeNode> popedList = new ArrayList<DefaultMutableTreeNode>();
				do{
					Object poped = stack.pop();
					if(poped.toString().equals("(")){ break;
					}else{ popedList.add((DefaultMutableTreeNode)poped); }
				}while(true);
				
				if(buffer.length() > 0){
					popedList.add(0, new DefaultMutableTreeNode(buffer.toString().trim()));
					buffer = new StringBuilder();
				}
				
				
				switch(popedList.size()){
				case 0: throw new AssertionError("empty brackets content"); 
				case 1:{ //{X} --> put X back to stack
					stack.push(popedList.get(0));
				}break;
				default:{ //{X Y Z} --> create a parent node and push onto stack
					DefaultMutableTreeNode parent = new DefaultMutableTreeNode("block");
					for(int index = popedList.size() -1 ; index>=0;index--){
						parent.add(popedList.get(index));
					}
					stack.push(parent);
				}
				}
			}break;
			default:{ // a char
				buffer.append(c);
			}
			}
		}
		
		//"( X Y Z" 
		//"( X Y ( Z"
//		if(stack.size() > 1){
//			//eliminate "("
//			//no necessary at this point
//		}
		
		return (DefaultMutableTreeNode) stack.pop();
	}
	
	
	public static interface PhaseProcessor{
		public String check(String input);
	}
	
	public static interface LevelTraversalProcessor{
		public void check(String input,int level, int childIndex);
	}
//	
//	public static void levelTravseral(DefaultMutableTreeNode root, LevelTraversalProcessor processor){
//		int level = 1;
//		if(root == null) return;
//		processor.check(root.getUserObject().toString(), 0, 0);
//		
//		//a queue where the 'processed' nodes are stored. 
//		List<DefaultMutableTreeNode> currentQueue = new ArrayList<DefaultMutableTreeNode>();
//		currentQueue.add(root);
//		while(currentQueue != null){
//			DefaultMutableTreeNode node = currentQueue.remove(0);
//			for(int childIndex = 0; childIndex<node.getChildCount(); childIndex++){
//				TreeNode child = node.getChildAt(childIndex);
//				
//			}
//			
//			level += 1;
//		}
//		
//	}

}












