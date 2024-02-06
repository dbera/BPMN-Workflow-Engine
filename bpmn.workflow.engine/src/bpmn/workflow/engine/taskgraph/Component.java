package bpmn.workflow.engine.taskgraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Component {
	// FIXME inherit from Vertex instead of redoing things
	String name = new String();
	TaskType type = TaskType.SUB_PROCESS;
	Set<ProcessTask> childTasks = new HashSet<ProcessTask>();
	Map<String,Edge> incomings = new HashMap<String, Edge>();
	Map<String,Edge> outgoings = new HashMap<String, Edge>();	
	
	public Component(String _name) {
		name = _name;
	}
	
	public void addChildTasks(List<ProcessTask> _childTasks) { 
		for(ProcessTask ct : _childTasks)
			childTasks.add(ct);
	}
	
	public void addChildTask(ProcessTask _childTask) {
		if (!childTasks.contains(_childTask)) {
			childTasks.add(_childTask);
		}
	}
	
	public void addIncomingEdge(String src, String expression) {
		Edge e = new Edge(src, name, expression);
		incomings.put(src, e);
	}
	
	public void addOutgoingEdge(String dst, String expression) {
		Edge e = new Edge(name, dst, expression);
		outgoings.put(dst, e);;
	}
	
	public Map<String,Edge> getOutgoingEdges(){
		return outgoings;
	}
	
	public Map<String,Edge> getIncomingEdges(){
		return incomings;
	}
}
