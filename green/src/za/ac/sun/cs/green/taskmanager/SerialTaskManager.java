package za.ac.sun.cs.green.taskmanager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import za.ac.sun.cs.green.Instance;
import za.ac.sun.cs.green.Service;
import za.ac.sun.cs.green.Green;
import za.ac.sun.cs.green.service.bvfactorizer.resources.SelectStore;
import za.ac.sun.cs.green.util.Reporter;

public class SerialTaskManager implements TaskManager {

	private final Green solver;

	private final Logger log;

	private int processedCount = 0;

	public SerialTaskManager(final Green solver) {
		this.solver = solver;
		log = solver.getLog();
	}

	public Object execute(Service parent, Instance parentInstance, Set<Service> services, Set<Instance> instances) {
		Object result = null;
		Map<String,Object> combinedResult = new HashMap<String,Object>();
		for (Service service : services) {
			for (Instance instance : instances) {
//				System.out.println(instance.getExpression());
				Object res = execute0(parent, parentInstance, service, instance);
				if (res != null) {
					if(res instanceof Map<?, ?>){
						@SuppressWarnings("unchecked")
						Map<String, Object> vm = (Map<String, Object>) res;
						if(vm.isEmpty()){//One of the queries returns false.
							result = false;
							break;
						}
						for(String var:vm.keySet()){
							if(combinedResult.containsKey(var)){
								//We need to do processing here
								//We need to find which bits inside vm is actually used for the current instance.
								@SuppressWarnings("unchecked")
								Set<SelectStore> ss = (Set<SelectStore>) instance.getData("SelectStore");
								int[] resultval = (int[]) combinedResult.get(var);
								int[] val = (int[]) vm.get(var);
								for(SelectStore s:ss){
									if(s.getArrayAccessed().getName() != var){
										continue;
									}
									int index = Integer.parseInt(s.getIndex().toString());
									resultval[index] = val[index];
								}
							}else{
								combinedResult.put(var, vm.get(var));
							}
						}
					}else if(result == null){
						result = res;
					}else if(res instanceof Boolean){
						//if it is boolean type
						if(result instanceof Boolean){
							result = (Boolean) result && (Boolean)res;
						}else{
							result = false;
						}
					}
				}
			}
		}
		if(result == null){
			result = combinedResult;
		}
		if (parent != null) {
			result = parent.allChildrenDone(parentInstance, result);
		}
		
		return result;
	}

	public Object execute0(Service parent, Instance parentInstance, Service service, Instance instance) {
		Object result = null;
		Set<Instance> subinstances = service.processRequest(instance);
		if ((subinstances != null) && (subinstances.size() > 0)) {
			Set<Service> subservices = solver.getService(service);
			if ((subservices != null) && (subservices.size() > 0)) {
				result = execute(service, instance, subservices, subinstances);
			} else {
				result = service.allChildrenDone(instance, result);
			}
		} else {
			result = service.allChildrenDone(instance, result);
		}
		if (parent != null) {
			result = parent.childDone(parentInstance, service, instance, result); 
		}
		return result;
	}
	
	@Override
	public Object process(final String serviceName, final Instance instance) {
		log.info("processing serviceName=\"" + serviceName + "\"");
		processedCount++;
		final Set<Service> services = solver.getService(serviceName);
		return execute(null, null, services, Collections.singleton(instance));
	}

	@Override
	public void report(Reporter reporter) {
		reporter.report(getClass().getSimpleName(), "processedCount = " + processedCount);
	}

	@Override
	public void shutdown() {
	}

}
