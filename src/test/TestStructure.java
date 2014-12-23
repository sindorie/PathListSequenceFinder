package test;

import support.Utility;
import support.arithmetic.Expression;
import support.arithmetic.ExpressionFactory;
import support.arithmetic.Formula;
import support.arithmetic.Variable;

public class TestStructure {

	public static void main(String[] args){
//		generalTest();
//		copyConstructor();
//		Integer.decode(nm)
		System.out.println(Integer.decode("0xC"));
		generalTest();
	}
	
	public static void copyConstructor(){
//		Expression expre = new Variable("x","i");
//		Expression e = new Expression(expre);
	}
	
	public static void generalTest(){
		String input1 = "(+ x:i (+ y:i z:i) (+ qwe:s $sdf:vcx))";
		String input2 = "x:i";
		Expression expre1 = ExpressionFactory.buildFromWenHao(input1);
		Expression expre2 = ExpressionFactory.buildFromWenHao(input2); 
		
		Formula f = new Formula("<=",expre1, expre2);
//		Utility.showTree(f);
		
		System.out.println(f.getVarDefStatments());
		System.out.println(f.toYicesStatement());
		
		f.renameVariable("x","x1");
		System.out.println();
		
		System.out.println(f.getVarDefStatments());
		System.out.println(f.toYicesStatement());
		
		System.out.println(f.getChildCount());
		
		
		System.out.println("Testclone"); 
		Formula cloned = f.clone();
		System.out.println(cloned.toYicesStatement());
		System.out.println("rght:"+cloned.getRight());
		
		
		System.out.println("Testreplacement"); 
		Variable var = new Variable("wqe","iop");
		boolean con = cloned.replace(cloned.getLeft(), var);
		System.out.println(con+":  "+cloned.toYicesStatement());
		
		System.out.println(f.getUniqueVarSet("\\$.*"));
	}
}
