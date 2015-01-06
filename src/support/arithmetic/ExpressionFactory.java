package support.arithmetic;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import support.Utility;


/**
 * @Purpose It turns a String input (prefix) into a formula
 * @author zhenxu
 *
 */
public class ExpressionFactory  {
	
	static Map<String,String> operationMap = new HashMap<String,String>();
	static Map<String,String> typeMap = new HashMap<String,String>();
	static{
		String[] list = {
			"add", "+",
			"sub", "-",
			"mul", "*",
			"dev", "/",
			"and", "and",
			"or", "or",
			"xor", "xor",
			"neg", "neg",  //this should be replace by xor X true
			
			"<","<",
			">",">",
			"=","=",
			"!=","/=",
			"<=","<=",
			">=",">=",
		};
		
		for(int i=0;i<list.length;i+=2){
			operationMap.put(list[i], list[i+1]);
		}
		//ignore shl shr ushr
		
		
		String[] typeList = {
			"I", "int",
			"F", "real",
			"D", "real",
			"b", "bool"
		};
		for(int i=0;i<typeList.length;i+=2){
			typeMap.put(typeList[i], typeList[i+1]);
		}
	}
	
	
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
	
	private static int furtherProcess(Expression expre){
		Enumeration<Expression> enumater = expre.depthFirstEnumeration();
		int hasUnknownOperater = 0;
		while(enumater.hasMoreElements()){
			Expression current = enumater.nextElement();
			if(current.isLeaf() == false){
				String content = current.getContent();
				String equvialent = operationMap.get(content);
				if(equvialent == null){
					hasUnknownOperater += 1;
					System.out.println("hasUnknownOperater:"+content);
				}else if(equvialent.equals("neg")){
					int count = current.getChildCount();
					if(count != 1) {
						System.out.println("neg child count:"+count);
						throw new AssertionError(); //how could neg has more than one children?
					}
					//turn (neg x) -> (xor x true)
					Constant trueCon = new Constant("true");
					current.setUserObject("xor");
					current.add(trueCon);
				}else current.setUserObject(equvialent);
			}
		}
		return hasUnknownOperater;
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
		
		
		int errorCount = furtherProcess(result);
		if(errorCount >0) return null;
		
		return result;		
	} 
	
	public static Expression buildVariableFromWenhao(String input){
		String[] parts = input.split(":");
		if(parts.length == 2){
			String type = typeMap.get(parts[0]);
			if(type != null) return new Variable(parts[0], parts[1]); 
		}else if(input.startsWith("#")){
			try{
				long val = Long.decode(input.replace("#", ""));
				return new Constant(val+"");
			}catch(NumberFormatException nfe){}
		}
		return new Constant(input);
	}
}
