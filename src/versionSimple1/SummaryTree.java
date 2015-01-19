package versionSimple1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import support.solver.YicesProcessInterface;
import Component.NodeContent;
import Component.WrappedSummary;
import analysis.Expression;
import analysis.Variable;

public class SummaryTree {
	private static final String YICES_CHECK = "(check)\n";
	private static int maxLevel = 5, maxSearchSpace = 20;
	private static int MAX_UNSOLVED = 0;
	private static YicesProcessInterface solver;
	
	public static DefaultMutableTreeNode buildSummaryTree(
			List<WrappedSummary> summarySet, WrappedSummary target){
		try {
			solver = new YicesProcessInterface(YicesProcessInterface.yicesLocation);
		} catch (IOException e) {
			throw new AssertionError("YicesProcessInterface initialization failures with "+YicesProcessInterface.yicesLocation);
		}
		
		return (new SummaryTree()).generateTreeSqeuence(summarySet, target);
	}
	
	public DefaultMutableTreeNode generateTreeSqeuence(List<WrappedSummary> summarySet, WrappedSummary target){
		NodeContent targeContnt = new NodeContent(target, target.conditions);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(targeContnt);
		List<DefaultMutableTreeNode> leaves = new ArrayList<DefaultMutableTreeNode>();
		leaves.add(root);
		int currentLevel = 0;
		for(currentLevel=0;currentLevel<maxLevel; currentLevel++){
			List<DefaultMutableTreeNode> newLeaves = new ArrayList<DefaultMutableTreeNode>();
			for(DefaultMutableTreeNode leaf : leaves){
				List<DefaultMutableTreeNode> expanded = expandLeaves(summarySet,leaf);
				if(expanded == null ) continue;
				newLeaves.addAll(expanded);
			}
			//population control
			if(newLeaves.size()>maxSearchSpace){
				int size = newLeaves.size();
				for( int k = size -1 ; k>=maxSearchSpace ; k--){ newLeaves.remove(k); }
				leaves = newLeaves;
			}else{ leaves = newLeaves; }
			if(leaves.isEmpty()) break;
		}
		return root;
	}
	
	/**
	 * Legacy code from previous version
	 * Given leaf, try to expand as much as possible. 
	 * If leaf is an entry which means the branch is complete, then return null
	 * If leaf is fully solved (No variable left), then return all possible entries.
	 * 
	 * If a pathsummary is found for the satisfaction with cumulative constraints,
	 *    if it is entry check if and only if the variables left are all in the entry
	 *    append it. 
	 * 
	 * @param summarySet
	 * @param leaf
	 * @return
	 */
	public  List<DefaultMutableTreeNode> expandLeaves(List<WrappedSummary> summarySet, DefaultMutableTreeNode leaf){
		NodeContent content = (NodeContent) leaf.getUserObject();
		if(content.summary.isEntry){ return null; }
		
		List<DefaultMutableTreeNode> result = new ArrayList<DefaultMutableTreeNode>();
		Set<Variable> vars = Expression.getUnqiueVarSet(content.cumulativeConstraint);
//		System.out.println("getUnqiueVarSet: "+vars.size());
//		for(Expression expre : content.cumulativeConstraint){
//			System.out.println(expre.toYicesStatement());
//		}
		if(vars.size() == 0){
			for(WrappedSummary sum : summarySet){
				if(sum.isEntry){
					NodeContent newContent = new NodeContent(sum,content.cumulativeConstraint);
					DefaultMutableTreeNode child = new DefaultMutableTreeNode(newContent);
					leaf.add(child);
					result.add(child);
				}
			};
			return result;
		}
		
		for(WrappedSummary summary : summarySet){ 
			//TODO check valid symbolic state
			//e.g. x = y+1; y = x+1 is arbitrary
			//assume such will result as x= y+1; y=y+1+1
			//assume no register var will be present e.g. v1,v2
			
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
			if(related == false) continue;
			
			//build statements 
			for(Expression oneConstraint : summary.conditions){
				copiedConstraint.add(oneConstraint);
			}
			List<String> statements = new ArrayList<String>();
			//add variable declaration statements
			Set<Variable> varSet = Expression.getUnqiueVarSet(copiedConstraint);
			for(Variable var : varSet){
				statements.add(var.toVariableDefStatement());
			}
			//add assertion statements
			for(Expression f : copiedConstraint){
				String assertion = Expression.createAssertion(f.toYicesStatement());
				statements.add(assertion);
			}
			statements.add(YICES_CHECK);
			
			//check satisfiability
			boolean sat = this.solver.solve(true, statements.toArray(new String[0]));
			if(sat == false) continue;
			
			//if the appended summary is entry, check if the amount of unsolved symbolic variable
			if(summary.isEntry){
				Set<Variable>  unsolved = Expression.getUnqiueVarSet(copiedConstraint);
				Set<Variable> whiteList = Expression.getUnqiueVarSet(summary.conditions);
				for(Variable var : whiteList){
					unsolved.remove(var);
				}
				if(unsolved.size() > MAX_UNSOLVED){
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
}
