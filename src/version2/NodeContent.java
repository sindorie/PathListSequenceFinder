package version2;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import support.arithmetic.Formula;
/**
 * A simple wrapper which encapsulates necessary data for the ConstraintTree
 * 
 * @author zhenxu
 *
 */
public class NodeContent implements Serializable{
	public ParsedSummary summary;
	public Set<Formula> cumulativeConstraint;
	NodeContent(ParsedSummary content){
		this.summary = content ;
	}
	NodeContent(ParsedSummary content, Set<Formula> constraint){
		this.summary = content ;
		this.cumulativeConstraint = constraint;
	}
	
	@Override
	public String toString(){
		return summary.toString();
	}
} 