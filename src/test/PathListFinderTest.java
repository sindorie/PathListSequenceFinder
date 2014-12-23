package test;

import java.util.ArrayList;

import concolic.Condition;
import concolic.Operation;
import concolic.PathSummary;

public class PathListFinderTest {

	public static void main(String[] args) {
		//not used
		
		
	}
//	static void test1(){
//		/*
//		 * ef: if(x>2) { target(); }
//		 * 	Conditions: 
//		 * 		"($staticF>>Lthe/app/Fields;->field1:I )", ">", "(#0x2 )"
//		 * 	Symbolic States:
//		 * 		NONE
//		 * 	
//		 * e1: x+=1;
//		 * 	Conditions:
//		 * 		NONE
//		 * 	Symbolic States:
//		 * 		"(= $staticF>>Lthe/app/Fields;->field1:I (+ $staticF>>Lthe/app/Fields;->field1:I #0x1) )"
//		 * 
//		 * e2: z+=1;
//		 * 	Conditions:
//		 * 		NONE
//		 * 	Symbolic States:
//		 * 		"(= $staticF>>Lthe/app/Fields;->field2:I (+ $staticF>>Lthe/app/Fields;->field2:I #0x1) )"
//		 * 
//		 * ec: x=0; //oncreate
//		 * 	Conditions:
//		 * 		NONE
//		 * 	Symbolic States:
//		 * 		(= $staticF>>Lthe/app/Fields;->field1:I 0x0)
//		 */
//		
//		//ef: if(x>2) { target(); } where x is $staticF>>Lthe/app/Fields;->field1:I
//		String[] sum1_conditions = { //left, op, right
//				"($staticF>>Lthe/app/Fields;->field1:I )", ">", "(#0x2 )",
//		};
//		String[] sum1_oprations = { //left, rightA, op, rightB
//
//		};
//		Integer[] executionLog1 = { };
//		PathSummary summary1 = constructPathSummary(sum1_conditions, sum1_oprations, executionLog1);
//		
//		
//		//e1: x+=1;
//		String[] sum2_conditions = { //left, op, right
//				//NONE
//		};
//		String[] sum2_oprations = { //left, rightA, op, rightB
//				"$staticF>>Lthe/app/Fields;->field1:I",
//				"=",
//				"(+ $staticF>>Lthe/app/Fields;->field1:I #0x1)"
//		};
//		Integer[] executionLog2 = { };
//		PathSummary summary2 = constructPathSummary(sum2_conditions, sum2_oprations, executionLog2);
//
//		
//		//e2: z+=1; where z is $staticF>>Lthe/app/Fields;->field2;
//		String[] sum3_conditions = { //left, op, right
//				//NONE
//		};
//		String[] sum3_oprations = { //left, rightA, op, rightB
//				"$staticF>>Lthe/app/Fields;->field2:I",
//				"=",
//				"(+ $staticF>>Lthe/app/Fields;->field2:I #0x1)"
//		};
//		Integer[] executionLog3 = { };
//		PathSummary summary3 = constructPathSummary(sum3_conditions, sum3_oprations, executionLog3);
//
//		//ec: x=0; which is an entry point
//		String[] sum4_conditions = { //left, op, right
//				//NONE
//		};
//		String[] sum4_oprations = { //left, rightA, op, rightB
//				"$staticF>>Lthe/app/Fields;->field1:I",
//				"=",
//				"(0x0)"
//		};
//		Integer[] executionLog4 = { };
//		PathSummary summary4 = constructPathSummary(sum4_conditions, sum4_oprations, executionLog4);
//		
//		
//		
//	}
//	
//	
//	static PathSummary constructPathSummary(String[] conditions, String[] operations, Integer... exuectionLog){
//		int con_count = conditions.length/3; //left, op, right
//		int ope_count = operations.length/4; //left, rightA, op, rightB
//		
//		PathSummary result = new PathSummary();
//		for(int i=0;i<con_count;i+=3){
//			Condition con = new Condition();
//			con.setLeft(conditions[i]);
//			con.setOp((conditions[i+1]));
//			con.setRight((conditions[i+2]));
//			result.addPathCondition(con);
//		}
//		
//		ArrayList<Operation> oplist = new ArrayList<Operation>();
//		for(int i=0;i<ope_count;i+=4){
//			Operation ope = new Operation();
//			ope.setLeft(conditions[i]);
//			ope.setRightA(conditions[i+1]);
//			ope.setNoOp(conditions[i+2] == null);
//			ope.setOp(conditions[i+2]);
//			ope.setRightB((conditions[i+3]));
//			oplist.add(ope);
//		}
//		result.setSymbolicStates(oplist);
//		
//		if(exuectionLog !=null && exuectionLog.length > 0){
//			for(Integer line : exuectionLog){
//				result.addExecutionLog(line);
//			}
//		}
//		return result;
//	}

}
