package support.arithmetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import support.Utility;


/**
 * @Purpose It turns a String input (prefix) into a formula
 * @author zhenxu
 *
 */
public class ExpressionFactory  {
	public static boolean DEBUG = false;
	static List<String> equality = new ArrayList<String>();
	static{
		String[] tmp = {
				">","<","<=",">=","="
		};
		for(String s:tmp){
			equality.add(s);
		}
	}
	
	public static Expression buildFromWenHao(String prefixString) { 
		prefixString = prefixString.trim();
		if(prefixString.equals(""))return null;
		prefixString = '(' + prefixString + ')';
		 
		int level = 0;
		Stack<Object> stack = new Stack<Object>();
		StringBuilder buffer = new StringBuilder();
		if(DEBUG)System.out.println(prefixString);
		
		for(int i =0; i<prefixString.length() ; i++){
			char c = prefixString.charAt(i);
			switch(c){
			case '\n': throw new AssertionError();
			case '\t': 
			case '(':{
				if(DEBUG)System.out.println("Op: '('");
				if(DEBUG)System.out.println(stack+"\n");
				if(buffer.length() > 0){
					String phase = buffer.toString();
					stack.push(phase);
					buffer = new StringBuilder();
				}
				level += 1;
				stack.push("(");
			}break;
			case ' ':{ //separators
				if(DEBUG)System.out.println("Op: ' '");
				if(DEBUG)System.out.println(stack+"\n");
				
				if(buffer.length() > 0){
					String phase = buffer.toString();
					stack.push(phase);
					buffer = new StringBuilder();
				}
			}break;
			case ')':{ //pop elements from stack until '(' is encountered
				if(DEBUG)System.out.println("Op: ')'");
				if(DEBUG)System.out.println(stack+"\n");
				
				if(buffer.length() > 0){
					String phase = buffer.toString();
					stack.push(phase);
					buffer = new StringBuilder();
				}
				
				List<Object> oList = new ArrayList<Object>();
				while(true){
					Object poped = stack.pop();
					if(poped.equals("(")) break;
					oList.add(poped);
				}
				switch(oList.size()){
				case 0:{ throw new AssertionError(); } 
				case 1:{
					Object single = oList.get(0);
					if(single instanceof String){
						Expression var = buildVariableFromWenhao(single.toString());
						stack.push(var);
					}else{ stack.push(single); }
				}break;
				default:{
					int count = oList.size();
					Expression created = null;
					for(int index = 0; index< count ; index++){
						Object o = oList.get(count - index - 1);
						if(index == 0){
							String operator = (String)o;
							created = new Expression(operator);
						}else{
							if(o instanceof Expression){
								created.add((Expression)o);
							}else{
								Expression var = buildVariableFromWenhao(o.toString());
								created.add(var);
							}
						}
					}
					stack.push(created);
				}
				}
				level -= 1;
			}break;
			default:{  buffer.append(c); }
			}
			if(level < 0) throw new AssertionError();
		}
		if(stack.size() != 1){
			System.out.println(stack);
			throw new AssertionError();
		}
		
		Expression result = (Expression) stack.pop();
		String operator = result.getContent();
		if(equality.contains(operator)){
			return new Formula(operator, (Expression)result.getChildAt(0), (Expression)result.getChildAt(1));
		}
		
		return result;		
	} 
	
	public static Expression buildVariableFromWenhao(String input){
		String[] parts = input.split(":");
		if(parts.length == 2){
			return new Variable(parts[0], parts[1]); 
		}else if(input.startsWith("#")){
			long val = Long.decode(input.replace("#", ""));
			return new Constant(val+"");
		}
		return new Constant(input);
	}
}
