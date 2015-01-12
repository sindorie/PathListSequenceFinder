package version2;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode; 

import concolic.Expression;
import concolic.PathSummary;
import concolic.Variable;
import support.GUI.SummarySelectionWindow;
import support.GUI.UIUtility;
import support.solver.YicesProcessInterface; 



public class PathListSequenceFinder {
	
	public final static boolean DEBUG = true;
	public static int MAX_UNSOLVED = 0;
	
	static boolean stepControl = false;
	static Map<String,String> typeMap = new HashMap<String,String>();
	static Map<String,String> operatorMap = new HashMap<String,String>();
	static Pattern tmpRgister = Pattern.compile("\\$?[vp]\\d?"); 
	static Pattern parameter = Pattern.compile("\\$?parameter\\d?"); 
	static Scanner sc;
	static{
		if(stepControl && DEBUG){
			sc = new Scanner(System.in);
		}
		
		String[] typePairs = {
				"I","int",
				//TODO
			};
		for(int i=0;i<typePairs.length;i+=2){
			typeMap.put(typePairs[i], typePairs[i+1]);
		}
		
		String[] operatorPairs = {
				"add", "+",
				"sub", "-",
				"mul", "*",
				"dev", "/",
				
				
				//do no support in this version TODO
//				"and", "and",
//				"or", "or",
//				"xor", "xor",
//				"neg", "neg",  //this should be replace by xor X true 
				
				"<","<",
				">",">",
				"=","=",
				"!=","/=",
				"<=","<=",
				">=",">=",
		};
		for(int i =0;i<operatorPairs.length;i+=2){
			operatorMap.put(operatorPairs[i], operatorPairs[i+1]);
		}
	}
	
	
	private int index = 0;
	private int maxLevel = 20;
	private int maxSearchSpace = 100;
	private YicesProcessInterface solver;
	public static final String YICES_CHECK = "(check)\n";
	
//	public static String variablePattern = "\\$.*";
//	public static String regVarPattern = "v.*";
//	public static String tmpVarPattern = "T.*";
	 
	public PathListSequenceFinder(YicesProcessInterface solver){ 
		this.solver = solver;
	}

	/**
	 * assume only one summary could reach target line
	 * @param rawList
	 * @param targetLineSignature
	 * @return
	 */
	public static void filterAndselect(List<PathSummary> rawList){
		//filter pathSummary 
		List<PathSummary> filtered = new ArrayList<PathSummary>();
//		List<WrappedSummary> wrappedList = new ArrayList<WrappedSummary>();
		DefaultListModel<WrappedSummary> model = new DefaultListModel<WrappedSummary>();
		for (PathSummary summary : rawList) {
			PathSummary copy = new PathSummary();
			//signature
			copy.setMethodSignature(summary.getMethodSignature());
			//log
			for(String line : summary.getExecutionLog()){
				copy.addExecutionLog(line);
			}
			//constraint
			copy.setPathCondition(transformExpression(summary.getPathCondition()));
			// expression
			copy.setSymbolicStates(transformExpression(summary.getSymbolicStates()));
			//add copy to list
			filtered.add(copy);
			WrappedSummary wrapped = new WrappedSummary(copy);
//			wrappedList.add(wrapped);
			model.addElement(wrapped);
		}
		
		SummarySelectionWindow selectionWindow = new SummarySelectionWindow();
		selectionWindow.setListModel(model);
		
		selectionWindow.show();

//		return findSummaryPath(wrappedList, target);
	}

	public DefaultMutableTreeNode findSummaryPath(List<WrappedSummary> summarySet, WrappedSummary target){
		if(DEBUG){
			System.out.println("input");
			System.out.println(summarySet);
			System.out.println(target);
		}
		
		NodeContent targeContnt = new NodeContent(target, target.conditions);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(targeContnt);
		List<DefaultMutableTreeNode> leaves = new ArrayList<DefaultMutableTreeNode>();
		leaves.add(root);
		int currentLevel = 0;
		for(currentLevel=0;currentLevel<maxLevel; currentLevel++){
			if(DEBUG) System.out.println("Level #"+currentLevel);
			
			List<DefaultMutableTreeNode> newLeaves = new ArrayList<DefaultMutableTreeNode>();
			for(DefaultMutableTreeNode leaf : leaves){
				List<DefaultMutableTreeNode> expanded = expandLeaves(summarySet,leaf);
				if(expanded == null ) continue;
				newLeaves.addAll(expanded);
			}
			if(DEBUG) System.out.println("Generatd Leaves amount: "+newLeaves.size());
			
			//population control
			if(newLeaves.size()>maxSearchSpace){
				System.out.println("maxSearchSpace achieved");
				int size = newLeaves.size();
				for( int k = size -1 ; k>=maxSearchSpace ; k--){ newLeaves.remove(k); }
				leaves = newLeaves;
			}else{ leaves = newLeaves; }
			if(leaves.isEmpty()) break;
		}
		if(DEBUG) System.out.println("Final level: "+ currentLevel+" vs max: "+maxLevel);
		
		return root;
	}
	
	private List<DefaultMutableTreeNode> expandLeaves(List<WrappedSummary> summarySet, DefaultMutableTreeNode leaf){
		if(DEBUG){
			System.out.println("expandLeaves");
		}
		NodeContent content = (NodeContent) leaf.getUserObject();
		if(DEBUG){
			System.out.println(content.summary.toStringDetail());
		}
		
		if(content.summary.isEntry){
			if(DEBUG) System.out.println("Target is entry");
			return null;
		}
		List<DefaultMutableTreeNode> result = new ArrayList<DefaultMutableTreeNode>();
//		Set<Variable> conVars = Expression.getUnqiueVarSet(content.cumulativeConstraint, variablePattern);
		
		for(WrappedSummary summary : summarySet){ 
			//TODO check valid symbolic state
			//e.g. x = y+1; y = x+1 is arbitrary
			//assume such will result as x= y+1; y=y+1+1
			//assume no register var will be present e.g. v1,v2
			if(DEBUG){
				System.out.println("----Current Summary----");
				System.out.println(summary.toStringDetail());
			}
			
			
			//Relativity checking
			Set<Expression> copiedConstraint = new HashSet<Expression>();
			for(Expression f: content.cumulativeConstraint){
				copiedConstraint.add(f.clone());
			}
			boolean related = false;
			for(int index = summary.symbolic.size()-1 ; index >=0 ; index--){
				Expression assign = summary.symbolic.get(index);
				if(assign.getChildCount() != 2) continue; //which should not be 
				
				for(Expression condition: copiedConstraint){
					boolean anyChange = condition.replace(
							((Expression)assign.getChildAt(0)).clone(), 
							((Expression)assign.getChildAt(1)).clone());
					related = related || anyChange;
				}
			}
			if(DEBUG){
				System.out.println("Relativity checking: "+ related);
			}
			if(related == false) continue;
			
			
			//build statements
			copiedConstraint.addAll(summary.conditions);
			for(Expression f :copiedConstraint){
				System.out.println(f.toYicesStatement());
			}
			
			
			List<String> statements = new ArrayList<String>();
			//define variables 
			System.out.println("getUnqiueVarSet");
			Set<Variable> varSet = Expression.getUnqiueVarSet(copiedConstraint);
			for(Variable var : varSet){
				statements.add(var.toVariableDefStatement());
			}
			//add assertion
			for(Expression f : copiedConstraint){
				String assertion = Expression.createAssertion(f.toYicesStatement());
				statements.add(assertion);
			}
			statements.add(YICES_CHECK);
			
			if(DEBUG){
				for(String state : statements){
					System.out.println(state);
				}
				if(stepControl){
					String next = sc.nextLine().trim();
					if(next.equals("0")) System.exit(0);
					else if(next.equalsIgnoreCase("9")){
						stepControl = false;
					}
				}
				System.out.println("Checking satisfiability");
			}
			
			
			//check satisfiability
			boolean sat = this.solver.solve(true, statements.toArray(new String[0]));
			if(DEBUG) System.out.println("sat: "+sat);
			if(sat == false) continue;
			
			//if the appended summary is entry, check if the amount of unsolved symbolic variable
			if(summary.isEntry){
				Set<Variable>  unsolved = Expression.getUnqiueVarSet(copiedConstraint);
				if(DEBUG){
					System.out.println("Remaining unsolved vars: "+unsolved);
				}
				if(unsolved.size() > MAX_UNSOLVED){
					if(DEBUG){
						System.out.println("Cancel appending entry summary: "+unsolved.size()+"> max:"+MAX_UNSOLVED);
					}
					continue;
				}
			}
			
			
			NodeContent newContent = new NodeContent(summary,copiedConstraint);
			DefaultMutableTreeNode child = new DefaultMutableTreeNode(newContent);
			leaf.add(child);
			result.add(child);
		}
		return result;
	}
	
	
	private List<NodeContent> prioritize(NodeContent node, List<NodeContent> qualified){
		//TODO
		return qualified;
	}
	
	
	
	
	private static  String fieldInstanceDigIn(Expression expre){
		String content = expre.getContent();
		//$Finstance (field_signature object_expression)
		if(expre.isLeaf()) return content;
		
		String postPart = ((Expression)expre.getChildAt(0) ).getContent();
		if(content.equalsIgnoreCase("$Finstance")){
			String prePart = fieldInstanceDigIn((Expression)expre.getChildAt(1));
//			if(prePart == null) return null;
			return prePart+postPart;
		}else{
			String prePart = ((Expression) expre.getChildAt(1)).getContent();
//			if(isValidParameter(prePart) == false) return null;
			return prePart + postPart;
		}
	}
	
	private static ArrayList<Expression> transformExpression(List<Expression> list){
		ArrayList<Expression> copiedConstraintList = new ArrayList<Expression>();
		 //TODO
 
		MAJOR: for (Expression expre : list) {
			Expression copyExpression = expre.clone();
			//condense -- find the special case for the field/object/static access 
			//$Fstatic (field_signature), $Finstance (field_signature object_expression), 
			//other special: $return , $api
			
			List<Expression> queue = new ArrayList<Expression>();
			queue.add(copyExpression);
			
			//check valid node
			while(queue.isEmpty() == false){
				Expression currentNode = queue.remove(0);
				String currentContent = currentNode.getContent();
				if(currentContent.equalsIgnoreCase("$Fstatic")){
					Expression child = (Expression)currentNode.getChildAt(0);
					String childContent = child.getContent();
					int lastIndex = childContent.lastIndexOf(":");
					
					String type = childContent.substring(lastIndex, childContent.length()).replace(";", "").trim();
					if(typeMap.get(type) == null) continue MAJOR;
					
					String fieldSig = childContent.replaceAll("\\/|(;->)|;", "");
					currentNode.setUserObject(fieldSig);
					currentNode.removeAllChildren();
				}else if(currentContent.equalsIgnoreCase("$Finstance")){
					//recursively dig in
					String condensedContent = fieldInstanceDigIn(currentNode);
					
					String[] parts = condensedContent.split(":");
					String last = parts[parts.length-1].replace(";", "").trim();
					
					if(typeMap.get(last) == null){ continue MAJOR;  }
					
					currentNode.setUserObject(condensedContent);
					currentNode.removeAllChildren();
				}else if(currentContent.equalsIgnoreCase("$return")){
					continue MAJOR;
				}else if(currentContent.equalsIgnoreCase("$api")){
					continue MAJOR;
				}else if(currentNode.isLeaf()){
//					if(isValidParameter(currentContent) == false){
//						continue MAJOR;
//					}
				}
				
				for(int i =0;i < currentNode.getChildCount() ; i++){
					queue.add((Expression)currentNode.getChildAt(i));
				}
			}
			
			Enumeration<Expression> nodeEnum = copyExpression.depthFirstEnumeration();
			while(nodeEnum.hasMoreElements()){
				Expression node = nodeEnum.nextElement();
				String content = node.getContent();
				if(node.isLeaf()){//check valid variable
					if(isTmpVariable(content)){
						continue MAJOR;
					}
					if(content.startsWith("#string")){
						//check string constant which is not supported yet
						//TODO
						continue MAJOR;
					}else if(content.startsWith("#")){
						//check if it is number
						String filterd = content.replace("#", "");
						
						try{  
							long val = Long.decode(filterd); 
							node.setUserObject(val+"");//treat it as constant
						}catch(NumberFormatException nfe){
							continue MAJOR; 
						}
					}else{
						content.replaceAll(";->|;|\\/", "");//remove ";->" or ";" or "\" 
						content.replaceAll("<|>", "_");//replace <> to _
						
						if(content.split(":").length <=1) continue MAJOR;
						
						String subString = content.substring(content.lastIndexOf(":")+1,content.length()).trim();
						
						String parsedType = typeMap.get(subString);
						if(parsedType == null) continue MAJOR;
						
						String name = content.substring(0, content.lastIndexOf(":")).replaceAll("[^a-zA-Z0-9-]", "").replaceAll("-", "_").trim();
						
						Variable var = new Variable(name,parsedType);
						
						Expression parent = (Expression) node.getParent();
						int index = parent.getIndex(node);
						assert index >= 0;
						parent.remove(index);
						parent.insert(var, index);
					}
				}else{
					//check valid operation
					String parsedOperator = operatorMap.get(content);
					if(parsedOperator == null) continue MAJOR;
					//valid operator 
					node.setUserObject(parsedOperator);
				}
			}
			copiedConstraintList.add(copyExpression);
		}
		return copiedConstraintList;
	}
	
	private static boolean isTmpVariable(String input){
		Matcher match1 =  tmpRgister.matcher(input);
		if(match1.find() && ((match1.end()-match1.start()) == input.length())) return true;

		Matcher match2 =  parameter.matcher(input);
		if(match2.find() && ((match2.end()-match2.start()) == input.length())) return true;

		return false;
	}
}

