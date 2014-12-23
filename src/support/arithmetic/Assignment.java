package support.arithmetic;


public class Assignment extends Formula{ 
	private static final long serialVersionUID = 1L;

	public Assignment(Variable var, Expression expre) {
		super("=",var,expre);
	}
	
	@Override
	public Variable getLeft(){
		return (Variable)super.getLeft();
	}
}
