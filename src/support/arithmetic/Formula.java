package support.arithmetic;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 
 * @author zhenxu
 *
 * @Purpose An effective data structure is needed for 1) the storage
 * of arithmetic formula, 2) data manipulation and 3) data management. 
 *
 * @Requirements 
 * 1.	Data storage -- representation of arithmetic formula
 * 2.	Data manipulation -- replacing on variable, merging of formulas
 * 3.	Data management -- records of variables
 */
public class Formula extends Expression{
	private static final long serialVersionUID = 1L;
	
	public Formula(String equalityOperator, Expression left, Expression right) {
		super(equalityOperator);
		this.add(left);
		this.add(right);
	}
	
	public Formula(Formula other){
		this(other.getUserObject().toString(),other.getLeft().clone(), other.getRight().clone());
	}
	
	public Expression getLeft(){
		return (Expression) this.getChildAt(0);
	}
	public Expression getRight(){
		return (Expression) this.getChildAt(1);
	}
	
	@Override
	public Formula clone(){
		return new Formula(this);
	}
	
	public List<String> getVarDefStatments(){
		List<String> result = new ArrayList<String>();
		DefaultMutableTreeNode leaf = this.getFirstLeaf();
		do{
			if(leaf instanceof Variable){
				Variable var = (Variable)leaf;
				String statemt = var.toVariableDefStatement();
				if(!result.contains(statemt)) result.add(statemt);
			}
			leaf = leaf.getNextLeaf();
		}while(leaf != null);
		return result;
	}
	
	public boolean renameVariable(String targetName, String newName){
		boolean anyChange = false;
		DefaultMutableTreeNode leaf =  this.getFirstLeaf(); 
		while(leaf != null){
			if(leaf instanceof Variable){
				Variable var = (Variable)leaf;
				if(var.name.equals(targetName)){
					var.name = newName;
					anyChange = true;
				}
			}
			leaf = leaf.getNextLeaf();
		}
		return anyChange;
	}
}
