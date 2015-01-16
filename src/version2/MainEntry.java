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
			UIModelGenerator builder = new UIModelGenerator(testApp);
			builder.buildOrRead(force);
			ExecutionEngine ee = new ExecutionEngine(testApp);
			ee.blackListOn = true;
			ee.useAdb = true;
			testApp = ee.buildPathSummaries(force,builder);
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
