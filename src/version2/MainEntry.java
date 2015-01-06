package version2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import main.Paths;
import concolic.Condition;
import concolic.ExecutionEngine;
import concolic.Operation;
import concolic.PathSummary;
import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import support.Utility;
import zhen.version1.Support.Bundle;
import zhen.version1.component.UIModelGraph;
import zhen.version1.component.UIState;
import analysis.StaticInfo;

public class MainEntry {
	static String storageFolder = "apkProfile/";
	
	public static void main(String[] args) {
		test1();
		
//		Bundle bundle = (Bundle) Utility.readObject("ModelObject/com.example.basicvariabletest");
//		UIModelGraph model = (UIModelGraph)bundle.os[0];
//		for(UIState state : model.getKnownVertices()){
//			System.out.println(state);
//			System.out.println(state.getIneffectiveEventList());
//		}
	}
	
	public static void test1(){
		String storagePath = "/home/zhenxu/workspace/APK/";
		String name = 
//				"TheApp.apk";
				"BasicVariableTest.apk";
		String apkPath = storagePath + name;
		
		
		StaticApp testApp = readOrBuildPathSummary(apkPath,name,false);	
		List<PathSummary> summaries = testApp.getAllPathSummaries();
		List<PathSummary> filtered = new ArrayList<PathSummary>();
		
		print(summaries);
		
		
//		for(PathSummary summary: summaries){
//			PathSummary tmp = new PathSummary();
//			String sig = summary.getMethodSignature();
//			System.out.println("Tab:\t"+summary.getMethodSignature());
//			tmp.setMethodSignature(sig);
//			
//			System.out.println("\t"+summary.getExecutionLog());
//			System.out.println("\tPathConditions");
//			ArrayList<Condition> filteredCondition = new ArrayList<Condition>();
//			for(Condition con: summary.getPathCondition()){
//				//a valid condition should not contain any temporary variable, unresolved function invocation
//				//e.g. $parameter#, v#, p#, #invoke, $return
//				System.out.println("\t"+con.getOp()+" "+ con.getLeft()+" "+con.getRight());
//				String left = con.getLeft();
//				String right = con.getRight();
//				if( left.contains("#invoke") || left.contains("$parameter") || left.contains("$return") ||
//					right.contains("#invoke") || right.contains("$parameter") || right.contains("$return")
//				){
//					
//				}else filteredCondition.add(con);
//			}
//			tmp.setPathCondition(filteredCondition);
//			
//			System.out.println("\tSymbolicStates");
//			ArrayList<Operation> filteredSymbolic = new ArrayList<Operation>();
//			for(Operation oper: summary.getSymbolicStates()){
//				System.out.println("\t"+oper.getOp()+" "+oper.getLeft()+" "+oper.getRightA()+" "+oper.getRightB());
//			
//				String left = oper.getLeft();
//				String right = oper.getRight();
//				
//				if( left.contains("#invoke") || left.contains("$parameter") || left.contains("$return") ||
//					right.contains("#invoke") || right.contains("$parameter") || right.contains("$return") ||
//					left.equals("p0") || left.equals("p1") || left.startsWith("v")
//				){
//					
//				}else filteredSymbolic.add(oper);
//			}
//			tmp.setSymbolicStates(filteredSymbolic);
//			
//			if(sig != null && sig.trim().equals("") && filteredSymbolic.size()!= 0){
//				filtered.add(tmp);
//			}
//		}
//		
//		System.out.println("------------");
//		System.out.println("filtered amount:"+ filtered.size());
//		for(PathSummary summary: filtered){
//			System.out.println(summary.getMethodSignature());
//			System.out.println("\ttPathConditions");
//			for(Condition con: summary.getPathCondition()){
//				System.out.println("\t"+con.getOp()+" "+ con.getLeft()+" "+con.getRight());
//			} 
//			System.out.println("\tSymbolicStates"); 
//			for(Operation oper: summary.getSymbolicStates()){
//				System.out.println("\t"+oper.getOp()+" "+oper.getLeft()+" "+oper.getRightA()+" "+oper.getRightB());
//			}
//		}
	}
	
	
	public static StaticApp readOrBuildPathSummary(String apkPath, String name, boolean force){
		String storagePath = storageFolder+name;
		File folderPath = new File(Paths.appDataDir+storageFolder);
		if(!folderPath.exists())folderPath.mkdirs();
		
		File target = new File(Paths.appDataDir+storageFolder+name);
		if(force  || !target.exists() ){
			StaticApp testApp = StaticInfo.initAnalysis(apkPath, force);
			ExecutionEngine ee = new ExecutionEngine(testApp);
			ee.blackListOn = true;
			ee.useAdb = true;
			testApp = ee.buildPathSummaries(force);
			boolean result = Utility.writeObject(storagePath, testApp);
			System.out.println(result);
			return testApp;
		}else{
			return (StaticApp) Utility.readObject(storagePath);
		}
	}
	
	public static void print(List<PathSummary> summaries){
		for(PathSummary summary: summaries){
			System.out.println(summary.getMethodSignature());
			System.out.println("\ttPathConditions");
			for(Condition con: summary.getPathCondition()){
				System.out.println("\t"+con.getOp()+" "+ con.getLeft()+" "+con.getRight());
			} 
			System.out.println("\tSymbolicStates"); 
			for(Operation oper: summary.getSymbolicStates()){
				System.out.println("\t"+oper.getOp()+" "+oper.getLeft()+" "+oper.getRightA()+" "+oper.getRightB());
			}
		}
	}
}
