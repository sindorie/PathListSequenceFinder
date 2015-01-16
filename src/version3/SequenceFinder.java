package version3;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.lang3.ArrayUtils;
import org.jgrapht.EdgeFactory;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleDirectedGraph;

import main.Paths;
import analysis.Expression;
import analysis.Variable;
import Component.ExpressionUtility;
import Component.NodeContent;
import Component.WrappedSummary;
import concolic.PathSummary;
import support.PathSummaryUIFactory;
import support.Utility;
import support.GUI.SummarySelectionWindow;
import support.GUI.UIUtility;
import support.solver.YicesProcessInterface;
import zhen.version1.UIModelGenerator;
import zhen.version1.Support.Bundle;
import zhen.version1.component.Event; 
import zhen.version1.component.Pair;
import zhen.version1.component.UIModelGraph;
import zhen.version1.component.UIModelGraph.ListenableDirectedMultigraph;
import zhen.version1.component.UIState;

public class SequenceFinder {
	private YicesProcessInterface solver;
	
	private DefaultListModel<WrappedSummary> wrappedSummaryListModel;
	private DefaultMutableTreeNode root;
	private List<PathSummary> filterSummary;
	private WrappedSummary target;
	private List<NodeContent[]> contentPath;
	private Map<Integer, List<Event[]>> indexToRawSequenceList;
	private Map<Integer, List<Event[]>> indexToInflatedSequenceList;
	private UIModelGenerator builder;
	
	private List<Event[]> rawSequenceList;
	private List<Event[]> eventSequenceList;
	private Map<String,Event[]> methodEventMap;
	
	public static int MAX_UNSOLVED = 0;
	public static boolean stepControl = false;	
	public static final String YICES_CHECK = "(check)\n";
	public final static boolean DEBUG = true;
	public static boolean showGUI = true;
	private int maxLevel = 5, maxSearchSpace = 20; // index = 0,
	
	public SequenceFinder(YicesProcessInterface solver){
		this.solver = solver;
	}

	public void loadOrOperate(String storageName, boolean force, UIModelGenerator builder, List<PathSummary> rawList){
		storageName = storageName+"_Sequence";
		File toRead = new File(Paths.appDataDir+storageName);
		if(toRead.exists() == false || force){
			operate(storageName, builder,rawList);
		}else{
			loadData(storageName);
//			procedure(storageName, builder);
			
			List<Event> list = builder.getEventDeposit();
			for(Event event :list){
				System.out.println(event);
				System.out.println(event.getMethodHits());
			}
			
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
			System.out.println("methodEventMap:");
			System.out.println(methodEventMap);
		}
	}

	private void saveData(String identifier){
		Bundle bundle = new Bundle(wrappedSummaryListModel,filterSummary,target,
				root,contentPath,rawSequenceList, eventSequenceList,methodEventMap,
				indexToRawSequenceList, indexToInflatedSequenceList); 
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
		this.indexToRawSequenceList = (Map<Integer, List<Event[]>>) bundle.os[8];
		this.indexToInflatedSequenceList = (Map<Integer, List<Event[]>>) bundle.os[9];
	}
	
	/**
	 * Will open a GUI to allow selection of target pathsummary and entry
	 * 
	 * @param UIModel
	 * @param rawList
	 */
	public void operate(final String storageName, final UIModelGenerator builder, final List<PathSummary> rawList){
		System.out.println("Input PathSummary size:"+rawList.size());
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
						procedure(storageName, builder);
					}
				});
				workerThread.start();
				return true;
			}
		});
		window.show();
	}
	
	private void procedure(String storageName, UIModelGenerator builder){
		List<WrappedSummary> list = new ArrayList<WrappedSummary>();
		for(int i =0 ;i < wrappedSummaryListModel.getSize();i++){
			list.add(wrappedSummaryListModel.getElementAt(i));
		}
		root = findSequence(list,target);
		buildSummaryEventMap(list,builder);
		
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
		
		System.out.println("contentPath:");
		for(NodeContent[] content : contentPath){
			System.out.println(Arrays.toString(content));
		}
		System.out.println("\ngetUniqueEventList:");
		for(Event event : builder.getUniqueEventList()){
			System.out.println(event.getMethodHits());
		}
		
		rawSequenceList = findRawEventSequence(contentPath);
		eventSequenceList = inflateToEventSequence(builder);

		eventSequenceCheck();
		saveData(storageName);
		
		System.out.println("Main job Complete");
		//TODO validation
	}
	
	private void eventSequenceCheck(){
		System.out.println("Conent checking:");
		System.out.println("rawSequenceList");
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
	
	private void buildSummaryEventMap(List<WrappedSummary> list, UIModelGenerator builder){
		methodEventMap = new HashMap<String,Event[]>();
		List<Event> uniqueList = builder.getUniqueEventList();
		for(WrappedSummary summary:list){
			List<Event> mappingResult = new ArrayList<Event>();
			for(Event event : uniqueList){ 
				List<String> hits = event.getMethodHits();
				
				String qualified = null;
				for(String hit : hits){
					if(hit.startsWith("Landroid/support") || hit.contains("<init>")) continue;
					qualified = hit; break;
				}
				if(qualified == null) continue;
				
				System.out.println(event);
				System.out.println("qualified:"+qualified);
				System.out.println(summary.methodSignature);
				System.out.println(qualified.equals((summary.methodSignature)));
				if(qualified.equals(summary.methodSignature)){
					mappingResult.add(event);
				}
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
	public List<Event[]> inflateToEventSequence(UIModelGenerator builder){
		ListenableDirectedMultigraph graph = builder.getUIModel().cloneWithNoLauncher();
		List<Event[]> result = new ArrayList<Event[]>();
		Major: for(Event[] toInflate : rawSequenceList){
			List<Event> eventBuffer = new ArrayList<Event>();
			if(toInflate.length == 0) continue;
			Event currentEvent = toInflate[0];
			if(!currentEvent.getSource().equals(UIState.Launcher)){
				System.out.println("uninflated sequence not starting with launcher");
				continue;
			}
			
			eventBuffer.add(currentEvent);
			for(int i=1;i<toInflate.length;i++){
				Event next = toInflate[i];
				if(!currentEvent.getTarget().equals(next.getSource())){
					List<Event> sequence = UIModelGraph.getEventSequence(graph, currentEvent.getTarget(), next.getSource());
					System.out.println("Different: "+currentEvent.getTarget() +" "+ next.getSource());
					if(sequence == null || sequence.isEmpty()) continue Major;
					eventBuffer.addAll(sequence);
				}
				eventBuffer.add(next);
				currentEvent = next;
			}
			result.add(eventBuffer.toArray(new Event[0]));
		}
		return result;
	}
	
	
	//TODO under construction -- primary focus
	/**
	 * Input: 
	 * 1. Raw sequence event list where each of them might trigger the path summary
	 * in the contentNode of the equivalent position. The constraint and symbolic states
	 * in the path summary is needed in this section.
	 * 
	 * 2. UIModel graph which is used to find an event possible at certain point. 
	 * 
	 * 3. Solver.
	 * 
	 * Methodology:
	 * The raw event sequence is not the actual event sequence the program needs to
	 * generate as the connectors is required to facilitate UI state transaction. 
	 * 
	 * In order to find connector(s) between two events e1 and e2, the following data
	 * are required: 
	 * 1. cumulative symbolic states which is the result of a sequence of events or
	 * path summary. 
	 * 2. UIModel graph which tells all known events at a certain UIState.
	 * 
	 * At the end e1, the destination UI UI_1 of e1 should be reached. A list of possible
	 * events are obtained from UI_1. Given an event-pathsummary Map, a list of path 
	 * summary is obtained for each event. A path summary is ignored if it tries to modify
	 * any symbolic states in the cumulative one. The solver then is used to check which
	 * path summary is possible so that a possible connector can be distinguished from 
	 * others. 
	 * 
	 * The connector will update the cumulative symbolic states.
	 * 
	 * A search which taken into above aspect is performed. 
	 * 
	 * @param builder
	 */
	public void findConnector(){
		ListenableDirectedMultigraph graph = builder.getUIModel().cloneWithNoLauncher();
		//There should be an counter part for each Event[] of rawSquenceList in the ContentPath
		//which can be identified by position index of the array
		
		for(int i=0;i<contentPath.size();i++){
			//find the arr which contains the path summary sequence. 
			NodeContent[] content = this.contentPath.get(i);
			//find the sequences given the index, the length of the sequence 
			//should be the same as the length of path summary sequence
			List<Event[]> sequences = indexToRawSequenceList.get(i);
			//a null or empty sequence indicates no satisfiable path available to the 
			//path summary sequence. This is due to a failure in finding an event for
			//a path summary
			if(sequences == null || sequences.isEmpty()) continue;
			
			for(Event[] toInflate : sequences){

				
			}
		}
	}
	
	private void inflateEventSequence(Event[] toInflate, NodeContent[] contents){
		assert toInflate.length == contents.length;
		ListenableDirectedMultigraph graph = builder.getUIModel().cloneWithNoLauncher();
		
		Map<Expression, Expression> cumulativeSymbolicStates = new HashMap<Expression,Expression>();
		for(int i = 1;i<toInflate.length;i++){
			Event e1 = toInflate[i-1];
			Event e2 = toInflate[i];
			
			if(e1.getTarget().equals(e2.getSource())){
				//TODO no connector is needed
				
				continue;
			}
			
			//Need to find the connector
			//obtain needed reference
			NodeContent content1 = contents[i-1];
			NodeContent content2 = contents[i];
			WrappedSummary p1 = content1.summary;
			WrappedSummary p2 = content2.summary;
			
			//combine symbolic states of p1 with cumulative one
//			for(Expression state : p1.symbolic){
//				Expression left = (Expression) state.getChildAt(0);
//				Expression right = (Expression) state.getChildAt(1);
//				cumulativeSymbolicStates.put(left, right);
//			}
			
			UIState current = e2.getTarget();
			
			Set<Event> edges = graph.edgesOf(current);
			//for safety, check if the 
		
		}
	}
	
	
	
	
	
	/**
	 * This implementation does not use a global graph
	 * @param graph
	 * @param origin
	 * @param destination
	 * @param startingSymbolicState
	 * @param targetConstraints
	 */
	public List<EventSummaryPair> forwardTrackingForBackWard(
			ListenableDirectedMultigraph graph, 
			UIState origin, UIState destination, 
			Map<Expression,Expression> startingSymbolicState,
			Set<Expression> targetConstraints ){

		DirectedMultigraph<UIStateSymbolicPair,EventSummaryPair> uiSymb_event_graph
		= new DirectedMultigraph<UIStateSymbolicPair,EventSummaryPair>(EventSummaryPair.class);
		
		int iteration = 0;
		int maxIter = 1024;
		Queue<UIStateSymbolicPair> queue = new LinkedList<UIStateSymbolicPair>();
		UIStateSymbolicPair root = new UIStateSymbolicPair(origin, startingSymbolicState);
		queue.add(root);
		while(queue.isEmpty() == false){
			UIStateSymbolicPair currentState = queue.poll();
			uiSymb_event_graph.addVertex(currentState);
			
			//an event-symbolic pair indicates the event will trigger this path Summary
			List<EventSummaryPair> possibleEventSymbolicPair
				= possibleEventsOfUIForBackward(graph,currentState,startingSymbolicState);
			
			for(EventSummaryPair pair : possibleEventSymbolicPair){
				UIState local_dest = pair.event.getTarget();
				//copy the symbolic records and upate it
				Map<Expression, Expression> symTable = new HashMap<Expression,Expression>();
				symTable.putAll(currentState.symbolic);
				Map<Expression,Expression> symb = listToMap(pair.summary.symbolic);
				symTable.putAll(symb);
				
				UIStateSymbolicPair nextState = new UIStateSymbolicPair(local_dest, symTable);
				if(uiSymb_event_graph.containsVertex(nextState)){
					continue;
				}else{
					uiSymb_event_graph.addVertex(nextState);
					uiSymb_event_graph.addEdge(currentState, nextState, pair);
					if(local_dest.equals(destination) && checkSatisfaction(symTable, targetConstraints)){
						//check the symbolic states is satisfied with constraint
						//this should work!
						List<EventSummaryPair> sequence = DijkstraShortestPath.findPathBetween(uiSymb_event_graph, root, nextState);		
						return sequence;
					}else{
						queue.add(nextState);
					}
				}
			}
			
			iteration+=1;
			if(iteration >= maxIter){
				break;
			}
		}
		return null;
	}

	private Map<Expression,Expression> listToMap(List<Expression> list){
		Map<Expression,Expression> map = new HashMap<Expression,Expression>();
		for(Expression expre : list){
			map.put((Expression)expre.getChildAt(0), (Expression)expre.getChildAt(1));
		}
		return map;
	}
	
	private boolean checkSatisfaction(Map<Expression, Expression> symbolic, Set<Expression> constraints){
		Set<Expression> copiedConstraint = new HashSet<Expression>();
		for(Expression f: constraints){
			copiedConstraint.add(f.clone());
		}
		return solver.solve(true, toYicesStatement(symbolic, copiedConstraint));
	}
	
	private String[] toYicesStatement(Map<Expression, Expression> symbolic, Set<Expression> constraints){
		//TODO check if variable is not initialized
		
		for(Expression constaint: constraints){
			for(Entry<Expression, Expression> entry : symbolic.entrySet()){
				constaint.replace(entry.getKey(), entry.getValue());
			}
		}
		
		List<String> statements = new ArrayList<String>();
		Set<Variable> varSet = Expression.getUnqiueVarSet(constraints);
		for(Variable var : varSet){
			statements.add(var.toVariableDefStatement());
		}
		//add assertion
		for(Expression f : constraints){
			String assertion = Expression.createAssertion(f.toYicesStatement());
			statements.add(assertion);
		}
		statements.add(YICES_CHECK);
		
		return statements.toArray(new String[0]);
	}
	
	
	
	private static class EventSummaryPair{
		public Event event;
		public WrappedSummary summary;
		public EventSummaryPair(Event event, WrappedSummary summary){
			this.event = event; this.summary = summary;
		}
	}
	
	private List<EventSummaryPair> possibleEventsOfUIForBackward(
			ListenableDirectedMultigraph graph,
			UIStateSymbolicPair state, Map<Expression,Expression> startingSymbolicState){
		Set<Event> edgeEvent = graph.edgesOf(state.ui);
		List<Event> loopEvent = state.ui.getIneffectiveEventList();
		edgeEvent.addAll(loopEvent);
		
		//TODO under construction
		Set<EventSummaryPair> filteredEventSet = new HashSet<EventSummaryPair>();
		Iterator<Event> iter = edgeEvent.iterator();
		while(iter.hasNext()){
			Event next = iter.next();
			if(!next.getSource().equals(state.ui)){ 
				//TODO for safety, check if the source UI is the same desired one. 
				continue;
			}else{ 
				//assumption there is only one summary possible to be satisfied
				WrappedSummary actual = null;
				WrappedSummary[] summaries = getPossibleSummary(next);
				for(WrappedSummary summary : summaries){
					if(checkSatisfaction(state.symbolic, summary.conditions)){
						actual = summary;
						break;
					}
				}
				if(actual == null) continue;
				
				//check if related to the starting one
				boolean related = false;
				for(Expression expre:actual.symbolic){
					Expression left = (Expression) expre.getChildAt(0);
//					Expression right = (Expression) expre.getChildAt(1);
					if(startingSymbolicState.containsKey(left)){
						related = true; break;
					}
				}
				if(!related){
					filteredEventSet.add(new EventSummaryPair(next, actual));
				}
			}
		}
		return null;
	}
	
	
	private WrappedSummary[] getPossibleSummary(Event event){
		//TODO
		return null;
	}
	

	private static class UIStateSymbolicPair{
		public UIState ui;
		public Map<Expression,Expression> symbolic;
		public UIStateSymbolicPair(UIState ui,Map<Expression,Expression> symbolic){
			this.ui = ui; this.symbolic = symbolic;
		}
		
		@Override
		public int hashCode(){
			return ui.index;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof UIStateSymbolicPair){
				UIStateSymbolicPair casted = (UIStateSymbolicPair)o;
				return casted.ui.equals(this.ui) && casted.symbolic.equals(this.symbolic);
			}
			return false;
		}
	}
	
	
	/**
	 * The current implementation may not choose connector correctly
	 * @param graph
	 * @param contentPath
	 * @return
	 */
	public List<Event[]> findRawEventSequence(List<NodeContent[]> contentPath){
		List<Event[]> result = new ArrayList<Event[]>();
		int index = 0;
		for(NodeContent[] contentList : contentPath){
			List<Event[]> sequence = unitProcess(contentList);
			//will not return null and empty sequence is needed
//			if(sequence == null || sequence.size() == 0) continue;
			List<Event[]> local_result = recursiveConstruct(sequence);
			result.addAll(local_result);
			indexToRawSequenceList.put(index, local_result);
			index+=1;
		}
		return result;
	}
	
	//low level permutation
	private List<Event[]> recursiveConstruct(List<Event[]> input){
		switch(input.size()){
		case 0: return new ArrayList<Event[]>();
		case 1:{ return input;}
		default:{
			Event[] currentColumn = input.get(0);
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
			List<Event> block = new ArrayList<Event>();
			List<Event> summaryEventSequence = node.summary.summaryReference.getEventSequence();
			if(summaryEventSequence != null && summaryEventSequence.size() > 0){
				//concolic generated
				Event trigger = summaryEventSequence.get(summaryEventSequence.size()-1);
				rawEventSequence.add(block.toArray(new Event[]{trigger}));
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
	
	 
	
	public DefaultMutableTreeNode findSequence(List<WrappedSummary> summarySet, WrappedSummary target){
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
//		Set<Variable> conVars = Expression.getUnqiueVarSet(content.cumulativeConstraint, variablePattern);
		
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
		public void onSequnceReady();
	}
}
