package za.ac.sun.cs.green.service.z3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Level;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.BitVecSort;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.FuncInterp;
import com.microsoft.z3.FuncInterp.Entry;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import com.microsoft.z3.enumerations.Z3_sort_kind;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.Alias;
import za.ac.sun.cs.green.expr.ArrayVariable;
import za.ac.sun.cs.green.expr.BitVectorConstant;
import za.ac.sun.cs.green.expr.Constant;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.IntConstant;
import za.ac.sun.cs.green.expr.IntVariable;
import za.ac.sun.cs.green.expr.Operation;
import za.ac.sun.cs.green.expr.RealConstant;
import za.ac.sun.cs.green.expr.RealVariable;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.expr.Visitor;
import za.ac.sun.cs.green.expr.VisitorException;
import za.ac.sun.cs.green.service.ModelService;

public class ModelZ3JavaService extends ModelService {
	
	Context ctx;
	Solver Z3solver;
	
	public ModelZ3JavaService(Green solver, Properties properties) {
		super(solver);
		HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
		try{
			ctx = new Context(cfg);		 
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("## Error Z3: Exception caught in Z3 JNI: \n" + e);
	    }
	}

	private String processFuncInterp(FuncInterp funcInterp){
		String ret="";
		Expr el = funcInterp.getElse();
		if(el.isNumeral()){
//			int elval = Integer.parseInt(el.toString());
			ret += el.toString();
			//Add el to the returned string
		} else {
			log.log(Level.WARNING, "Error unsupported type for variable " + el);
			return null;
		}
		
		for(Entry e: funcInterp.getEntries()){
			if(e.getNumArgs() > 1){
				System.out.println("ModelZ3JavaService Error: entry has more than 1 argument, should be a single index");
				return null;
			}
			Expr[] args = e.getArgs();
			Expr arg = args[0];
			int index;
			if(arg.isNumeral()){
				index = Integer.parseInt(arg.toString());
			} else {
				log.log(Level.WARNING, "Error unsupported type for variable " + arg);
				return null;
			}
			Expr argVal = e.getValue();
			if(argVal.isNumeral()){
//				val[index] = Integer.parseInt(argVal.toString());
				//add the new pair to the returned string
				ret+="|"+index+","+argVal.toString();
			}else{
				log.log(Level.WARNING, "Error unsupported type for variable " + argVal);
				return null;
			}
		}
		return ret;
	}
	
	@Override
	protected Map<String, Object> model(Instance instance) {		
		HashMap<String,Object> results = new HashMap<String, Object>();
		// translate instance to Z3 
		//Z3JavaTranslator translator = new Z3JavaTranslator(ctx);
		Explorer ex = new Explorer(ctx);
		BoolExpr expr = ex.explore(instance.getExpression());
		// model should now be in ctx
		try {
			Z3solver = ctx.mkSolver();
			Z3solver.add(expr);
		} catch (Z3Exception e1) {
			log.log(Level.WARNING, "Error in Z3"+e1.getMessage());
		}
		//solve 		
		try { // Real Stuff is still untested
			if (Status.SATISFIABLE == Z3solver.check()) {
				//getVariableMap is not working probably, some variable is missing.
				Map<Variable, Expr> variableMap = ex.getVariableMap();
				Model model = Z3solver.getModel();

/*				
				System.out.println(model.toString());
				//Instead of creating the mapping by ourself, we do it by using Z3 natively.
				//We start by getting FuncDecls, and check for arity. if arity is not zero, we get declaration of it.
				//The var name will be in FuncDecls, and the values are in FuncDeclsImpl, we need to get 

				
				FuncDecl[] funcDecls = model.getFuncDecls();
				FuncDecl[] constDecls = model.getConstDecls();
				for(FuncDecl funcDecl:funcDecls){
					System.out.println("funcDecls " + funcDecl.toString());
					if(funcDecl.getArity() != 0){
						//We have a non-constant funcDecl;
						String arrayName = funcDecl.getName().toString();
						FuncInterp funcInterp = model.getFuncInterp(funcDecl);
						String val = processFuncInterp(funcInterp);
						results.put(arrayName, val);
					}
					else{//If constant array
						System.out.println("Arity Zero: " + funcDecl.toString());					
					}
				}
				
				for(FuncDecl constDecl:constDecls){
					System.out.println("constDecls " + constDecl.toString());
					System.out.println("Arity is " + constDecl.getArity());
					String arrayName = constDecl.getName().toString();
					Expr constExpr = model.getConstInterp(constDecl);
					System.out.println("constantExpr: " + constExpr.toString());
					
					if(constDecl.getArity() == 0){
						//We have a non-constant funcDecl;
						String arrayName = constDecl.getName().toString();
						FuncInterp funcInterp = model.getFuncInterp(constDecl);
						String val = processFuncInterp(funcInterp);
						results.put(arrayName, val);
					}
					else{//If constant array
						System.out.println("Arity non-zero: " + constDecl.toString());					
					}
				}
				*/

				for(Map.Entry<Variable,Expr> entry : variableMap.entrySet()) {
					Variable greenVar = entry.getKey();
					System.out.println("Getting Variable: " + greenVar.getName() + " from Z3solver");
					Expr z3Var = entry.getValue();
					ArrayList<Expr> z3Val = new ArrayList<Expr>();
					if(greenVar instanceof ArrayVariable){
						ArrayVariable aVar = (ArrayVariable) greenVar;
						int size = aVar.getIndexSize().getValue();
						z3Val.ensureCapacity(size);
						for(int i = 0; i < size; i++){
							Expr readByte = ctx.mkSelect((ArrayExpr)z3Var, ctx.mkBV(String.valueOf(i),32));						
							z3Val.add(i,model.evaluate(readByte, false));							
						}
					}
					else{
						z3Val.add(model.evaluate(z3Var, false));						
					}
					int[] val;
					val = new int[z3Val.size()];
//					Gladtbx: Need to parse the returned result z3Val.
//					Need to construct a read expr to read the greenVar byte by byte
//					So hopefully we can get the returned type as int.
//					log.log(Level.WARNING,z3Val.getASTKind().toString());
					for(int i = 0; i < z3Val.size(); i++){
						if(z3Val.get(i).isNumeral()){
							val[i] = Integer.parseInt(z3Val.get(i).toString());
						} else {
							log.log(Level.WARNING, "Error unsupported type for variable " + z3Val);
							return null;
						}
					}
					/*
					if (z3Val.isIntNum()) {
						val = Integer.parseInt(z3Val.toString());
					} else if (z3Val.isRatNum()) {
						val = Double.parseDouble(z3Val.toString());
					}
					*/
					results.put(greenVar.getName(), val);
					String logMessage = "" + greenVar + " has value " + val.toString();
					log.log(Level.INFO,logMessage);
				}
			} else {
				log.log(Level.WARNING,"constraint has no model, it is infeasible");
				return results;
			}
		} catch (Z3Exception e) {
			log.log(Level.WARNING, "Error in Z3 "+e.getMessage());
		}
		return results;
	}
	
	private class Explorer extends Visitor{

		private Context ctx;
		private Stack<Expr> stack;
		private Map<Variable, Expr> vm;

		public Explorer(Context ctx){
			this.ctx = ctx; 
			this.stack = new Stack<Expr>();
			this.vm = new HashMap<Variable, Expr>();
		}

		public Map<Variable, Expr> getVariableMap() {
			return vm;
		}

		public BoolExpr explore(Expression expression) {
			try {
				expression.accept(this);
			} catch (VisitorException e) {
				e.printStackTrace();
			}
			return (BoolExpr) stack.pop();
		}

		@Override
		public void postVisit(Variable variable) {
			Expr v = vm.get(variable);
			if(v == null){
				try{
					if(variable instanceof RealVariable){
						v = ctx.mkRealConst(variable.toString());
					}else if(variable instanceof IntVariable){
						v = (ctx.mkBVConst(variable.toString(), 32));
						//					stack.push(ctx.mkIntConst(variable.toString()));
					}else if(variable instanceof ArrayVariable){
						Sort index = ctx.mkBitVecSort(((ArrayVariable) variable).getIndexSize().getValue());
						Sort mem = ctx.mkBitVecSort(((ArrayVariable) variable).getMemSizeBits().getValue());
						v = (ctx.mkArrayConst(variable.getName(), index, mem));
					}else{
						new java.lang.Exception().printStackTrace();
						System.exit(1);
					}
					vm.put(variable, v);
				}catch(Z3Exception e){
					e.printStackTrace();
					System.exit(1);
				}
			}
			stack.push(v);
		}

		@Override
		public void postVisit(Constant constant){
			try{
				if(constant instanceof RealConstant){
					stack.push(ctx.mkReal(constant.toString()));
				}else if(constant instanceof IntConstant){
					stack.push(ctx.mkBV(constant.toString(), 32));
					//					stack.push(ctx.mkInt(constant.toString()));
				}else if(constant instanceof BitVectorConstant){
					stack.push(ctx.mkBV(((BitVectorConstant) constant).getValue().longValue(), ((BitVectorConstant) constant).getNumBits()));
				}else{
					new java.lang.Exception().printStackTrace();
					System.exit(1);
				}
			}catch(Z3Exception e){
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		@Override
		public void postVisit(Alias alias){
			try {
				alias.getExpression().accept(this);
			} catch (VisitorException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void postVisit(Operation operation){
			try{
				if(operation.getOperator() == Operation.Operator.EQ){
					Expr right = stack.pop();
					
					Expr left = null;
					if(stack.size() == 0){
						System.out.println("here");
					}else{
						left = stack.pop();
					}
					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
					}

					stack.push(ctx.mkEq(left,  right));
				}else if(operation.getOperator() == Operation.Operator.NE){
					Expr right = stack.pop();
					Expr left = stack.pop();
					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
					}

					stack.push(ctx.mkNot(ctx.mkEq(left,  right)));
				}else if(operation.getOperator() == Operation.Operator.LE){
					Expr right = stack.pop();
					Expr left = stack.pop();
					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
						stack.push(ctx.mkBVSLE((BitVecExpr) left, (BitVecExpr) right));
					}else{
						stack.push(ctx.mkLe((ArithExpr)left,  (ArithExpr) right));
					}
				}else if(operation.getOperator() == Operation.Operator.LT){
					Expr right = stack.pop();
					Expr left = stack.pop();
					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
						stack.push(ctx.mkBVSLT((BitVecExpr) left, (BitVecExpr) right));
					}else{
						stack.push(ctx.mkLt((ArithExpr)left,  (ArithExpr) right));
					}
				}else if(operation.getOperator() == Operation.Operator.GE){
					Expr right = stack.pop();
					Expr left = stack.pop();
					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
						stack.push(ctx.mkBVSGE((BitVecExpr) left, (BitVecExpr) right));
					}else{
						stack.push(ctx.mkGe((ArithExpr)left,  (ArithExpr) right));
					}
				}else if(operation.getOperator() == Operation.Operator.GT){
					Expr right = stack.pop();
					Expr left = stack.pop();
					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
						stack.push(ctx.mkBVSGT((BitVecExpr) left, (BitVecExpr) right));
					}else{
						stack.push(ctx.mkGt((ArithExpr)left,  (ArithExpr) right));
					}
				}else if(operation.getOperator() == Operation.Operator.AND){
					BoolExpr right = (BoolExpr) stack.pop();
					BoolExpr left = (BoolExpr) stack.pop();
					stack.push(ctx.mkAnd(left,  right));
				}else if(operation.getOperator() == Operation.Operator.ADD){
					Expr right = stack.pop();
					Expr left = stack.pop();

					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
						stack.push(ctx.mkBVAdd((BitVecExpr)left,  (BitVecExpr)right));
					}else{
						stack.push(ctx.mkAdd((ArithExpr)left,  (ArithExpr)right));
					}
				}else if(operation.getOperator() == Operation.Operator.SUB){
					Expr right = stack.pop();
					Expr left = stack.pop();

					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
						stack.push(ctx.mkBVSub((BitVecExpr)left,  (BitVecExpr)right));
					}else{
						stack.push(ctx.mkSub((ArithExpr)left,  (ArithExpr)right));
					}
				}else if(operation.getOperator() == Operation.Operator.MUL){
					Expr right = stack.pop();
					Expr left = stack.pop();

					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
						stack.push(ctx.mkBVMul((BitVecExpr)left,  (BitVecExpr)right));
					}else{
						stack.push(ctx.mkMul((ArithExpr)left,  (ArithExpr)right));
					}
				}else if(operation.getOperator() == Operation.Operator.OR){
					BoolExpr right = (BoolExpr) stack.pop();
					BoolExpr left = (BoolExpr) stack.pop();
					stack.push(ctx.mkOr(left,  right));
				}else if(operation.getOperator() == Operation.Operator.DIV){
					Expr right = stack.pop();
					Expr left = stack.pop();

					if(left instanceof BitVecExpr || right instanceof BitVecExpr){
						if(! (left instanceof BitVecExpr)){
							left = ctx.mkInt2BV(32, (IntExpr)left);
						}
						if(! (right instanceof BitVecExpr)){
							right = ctx.mkInt2BV(32, (IntExpr)right);
						}
						stack.push(ctx.mkBVSDiv((BitVecExpr)left,  (BitVecExpr)right));
					}else{
						stack.push(ctx.mkDiv((ArithExpr)left,  (ArithExpr)right));
					}
				}else if(operation.getOperator() == Operation.Operator.SQRT){
					ArithExpr right = (ArithExpr) stack.pop();
					stack.push(ctx.mkSub(right, ctx.mkReal("0.5")));
				}else if(operation.getOperator() == Operation.Operator.BVAND){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					if(right instanceof ArithExpr){
						right = ctx.mkInt2BV(32, (IntExpr) right);
					}

					if(left instanceof ArithExpr){
						left = ctx.mkInt2BV(32, (IntExpr) left);
					}
					stack.push(ctx.mkBVAND((BitVecExpr)left, (BitVecExpr) right));

				}else if(operation.getOperator() == Operation.Operator.BVOR){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					if(right instanceof ArithExpr){
						right = ctx.mkInt2BV(32, (IntExpr) right);
					}

					if(left instanceof ArithExpr){
						left = ctx.mkInt2BV(32, (IntExpr) left);
					}
					stack.push(ctx.mkBVOR((BitVecExpr)left, (BitVecExpr) right));

				}else if(operation.getOperator() == Operation.Operator.BVXOR){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					if(right instanceof ArithExpr){
						right = ctx.mkInt2BV(32, (IntExpr) right);
					}

					if(left instanceof ArithExpr){
						left = ctx.mkInt2BV(32, (IntExpr) left);
					}
					stack.push(ctx.mkBVXOR((BitVecExpr)left, (BitVecExpr) right));

				}
				else if(operation.getOperator() == Operation.Operator.BVLSHL){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					if(right instanceof ArithExpr){
						right = ctx.mkInt2BV(32, (IntExpr) right);
					}

					if(left instanceof ArithExpr){
						left = ctx.mkInt2BV(32, (IntExpr) left);
					}
					stack.push(ctx.mkBVSHL((BitVecExpr)left, (BitVecExpr) right));

				}else if(operation.getOperator() == Operation.Operator.BVASHR){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					if(right instanceof ArithExpr){
						right = ctx.mkInt2BV(32, (IntExpr) right);
					}

					if(left instanceof ArithExpr){
						left = ctx.mkInt2BV(32, (IntExpr) left);
					}
					stack.push(ctx.mkBVASHR((BitVecExpr)left, (BitVecExpr) right));

				}else if(operation.getOperator() == Operation.Operator.BVLSHR){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					if(right instanceof ArithExpr){
						right = ctx.mkInt2BV(32, (IntExpr) right);
					}

					if(left instanceof ArithExpr){
						left = ctx.mkInt2BV(32, (IntExpr) left);
					}
					stack.push(ctx.mkBVLSHR((BitVecExpr)left, (BitVecExpr) right));

				}else if(operation.getOperator() == Operation.Operator.MOD){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					if(right instanceof ArithExpr){
						right = ctx.mkInt2BV(32, (IntExpr) right);
					}

					if(left instanceof ArithExpr){
						left = ctx.mkInt2BV(32, (IntExpr) left);
					}
					stack.push(ctx.mkBVSMod((BitVecExpr)left, (BitVecExpr) right));

				}else if(operation.getOperator() == Operation.Operator.SELECT){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkSelect((ArrayExpr) left, right));
				}else if(operation.getOperator() == Operation.Operator.STORE){
					Expr right = (Expr) stack.pop();
					Expr middle = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkStore((ArrayExpr) left, middle, right));
				}else if(operation.getOperator() == Operation.Operator.CONCAT){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkConcat((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVSGE){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVSGE((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVSGT){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVSGT((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVSLE){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVSLE((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVSLT){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVSLT((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.ITE){
					Expr right = (Expr) stack.pop();
					Expr middle = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkITE((BoolExpr)left, middle, right));
				}else if(operation.getOperator() == Operation.Operator.BVUGE){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVUGE((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVUGT){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVUGT((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVULE){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVULE((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVULT){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVULT((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVSUB){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVSub((BitVecExpr)left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVSIGNEXTEND){
					Expr right = (Expr) stack.pop();
					int left = ((BitVecNum) stack.pop()).getInt();
					stack.push(ctx.mkSignExt(left, (BitVecExpr)right));
				}else if((operation.getOperator() == Operation.Operator.BVSHL) || (operation.getOperator() == Operation.Operator.BVLSHL)){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVSHL((BitVecExpr) left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVASHR){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVASHR((BitVecExpr) left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVLSHR){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVLSHR((BitVecExpr) left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVMUL){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVMul((BitVecExpr) left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVEXTRACT){
					Expr right = (Expr) stack.pop();
					int middle = ((BitVecNum) stack.pop()).getInt();
					int left = ((BitVecNum)(Expr) stack.pop()).getInt();
					stack.push(ctx.mkExtract(left, middle, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVADD){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVAdd((BitVecExpr) left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVZEROEXTEND){
					Expr right = (Expr) stack.pop();
					int left = ((BitVecNum) stack.pop()).getInt();
					stack.push(ctx.mkZeroExt(left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVSREM){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVSRem((BitVecExpr) left, (BitVecExpr)right));
				}else if(operation.getOperator() == Operation.Operator.BVSDIV){
					Expr right = (Expr) stack.pop();
					Expr left = (Expr) stack.pop();
					stack.push(ctx.mkBVSDiv((BitVecExpr) left, (BitVecExpr)right));
				}else{
					System.out.println(operation.getOperator().toString());
					new java.lang.Exception().printStackTrace();
					System.exit(1);
				}
			}catch(Z3Exception e){
				e.printStackTrace();
				System.exit(1);
			}
		}	
	}
}
