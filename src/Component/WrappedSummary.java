package Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set; 

import zhen.version1.component.Event;
import analysis.Expression;
import concolic.PathSummary;

public class WrappedSummary implements Serializable{
	public final Set<Expression> conditions;
	public final List<Expression> symbolic;
	public final List<String> executionLog;
	public boolean isEntry;
	public String methodSignature;
	public PathSummary summaryReference;
	static int aindex = 0;
	public int index;
	
	public WrappedSummary(PathSummary summary){
		this(summary,false);
	}
	
	public WrappedSummary(PathSummary summary,boolean isEntry){
		conditions = new HashSet<Expression>();
		conditions.addAll(summary.getPathCondition());
		symbolic = summary.getSymbolicStates();
		executionLog = summary.getExecutionLog();
		methodSignature = summary.getMethodSignature();
		index = aindex++;
		this.isEntry = isEntry;
		summaryReference = summary;
	}
	
	public String toString(){
//		return "#"+index+" "+conditions.size()+":"+symbolic.size()+":"+executionLog.size()+":"+isEntry;
		return String.format("%20s", this.methodSignature);
	}
	
	public String toStringDetail(){
		StringBuilder sb = new StringBuilder();
		sb.append("ParsedSummary #"+index+" "+(isEntry?"is entry":"")+"\n");
		sb.append(" Conditions: \n");
		for(Expression f : conditions){
			sb.append("\t"+f.toYicesStatement()+"\n");
		}
		sb.append(" SymbolicState: \n");
		for(Expression a : symbolic){
			sb.append("\t"+a.toYicesStatement()+"\n");
		}
		sb.append(" ExecutionLog: ");
		sb.append(executionLog+"\n");

		return sb.toString();
	}
}
