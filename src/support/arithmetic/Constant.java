package support.arithmetic;

public class Constant extends Expression{

	public Constant(String value) {
		super(value);
		this.setAllowsChildren(false);
	}
	
	@Override
	public Constant clone(){
		return new Constant(this.getContent());
	}

	@Override
	public String toYicesStatement(){
		return this.getContent();
	}
}
