package version2;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import main.Paths;
import concolic.ExecutionEngine;
import concolic.Expression;
import concolic.PathSummary;
import concolic.Variable;
import staticFamily.StaticApp;
import support.PathSummaryUIFactory;
import support.Utility; 
import support.GUI.UIUtility;
import zhen.version1.UIModelGenerator;
import zhen.version1.component.UIModelGraph;
import analysis.StaticInfo;

public class MainEntry {
	static String storageFolder = "apkProfile/";

	public static void main(String[] args) {
		test1();

		// Bundle bundle = (Bundle)
		// Utility.readObject("ModelObject/com.example.basicvariabletest");
		// UIModelGraph model = (UIModelGraph)bundle.os[0];
		// for(UIState state : model.getKnownVertices()){
		// System.out.println(state);
		// System.out.println(state.getIneffectiveEventList());
		// }
	}

	public static void test1() {
		String storagePath = "/home/zhenxu/workspace/APK/";
		String name =
		// "TheApp.apk";
		"BasicVariableTest.apk";
		String apkPath = storagePath + name;

		StaticApp testApp = readOrBuildPathSummary(apkPath, name, false);
		List<PathSummary> summaries = testApp.getAllPathSummaries();

//		System.out.println("Retrieving UI Model");
//		
//		UIModelGenerator modelGen = new UIModelGenerator(testApp);
//		modelGen.buildOrRead(false);
//		UIModelGraph model = modelGen.getUIModel();

		System.out.println("Process finished");
		UIUtility.showComponent(
				PathSummaryUIFactory.buildSummaryListComponent(summaries),
				JFrame.DISPOSE_ON_CLOSE);

		
		PathListSequenceFinder.filterAndselect(summaries);
		
//		System.out.println("Tranforming pathsummary");
//		// clone and filter Expressions
//		List<PathSummary> filtered = new ArrayList<PathSummary>();
//		for (PathSummary summary : summaries) {
//			PathSummary copy = new PathSummary();
//
//			//signature
//			copy.setMethodSignature(summary.getMethodSignature());
//
//			//log
//			for(String line : summary.getExecutionLog()){
//				copy.addExecutionLog(line);
//			}
//
//			//constraint
//			copy.setPathCondition(transformExpression(summary.getPathCondition()));
//
//			// expression
//			copy.setSymbolicStates(transformExpression(summary.getSymbolicStates()));
//			
//			//add copy to list
//			filtered.add(copy);
//		}
//
//		// print(summaries);
//		System.out.println("Show filtered pathsummary");
//		UIUtility.showComponent(
//				"Filtered PathSummary",
//				PathSummaryUIFactory.buildSummaryListComponent(filtered),
//				JFrame.DISPOSE_ON_CLOSE);
	}
	
	
	private static boolean matchAll(String regax, String input){
		Pattern partten = Pattern.compile(regax); 
		Matcher match =  partten.matcher(input);
		boolean found = match.find();
		if(found == false) return false;
		return (match.start()-match.end()) == input.length();
	}
	static String TYPES = "I";
	static Map<String,String> typeMap = new HashMap<String,String>();
	static{
		String[] pairs = {
			"I","int",
			//TODO
		};
		for(int i=0;i<pairs.length;i+=2){
			typeMap.put(pairs[i], pairs[i+1]);
		}
	}
	
	private static ArrayList<Expression> transformExpression(List<Expression> list){
		ArrayList<Expression> copiedConstraintList = new ArrayList<Expression>();
		 //TODO
 
		MAJOR: for (Expression expre : list) {
			Expression copyExpression = expre.clone();
			//condense -- find the special case for the field/object/static access 
			//$Fstatic (field_signature), $Finstance (field_signature object_expression), 
			//other special: $return , $api
			
			List<Expression> queue = new ArrayList<Expression>();
			queue.add(copyExpression);
			
			//check valid node
			while(queue.isEmpty() == false){
				Expression currentNode = queue.remove(0);
				String currentContent = currentNode.getContent();
				if(currentContent.equalsIgnoreCase("$Fstatic")){
					Expression child = (Expression)currentNode.getChildAt(0);
					String childContent = child.getContent();
					int lastIndex = childContent.lastIndexOf(":");
					
					String type = childContent.substring(lastIndex, childContent.length()).replace(";", "").trim();
					if(!TYPES.contains(type)) continue MAJOR;
					
					String fieldSig = childContent.replaceAll("\\/|(;->)|;", "");
					currentNode.setUserObject(fieldSig);
					currentNode.removeAllChildren();
				}else if(currentContent.equalsIgnoreCase("$Finstance")){
					//recursively dig in
					String condensedContent = fieldInstanceDigIn(currentNode);
					
					String[] parts = condensedContent.split(":");
					String last = parts[parts.length-1].replace(";", "").trim();
					
					if(!TYPES.contains(last)){ continue MAJOR;  }
					
					currentNode.setUserObject(condensedContent);
					currentNode.removeAllChildren();
				}else if(currentContent.equalsIgnoreCase("$return")){
					continue MAJOR;
				}else if(currentContent.equalsIgnoreCase("$api")){
					continue MAJOR;
				}else if(currentNode.isLeaf()){
//					if(isValidParameter(currentContent) == false){
//						continue MAJOR;
//					}
				}
				
				for(int i =0;i < currentNode.getChildCount() ; i++){
					queue.add((Expression)currentNode.getChildAt(i));
				}
			}
			
			Enumeration<Expression> nodeEnum = copyExpression.depthFirstEnumeration();
			while(nodeEnum.hasMoreElements()){
				Expression node = nodeEnum.nextElement();
				String content = node.getContent();
				if(node.isLeaf()){//check valid variable
					if(isTmpVariable(content)){
						continue MAJOR;
					}
					if(content.startsWith("#string")){
						//check string constant which is not supported yet
						//TODO
						continue MAJOR;
					}else if(content.startsWith("#")){
						//check if it is number
						String filterd = content.replace("#", "");
						
						try{  
							long val = Long.decode(filterd); 
							node.setUserObject(val+"");//treat it as constant
						}catch(NumberFormatException nfe){
							continue MAJOR; 
						}
					}else{
						content.replaceAll(";->|;|\\/", "");//remove ";->" or ";" or "\" 
						content.replaceAll("<|>", "_");//replace <> to _
						
						if(content.split(":").length <=1) continue MAJOR;
						
						String subString = content.substring(content.lastIndexOf(":")+1,content.length()).trim();
						
						String parsedType = typeMap.get(subString);
						if(parsedType == null) continue MAJOR;
						
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
					if(parsedOperator == null) continue MAJOR;
					//valid operator 
					node.setUserObject(parsedOperator);
				}
			}

			
			copiedConstraintList.add(copyExpression);
		}
		
		return copiedConstraintList;
	}
	
	
	
	private static Map<String,String> operatorMap = new HashMap<String,String>();
	static{
		String[] parts = {
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
		for(int i =0;i<parts.length;i+=2){
			operatorMap.put(parts[i], parts[i+1]);
		}
	}
	
	private static Pattern tmpRgister = Pattern.compile("\\$?[vp]\\d?"); 
	private static Pattern parameter = Pattern.compile("\\$?parameter\\d?"); 
	private static boolean isTmpVariable(String input){
		Matcher match1 =  tmpRgister.matcher(input);
		if(match1.find() && ((match1.end()-match1.start()) == input.length())) return true;

		Matcher match2 =  parameter.matcher(input);
		if(match2.find() && ((match2.end()-match2.start()) == input.length())) return true;

		return false;
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

	public static StaticApp readOrBuildPathSummary(String apkPath, String name,
			boolean force) {
		String storagePath = storageFolder + name;
		File folderPath = new File(Paths.appDataDir + storageFolder);
		if (!folderPath.exists())
			folderPath.mkdirs();

		File target = new File(Paths.appDataDir + storageFolder + name);
		if (force || !target.exists()) {
			StaticApp testApp = StaticInfo.initAnalysis(apkPath, force);
			ExecutionEngine ee = new ExecutionEngine(testApp);
			ee.blackListOn = true;
			ee.useAdb = true;
			testApp = ee.buildPathSummaries(force);
			boolean result = Utility.writeObject(storagePath, testApp);
			System.out.println(result);
			return testApp;
		} else {
			return (StaticApp) Utility.readObject(storagePath);
		}
	}

	// public static void print(List<PathSummary> summaries){
	// for(PathSummary summary: summaries){
	// System.out.println(summary.getMethodSignature());
	// System.out.println("\tPathConditions");
	// for(Condition con: summary.getPathCondition()){
	// System.out.println("\t"+con.getOp()+" "+
	// con.getLeft()+" "+con.getRight());
	// }
	// System.out.println("\tSymbolicStates");
	// for(Operation oper: summary.getSymbolicStates()){
	// System.out.println("\t"+oper.getOp()+" "+oper.getLeft()+" "+oper.getRightA()+" "+oper.getRightB());
	// }
	// }
	// }
}
