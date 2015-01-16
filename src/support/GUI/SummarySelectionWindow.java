package support.GUI;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener; 

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JLabel; 
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import analysis.Expression;
import Component.WrappedSummary;

public class SummarySelectionWindow {

	public JFrame frame;
	private boolean activited = false;
	public SummaryListPanel selectionPanel; // the panel on the left which does not change
	private final static int LEFT = 1, MIDDLE =2, RIGHT = 3;
	private Action action;
	public WrappedSummary target; 
	private JSplitPane layer1, layer2;
	
	public SummarySelectionWindow() { 
		initialize();
	}

	private JLabel middleEmpty = new JLabel("Empty");
	private JLabel rightEmpty = new JLabel("Empty");
	
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 600, 450);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		layer1 = new JSplitPane();
		layer2 = new JSplitPane();
		layer1.setRightComponent(layer2);
		layer1.setResizeWeight(0);
		layer2.setResizeWeight(0);
		layer1.setDividerLocation(200);
		layer1.setDividerSize(3);
		layer2.setDividerSize(3);
		frame.getContentPane().add(layer1,BorderLayout.CENTER);
		selectionPanel = new SummaryListPanel();
		addToSplitPanelHelper(LEFT,selectionPanel);
		addToSplitPanelHelper(MIDDLE,middleEmpty);
		addToSplitPanelHelper(RIGHT,rightEmpty);
		selectionPanel.summaryList.addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting() == false){
					WrappedSummary summary = selectionPanel.summaryList.getSelectedValue();
					if(summary != null){
						addToSplitPanelHelper(RIGHT,rightEmpty);
						SummaryDetailPanel toDisplay = new SummaryDetailPanel(summary);
						setupMiddlePanelListener(toDisplay);
						addToSplitPanelHelper(MIDDLE,toDisplay);
						selectionPanel.isEntryCheckBox.setSelected(summary.isEntry);
						selectionPanel.isTargetCheckBox.setSelected(summary == target);
					}
				}
			}
		});
		
		selectionPanel.isTargetCheckBox.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(activited) return;
				if(selectionPanel.isTargetCheckBox.isSelected()){
					SummarySelectionWindow.this.target = selectionPanel.summaryList.getSelectedValue();
				}else if(target == selectionPanel.summaryList.getSelectedValue()){
					target = null;
				}
			}
		});
		
		selectionPanel.isEntryCheckBox.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(activited) return;
				WrappedSummary summary = selectionPanel.summaryList.getSelectedValue();
				if(summary != null){
					summary.isEntry = selectionPanel.isEntryCheckBox.isSelected();
				}
			}
		});
		
		selectionPanel.activiateButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
//				if(activited) return;
//				if(target == null){
//					selectionPanel.informationLabel.setText("Empty Target");
//				}
//				
//				boolean foundEntry = false;
//				
//				ListModel<WrappedSummary> listmodel = selectionPanel.summaryList.getModel();
//				for(int i = 0; i<selectionPanel.summaryList.getModel().getSize();i++){
//					WrappedSummary child = listmodel.getElementAt(i);
//					if(child.isEntry){
//						foundEntry = true;
//						break;
//					}
//				}
//						
//				if(foundEntry == false){
//					selectionPanel.informationLabel.setText("Empty entry");
//				}
				
				activate();
//				activited = true;
			}
		});
	}
	
	private void activate(){
		if(this.activited){
			selectionPanel.informationLabel.setText("already activited");
			return;
		}
		
		if(action != null){
			activited = action.activate(this);
			if(activited){
				selectionPanel.informationLabel.setText("activated");
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			}
		}else{
			selectionPanel.informationLabel.setText("No operation specified");
		}
		
//		Thread find = new Thread(new Runnable(){
//			@Override
//			public void run() {
//				selectionPanel.informationLabel.setText("activated");
//				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//				YicesProcessInterface solver = null;
//				try {
//					solver = new YicesProcessInterface(Paths.yicesBinLocation);
//				} catch (IOException e) {
//					selectionPanel.informationLabel.setText("Yices process starting error");
//					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//					return;
//				}
//				PathListSequenceFinder sequenceFinder = new PathListSequenceFinder(solver);
//				DefaultListModel<WrappedSummary> reference = (DefaultListModel<WrappedSummary>) selectionPanel.summaryList.getModel();
//				ArrayList<WrappedSummary> list = new ArrayList<WrappedSummary>();
//				for(int i =0 ;i < reference.getSize();i++){
//					list.add(reference.getElementAt(i));
//				}
//				DefaultMutableTreeNode tree = sequenceFinder.findSummaryPath(list, target);
//				UIUtility.showComponent("result", new JTree(tree), JFrame.EXIT_ON_CLOSE);
//				
//				DefaultMutableTreeNode node = tree.getFirstLeaf();
//				List<NodeContent[]> contentPath = new ArrayList<NodeContent[]>(); 
//				while(node != null){
//					NodeContent content = (NodeContent)node.getUserObject();
//					if(content.summary.isEntry){
//						NodeContent[] path = (NodeContent[])node.getUserObjectPath();
//						contentPath.add(path);
//					}
//				}
//			}
//			
//		});
//		
//		find.start();
	}
	
	private void setupMiddlePanelListener(SummaryDetailPanel toDisplay){
		toDisplay.constraintList.addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting() == false){
					Expression express = toDisplay.constraintList.getSelectedValue();
					if(express != null){
						ExpressionsDetailPanel expressDetail = new ExpressionsDetailPanel(express);
//						right.setViewportView(expressDetail);
						addToSplitPanelHelper(RIGHT,expressDetail);
						toDisplay.state = 0;
					}
				}
			}
		});
		
		toDisplay.symbolicList.addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting() == false){
					Expression express = toDisplay.symbolicList.getSelectedValue();
					if(express != null){
						ExpressionsDetailPanel expressDetail = new ExpressionsDetailPanel(express);
//						right.setViewportView(expressDetail);
						addToSplitPanelHelper(RIGHT,expressDetail);
						toDisplay.state = 1;
					}
				}
			}
		});
		toDisplay.constraintList.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				if(toDisplay.state != 0){
					Expression express = toDisplay.constraintList.getSelectedValue();
					if(express != null){
//						right.setViewportView(new ExpressionsDetailPanel(express));
						addToSplitPanelHelper(RIGHT,new ExpressionsDetailPanel(express));
					}
				}
			}
			@Override
			public void focusLost(FocusEvent e) {}
		});
		toDisplay.symbolicList.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
				if(toDisplay.state != 1){
					Expression express = toDisplay.symbolicList.getSelectedValue();
					if(express != null){
//						right.setViewportView(new ExpressionsDetailPanel(express));
						addToSplitPanelHelper(RIGHT,new ExpressionsDetailPanel(express));
					}
				}
			}
			@Override
			public void focusLost(FocusEvent e) {}
		});
		
	}
	
	public void setAction(Action action){
		this.action = action;
	}
	
	public void setListModel(DefaultListModel<WrappedSummary> model){
		this.selectionPanel.summaryList.setModel(model);
	}

	public void show(){
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	
//	private static void addToContainerHelper(Container con, JComponent component){
//		con.removeAll();
//		con.add(component);
//	}
	private void addToSplitPanelHelper(int position, JComponent component){
		switch(position){
		case LEFT:{
			layer1.setLeftComponent(component);
		}break;
		case MIDDLE:{
			layer2.setLeftComponent(component);
		}break;
		case RIGHT:{
			layer2.setRightComponent(component);
		}break;
		}
	}
	
	public static interface Action{
		public boolean activate(final SummarySelectionWindow window);
	}
	
	/**
	 * Launch the application.
	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					SummarySelectionWindow window = new SummarySelectionWindow();
//					window.frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}
}
