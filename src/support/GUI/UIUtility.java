package support.GUI;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.lang3.StringEscapeUtils;
 

public class UIUtility {

	public static void showComponent(final JComponent component){
		showComponent("Demo",component,JFrame.EXIT_ON_CLOSE);
	}
	
	public static void showComponent(final JComponent component, int closeOperation){
		showComponent("Demo",component,closeOperation);
	}
	
	public static void showComponent(String name, final JComponent component, int closeOperation){
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame(name);
                frame.setLayout(new GridLayout(1,1));
                frame.setDefaultCloseOperation(closeOperation);

                //Add content to the window.
                frame.add(component);

                //Display the window.
                frame.pack();
                frame.setVisible(true);
            }
        });
	}
	
	@SuppressWarnings("serial")
	public static class HtmlWrappedJLabel extends JLabel{
		public HtmlWrappedJLabel(String input){
			super(input);
		}
		@Override
		public void setText(String input){
			input = StringEscapeUtils.escapeHtml4(input);
			super.setText("<html>"+input+"</html>");
		}
	}
}
