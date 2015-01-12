package support.GUI;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.BoxLayout;
import javax.swing.JLabel; 
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import main.Paths;
import concolic.Expression;
import support.solver.YicesProcessInterface;
import version2.NodeContent;
import version2.PathListSequenceFinder;
import version2.WrappedSummary;

public class SummarySelectionWindow {

	private JFrame frame;
	private boolean activited = false;
	private SummaryListPanel selectionPanel; // the panel on the left which does not change
	private final static int LEFT = 1, MIDDLE =2, RIGHT = 3;
//	private Container left, middle, right; 
	WrappedSummary target; 
	JSplitPane layer1, layer2;
	
	public SummarySelectionWindow() { 
		initialize();
	}

	private JLabel middleEmpty = new JLabel("Empty");
	private JLabel rightEmpty = new JLabel("Empty");
	
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 600, 450);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));

		
		layer1 = new JSplitPane();
		layer2 = new JSplitPane();
		layer1.setRightComponent(layer2);
		
//		left = new Container();
//		middle = new Container();
//		right = new Container();
		
//		layer1.setLeftComponent(left);
//		layer2.setLeftComponent(middle);
//		layer2.setRightComponent(right);
		
		layer1.setResizeWeight(0);
		layer2.setResizeWeight(0);
		layer1.setDividerLocation(200);
		layer1.setDividerSize(3);
		layer2.setDividerSize(3);
		
		frame.getContentPane().add(layer1,BorderLayout.CENTER);
		
//		Dimension min = new Dimension(50,50);
//		left.setMinimumSize(min);
//		middle.setMinimumSize(min);
//		right.setMinimumSize(min);
		
//		left.setMaximumSize(new Dimension(200,5000));
//		middle.setMaximumSize(new Dimension(500,5000));
		
		selectionPanel = new SummaryListPanel();
		
//		left.add(selctionPanel);
//		middle.add(middleEmpty);
//		right.add(rightEmpty);
		
		addToSplitPanelHelper(LEFT,selectionPanel);
		addToSplitPanelHelper(MIDDLE,middleEmpty);
		addToSplitPanelHelper(RIGHT,rightEmpty);
		
//		frame.getContentPane().add(left,BorderLayout.LINE_START);
//		frame.getContentPane().add(middle,BorderLayout.CENTER);
//		frame.getContentPane().add(right,BorderLayout.LINE_END);
		
		selectionPanel.summaryList.addListSelectionListener(new ListSelectionListener(){
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if(e.getValueIsAdjusting() == false){
					WrappedSummary summary = selectionPanel.summaryList.getSelectedValue();
					if(summary != null){
						addToSplitPanelHelper(RIGHT,rightEmpty);
						
						SummaryDetailPanel toDisplay = new SummaryDetailPanel(summary);
						setupMiddlePanelListener(toDisplay);
//						middle.setViewportView(toDisplay);
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
				if(activited) return;
				
				//check target is selected
				if(target == null){
					selectionPanel.informationLabel.setText("Empty Target");
				}
				
				boolean foundEntry = false;
				
				ListModel<WrappedSummary> listmodel = selectionPanel.summaryList.getModel();
				for(int i = 0; i<selectionPanel.summaryList.getModel().getSize();i++){
					WrappedSummary child = listmodel.getElementAt(i);
					if(child.isEntry){
						foundEntry = true;
						break;
					}
				}
						
				if(foundEntry == false){
					selectionPanel.informationLabel.setText("Empty entry");
				}
				
				activate();
				activited = true;
			}
		});
	}
	
	private void activate(){
		if(this.activited){
			selectionPanel.informationLabel.setText("already activited");
			return;
		}
		
		Thread find = new Thread(new Runnable(){
			@Override
			public void run() {
				selectionPanel.informationLabel.setText("activated");
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				YicesProcessInterface solver = null;
				try {
					solver = new YicesProcessInterface(Paths.yicesBinLocation);
				} catch (IOException e) {
					selectionPanel.informationLabel.setText("Yices process starting error");
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					return;
				}
				PathListSequenceFinder sequenceFinder = new PathListSequenceFinder(solver);
				DefaultListModel<WrappedSummary> reference = (DefaultListModel<WrappedSummary>) selectionPanel.summaryList.getModel();
				ArrayList<WrappedSummary> list = new ArrayList<WrappedSummary>();
				for(int i =0 ;i < reference.getSize();i++){
					list.add(reference.getElementAt(i));
				}
				DefaultMutableTreeNode tree = sequenceFinder.findSummaryPath(list, target);
				UIUtility.showComponent("result", new JTree(tree), JFrame.EXIT_ON_CLOSE);
				
				DefaultMutableTreeNode node = tree.getFirstLeaf();
				List<NodeContent[]> contentPath = new ArrayList<NodeContent[]>(); 
				while(node != null){
					NodeContent content = (NodeContent)node.getUserObject();
					if(content.summary.isEntry){
						NodeContent[] path = (NodeContent[])node.getUserObjectPath();
						contentPath.add(path);
					}
				}
			}
			
		});
		
		find.start();
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
	
	public static interface OperationInterface{
		
		public void activate();
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
