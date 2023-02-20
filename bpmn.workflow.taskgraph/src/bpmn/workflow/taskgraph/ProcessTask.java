package bpmn.workflow.taskgraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcessTask {
	String name = new String();
	TaskType type = TaskType.TASK;
	Set<ProcessTask> successors = new HashSet<ProcessTask>();
	//Set<ProcessTask> predecessor = new HashSet<ProcessTask>
	
	ProcessTask(String _name, TaskType _type, List<ProcessTask> succList) {
		name = _name;
		type = _type;
		addSuccTasks(succList);
	}
	
	public boolean isSuccPresent(ProcessTask _pt) {
		for(ProcessTask pt : successors) {
			if(pt.name.equals(_pt.name)) return true;
		}
		return false;
	}
	
	public void addSuccTasks(List<ProcessTask> tl) { 
		for(ProcessTask pt : tl)
			addSuccTask(pt);
	}
	public void addSuccTask(ProcessTask t) { 
		if(!isSuccPresent(t)) successors.add(t); 
	}
}
