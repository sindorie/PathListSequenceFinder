package test;

import java.util.ArrayList;
import java.util.List;

import support.Utility;
import support.arithmetic.Assignment;
import support.arithmetic.ExpressionFactory;
import support.arithmetic.Formula;
import support.arithmetic.Variable;
import version2.ParsedSummary;

public class TestSummary {

	public static void main(String[] args){
		for(ParsedSummary sum : basicSummary3()){
			System.out.println(sum.toStringDetail());
		}
	}
	/**
	 * [
	 * #4 2:0:0:false, 	ef c:x>=2 y>=2
	 * #6 1:1:0:false, 	e2 c:z>=2 ; s:y+=1
	 * #6 1:1:0:false, 	e2 c:z>=2 ; s:y+=1
	 * #7 0:1:0:false,	e3 s:z+=1
	 * #7 0:1:0:false, 	e3 s:z+=1
	 * #5 2:1:0:false, 	e1 c:z>=0 z<2 ; s: x+=1
	 * #5 2:1:0:false, 	e1 c:z>=0 z<2 ; s: x+=1
	 * #8 0:3:0:true]	ec s:x=0 y=0 z=0
	 * @return
	 */
	public static List<ParsedSummary> basicSummary3(){
		//ef c:x>=2 y>=2
		//e1 c:z>=0 z<2 ; s: x+=1
		//e2 c:z>=2 ; s:y+=1
		//e3 s:z+=1
		//ec s:x=0 y=0 z=0
		
		//ef c:x>=2 y>=2
		String[] csf = {
				">= $x:int 2",
				">= $y:int 2"
		};
		ParsedSummary ef = ParsedSummary.buildSummaryFromWenhao(csf, null, null, false);
		
		//e1 c:z>=0 z<2 ; s: x+=1
		String[] cs1 = {
			">= $z:int 0",
			"< $z:int 2"
		};
		String[] ss1 ={
				"$x:int", "+ $x:int #0x01"
		};
		ParsedSummary e1 = ParsedSummary.buildSummaryFromWenhao(cs1, ss1, null, false);
		
		//e2 c:z>=2 ; s:y+=1
		String[] cs2 = {
				">= $z:int 2"
		};
		String[] ss2 ={
				"$y:int", "+ $y:int #0x01"
		};
		ParsedSummary e2 = ParsedSummary.buildSummaryFromWenhao(cs2, ss2, null, false);

		//e3 s:z+=1
		String[] ss3 ={
				"$z:int","+ $z:int #0x01"
		};
		ParsedSummary e3 = ParsedSummary.buildSummaryFromWenhao(null, ss3, null, false);
		
		//ec s:x=0 y=0 z=0
		String[] ssc ={
				"$x:int", "#0x0",
				"$y:int", "#0x0",
				"$z:int", "#0x0",
		};
		ParsedSummary ec = ParsedSummary.buildSummaryFromWenhao(null, ssc, null, true);
		
		return Utility.createList(ef,e1,e2,e3,ec);
	}
	
	public static List<ParsedSummary> basicSummary2(){
		//e1: s-> x+=1;
		//e2: s-> y+=1;
		//ec: s-> x=0; y=0;
		//ef: c-> x>2; y>2;
		
		String[] cs1 = {
				"> $x:int #0x2",
				"> $y:int #0x2"
		};
		ParsedSummary ef = ParsedSummary.buildSummaryFromWenhao(cs1, null, null, false);
		
		String[] ss1 = {
				"$x:int","(+ $x:int #0x1)"
		};
		ParsedSummary e1 = ParsedSummary.buildSummaryFromWenhao(null, ss1, null, false);
		
		String[] ss2 = {
				"$y:int","(+ $y:int #0x1)"
		};
		ParsedSummary e2 = ParsedSummary.buildSummaryFromWenhao(null, ss2, null, false);
		
		String[] ssc = {
				"$x:int","#0x0",
				"$y:int","#0x0"
		};
		ParsedSummary ec = ParsedSummary.buildSummaryFromWenhao(null, ssc, null, true);
		
		return Utility.createList(ef,e1,e2,ec);
	}
	
	public static List<ParsedSummary> basicSummary1(){
//	 	ef: c->x>2;
//		e1: s->x+=1;
//	 	e2: s->z+=1;
//	 	ec: s->x=0;
		
		Formula ef_con = new Formula( ">", new Variable("$x","int"),ExpressionFactory.buildFromWenHao("#0x2") );
		Assignment e1_sym = new Assignment(new Variable("$x","int"),ExpressionFactory.buildFromWenHao("(+ $x:int #0x1)"));
		Assignment s2_sym = new Assignment(new Variable("$z","int"),ExpressionFactory.buildFromWenHao("(+ $z:int #0x1)"));
		Assignment sc_sym = new Assignment(new Variable("$x","int"),ExpressionFactory.buildFromWenHao("#0x0"));

		//Formula, Assignment, Integer
		ParsedSummary ef = new ParsedSummary(
				Utility.createList(ef_con),
				new ArrayList<Assignment>(), 
				new ArrayList<Integer>()
				);
		
		ParsedSummary e1 = new ParsedSummary(
				new ArrayList<Formula>(),
				Utility.createList(e1_sym), 
				new ArrayList<Integer>()
				);
		ParsedSummary e2 = new ParsedSummary(
				new ArrayList<Formula>(),
				Utility.createList(s2_sym), 
				new ArrayList<Integer>()
				);
		ParsedSummary ec = new ParsedSummary(
				new ArrayList<Formula>(),
				Utility.createList(sc_sym), 
				new ArrayList<Integer>()
				);
		ec.isEntry = true;
			
		return Utility.createList(ef,e1,e2,ec);
	}
}