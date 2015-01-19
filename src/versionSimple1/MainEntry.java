package versionSimple1;

import java.io.File;

public class MainEntry {
	static String apkStroage = "/home/zhenxu/workspace/APK/";
	
	
	public static void main(String[] args) {
//		operationCalculator();
//		operationBasicVarTest();
		operationTip();	//crashed 
//		operationMunch();
//		operationManager();
	}
	
	public static void operationMunch(){
		String[] lines = {
				"info.bpace.munchlife.MunchLifeActivity:192",
				"info.bpace.munchlife.MunchLifeActivity:194",
				"info.bpace.munchlife.MunchLifeActivity:196",
				"info.bpace.munchlife.MunchLifeActivity$3:347",
				"info.bpace.munchlife.MunchLifeActivity$3:349",
				"info.bpace.munchlife.MunchLifeActivity$4:367",
		};
		String apkName = "info.bpace.munchlife.apk";
		operation(apkStroage+apkName, lines);
	}
	
	public static void operationManager(){
		String[] lines = {
				"com.nexes.manager.DirectoryInfo:77",
				"com.nexes.manager.DirectoryInfo:73",
				"com.nexes.manager.FileManager:240",
				"com.nexes.manager.FileManager:260",
				"com.nexes.manager.FileManager:287",
				"com.nexes.manager.FileManager:643",
		};
		String apkName = "com.nexes.manager.apk";
		operation(apkStroage+apkName, lines);
	}

	public static void operationTip(){
		String[] lines = {
				"net.mandaria.tippytipperlibrary.activities.Total:575",
				"net.mandaria.tippytipperlibrary.activities.About$2:102",
				"net.mandaria.tippytipperlibrary.activities.AplitBill$1:58",
				"net.mandaria.tippytipperlibrary.activities.SplitBill$1:58"
		};
		String apkName = "net.mandaria.tippytipper.apk";
		operation(apkStroage+apkName, lines);
	}
	
	public static void operation(String apkPath, String... targetLines){
		SequenceGeneration operation  = new SequenceGeneration();
		operation.generate(false, apkPath, targetLines);
	}
	

	public static void operationCalculator() {
		String apkPath = apkStroage + "CalcA.apk";
		int[] lineNumbers = {428, 430, 434, 439, 455, 482, 485, 486, 507};
		String prefix = "com.bae.drape.gui.calculator.CalculatorActivity:";
		String[] targetLines = new String[lineNumbers.length];
		for(int i =0;i<lineNumbers.length;i++){
			targetLines[i] = prefix+lineNumbers[i];
		}
		
		SequenceGeneration operation  = new SequenceGeneration();
		operation.generate(false, apkPath, targetLines);
	}
	
	static void operationBasicVarTest(){
		String apkPath = apkStroage + "BasicVariableTest.apk";
		int[] lineNumbers = {39};
		String prefix = "com.example.basicvariabletest.MainActivity:";
		String[] targetLines = new String[lineNumbers.length];
		for(int i =0;i<lineNumbers.length;i++){
			targetLines[i] = prefix+lineNumbers[i];
		}
		
		SequenceGeneration operation  = new SequenceGeneration();
		operation.generate(false, apkPath, targetLines);
	}
}