package support.GUI;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JCheckBox;

import support.Utility;
import version2.WrappedSummary;

import javax.swing.JButton;

public class SummaryListPanel extends JPanel {

	public JCheckBox isEntryCheckBox,isTargetCheckBox;
	public JList<WrappedSummary> summaryList;
	public JButton activiateButton;
	public JLabel informationLabel;
	public int nameWidth = 30;
	public SummaryListPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		
		JLabel summayTitle = new JLabel("Summary list");
		summaryList = new JList<WrappedSummary>();
		isEntryCheckBox = new JCheckBox("isEntry");
		isTargetCheckBox = new JCheckBox("isTarget");
		
		summaryList.setCellRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
				JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				WrappedSummary sum = (WrappedSummary)value;
				label.setText(stringFormatHelper(sum.methodSignature));
				return label; 
			}
		});
		
		activiateButton = new JButton("activiate");
		informationLabel = new JLabel("");
		
		summaryList.setAlignmentX(JList.LEFT_ALIGNMENT);
		
		summaryList.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		isEntryCheckBox.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		isTargetCheckBox.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		activiateButton.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		informationLabel.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		
		
		add(summayTitle);
		add(summaryList);
		add(isEntryCheckBox);
		add(isTargetCheckBox);
		add(activiateButton);
		add(informationLabel);
	}
	
	private String stringFormatHelper(String input){
		input = Utility.formatMethodName(input);
		if(input.length() > nameWidth){
			return input.substring(0, nameWidth-3)+"...";
		}
		return input;
	}

}
