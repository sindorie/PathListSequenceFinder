package Component;

import java.io.Serializable; 
import java.util.Set;

import analysis.Expression;
 
/**
 * A simple wrapper which encapsulates necessary data for the ConstraintTree
 * 
 * @author zhenxu
 *
 */
public class NodeContent implements Serializable{
	public WrappedSummary summary;
	public Set<Expression> cumulativeConstraint;
	NodeContent(WrappedSummary content){
		this.summary = content ;
	}
	public NodeContent(WrappedSummary content, Set<Expression> constraint){
		this.summary = content ;
		this.cumulativeConstraint = constraint;
	}
	
	@Override
	public String toString(){
		return summary.toString();
	}
} 