package version3;

import java.io.File;
import java.io.IOException;
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
import concolic.PathSummary; 
import staticFamily.StaticApp;
import support.PathSummaryUIFactory;
import support.Utility; 
import support.GUI.UIUtility;
import support.solver.YicesProcessInterface;
import zhen.version1.UIModelGenerator;
import zhen.version1.component.Event;
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

		procedure(apkPath,false);
		
//		StaticApp testApp = readOrBuildPathSummary(apkPath, name, false);
//		List<PathSummary> summaries = testApp.getAllPathSummaries();
//		ExecutionEngine engine = new ExecutionEngine(testApp);
//		
//		
//		engine.buildPathSummaries(forceAllStep, builder)
//		
////		System.out.println("Retrieving UI Model");
////		
////		UIModelGenerator modelGen = new UIModelGenerator(testApp);
////		modelGen.buildOrRead(false);
////		UIModelGraph model = modelGen.getUIModel();
//
//		System.out.println("Process finished");
//		UIUtility.showComponent(
//				PathSummaryUIFactory.buildSummaryListComponent(summaries),
//				JFrame.DISPOSE_ON_CLOSE);
//
//		
//		PathListSequenceFinder.filterAndselect(summaries);
	}
	
//	public static StaticApp readOrBuildPathSummary(String apkPath, String name,
//			boolean force) {
//		String storagePath = storageFolder + name;
//		File folderPath = new File(Paths.appDataDir + storageFolder);
//		if (!folderPath.exists())
//			folderPath.mkdirs();
//
//		File target = new File(Paths.appDataDir + storageFolder + name);
//		if (force || !target.exists()) {
//			StaticApp testApp = StaticInfo.initAnalysis(apkPath, force);
//			UIModelGenerator builder = new UIModelGenerator(testApp);
//			builder.buildOrRead(force);
//			ExecutionEngine ee = new ExecutionEngine(testApp);
//			ee.blackListOn = true;
//			ee.useAdb = true;
//			testApp = ee.buildPathSummaries(force,builder);
//			boolean result = Utility.writeObject(storagePath, testApp);
//			System.out.println(result);
//			return testApp;
//		} else {
//			return (StaticApp) Utility.readObject(storagePath);
//		}
//	}
	
	public static void procedure(String apkPath, boolean force){
		StaticApp testApp = StaticInfo.initAnalysis(apkPath, force);
		
		String pkgName = testApp.getPackageName();
		tools.Adb adb = new tools.Adb();
		adb.uninstallApp(pkgName);
		adb.installApp(testApp.getSootAppPath());
		
		UIModelGenerator builder = new UIModelGenerator(testApp);
		builder.buildOrRead(force);
		builder.getUIModel().enableGUI();
		for(Event event: builder.getEventDeposit()){
			System.out.println(event.toString());
			System.out.println(event.getMethodHits());
		}
		
		ExecutionEngine ee = new ExecutionEngine(testApp);
		ee.blackListOn = true;
		ee.useAdb = true;
		List<PathSummary> summaries = ee.buildPathSummaries(force, builder);
		YicesProcessInterface yices;
		try {
			yices = new YicesProcessInterface(YicesProcessInterface.yicesLocation);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Yices initialization fails");
			return;
		}
		SequenceFinder finder = new SequenceFinder(yices);
		finder.loadOrOperate(pkgName,false, builder, summaries);
		
		
	}

}
