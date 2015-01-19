package versionSimple1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import zhen.version1.component.Event;
import Component.WrappedSummary;

public class SequenceGenerationResultDetail implements Serializable{ 
	
	private static final long serialVersionUID = 1L;
	//the target line
	public String targetLine;
	//the path summary which triggers the target line
	//null means no summary is found to trigger the target line
	public WrappedSummary targetSummary;
	//the summary sequence starts from an entry to target summary
	//null represents a broken sequence due to a failure of finding
	//a satisfiable summary sequence to an entry
	public WrappedSummary[] summarySequence;
	//the event sequence where each event might trigger the path 
	//summary of the same index in summarySequence
	//null represents a broken sequence due to a failure of finding
	//an event which might trigger a path summary
	public Event[] rawEventSequence;
	//the event sequence where connectors are added.
	//null represents a broken sequence due to a failure of finding
	//connector for two sequential events. 
	public Event[] inflatedEventSequence;
	//flag for telling if the sequence has been validated. 
	public int state;
	//flags
	public final static int INVALID = -1, UNVALIDATED = 0, FAILURE = 1, SUCCESSFUL = 2;
	
	public SequenceGenerationResultDetail(String targetline,
			WrappedSummary targetSummary,
			WrappedSummary[] summarySequence,
			Event[] rawEventSequence,
			Event[] inflatedEventSequence){
		this.targetLine = targetline;
		this.targetSummary = targetSummary;
		this.summarySequence = summarySequence;
		this.rawEventSequence = rawEventSequence;
		this.inflatedEventSequence = inflatedEventSequence;
		this.state = UNVALIDATED;
	}
	
	public SequenceGenerationResultDetail(String targetline,
			WrappedSummary targetSummary,
			WrappedSummary[] summarySequence,
			Event[] rawEventSequence,
			Event[] inflatedEventSequence,
			int state
			){
		this.targetLine = targetline;
		this.targetSummary = targetSummary;
		this.summarySequence = summarySequence;
		this.rawEventSequence = rawEventSequence;
		this.inflatedEventSequence = inflatedEventSequence;
		this.state = state;
	}
	
	public static List<SequenceGenerationResultDetail> retriveValidationList(List<SequenceGenerationResultDetail> resultDetails){
		List<SequenceGenerationResultDetail> result = new ArrayList<SequenceGenerationResultDetail>();
		for(SequenceGenerationResultDetail record :resultDetails){
			if(record.inflatedEventSequence != null && record.state == UNVALIDATED){
				result.add(record);
			}
		}
		return result;
	}
}
