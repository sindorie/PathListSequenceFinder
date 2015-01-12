package support.GUI;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import version2.WrappedSummary;
import concolic.Expression;

public class SummaryDetailPanel extends JPanel {

	public JLabel signatureLabel;
	public JList<String> exectionLogList;
	public JList<Expression> constraintList, symbolicList;
	public JLabel extraInfoLabel;
	public int state = -1;
	public SummaryDetailPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JLabel signatureTitle = new JLabel("Signature:");
		signatureLabel = new UIUtility.HtmlWrappedJLabel("");
		JLabel executionTitle = new JLabel("Execution Logs:");
		exectionLogList = new JList<String>();
		JLabel constraintTitle = new JLabel("Constraints:");
		constraintList = new JList<Expression>();
		JLabel symbolicTitle = new JLabel("Symbolics:");
		symbolicList = new JList<Expression>();
		JLabel extraInfoTitle = new JLabel("Extra:");
		extraInfoLabel = new JLabel();
		
		exectionLogList.setAlignmentX(JList.LEFT_ALIGNMENT);
		symbolicList.setAlignmentX(JList.LEFT_ALIGNMENT);
		
		signatureLabel.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		exectionLogList.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		constraintList.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		symbolicList.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		extraInfoLabel.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		
		
		add(signatureTitle);
		add(signatureLabel);
		add(executionTitle);
		add(exectionLogList);
		add(constraintTitle);
		add(constraintList);
		add(symbolicTitle);
		add(symbolicList);
		add(extraInfoTitle);
		add(extraInfoLabel);
	}
	
	public SummaryDetailPanel(WrappedSummary summary){
		this();
		this.setWrappedSummary(summary);
	}
	
	
	public void setWrappedSummary(WrappedSummary summary){
		if(summary == null) return;
		this.signatureLabel.setText(summary.methodSignature);
		DefaultListModel<String> logModel = new DefaultListModel<String>();
		for(String log : summary.executionLog){
			logModel.addElement(log);
		}
		this.exectionLogList.setModel(logModel);

		DefaultListModel<Expression> constraintModel = new DefaultListModel<Expression>();
		for(Expression constraint : summary.conditions){
			constraintModel.addElement(constraint);
		}
		this.constraintList.setModel(constraintModel);
		
		DefaultListModel<Expression> symbolicModel = new DefaultListModel<Expression>();
		for(Expression symbolic : summary.symbolic){
			constraintModel.addElement(symbolic);
		}
		this.symbolicList.setModel(symbolicModel);
		
		this.extraInfoLabel.setText("#"+summary.index+" summary");
	}
}
