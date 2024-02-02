package bpmn.workflow.engine.taskgraph;

import java.util.HashSet;
import java.util.Set;

public class Vertex {
	String name = new String();
	String pname = new String();
	String dataType = new String();
	TaskType type = TaskType.TASK;
	Set<Edge> incomings = new HashSet<Edge>();
	Set<Edge> outgoings = new HashSet<Edge>();
	
	public Vertex(String _name) {
		name = _name;
	}
	
	public Vertex(String _name, TaskType _type) {
		name = _name;
		type = _type;
	}
	
	public TaskType getType() {
		return type;
	}
	public void setType(TaskType _type) {
		type = _type;
	}
	
	public void setDataType(String _dataType) {
		dataType = _dataType;
	}
	
	public String getDataType() {
		return dataType;
	}
	
	public void setPname(String _pname) {
		/* Set parent name. */
		pname = _pname;
	}
	
	public void addIncomingEdge(String src, String expression) {
		Edge e = new Edge(src, name, expression);
		incomings.add(e);
	}
	
	public void addOutgoingEdge(String dst, String expression) {
		Edge e = new Edge(name, dst, expression);
		outgoings.add(e);
	}
	
	public Set<Edge> getOutgoingEdges(){
		return outgoings;
	}
	
	public Set<Edge> getIncomingEdges(){
		return incomings;
	}
	
	public String getName() {
		return name;
	}
}
