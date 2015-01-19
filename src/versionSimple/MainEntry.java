package versionSimple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import main.Paths;
import concolic.ExecutionEngine; 
import concolic.PathSummary; 
import concolic.Validation;
import staticFamily.StaticApp;
import support.solver.YicesProcessInterface;
import versionSimple.SequenceFinder.OperationEvent;
import zhen.version1.UIModelGenerator;
import zhen.version1.component.Event;
import analysis.StaticInfo;

public class MainEntry {
	static String storageFolder = "apkProfile/";

	public static void main(String[] args) {
		test1();
	}

	public static void test1() {
		String apkPath = "/home/zhenxu/workspace/APK/"
				+ "CalcA.apk";
//				+ "TheApp.apk";
//				+ "BasicVariableTest.apk";
			
		procedure(apkPath,false);
	}
	
	public static void procedure(String apkPath, boolean force){
		String targetLine = "com.bae.drape.gui.calculator.CalculatorActivity:485";
		StaticApp testApp = StaticInfo.initAnalysis(apkPath, force);
		System.out.println(testApp.getMainActivity().getJavaName());
		
		String pkgName = testApp.getPackageName();
		tools.Adb adb = new tools.Adb();
		adb.uninstallApp(pkgName);
		adb.installApp(testApp.getSootAppPath());
		

		System.out.println("UIModelGenerator");
		UIModelGenerator builder = new UIModelGenerator(testApp);
		builder.buildOrRead(force);
//		builder.getUIModel().enableGUI();
		

		System.out.println("ExecutionEngine");
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
		finder.setPostOperation(new OperationEvent(){
			@Override
			public void onSequenceReady(SequenceFinder finder) {
				finder.checkData();
				
				//there could be press button
//				System.out.println("onSequenceReady");
//				List<Event[]> sequences = finder.getSequenceList();
//				Validation val = new Validation(testApp,builder.getExecutor());
//				ArrayList<String> logs = (ArrayList<String>)finder.getSelectedTarget().summaryReference.getExecutionLog();
//				System.out.println("logs: "+logs);
//				for(Event[] sequence : sequences){
//					boolean reached = val.validateSequence(Arrays.asList(sequence), logs);
//					if(reached){
//						System.out.println("reached:\n"+Arrays.toString(sequence));
//						break;
//					}
//				}	
			}
		});
		builder.getUniqueEventList();
		finder.operate(testApp, pkgName,false, builder, summaries,targetLine);
		
//		StaticApp app, String stroageName, boolean force,
//		UIModelGenerator builder, List<PathSummary> rawList,
//		String targetMethodSig
	}

}
