package za.ac.sun.cs.green.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.expr.*;
import za.ac.sun.cs.green.util.Reporter;

public abstract class ModelService extends BasicService {

	private static final String SERVICE_KEY = "MODEL"
			+ ":";

	private int invocationCount = 0;

	private int cacheHitCount = 0;
	
	private int cacheMissCount = 0;
	
	private long timeConsumption = 0;

	public ModelService(Green solver) {
		super(solver);
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "invocationCount = " + invocationCount);
		reporter.report(getClass().getSimpleName(), "cacheHitCount = " + cacheHitCount);
		reporter.report(getClass().getSimpleName(), "cacheMissCount = " + cacheMissCount);
		reporter.report(getClass().getSimpleName(), "timeConsumption = " + timeConsumption);
	}

	@Override
	public Object allChildrenDone(Instance instance, Object result) {
		return instance.getData(getClass());
	}
	
	@Override
	public Set<Instance> processRequest(Instance instance) {
		@SuppressWarnings("unchecked")
		Map<String,Object> result = (Map<String,Object>) instance.getData(getClass());
		if (result == null) {
			result = solve0(instance);
			if (result != null) {
				instance.setData(getClass(), result);
			}
		}
		return null;
	}

	private Map<String,Object> solve0(Instance instance) {
		invocationCount++;
		String key = SERVICE_KEY + instance.getFullExpression().toString();
		@SuppressWarnings("unchecked")
		HashMap<String,Object> result =(HashMap<String,Object>) store.get(key);
		if (result == null) {
			cacheMissCount++;
			result = solve1(instance);
			if (result != null) {
				store.put(key, result);
			}
		} else {
			cacheHitCount++;
		}
		return result;
	}

	private HashMap<String,Object> solve1(Instance instance) {
		long startTime = System.currentTimeMillis();
		HashMap<String,Object> result = (HashMap<String, Object>) model(instance); 
		timeConsumption += System.currentTimeMillis() - startTime;
		return result; // change this!
	}

	protected abstract Map<String, Object> model(Instance instance);
	
	public int getCacheMissCount(){
		return cacheMissCount;
	}

}
