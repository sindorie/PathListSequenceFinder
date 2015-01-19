package versionSimple;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.lang3.ArrayUtils;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;

import main.Paths;
import analysis.Expression;
import analysis.Variable;
import Component.ExpressionUtility;
import Component.NodeContent;
import Component.WrappedSummary;
import concolic.PathSummary;
import staticFamily.StaticApp;
import support.PathSummaryUIFactory;
import support.Utility;
import support.GUI.SummarySelectionWindow;
import support.GUI.UIUtility;
import support.solver.YicesProcessInterface;
import zhen.version1.UIModelGenerator;
import zhen.version1.Support.Bundle;
import zhen.version1.component.Event; 
import zhen.version1.component.UIModelGraph.ListenableDirectedMultigraph;
import zhen.version1.component.UIState;

public class SequenceFinder {
	private YicesProcessInterface solver;
	
	private DefaultListModel<WrappedSummary> wrappedSummaryListModel;
	private DefaultMutableTreeNode root;
	private List<PathSummary> filterSummary;
	private WrappedSummary target;
	private List<NodeContent[]> contentPath;
	private List<Event[]> rawSequenceList;
	private List<Event[]> eventSequenceList;
	private Map<String,Event[]> methodEventMap;
	
	private List<PathSummary> rawList;
	private UIModelGenerator builder;
	
	private static int MAX_UNSOLVED = 0, ConnectorPathAmount = 5;
	private static final String YICES_CHECK = "(check)\n";
	private static boolean showGUI = true;
	private int maxLevel = 5, maxSearchSpace = 20;
	private OperationEvent task;
	public final static boolean DEBUG = true;
	
	public SequenceFinder(YicesProcessInterface solver){
		this.solver = solver;
	}

	public List<Event[]> getSequenceList(){
		return eventSequenceList;
	}
	
	public WrappedSummary getSelectedTarget(){
		return this.target;
	}
	
	public void operate(StaticApp app, String stroageName, boolean force,
			UIModelGenerator builder, List<PathSummary> rawList,
			String targetMethodSig){	
		
		this.builder = builder;
		this.rawList = rawList;

		PathSummary local_target = null;
		
		for(PathSummary sum : rawList){
			ArrayList<String> logs = sum.getExecutionLog();
			if(logs.contains(targetMethodSig)){
				System.out.println(logs);
				local_target = sum;
				break;
			}
		}
		if(local_target == null){
			System.out.println("cannot identify target in pathsummary by "+targetMethodSig);
			return;
		}
		
		filterAndSelect(rawList);
		target = new WrappedSummary(local_target);
		
		int count = 0;
		for(int i=0;i<wrappedSummaryListModel.getSize();i++){
			WrappedSummary element = wrappedSummaryListModel.get(i);
			String sig = element.methodSignature;
			if(sig.contains("onCreate")){
				element.isEntry = true;
				count += 1;
			}
		}
		if(count == 0){
			System.out.println("no entry identified");
			return;
		}
		
		procedure(stroageName);
	}
	
	
	
	public void loadOrOperate(String storageName, boolean force, UIModelGenerator builder, List<PathSummary> rawList){
		storageName = storageName+"_Sequence";
		this.rawList = rawList;
		this.builder = builder;
		
		File toRead = new File(Paths.appDataDir+storageName);
		if(toRead.exists() == false || force){
			operate(storageName);
		}else{
			System.out.println("here1");
			loadData(storageName);
			System.out.println("here2");
			if(task != null) task.onSequenceReady(this);
		}
	}
	
	public void checkData(){
		UIUtility.showComponent(PathSummaryUIFactory.buildSummaryListComponent(rawList));
		SummarySelectionWindow window = new SummarySelectionWindow();
		window.setListModel(wrappedSummaryListModel);
		window.show();
		
		System.out.println("contentPath:");
		for(NodeContent[] content : contentPath){
			System.out.println(Arrays.toString(content));
		}
		System.out.println("\ngetUniqueEventList:");
		for(Event event : builder.getUniqueEventList()){
			System.out.println(event.getMethodHits());
		}
		
		System.out.println("\nrawSequenceList");
		if(rawSequenceList != null)
		for(Event[] arr : rawSequenceList){
			System.out.println(Arrays.toString(arr));
		}
		System.out.println("\neventSequenceList");	
		if(eventSequenceList != null)
		for(Event[] arr : eventSequenceList){
			System.out.println(Arrays.toString(arr));
		}
	}
	
	public void setPostOperation(OperationEvent task){
		this.task = task;
	}

	private void saveData(String identifier){
		Bundle bundle = new Bundle(wrappedSummaryListModel,filterSummary,target,
				root,contentPath,rawSequenceList, eventSequenceList,methodEventMap
				); 
		boolean result = Utility.writeObject(identifier, bundle);
		if(result == false) System.out.println("Save data failure");
	}
	
	@SuppressWarnings("unchecked")
	private void loadData(String identifier){
		Bundle bundle = (Bundle) Utility.readObject(identifier);
		if(bundle == null) System.out.println("Storage :"+identifier + " does not exist");
		this.wrappedSummaryListModel = (DefaultListModel<WrappedSummary>) bundle.os[0];
		this.filterSummary = (List<PathSummary>) bundle.os[1];
		this.target = (WrappedSummary) bundle.os[2];
		this.root = (DefaultMutableTreeNode) bundle.os[3];
		this.contentPath = (List<NodeContent[]>) bundle.os[4];
		this.rawSequenceList = (List<Event[]>) bundle.os[5];
		this.eventSequenceList = (List<Event[]>) bundle.os[6];
		this.methodEventMap = (Map<String, Event[]>) bundle.os[7];
	}
	
	/**
	 * Will open a GUI to allow selection of target pathsummary and entry
	 * 
	 * @param UIModel
	 * @param rawList
	 */
	private void operate(final String storageName){
		UIUtility.showComponent(PathSummaryUIFactory.buildSummaryListComponent(rawList));
		
		filterAndSelect(rawList);
		if(wrappedSummaryListModel.isEmpty()) System.out.println("Wrappped summary list model is empty");
		
		//let the user choose the target and entry 
		SummarySelectionWindow window = new SummarySelectionWindow();
		window.setListModel(wrappedSummaryListModel);
		window.setAction(new SummarySelectionWindow.Action() {
			@Override
			public boolean activate(final SummarySelectionWindow window) {
				Thread workerThread = new Thread(new Runnable(){
					@Override
					public void run() {
						target = window.target;
						procedure(storageName);
					}
				});
				workerThread.start();
				return true;
			}
		});
		window.show();
	}
	
	private void procedure(String storageName){
		List<WrappedSummary> list = new ArrayList<WrappedSummary>();
		for(int i =0 ;i < wrappedSummaryListModel.getSize();i++){
			list.add(wrappedSummaryListModel.getElementAt(i));
		}
		root = findSequence(list,target);
		buildSummaryEventMap(list);
		
		if(showGUI){
			UIUtility.showComponent("result", new JTree(root), JFrame.DISPOSE_ON_CLOSE);
		}
		//retrieve a list of sequence of nodeContent
		DefaultMutableTreeNode leaf = root.getFirstLeaf();
		contentPath = new ArrayList<NodeContent[]>(); 
		while(leaf != null){
			NodeContent content = (NodeContent)leaf.getUserObject();
			if(content.summary.isEntry){
				Object[] arr = leaf.getUserObjectPath();
				NodeContent[] path = Arrays.copyOf(arr, arr.length, NodeContent[].class);
				ArrayUtils.reverse(path);
				contentPath.add(path);
			}
			leaf = leaf.getNextLeaf();
		}
		System.out.println("workerThread primary job finished");
		rawSequenceList = findRawEventSequence(contentPath);
		eventSequenceList = inflateToEventSequence();
		
		saveData(storageName);
		System.out.println("Main job Complete");
		
		if(this.task!=null) task.onSequenceReady(this);
	}
	
	private void buildSummaryEventMap(List<WrappedSummary> list){
		methodEventMap = new HashMap<String,Event[]>();
		List<Event> uniqueList = builder.getUniqueEventList();
		for(WrappedSummary summary:list){
			List<Event> mappingResult = new ArrayList<Event>();
			for(Event event : uniqueList){ 
				List<String> hits = event.getMethodHits();
				
				if(hits.contains(summary.methodSignature)){
					mappingResult.add(event);
				}
				
//				String qualified = null;
//				for(String hit : hits){
//					if(hit.startsWith("Landroid/support") || hit.contains("<init>")) continue;
//					qualified = hit; break;
//				}
//				if(qualified == null) continue;
//				
//				System.out.println(event);
//				System.out.println("qualified:"+qualified);
//				System.out.println(summary.methodSignature);
//				System.out.println(qualified.equals((summary.methodSignature)));
//				if(qualified.equals(summary.methodSignature)){
//					mappingResult.add(event);
//				}
			}
			if(!mappingResult.isEmpty())
				methodEventMap.put(summary.methodSignature, mappingResult.toArray(new Event[0]));
		}
	}
	
	/**
	 * current implementation may not find connector correctly
	 * assume launch should be included
	 * @param builder
	 * @return
	 */
	private List<Event[]> inflateToEventSequence(){
		ListenableDirectedMultigraph graph = builder.getUIModel().cloneWithNoLauncher();
		List<Event[]> result = new ArrayList<Event[]>();
		
		Major: for(Event[] toInflate : rawSequenceList){
			if(toInflate.length == 0) continue;
			if(toInflate.length == 1){
				result.add(toInflate);
				continue;
			}
			
			Map<Integer,List<GraphPath<UIState, Event>>> intermediaPathBuffer = 
					new HashMap<Integer, List<GraphPath<UIState, Event>>>();
			
			Event currentEvent = toInflate[0];
			for(int i=1;i<toInflate.length;i++){
				Event next = toInflate[i];
				if(!currentEvent.getTarget().equals(next.getSource())){
					List<GraphPath<UIState, Event>> kPath = 
							getKPath(graph,currentEvent.getTarget(),next.getSource());
					if(kPath.isEmpty()){ continue Major;}
					intermediaPathBuffer.put(i, kPath);
				}
				currentEvent = next;
			}
			
			List<Event[]> constructed = recursiveConnectList(intermediaPathBuffer, toInflate, 0);
			result.addAll(constructed);
		}
		return result;
	}
	
	private List<UIState> included = new ArrayList<UIState>();
	private List<KShortestPaths<UIState, Event>> kShortestPath = new ArrayList<KShortestPaths<UIState, Event>>();
	private List<GraphPath<UIState, Event>> getKPath(ListenableDirectedMultigraph graph,
			UIState start, UIState dest){
		int index = included.indexOf(start);
		if(index >= 0){
			KShortestPaths<UIState, Event> kPath = kShortestPath.get(index);
			return kPath.getPaths(dest);
		}else{
			KShortestPaths<UIState, Event> kPath = new KShortestPaths<UIState, Event>(graph, start,ConnectorPathAmount );
			included.add(start);
			kShortestPath.add(kPath);
			return kPath.getPaths(dest);
		}
	}
	
	private List<Event[]> recursiveConnectList(
			Map<Integer,List<GraphPath<UIState, Event>>> intermediaPathBuffer,
			Event[] toInflate, int index){
		if(index == toInflate.length - 1){// the end;
			List<Event[]> result = new ArrayList<Event[]>();
			result.add( new Event[]{toInflate[index]});
			return result;
		}else{
			List<Event[]> result = new ArrayList<Event[]>();
			List<Event[]> local_list = new ArrayList<Event[]>();
			Event current = toInflate[index];
			List<GraphPath<UIState, Event>> paths = intermediaPathBuffer.get(index+1);
			if(paths == null){
				local_list.add(new Event[]{current});
			}else{
				for(GraphPath<UIState, Event> path: paths){
					List<Event> events = path.getEdgeList();
					events.add(0, current);
					local_list.add(events.toArray(new Event[0]));
				}
			}
			List<Event[]> toConcats = recursiveConnectList(intermediaPathBuffer, toInflate, index+1);
			
			for(Event[] toConcat : toConcats){
				for(Event[] currentList : local_list){
					Event[] singleResult = ArrayUtils.addAll(currentList, toConcat);
					result.add(singleResult);
				}	
			}
			
			return result;
		}
	}
	
	/**
	 * The current implementation may not choose connector correctly
	 * @param graph
	 * @param contentPath
	 * @return
	 */
	private List<Event[]> findRawEventSequence(List<NodeContent[]> contentPath){
		for(NodeContent[] contents : contentPath){
			System.out.println("contentPath:"+Arrays.toString(contents));
		}
		
		
		List<Event[]> result = new ArrayList<Event[]>();
		int index = 0;
		for(NodeContent[] contentList : contentPath){
			List<Event[]> sequence = unitProcess(contentList);
			for(Event[] columns : sequence){
				System.out.println("columns: "+Arrays.toString(columns));
			}
			
			//will not return null and empty sequence is needed
//			if(sequence == null || sequence.size() == 0) continue;
			List<Event[]> local_result = recursiveConstruct(sequence);
			for(Event[] local : local_result){
				System.out.println("local_result:"+Arrays.toString(local));
			}
			
			result.addAll(local_result);
			index+=1;
		}
		return result;
	}
	
	//low level permutation
	private List<Event[]> recursiveConstruct(List<Event[]> input){
		switch(input.size()){
		case 0: return new ArrayList<Event[]>();
		case 1:{ 
			Event[] colEvent = input.get(0);
			List<Event[]> result = new ArrayList<Event[]>();
			for(Event singleEvent : colEvent){
				result.add(new Event[]{singleEvent});
			}
			return result;
		}
		default:{
			Event[] currentColumn = input.get(0);
			System.out.println("recursiveConstruct: "+Arrays.toString(currentColumn));
			List<Event[]> sequences = recursiveConstruct(input.subList(1, input.size()));

			List<Event[]> result = new ArrayList<Event[]>();
			for(Event cell : currentColumn){
				for(Event[] sequence : sequences){
					List<Event> list = new ArrayList<Event>();
					list.add(cell);
					for(Event element : sequence){
						list.add(element);
					}
					result.add(list.toArray(new Event[0]));
				}
			}
			return result;
		}
		}
	}
	
	private List<Event[]> unitProcess(NodeContent[] contentList){
		//first step map to event. 
		List<Event[]> rawEventSequence = new ArrayList<Event[]>();
		for(NodeContent node:contentList){
			List<Event> summaryEventSequence = node.summary.summaryReference.getEventSequence();
			if(summaryEventSequence != null && summaryEventSequence.size() > 0){
				//concolic generated
				Event trigger = summaryEventSequence.get(summaryEventSequence.size()-1);
				rawEventSequence.add(new Event[]{trigger});
			}else{ 
				//Symbolic generated
				Event[] triggers = methodEventMap.get(node.summary.methodSignature);
				if(triggers==null || triggers.length == 0){
					System.out.println("\tcannot find event for "+node.summary.methodSignature);
					return new ArrayList<Event[]>(); // broken
				}
				rawEventSequence.add(triggers);
			}
		}
		return rawEventSequence;
	}
	
	private DefaultMutableTreeNode findSequence(List<WrappedSummary> summarySet, WrappedSummary target){
		if(DEBUG){
			System.out.println("input");
			System.out.println(summarySet);
			System.out.println(target);
		}
		
		NodeContent targeContnt = new NodeContent(target, target.conditions);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(targeContnt);
		List<DefaultMutableTreeNode> leaves = new ArrayList<DefaultMutableTreeNode>();
		leaves.add(root);
		int currentLevel = 0;
		for(currentLevel=0;currentLevel<maxLevel; currentLevel++){
			if(DEBUG) System.out.println("Level #"+currentLevel);
			
			List<DefaultMutableTreeNode> newLeaves = new ArrayList<DefaultMutableTreeNode>();
			for(DefaultMutableTreeNode leaf : leaves){
				List<DefaultMutableTreeNode> expanded = expandLeaves(summarySet,leaf);
				if(expanded == null ) continue;
				newLeaves.addAll(expanded);
			}
			if(DEBUG) System.out.println("Generatd Leaves amount: "+newLeaves.size());
			
			//population control
			if(newLeaves.size()>maxSearchSpace){
				System.out.println("maxSearchSpace achieved");
				int size = newLeaves.size();
				for( int k = size -1 ; k>=maxSearchSpace ; k--){ newLeaves.remove(k); }
				leaves = newLeaves;
			}else{ leaves = newLeaves; }
			if(leaves.isEmpty()) break;
		}
		if(DEBUG) System.out.println("Final level: "+ currentLevel+" vs max: "+maxLevel);
		
		return root;
	}
	
	private List<DefaultMutableTreeNode> expandLeaves(List<WrappedSummary> summarySet, DefaultMutableTreeNode leaf){
		if(DEBUG){
			System.out.println("expandLeaves");
		}
		NodeContent content = (NodeContent) leaf.getUserObject();
		if(DEBUG){
			System.out.println(content.summary.toStringDetail());
		}
				
		if(content.summary.isEntry){
			if(DEBUG) System.out.println("Target is entry");
			return null;
		}
		
		List<DefaultMutableTreeNode> result = new ArrayList<DefaultMutableTreeNode>();
		Set<Variable> vars = Expression.getUnqiueVarSet(content.cumulativeConstraint);
		if(vars.size() == 0){
			for(WrappedSummary sum : summarySet){
				if(sum.isEntry){
					NodeContent newContent = new NodeContent(sum,content.cumulativeConstraint);
					DefaultMutableTreeNode child = new DefaultMutableTreeNode(newContent);
					leaf.add(child);
					result.add(child);
				}
			};
			return result;
		}
		
		for(WrappedSummary summary : summarySet){ 
			//TODO check valid symbolic state
			//e.g. x = y+1; y = x+1 is arbitrary
			//assume such will result as x= y+1; y=y+1+1
			//assume no register var will be present e.g. v1,v2
			if(DEBUG){
				System.out.println("----Current Summary----");
				System.out.println(summary.toStringDetail());
			}
			
			//Relativity checking
			Set<Expression> copiedConstraint = new HashSet<Expression>();
			for(Expression f: content.cumulativeConstraint){
				copiedConstraint.add(f.clone());
			}
			boolean related = false;
			for(int index = summary.symbolic.size()-1 ; index >=0 ; index--){
				Expression assign = summary.symbolic.get(index);
				if(assign.getChildCount() != 2) continue; //which should not be 
				
				for(Expression condition: copiedConstraint){
					boolean anyChange = condition.replace(
							((Expression)assign.getChildAt(0)).clone(), 
							((Expression)assign.getChildAt(1)).clone());
					related = related || anyChange;
				}
			}
			if(DEBUG){
				System.out.println("Relativity checking: "+ related);
			}
			if(related == false) continue;
			
			
			//build statements
			for(Expression single : summary.conditions){
				copiedConstraint.add(single);
			}
			
			
//			copiedConstraint.addAll(summary.conditions);
			for(Expression f :copiedConstraint){
				System.out.println(f.toYicesStatement());
			}
			
			
			List<String> statements = new ArrayList<String>();
			//define variables 
			System.out.println("getUnqiueVarSet");
			Set<Variable> varSet = Expression.getUnqiueVarSet(copiedConstraint);
			for(Variable var : varSet){
				statements.add(var.toVariableDefStatement());
			}
			//add assertion
			for(Expression f : copiedConstraint){
				String assertion = Expression.createAssertion(f.toYicesStatement());
				statements.add(assertion);
			}
			statements.add(YICES_CHECK);
			
			if(DEBUG){
				for(String state : statements){
					System.out.println(state);
				}
//				if(stepControl){
//					String next = sc.nextLine().trim();
//					if(next.equals("0")) System.exit(0);
//					else if(next.equalsIgnoreCase("9")){
//						stepControl = false;
//					}
//				}
				System.out.println("Checking satisfiability");
			}
			
			
			//check satisfiability
			boolean sat = this.solver.solve(true, statements.toArray(new String[0]));
			if(DEBUG) System.out.println("sat: "+sat);
			if(sat == false) continue;
			
			//if the appended summary is entry, check if the amount of unsolved symbolic variable
			if(summary.isEntry){
				Set<Variable>  unsolved = Expression.getUnqiueVarSet(copiedConstraint);
				if(DEBUG){
					System.out.println("Remaining unsolved vars: "+unsolved);
				}
				
				Set<Variable> whiteList = Expression.getUnqiueVarSet(summary.conditions);
				for(Variable var : whiteList){
					unsolved.remove(var);
				}
				
				if(unsolved.size() > MAX_UNSOLVED){
					if(DEBUG){
						System.out.println("Cancel appending entry summary: "+unsolved.size()+"> max:"+MAX_UNSOLVED);
					}
					continue;
				}
			}
			
			
			NodeContent newContent = new NodeContent(summary,copiedConstraint);
			DefaultMutableTreeNode child = new DefaultMutableTreeNode(newContent);
			leaf.add(child);
			result.add(child);
		}
		return result;
	}
 
	private void filterAndSelect(List<PathSummary> rawList){
		filterSummary = new ArrayList<PathSummary>();
		wrappedSummaryListModel = new DefaultListModel<WrappedSummary>();
		for (PathSummary summary : rawList) {
			PathSummary copy = new PathSummary(); 
			copy.setMethodSignature(summary.getMethodSignature());
			for(String line : summary.getExecutionLog()){ copy.addExecutionLog(line);}
			List<Expression> symbolicList = ExpressionUtility.transform(summary.getSymbolicStates());
			List<Expression> constraintList = ExpressionUtility.transform(summary.getPathCondition());
			copy.setSymbolicStates(Utility.castToArrayList(symbolicList));
			copy.setPathCondition(Utility.castToArrayList(constraintList));
			filterSummary.add(copy);
			WrappedSummary wrapped = new WrappedSummary(copy);
			wrappedSummaryListModel.addElement(wrapped);
		}
	}
	
	private boolean checkEntryExistance(){
		for(int i= 0;i<wrappedSummaryListModel.getSize();i++){
			WrappedSummary wrapped = wrappedSummaryListModel.get(i);
			if(wrapped.isEntry) return true;
		}
		return false;
	}
	
	public static interface OperationEvent{
		public void onSequenceReady(SequenceFinder finder);
	}
}
