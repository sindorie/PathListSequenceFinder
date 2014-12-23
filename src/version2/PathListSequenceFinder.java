package version2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import support.arithmetic.Assignment;
import support.arithmetic.Expression;
import support.arithmetic.Formula;
import support.arithmetic.Variable;
import support.solver.YicesProcessInterface; 



public class PathListSequenceFinder {
	
	public final static boolean DEBUG = true;
	
	static boolean stepControl = false;
	static Scanner sc;
	static{
		if(stepControl && DEBUG){
			sc = new Scanner(System.in);
		}
	}
	
	public static int MAX_UNSOLVED = 0;
	public static String variablePattern = "\\$.*";
	public static String regVarPattern = "v.*";
	public static String tmpVarPattern = "T.*";
	private int index = 0;
	
	private int maxLevel = 20;
	private int maxSearchSpace = 100;
	private YicesProcessInterface solver;
	
	public static final String YICES_CHECK = "(check)\n";
	public PathListSequenceFinder(YicesProcessInterface solver){ 
		this.solver = solver;
	}

	public DefaultMutableTreeNode findSummaryPath(Set<ParsedSummary> summarySet, ParsedSummary target){
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
	
	private List<DefaultMutableTreeNode> expandLeaves(Set<ParsedSummary> summarySet, DefaultMutableTreeNode leaf){
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
		
		for(ParsedSummary summary : summarySet){
			//TODO check valid symbolic state
			//e.g. x = y+1; y = x+1 is arbitrary
			//assume such will result as x= y+1; y=y+1+1
			//assume no register var will be present e.g. v1,v2
			if(DEBUG){
				System.out.println("----Current Summary----");
				System.out.println(summary.toStringDetail());
			}
			
			
			//Relativity checking
			Set<Formula> copiedConstraint = new HashSet<Formula>();
			for(Formula f: content.cumulativeConstraint){
				copiedConstraint.add(f.clone());
			}
			boolean related = false;
			for(int index = summary.symbolic.size()-1 ; index >=0 ; index--){
				Assignment assign = summary.symbolic.get(index);
				for(Formula condition: copiedConstraint){
					boolean anyChange = condition.replace(assign.getLeft(), assign.getRight().clone());
					related = related || anyChange;
				}
			}
			if(DEBUG){
				System.out.println("Relativity checking: "+ related);
			}
			if(related == false) continue;
			
			
			//build statements
			copiedConstraint.addAll(summary.conditions);
			for(Formula f :copiedConstraint){
				System.out.println(f.toYicesStatement());
			}
			
			
			List<String> statements = new ArrayList<String>();
			//define variables 
			System.out.println("getUnqiueVarSet");
			Set<Variable> varSet = Expression.getUnqiueVarSet(copiedConstraint, variablePattern);
			for(Variable var : varSet){
				statements.add(var.toVariableDefStatement());
			}
			//add assertion
			for(Formula f : copiedConstraint){
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
				Set<Variable>  unsolved = Expression.getUnqiueVarSet(copiedConstraint,variablePattern);
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
	
}

