package support.GUI;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;

import concolic.Expression;
import concolic.Variable;

import javax.swing.BoxLayout;

public class ExpressionsDetailPanel extends JPanel {

	public JLabel yiceStatementLabel;
	public JList<String> varDefList;
	public JTree expressionTree;
	
	
	public ExpressionsDetailPanel(Expression express) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JLabel yicesTitle = new JLabel("Yice Statement:");
		yiceStatementLabel = new JLabel(express.toYicesStatement());
		yiceStatementLabel.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		
		
		JLabel varTitle = new JLabel("Variable definitions:");
		
		varDefList = new JList<String>();
		DefaultListModel<String> varModel = new DefaultListModel<String>();
		for(Variable var : express.getUniqueVarSet()){
			varModel.addElement(var.toYicesStatement());
		}
		varDefList.setModel(varModel);
		varDefList.setAlignmentX(JList.LEFT_ALIGNMENT);
		varDefList.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		
		JLabel expressTreeTitle = new JLabel("Tree Structure");
		expressionTree = new JTree(express);
		expressionTree.setBorder(BorderFactory.createEmptyBorder(10,10,20,10));
		
		add(yicesTitle);
		add(yiceStatementLabel);
		add(varTitle);
		add(varDefList);
		add(expressTreeTitle);
		add(expressionTree);
	}

}
