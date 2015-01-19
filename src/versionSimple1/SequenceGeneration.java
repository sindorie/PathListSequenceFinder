package versionSimple1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.Paths;
import concolic.ExecutionEngine;
import concolic.PathSummary;
import concolic.Validation;
import staticFamily.StaticApp;
import staticFamily.StaticClass;
import support.PathSummaryUIFactory;
import support.Utility;
import support.GUI.UIUtility;
import zhen.version1.UIModelGenerator;
import analysis.StaticInfo;

public class SequenceGeneration { 
	private String logFileName = "";
	private PrintWriter writer = null;;
	
	public void generate(boolean force, String apkPath, String... targetLines){
		StaticApp testApp = StaticInfo.initAnalysis(apkPath, force);
		File dir = new File(Paths.appDataDir+"SequenceGeneration/");
		dir.mkdirs();
		logFileName = Paths.appDataDir+"SequenceGeneration/"
				+testApp.getPackageName().replace("[^a-zA-Z0-9]", "");
		File logFile = new File(logFileName);
		if(writer == null){
			try {
				writer = new PrintWriter(logFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println(testApp.getPackageName());
		System.out.println(testApp.getMainActivity().getJavaName());
		for(StaticClass act : testApp.getActivities()){
			System.out.println(act.getJavaName());
		}
		
		tools.Adb adb = new tools.Adb();
		adb.uninstallApp(testApp.getPackageName());
		adb.installApp(testApp.getSootAppPath());
		
		System.out.println("UIModelGenerator");
		UIModelGenerator builder = new UIModelGenerator(testApp);
		builder.buildOrRead(force);
		
		System.out.println("ExecutionEngine");
		ExecutionEngine ee = new ExecutionEngine(testApp);
		ee.blackListOn = true;
		ee.useAdb = true;
		List<PathSummary> summaries = ee.buildPathSummaries(force, builder);
		UIUtility.showComponent(PathSummaryUIFactory.buildSummaryListComponent(summaries));
		Set<String> lines = new HashSet<String>();
		for(PathSummary summary : summaries){
			for(String exlog : summary.getExecutionLog()){
				lines.add(exlog);
			}
		}
		
		for(String line : lines){
			logMessage(line);
		}
		logMessage("*************************************\n");
		
		System.out.println("SequenceFinder");
		SequenceFinder finder = new SequenceFinder(testApp, summaries, 
				builder.getUIModel().cloneWithNoLauncher(),
				builder.getUniqueEventList());
		List<SequenceGenerationResultDetail> generatedResult = finder.findSequence(force, targetLines);
		System.out.println("Generated result size : "+generatedResult.size());
		for(SequenceGenerationResultDetail record : generatedResult){ 
			System.out.println(record.state+" vs "+SequenceGenerationResultDetail.SUCCESSFUL);
			System.out.println(record.inflatedEventSequence != null);
		}
		
		
		System.out.println("Validation");
		List<SequenceGenerationResultDetail> toValidate = 
				SequenceGenerationResultDetail.retriveValidationList(generatedResult);
		System.out.println("to validate size : "+toValidate.size());
		for(SequenceGenerationResultDetail record : toValidate){
			System.out.println("To validate : "+record.targetLine);
			System.out.println("To validate : "+Arrays.toString(record.inflatedEventSequence));
		}
		
		
		Map<String, SequenceGenerationResultDetail> recordMap = new HashMap<String, SequenceGenerationResultDetail>();
		//To save time, once target line is reached, do not validate others sequence
		//which try to reach this target line.
		System.out.println("Saving SequenceGenerationResultDetail");
		int count = 0;
		int reachedCount = 0;

		for(SequenceGenerationResultDetail record : toValidate){
			if(recordMap.get(record.targetLine) == null){
				System.out.println((toValidate.size() - count) + " Sequences to validate");
				Validation validator = new Validation(testApp, builder.getExecutor());
				validator.useAdb = false;
				boolean reachable = validator.validateSequence(
						Arrays.asList(record.inflatedEventSequence),
						(ArrayList)record.targetSummary.executionLog);
				
				reachable |= validator.getJdbResult().contains(record.targetLine);
				if(reachable){ 
					reachedCount += 1;
					record.state = SequenceGenerationResultDetail.SUCCESSFUL;
					recordMap.put(record.targetLine, record);
				}else{ record.state = SequenceGenerationResultDetail.FAILURE; }
				
				System.out.println("********Total of "+reachedCount+" lines reached*********");
				
				logMessage("Target: "+record.targetLine);
				for(String jdbLog : validator.getJdbResult()){
					logMessage(jdbLog);
				}
				logMessage("Event Sequence: "+Arrays.toString(record.inflatedEventSequence));
				logMessage("Raw Event Sequence: "+Arrays.toString(record.rawEventSequence));
				logMessage("Summary Sequence: "+Arrays.toString(record.summarySequence));
				logMessage("Target summary: "+record.targetSummary);
				logMessage("State: "+record.state);
				logMessage("*************************************************************************\n");
				
			}
			count += 1;
		}
		
		String recordStroage = "report/"+ testApp.getPackageName().replaceAll("[^a-zA-Z0-9]", "")+"report";
		Utility.writeObject(recordStroage , recordMap);
		
		for(String line : targetLines){
			SequenceGenerationResultDetail detail = recordMap.get(line);
			if(detail == null){
				System.out.println("Fail to reach: "+line);
			}else{
				System.out.println("Succeed to reach: "+line);
				System.out.println(Arrays.toString(detail.inflatedEventSequence));
			}
		}
		System.out.println("Completed");
		
		if(writer != null) writer.close();
	} 
	private void logMessage(String... mes){
		for(String s:mes){
			writer.println(s);
		}
		writer.flush();
	}
}
