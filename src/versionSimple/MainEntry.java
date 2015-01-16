package versionSimple;

import java.io.IOException;
import java.util.List;
import main.Paths;
import concolic.ExecutionEngine; 
import concolic.PathSummary; 
import staticFamily.StaticApp;
import support.solver.YicesProcessInterface;
import versionSimple.SequenceFinder.OperationEvent;
import zhen.version1.UIModelGenerator;
import analysis.StaticInfo;

public class MainEntry {
	static String storageFolder = "apkProfile/";

	public static void main(String[] args) {
		test1();
	}

	public static void test1() {
		String storagePath = "/home/zhenxu/workspace/APK/";
		String name =
		// "TheApp.apk";	
		"BasicVariableTest.apk";
		String apkPath = storagePath + name;

		procedure(apkPath,false);
	}
	
	public static void procedure(String apkPath, boolean force){
		StaticApp testApp = StaticInfo.initAnalysis(apkPath, force);
		
		String pkgName = testApp.getPackageName();
		tools.Adb adb = new tools.Adb();
		adb.uninstallApp(pkgName);
		adb.installApp(testApp.getSootAppPath());
		
		UIModelGenerator builder = new UIModelGenerator(testApp);
		builder.buildOrRead(force);
		builder.getUIModel().enableGUI();
		
		ExecutionEngine ee = new ExecutionEngine(testApp);
		ee.blackListOn = true;
		ee.useAdb = true;
		List<PathSummary> summaries = ee.buildPathSummaries(force, builder);
		YicesProcessInterface yices;
		try {
			yices = new YicesProcessInterface(Paths.yicesLocation);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Yices initialization fails");
			return;
		}
		SequenceFinder finder = new SequenceFinder(yices);
		finder.loadOrOperate(pkgName,false, builder, summaries);
		finder.setPostOperation(new OperationEvent(){
			@Override
			public void onSequenceReady(SequenceFinder finder) {
				finder.checkData();
				//there could be press button
//				List<Event[]> sequences = finder.getSequenceList();
//				for(Event[] sequence : sequences){
//					
//				}	
			}
		});
	}

}
