package Component;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import analysis.Expression;
import analysis.Variable;


public class ExpressionUtility {

	static Map<String,String> typeMap = new HashMap<String,String>();
	static Map<String,String> operatorMap = new HashMap<String,String>();
	static Pattern tmpRgister = Pattern.compile("\\$?[vp]\\d?"); 
	static Pattern parameter = Pattern.compile("\\$?parameter\\d?"); 
	static Scanner sc;
	static{
		String[] typePairs = {
				"I","int", //TODO
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

	
	/**
	 * Will deeply clone the expression and operate on the clone. 
	 * @param expres
	 * @return
	 */
	public static List<Expression> transform(List<Expression> expres){
		List<Expression> result = new ArrayList<Expression>();
		for(Expression expre : expres){
			Expression cloned = expre.clone();
			if(transform(cloned)){
				result.add(cloned);
			}
		}
		return result;
	}
	
	
	/**
	 * Given a expression which needs to be transformed,
	 * Temporary variable/registers (both name and type), API calls, return statement
	 * are checked and transformed if possible. 
	 * 
	 * Will not clone the input expression
	 * Current implementation will not resolve any API call, return statement is ignored.
	 * @param expre
	 * @return true when transformation is successful. 
	 */
	public static boolean transform(Expression expre){
		//condense -- find the special case for the field/object/static access 
		//$Fstatic (field_signature), $Finstance (field_signature object_expression), 
		//other special: $return , $api
		
		List<Expression> queue = new ArrayList<Expression>();
		queue.add(expre);
		
		//check valid node
		while(queue.isEmpty() == false){
			Expression currentNode = queue.remove(0);
			String currentContent = currentNode.getContent();
			if(currentContent.equalsIgnoreCase("$Fstatic")){
				Expression child = (Expression)currentNode.getChildAt(0);
				String childContent = child.getContent();
				int lastIndex = childContent.lastIndexOf(":");
				
				String type = childContent.substring(lastIndex, childContent.length()).replace(";", "").trim();
				if(typeMap.get(type) == null) return false;
				
				String fieldSig = childContent.replaceAll("\\/|(;->)|;", "");
				currentNode.setUserObject(fieldSig);
				currentNode.removeAllChildren();
			}else if(currentContent.equalsIgnoreCase("$Finstance")){
				//recursively dig in
				String condensedContent = fieldInstanceDigIn(currentNode);
				
				String[] parts = condensedContent.split(":");
				String last = parts[parts.length-1].replace(";", "").trim();
				
				if(typeMap.get(last) == null){ return false;  }
				
				currentNode.setUserObject(condensedContent);
				currentNode.removeAllChildren();
			}else if(currentContent.equalsIgnoreCase("$return")){
				return false;
			}else if(currentContent.equalsIgnoreCase("$api")){
				return false;
			}else if(currentNode.isLeaf()){
//				if(isValidParameter(currentContent) == false){
//					continue MAJOR;
//				}
			}
			
			for(int i =0;i < currentNode.getChildCount() ; i++){
				queue.add((Expression)currentNode.getChildAt(i));
			}
		}
		
		Enumeration<Expression> nodeEnum = expre.depthFirstEnumeration();
		while(nodeEnum.hasMoreElements()){
			Expression node = nodeEnum.nextElement();
			String content = node.getContent();
			if(node.isLeaf()){//check valid variable
				if(isTmpVariable(content)){
					return false;
				}
				if(content.startsWith("#string")){
					//check string constant which is not supported yet
					//TODO
					return false;
				}else if(content.startsWith("#")){
					//check if it is number
					String filterd = content.replace("#", "");
					
					try{  
						long val = Long.decode(filterd); 
						node.setUserObject(val+"");//treat it as constant
					}catch(NumberFormatException nfe){
						return false; 
					}
				}else{
					content.replaceAll(";->|;|\\/", "");//remove ";->" or ";" or "\" 
					content.replaceAll("<|>", "_");//replace <> to _
					if(content.split(":").length <=1) return false;
					
					String subString = content.substring(content.lastIndexOf(":")+1,content.length()).trim();
					String parsedType = typeMap.get(subString);
					if(parsedType == null) return false;
					
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
				if(parsedOperator == null) return false;
				//valid operator 
				node.setUserObject(parsedOperator);
			}
		}
		return true;
	}
	
	private static String fieldInstanceDigIn(Expression expre){
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
	
	private static boolean isTmpVariable(String input){
		Matcher match1 =  tmpRgister.matcher(input);
		if(match1.find() && ((match1.end()-match1.start()) == input.length())) return true;

		Matcher match2 =  parameter.matcher(input);
		if(match2.find() && ((match2.end()-match2.start()) == input.length())) return true;

		return false;
	}
}
