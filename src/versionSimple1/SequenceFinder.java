package versionSimple1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.lang3.ArrayUtils;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;

import analysis.Expression;
import analysis.Variable;
import Component.ExpressionUtility;
import Component.NodeContent;
import Component.WrappedSummary;
import concolic.PathSummary;
import staticFamily.StaticApp;
import support.Utility;
import support.GUI.UIUtility;
import support.solver.YicesProcessInterface;
import zhen.version1.UIModelGenerator;
import zhen.version1.component.Event; 
import zhen.version1.component.UIModelGraph.ListenableDirectedMultigraph;
import zhen.version1.component.UIState;

public class SequenceFinder {
	private StaticApp app; // will be used for the check the oncreate for the main activity 
	private List<WrappedSummary> summaries;
	private List<WrappedSummary> entrySummary;
	private Map<String, Event[]> methodSignatureToEvents;
	private boolean recordFailure = true;
	
	private List<UIState> KthPathUIStateProcessed = new ArrayList<UIState>();
	//map is not used to the illy defined hash function
	private List<KShortestPaths<UIState, Event>> kShortestPath = new ArrayList<KShortestPaths<UIState, Event>>();
	
	private List<Event> uniqueEventList;
	private ListenableDirectedMultigraph graph;
	private static int MAX_UNSOLVED = 0, ConnectorPathAmount = 5;
	private static final String YICES_CHECK = "(check)\n";
	private int maxLevel = 5, maxSearchSpace = 20;
	public final static boolean DEBUG = true;
	public final static String storageLocation = "SequenceFinder/";
	
	/**
	 * 
	 * @param app
	 * @param rawSummaries
	 * @param builder
	 */
	public SequenceFinder(StaticApp app, List<PathSummary> rawSummaries, 
			ListenableDirectedMultigraph graph, List<Event> uniqueList){
		this.app = app;
		this.graph = graph;
		this.uniqueEventList = uniqueList;
		this.entrySummary = new ArrayList<WrappedSummary>();
		this.summaries = convertAndFilterSummaries(app, rawSummaries);
		this.methodSignatureToEvents = buildMapBetweenMethodSignatureAndEvents(summaries);
	}

	/**
	 * Given an app and its path summaries, prepare data:
	 * filer, label and wrap summary. 
	 * @param app -- needed for the main activity call name which used to 
	 *				used to identify the onCreate as entry point
	 * @param rawSummaries	
	 * @return
	 */
	private List<WrappedSummary> convertAndFilterSummaries(StaticApp app, List<PathSummary> rawSummaries){
		String mainActivityName = app.getMainActivity().getJavaName();
		List<WrappedSummary> result = new ArrayList<WrappedSummary>();
		String prefix = "L"+mainActivityName.replaceAll("\\.", "/") + ";";
		printMessage("Prefix: "+prefix);
		
		int entryCount = 0;
		for(PathSummary rawSummary : rawSummaries){
			//clone and transform the path summary
			//NOTE: the potential shallow clone issue for PathSummary.clone 
			//		Namely Expression.clone is not called. 
			//		ExpressionUtility.transform handles both clone and transformation
			PathSummary copy = new PathSummary(); 
			String signature = rawSummary.getMethodSignature();
			assert signature != null && !signature.equals("");
			copy.setMethodSignature(signature);
			for(String line : rawSummary.getExecutionLog()){ copy.addExecutionLog(line);}
			List<Expression> symbolicList = ExpressionUtility.transform(rawSummary.getSymbolicStates());
			List<Expression> constraintList = ExpressionUtility.transform(rawSummary.getPathCondition());
			copy.setSymbolicStates(Utility.castToArrayList(symbolicList));
			copy.setPathCondition(Utility.castToArrayList(constraintList));
			

			//check if it is entry
			boolean isEntry = 
					signature.startsWith(prefix) &&
					signature.contains("onCreate");
			printMessage("signature: "+signature);
			
			WrappedSummary wrapped = new WrappedSummary(copy, isEntry);
			result.add(wrapped);
			
			if(isEntry){
				entryCount += 1;
				entrySummary.add(wrapped);
			}
		}
		printMessage("entryCount: "+entryCount);
		return result; 
	}
	
	/**
	 * build a map between method signature which comes from path summary and events where 
	 * method hits include method signature. 
	 * @param list
	 * @return
	 */
	private Map<String,Event[]> buildMapBetweenMethodSignatureAndEvents(List<WrappedSummary> list){
		Map<String,Event[]> methodEventMap = new HashMap<String,Event[]>();
		for(WrappedSummary summary:list){
			List<Event> mappingResult = new ArrayList<Event>();
			for(Event event : uniqueEventList){ 
				List<String> hits = event.getMethodHits();
				if(hits.contains(summary.methodSignature)){
					mappingResult.add(event);
				}
			}
			if(!mappingResult.isEmpty())
				methodEventMap.put(summary.methodSignature, mappingResult.toArray(new Event[0]));
		}
		return methodEventMap;
	}
	
	
	/**
	 * Given a list of target lines, find event sequences represented in detail
	 * @param targetlines
	 * @return
	 */
	public List<SequenceGenerationResultDetail> findSequence(boolean force, String... targetlines){
		List<SequenceGenerationResultDetail> globalStorage = new ArrayList<SequenceGenerationResultDetail>();
		
		TOP: for(String target: targetlines){
			String identifier = storageLocation+target.replaceAll("[^a-zA-Z0-9]", "") + "_identifier";
			if(force == false){
				Object reading = Utility.readObject(identifier);
				if(reading != null){
					List<SequenceGenerationResultDetail> localStorage = 
							(List<SequenceGenerationResultDetail>)reading;
					globalStorage.addAll(localStorage);
					continue TOP;
				}
			}
			
			List<SequenceGenerationResultDetail> localStorage = new ArrayList<SequenceGenerationResultDetail>();
			//locate a list of target summary which encapsulate the current target line
			List<WrappedSummary> targetSummaryList = locateTargetPathSummary(target);
			if(targetSummaryList == null || targetSummaryList.size() == 0){
				System.out.println("Cannot locate Path summary for "+target);
				if(recordFailure){
					localStorage.add(new SequenceGenerationResultDetail(
							target, null,null,null,null
							,SequenceGenerationResultDetail.INVALID));
				}
				continue TOP;
			}
			
			SEC: for(WrappedSummary oneTargetSummary : targetSummaryList){
				//find a list of path summary sequence from an entry to the target path summary
				List<WrappedSummary[]> anchorSequenceList = findSummarySequences(oneTargetSummary);
				if(anchorSequenceList == null || anchorSequenceList.size() == 0){
					if(recordFailure){
						localStorage.add(new SequenceGenerationResultDetail(
								target, oneTargetSummary,null,null,null
								,SequenceGenerationResultDetail.INVALID));
					}
					continue SEC;
				}
				
				THD: for(WrappedSummary[] anchorSequence : anchorSequenceList ){
					//convert a list of path summary to a list of events
					List<Event[]> rawEventSequenceList = convertToRawEventSequence(anchorSequence);
					if(rawEventSequenceList == null || rawEventSequenceList.size() == 0){
						if(recordFailure){
							localStorage.add(new SequenceGenerationResultDetail(
									target, oneTargetSummary,anchorSequence,null,null
									,SequenceGenerationResultDetail.INVALID));
						}
						continue THD;
					}
					
					FOR: for(Event[] toInflate : rawEventSequenceList){
						//add connector between events 
						List<Event[]> inflatedList = inflateRawEventSequence(toInflate);
						if(inflatedList== null || inflatedList.size() == 0){
							if(recordFailure){
								localStorage.add(new SequenceGenerationResultDetail(
										target, oneTargetSummary,anchorSequence,toInflate,null
										,SequenceGenerationResultDetail.INVALID));
							}
							continue FOR;
						}
						//successful
						for(Event[] completedSequence : inflatedList){
							localStorage.add(new SequenceGenerationResultDetail(
									target, oneTargetSummary,anchorSequence,toInflate,completedSequence));
						}
					}
					
				}
			}
			Utility.writeObject(identifier, localStorage);
			globalStorage.addAll(localStorage);
		}
		return globalStorage;
	}
	
	/**
	 * Given a target line, find all the path summary where the String occurs in the 
	 * execution logs
	 * @param targetLine
	 * @return
	 */
	private List<WrappedSummary> locateTargetPathSummary(String targetLine){
		List<WrappedSummary> result = new ArrayList<WrappedSummary>();
		for(WrappedSummary summary : this.summaries){
			if(summary.executionLog.contains(targetLine)){
				result.add(summary);
			}
		}
		return result;
	}
	
	/**
	 * Given a target Summary, find a list of path summary sequence
	 * @param targetSummary
	 * @return
	 */
	private List<WrappedSummary[]> findSummarySequences(WrappedSummary targetSummary){
		
//		DefaultMutableTreeNode treeRoot = generateTreeSqeuence(this.summaries,targetSummary);
		
		DefaultMutableTreeNode treeRoot = SummaryTree.buildSummaryTree(summaries, targetSummary);
//		UIUtility.showComponent("result", new JTree(treeRoot), JFrame.DISPOSE_ON_CLOSE);
		
		DefaultMutableTreeNode leaf = treeRoot.getFirstLeaf();
		List<WrappedSummary[]> result = new ArrayList<WrappedSummary[]>();

		while(leaf != null){
			Object[] arr = leaf.getUserObjectPath();
			WrappedSummary[] path = new WrappedSummary[arr.length];
			int index = 0;
			for(Object element : arr){
				NodeContent content = (NodeContent)element;
				path[index] = content.summary;
				index += 1;
			}
			if(path[path.length-1].isEntry){
				//Note: path start from target to entry
				// 		and therefore it needs to be reversed
				ArrayUtils.reverse(path);
				result.add(path);
			}
			DefaultMutableTreeNode nextLeaf = leaf.getNextLeaf();
			if(leaf == nextLeaf) break; //no sure if this would happen -- for safety
			else leaf = nextLeaf;
		}
		return result;
	}
	
	/**
	 * For each path summary, find all possible events which might trigger the path
	 * summary. Then use permutation to construct a list of event sequences which 
	 * do not yet contain connectors.
	 * @param anchors
	 * @return
	 */
	private List<Event[]> convertToRawEventSequence(WrappedSummary[] anchors){
		printMessage("Summary sequence: "+Arrays.toString(anchors));
		//each element in matrix represents a list of events which could trigger
		//the path summary in the corresponding position of the input 
		List<Event[]> matrix = new ArrayList<Event[]>(); 
		for(WrappedSummary summary : anchors){
			PathSummary rawSumm = summary.summaryReference;
			List<Event> appliedSequence = rawSumm.getEventSequence();
			if(appliedSequence != null && appliedSequence.size() > 0){
				Event trigger = appliedSequence.get(appliedSequence.size()-1);
				matrix.add(new Event[]{trigger});
			}else{ //should not happen
				Event[] possibleEvents = methodSignatureToEvents.get(summary.methodSignature);
				if(possibleEvents == null || possibleEvents.length == 0){
					return null; // which means a failure 
				}
				matrix.add(possibleEvents);
			}
		}
		return matrixToList(matrix);
	}
	
	/**
	 * each column in the matrix represents a list of events which might trigger a path
	 * summary. use a permutation to expand the matrix to a list of sequence. 
	 * @param matrix
	 * @return
	 */
	private List<Event[]> matrixToList(List<Event[]> matrix){
		return matrixToList(matrix,0);
	}
	private List<Event[]> matrixToList(List<Event[]> matrix, int level){
		if(level == matrix.size() -1 ){ 
			//base case when the last column has been reached
			Event[] current = matrix.get(level);
			List<Event[]> lastColumnResult = new ArrayList<Event[]>();
			for(Event event : current){
				lastColumnResult.add(new Event[]{event});
			}
			return lastColumnResult;
		}
		List<Event[]> result = new ArrayList<Event[]>();
		Event[] column = matrix.get(level);
		List<Event[]> succeedingList = matrixToList(matrix, level+1);
		for(Event currentHead: column){
			for(Event[] succedingEvents : succeedingList){
				//combine event with succeeding events 
				List<Event> sequence = new ArrayList<Event>(Arrays.asList(succedingEvents));
				sequence.add(0,currentHead);
				result.add(sequence.toArray(new Event[0]));
			}
		}
		return result;
	}
	
	
	/**
	 * Given a sequence of events, insert connectors between each two sequential 
	 * events. 
	 * @param toInflate
	 * @return a list of event sequence
	 */
	private List<Event[]> inflateRawEventSequence(Event[] toInflate){
		assert toInflate.length > 0; // this should not happen
		printMessage("ToinFlate: "+Arrays.toString(toInflate));
		
		if(toInflate.length == 1){ // no need for connector
			List<Event[]> result =  new ArrayList<Event[]>();
			result.add(toInflate);
			return result; 
		}
		
		Map<Integer,List<GraphPath<UIState, Event>>> intermediaPathBuffer = 
				new HashMap<Integer, List<GraphPath<UIState, Event>>>();
		Event currentEvent = toInflate[0];
		printMessage("initial Event:"+ currentEvent);
		for(int i=1;i<toInflate.length;i++){
			Event next = toInflate[i];
			printMessage(i+"-th Event:"+ next);
			if(!currentEvent.getTarget().equals(next.getSource())){
				List<GraphPath<UIState, Event>> kPath = 
						getKPath(currentEvent.getTarget(),next.getSource());
				if(kPath == null || kPath.isEmpty()){ 
					return null; // which means a failure	
				} 
				intermediaPathBuffer.put(i, kPath);
			}
			currentEvent = next;
		}
			
		List<Event[]> constructed = recursiveConnectList(intermediaPathBuffer, toInflate, 0);
		return constructed;
	}
	
	/**
	 * Help insert connectors between events
	 * multiple paths could exist between two UIState, 
	 * use permutation at this level
	 * 
	 * Event -> paths -> Event -> paths -> ...
	 * where paths contains multiple event sequence
	 * Local is considered as:
	 * Event -> paths 
	 * local_list contains all permutation between event and paths
	 * at local level. 
	 * 
	 * Succeeding list contains all possible sequences afterwards. 
	 * 
	 * Use permutation to produce a combined result sequences. 
	 * 
	 * @param intermediaPathBuffer
	 * @param toInflate
	 * @param index
	 * @return
	 */
	private List<Event[]> recursiveConnectList(
			Map<Integer,List<GraphPath<UIState, Event>>> intermediaPathBuffer,
			Event[] toInflate, int index){
		
		if(index == toInflate.length - 1){
			//base case where the last event has been reached
			List<Event[]> result = new ArrayList<Event[]>();
			result.add( new Event[]{toInflate[index]});
			return result;
		}else{
			List<Event[]> partialEventSequence = new ArrayList<Event[]>();
			//local list stores all sequences given current node and paths to the next UI 
			List<Event[]> local_lists = new ArrayList<Event[]>();
			Event current = toInflate[index];
			List<GraphPath<UIState, Event>> UIPaths = intermediaPathBuffer.get(index+1);
			if(UIPaths == null){
				local_lists.add(new Event[]{current}); 
			}else{
				for(GraphPath<UIState, Event> path: UIPaths){
					List<Event> events = path.getEdgeList();
					events.add(0, current);
					local_lists.add(events.toArray(new Event[0]));
				}
			}
			List<Event[]> succeedingSequence = recursiveConnectList(intermediaPathBuffer, toInflate, index+1);
			for(Event[] sequence : succeedingSequence){
				for(Event[] currentList : local_lists){
					Event[] singleResult = ArrayUtils.addAll(currentList, sequence);
					partialEventSequence.add(singleResult);
				}	
			}
			return partialEventSequence;
		}
	}
	
	/**
	 * Given a starting point of UIstate, find the first k shortest path to the destination UI
	 * @param start 
	 * @param dest
	 * @return
	 */
	private List<GraphPath<UIState, Event>> getKPath(UIState start, UIState dest){
		printMessage("start: "+start);
		printMessage("dest:  "+dest);
		int index = KthPathUIStateProcessed.indexOf(start);
		if(index >= 0){
			KShortestPaths<UIState, Event> kPath = kShortestPath.get(index);
			return kPath.getPaths(dest);
		}else{
			if(dest.isLauncher || dest.isLauncher){
				printMessage("get Path launcher encountered");
				return null;
			}
			KShortestPaths<UIState, Event> kPath = new KShortestPaths<UIState, Event>(graph, start,ConnectorPathAmount );
			KthPathUIStateProcessed.add(start);
			kShortestPath.add(kPath);
			return kPath.getPaths(dest);
		}
	}

	/**
	 * Legacy code from previous version. User a tree structure to store generated sequences.
	 * During sequence generation, nodes are appended to leaf if satisfaction holds between
	 * symbolic states and cumulative constraints. 
	 * 
	 * An entry point will be appended to leaf if the amount of undetermined variables other
	 * than the ones within the entry summary for the combined constraint do not exceed a 
	 * limit (0 by default) 
	 * 
	 * @param summarySet
	 * @param target
	 * @return the root of tree which can be used for GUI demo
	 */
//	private DefaultMutableTreeNode generateTreeSqeuence(List<WrappedSummary> summarySet, WrappedSummary target){
//		NodeContent targeContnt = new NodeContent(target, target.conditions);
//		DefaultMutableTreeNode root = new DefaultMutableTreeNode(targeContnt);
//		List<DefaultMutableTreeNode> leaves = new ArrayList<DefaultMutableTreeNode>();
//		leaves.add(root);
//		int currentLevel = 0;
//		for(currentLevel=0;currentLevel<maxLevel; currentLevel++){
//			List<DefaultMutableTreeNode> newLeaves = new ArrayList<DefaultMutableTreeNode>();
//			for(DefaultMutableTreeNode leaf : leaves){
//				List<DefaultMutableTreeNode> expanded = expandLeaves(summarySet,leaf);
//				if(expanded == null ) continue;
//				newLeaves.addAll(expanded);
//			}
//			//population control
//			if(newLeaves.size()>maxSearchSpace){
//				int size = newLeaves.size();
//				for( int k = size -1 ; k>=maxSearchSpace ; k--){ newLeaves.remove(k); }
//				leaves = newLeaves;
//			}else{ leaves = newLeaves; }
//			if(leaves.isEmpty()) break;
//		}
//		return root;
//	}
	
	/**
	 * Legacy code from previous version
	 * expand leaves 
	 * 
	 * @param summarySet
	 * @param leaf
	 * @return
	 */
//	private List<DefaultMutableTreeNode> expandLeaves(List<WrappedSummary> summarySet, DefaultMutableTreeNode leaf){
//		NodeContent content = (NodeContent) leaf.getUserObject();
//		if(content.summary.isEntry){ return null; }
//		
//		List<DefaultMutableTreeNode> result = new ArrayList<DefaultMutableTreeNode>();
//		Set<Variable> vars = Expression.getUnqiueVarSet(content.cumulativeConstraint);
//		if(vars.size() == 0){
//			for(WrappedSummary sum : summarySet){
//				if(sum.isEntry){
//					NodeContent newContent = new NodeContent(sum,content.cumulativeConstraint);
//					DefaultMutableTreeNode child = new DefaultMutableTreeNode(newContent);
//					leaf.add(child);
//					result.add(child);
//				}
//			};
//			return result;
//		}
//		
//		for(WrappedSummary summary : summarySet){ 
//			//TODO check valid symbolic state
//			//e.g. x = y+1; y = x+1 is arbitrary
//			//assume such will result as x= y+1; y=y+1+1
//			//assume no register var will be present e.g. v1,v2
//			
//			//Relativity checking
//			Set<Expression> copiedConstraint = new HashSet<Expression>();
//			for(Expression f: content.cumulativeConstraint){
//				copiedConstraint.add(f.clone());
//			}
//			boolean related = false;
//			for(int index = summary.symbolic.size()-1 ; index >=0 ; index--){
//				Expression assign = summary.symbolic.get(index);
//				if(assign.getChildCount() != 2) continue; //which should not be 
//				
//				for(Expression condition: copiedConstraint){
//					boolean anyChange = condition.replace(
//							((Expression)assign.getChildAt(0)).clone(), 
//							((Expression)assign.getChildAt(1)).clone());
//					related = related || anyChange;
//				}
//			}
//			if(related == false) continue;
//			
//			//build statements 
//			for(Expression oneConstraint : summary.conditions){
//				copiedConstraint.add(oneConstraint);
//			}
//			List<String> statements = new ArrayList<String>();
//			//add variable declaration statements
//			Set<Variable> varSet = Expression.getUnqiueVarSet(copiedConstraint);
//			for(Variable var : varSet){
//				statements.add(var.toVariableDefStatement());
//			}
//			//add assertion statements
//			for(Expression f : copiedConstraint){
//				String assertion = Expression.createAssertion(f.toYicesStatement());
//				statements.add(assertion);
//			}
//			statements.add(YICES_CHECK);
//			
//			//check satisfiability
//			boolean sat = this.solver.solve(true, statements.toArray(new String[0]));
//			if(sat == false) continue;
//			
//			//if the appended summary is entry, check if the amount of unsolved symbolic variable
//			if(summary.isEntry){
//				Set<Variable>  unsolved = Expression.getUnqiueVarSet(copiedConstraint);
//				Set<Variable> whiteList = Expression.getUnqiueVarSet(summary.conditions);
//				for(Variable var : whiteList){
//					unsolved.remove(var);
//				}
//				if(unsolved.size() > MAX_UNSOLVED){
//					continue;
//				}
//			}
//			NodeContent newContent = new NodeContent(summary,copiedConstraint);
//			DefaultMutableTreeNode child = new DefaultMutableTreeNode(newContent);
//			leaf.add(child);
//			result.add(child);
//		}
//		return result;
//	}
	
	private static void printMessage(String... inputs){
		for(String input : inputs){
			System.out.println(input);
		}
	}
}
