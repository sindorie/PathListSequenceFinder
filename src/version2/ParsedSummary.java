package version2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import support.arithmetic.Assignment;
import support.arithmetic.ExpressionFactory;
import support.arithmetic.Formula;
import support.arithmetic.Variable;

public class ParsedSummary implements Serializable{
	public final Set<Formula> conditions;
	public final List<Assignment> symbolic;
	public final List<Integer> executionLog;
	public boolean isEntry;
	public String startFunction;
	static int aindex = 0;
	public int index;
	
	public ParsedSummary(){
		this.conditions = new HashSet<Formula>();
		this.symbolic  = new ArrayList<Assignment>(); 
		this.executionLog = new ArrayList<Integer>();
		index = aindex++;
	}
	
	public ParsedSummary(List<Formula> conditions, List<Assignment> symbolic, List<Integer> executionLog){
		this();
		this.conditions.addAll(conditions);
		this.symbolic.addAll(symbolic);
		this.executionLog.addAll(executionLog);
	}
	
	public void addConditions(Formula f){this.conditions.add(f);}
	public void addSymbolic(Assignment f){this.symbolic.add(f);}
	public void addExectionLog(int line){this.executionLog.add(line);}
	
	public String toString(){
		return "#"+index+" "+conditions.size()+":"+symbolic.size()+":"+executionLog.size()+":"+isEntry;
	}
	
	public String toStringDetail(){
		StringBuilder sb = new StringBuilder();
		sb.append("ParsedSummary #"+index+" "+(isEntry?"is entry":"")+"\n");
		sb.append(" Conditions: \n");
		for(Formula f : conditions){
			sb.append("\t"+f.toYicesStatement()+"\n");
		}
		sb.append(" SymbolicState: \n");
		for(Assignment a : symbolic){
			sb.append("\t"+a.toYicesStatement()+"\n");
		}
		sb.append(" ExecutionLog: ");
		sb.append(executionLog+"\n");
		
		return sb.toString();
	}
	
	public static ParsedSummary buildSummaryFromWenhao(String[] conditionString, String[] assignString, int[] log, boolean isEntry){
		ParsedSummary result = new ParsedSummary();
	
		if(conditionString!=null)
		for(int i=0;i<conditionString.length ; i++ ){
			Formula f = (Formula) ExpressionFactory.buildFromWenHao(conditionString[i]);
			result.addConditions(f);
		}
		
		//variable, rightString
		if(assignString!=null)
		for(int i=0; i<assignString.length;i+=2 ){
			Assignment assign = new Assignment(
					(Variable) ExpressionFactory.buildFromWenHao(assignString[i]),
					ExpressionFactory.buildFromWenHao(assignString[i+1])
					);
			result.addSymbolic(assign);
		}
		if(log!=null)
		for(int line : log){
			result.addExectionLog(line);
		}
		result.isEntry = isEntry;
		return result;
	}
}
