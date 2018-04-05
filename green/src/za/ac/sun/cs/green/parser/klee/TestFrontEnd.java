package za.ac.sun.cs.green.parser.klee;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.Properties;

import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.expr.Expression;
import za.ac.sun.cs.green.expr.Variable;
import za.ac.sun.cs.green.resources.Pair;
import za.ac.sun.cs.green.store.memory.MemoryStore;
import za.ac.sun.cs.green.util.Configuration;

// This class is intended to be a front end for a Klee output file generated by
// using the command line option --debug-solver-log=solver:smt
public class TestFrontEnd {
	public static void main(String [] args){
		if(args.length != 2){
			System.out.println("There should be two arguments: \n"
					+ "    1) A path to properties file\n"
					+ "    2) A path to Klee SMT output file\n");
			System.exit(1);
		}

		Properties p = new Properties();
		try {
			p.load(new FileInputStream(args[0]));
		} catch (Exception e) {
			System.err.println("Problem loading .properties file");
			System.exit(1);
		}

		System.out.println("Hi Eric");
		KleeOutputParser parser = new KleeOutputParser(new File(args[1]));
		TestFrontEnd tfe = new TestFrontEnd();
		tfe.run(p, parser);
	}

	public void run(Properties p, KleeOutputParser parser){
		// Create a green Solver that we will pass all of the instances to.
		Green solver = new Green(p);		
		new Configuration(solver, p).configure();
		solver.setStore(new MemoryStore(solver, p));

		int instanceNumber = 0;
		
		long timeSpentSolver = 0;		
	
		while(parser.hasNext() && instanceNumber < 1000000){
			// Get the Expression, and the Klee Calculated SAT Value 
			Pair<Expression, Boolean> pair = parser.getNext();

			System.out.println("Serving instance " + instanceNumber);
			instanceNumber ++;

			Instance instance = new Instance(solver, null, pair.first);

            long temp = System.currentTimeMillis();
			//Boolean ret = (Boolean) instance.request("sat");
            Boolean ret = false;
            Object requestRet = instance.request("sat");
            if(requestRet != null){
            	if(requestRet instanceof Map<?, ?>){
                	@SuppressWarnings("unchecked")
					Map<Variable, Object> vm = (Map<Variable, Object>) requestRet;
                	System.out.println("Get Map: " + vm.toString());         
                	ret = true;
            	}else{
            		ret = (Boolean) requestRet;
            	}
            }
            timeSpentSolver += (System.currentTimeMillis() - temp);

			solver.report();
			if(pair.second != null && ! ret.equals(pair.second)){
				System.out.println("Careful");
			}else{
				System.out.println("It matches!!!");
			}

			System.out.println("Served instance, " + (instanceNumber - 1) + ", Time Spent So Far, " + timeSpentSolver);
		}

		parser.close();
	}
}
