package support;

import java.awt.Component;
import java.awt.Dimension; 
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener; 

import org.apache.commons.lang3.StringEscapeUtils;

import analysis.Expression;
import analysis.Variable;
import Component.WrappedSummary; 
import concolic.PathSummary; 

public class PathSummaryUIFactory {
//	private final static JLabel EMPTYLABEL = new JLabel("Empty");
//	static{
//		EMPTYLABEL.setMaximumSize(new Dimension(50,50));
//	}
	private PathSummaryUIFactory(){}
	
	public static JComponent buildSummaryUIComponent(PathSummary summary, int index){
		PathSummaryUIBuilder worker = new PathSummaryUIBuilder(summary, index);
		return worker.build();
	}
	public static JComponent buildSummaryUIComponent(PathSummary summary){
		PathSummaryUIBuilder worker = new PathSummaryUIBuilder(summary, -1);
		return worker.build();
	}
	
	public static JComponent buildSummaryListComponent(List<PathSummary> list){
		JTabbedPane tabbedPane = new JTabbedPane();
		int index = 0;
		for(PathSummary summary : list){
			JComponent summaryComponent = buildSummaryUIComponent(summary, index);
			tabbedPane.add(summaryComponent.getName(), summaryComponent);
			index += 1;
		}
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		return tabbedPane;
	}
	
	private static class PathSummaryUIBuilder{
		final static boolean ISCONSTRAINT = true;  
		
		int index; PathSummary summary;
		PathSummaryUIBuilder(PathSummary summary, int index){
			this.summary = summary; this.index = index;
		}
		PathSummaryUIBuilder(PathSummary summary){ this(summary, -1); }
		
		JSplitPane parent;
		JComponent left; 
		
		JComponent build(){
			parent = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			 
			addLeftComponent();
			updateRightPanel(-1,false);
			
			parent.setLeftComponent(left);
			
			parent.setDividerSize(3);  
			parent.setResizeWeight(0); 
			parent.setPreferredSize(new Dimension(600, 400)); 
			
			return parent;
		}

		/**
		 * Create the left panel based on the given pathSummary
		 * UI format:
		 * 	Method signature
		 * 	Log record
		 * 	Condition statements list
		 * 	Symbolic statements list
		 * @return
		 */
		private void addLeftComponent(){
			left = new JPanel();
			left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
			left.setMaximumSize(new Dimension(400,-1));
			
			addSignatureComponent();	 
			addLogComponent();  		 
			addExpressionListComponent(ISCONSTRAINT);
			addExpressionListComponent(!ISCONSTRAINT);  
		}
		
		/**
		 * Create and add the method signature component to the container. 
		 * check if the signature is nonempty. 
		 */
		private void addSignatureComponent(){
			String signature = summary.getMethodSignature();
			if(signature==null || signature.trim().equals("")){
				if(index >=0 ){ signature = "PathSummary #"+index;
				}else{ signature = "Empty Method Signature"; }
			}
			String raw = "Signature:<br>  "+StringEscapeUtils.escapeHtml4(signature.replace("->", " ->"));
 
			JLabel signatureLabel = new HtmlWrappedJLabel(raw);
//			String name = signature.f

			this.parent.setName(getProperName(signature));
//			signatureLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			left.add(signatureLabel);
		}
		
		/**
		 * create and add the log record label to the container
		 */
		private void addLogComponent(){
			String logString = summary.getExecutionLog().toString().replaceAll("[\\]\\[]", "");//remove '[' and ']' 
			JLabel logLabel = new HtmlWrappedJLabel(logString);
//			logLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			left.add(logLabel);
		}
		
		private static String getPrefix(boolean isConstraint){
			if(isConstraint){ return "Constraints"; 
			}else{ return "Symbolics"; }
		}
		
		private void addExpressionListComponent(final boolean isConstraint){
			//retrieve and set needed data reference
			List<Expression> expressions = isConstraint? summary.getPathCondition() : summary.getSymbolicStates();
			final String prefix = getPrefix(isConstraint);
			//add title 
			JLabel title = new HtmlWrappedJLabel(prefix+" ("+expressions.size()+")"); 
			left.add(title);
			//no need to preceed if list is empty
			if(expressions.size() == 0) return;
			
			DefaultListModel<Expression> listModel = new DefaultListModel<Expression>(); 
			for(Expression expr : expressions){ listModel.addElement(expr); }
			JList<Expression> list = new JList<Expression>(listModel);
			
			//set the attrbutes 
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.setLayoutOrientation(JList.VERTICAL);
			list.setAlignmentX(JList.LEFT_ALIGNMENT);
			//override default text label which is the result of toSting() from the element originally
			list.setCellRenderer(new DefaultListCellRenderer(){
				@Override
				public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
					// I know DefaultListCellRenderer always returns a JLabel
					// super setups up all the defaults
					JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					// "value" is whatever object you put into the list, you can use it however you want here
					label.setText("    #" + index+" "+prefix);
					return label; 
				}
			});
			
			list.addFocusListener(new FocusListener(){
				@Override
				public void focusGained(FocusEvent e) {
					int expressionIndex = list.getSelectedIndex();
					if(index >= 0){
						updateRightPanel(expressionIndex,isConstraint);
					}
				}
				@Override
				public void focusLost(FocusEvent e) { }
			});
			
			list.addListSelectionListener(new ListSelectionListener(){
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if(e.getValueIsAdjusting() == false){
						int expressionIndex = list.getSelectedIndex();
						if(expressionIndex >= 0){
							updateRightPanel(expressionIndex,isConstraint);
						}
					}
				}
			}); 
			left.add(list);
		}
		
		
		private void updateRightPanel(int expressionIndex, boolean isConstraint){
			Component toRemove = this.parent.getRightComponent();
			if(toRemove!= null){ this.parent.remove(toRemove);}
		
			if(expressionIndex < 0){
				this.parent.setRightComponent(new JLabel("Empty"));
				return;
			}
			
			final Expression express = isConstraint?
					summary.getPathCondition().get(expressionIndex) : 
					summary.getSymbolicStates().get(expressionIndex);
					
			JPanel right = new JPanel();
			right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
			final String prefix = getPrefix(isConstraint);
			
			JLabel title = new HtmlWrappedJLabel(prefix+" #"+expressionIndex);
			right.add(title);
			
			String yicesContent = express.toYicesStatement(); 
			JLabel yicesStatement = new HtmlWrappedJLabel("Yices:<br>"+StringEscapeUtils.escapeHtml4(yicesContent));
			right.add(yicesStatement);
			
			
			StringBuilder sb = new StringBuilder();
			sb.append("Assertions: ");
			for(Variable var: express.getUniqueVarSet()){
				sb.append(StringEscapeUtils.escapeHtml4(var.toVariableDefStatement())+"<br>");
			}
			JLabel assertionStatement = new HtmlWrappedJLabel(sb.toString());
			right.add(assertionStatement);
			
			JTree tree =new JTree(express);
			JScrollPane scroll = new JScrollPane(tree); 
			right.add(scroll);
//			if(parent.getWidth() != 0){
//				int targetWidth = (int) (parent.getWidth()* 0.3);
//				int width = targetWidth > 50 ? targetWidth : 50;
//				left.setPreferredSize(new Dimension(width, parent.getHeight()));
//				right.setPreferredSize(new Dimension(parent.getWidth()-width, parent.getHeight()));
//			}
			
			
			this.parent.setRightComponent(right);	
		}
		
		private static String getProperName(String input){
			//asd(qwe/qwe/qwe)
			String parts1[] = input.split(";->");
			if(parts1.length <= 1) return input;
			
			//function name and parameter
			String para = "";
			String functionName = parts1[1].split("\\(")[0];
			Pattern paramter = Pattern.compile("\\(([^\\)]*)\\)");//find (asd/asd/asd)
			Matcher matcher = paramter.matcher(parts1[1]);
			boolean found = matcher.find();
			if(found){
				String raw = matcher.group(1).replaceAll("\\(|\\)|;", "");
				String parParts[] = raw.split("\\/");
				para = parParts[parParts.length-1];
			}else{
				
			}
			
			String parts2[] = parts1[0].split("\\/");
			if(parts2.length <=1) return parts2[0] +" "+ functionName+"("+para+")";
			else return parts2[parts2.length-1] +" "+ functionName+"("+para+")";
		}
		
	} 

	@SuppressWarnings("serial")
	private static class HtmlWrappedJLabel extends JLabel{
		public HtmlWrappedJLabel(String input){
			super(input);
		}
		@Override
		public void setText(String input){
			input.replaceAll("\\s", "&nbsp;");
			super.setText("<html>"+input+"</html>");
		}
	}
	
	
	private static String stringFormatHelper(String input){
		if(input.length() > 30){
			return input.substring(0, 27)+"...";
		}
		return input;
	}
	
//	private static class TextNote extends JTextArea {
//	    public TextNote(String text) {
//	        super(text);
//	        setBackground(null);
//	        setEditable(false);
//	        setBorder(null);
//	        setLineWrap(true); 
//	        setFocusable(false);
//	    }
//	}
	
//	private static String createHTMLWhiteSpace(int amount){
//		String result ="";
//		for(int i=0;i<amount;i++){
//			result += "&nbsp;";
//		}
//		return result;
//	}

}
