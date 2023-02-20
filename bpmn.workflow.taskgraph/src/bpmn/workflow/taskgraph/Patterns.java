package bpmn.workflow.taskgraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Patterns {
	public String name = "graph1";
	private List<ProcessTask> tasks = new ArrayList<ProcessTask>();
	public List<Edge> edges = new ArrayList<Edge>();
	public List<Vertex> vertices = new ArrayList<Vertex>();
	public List<DataType> types = new ArrayList<DataType>();
	
	public void addTask(ProcessTask curr) 
	{
		System.out.println(" Creating Process Task: " + curr.name);
		System.out.println(" Type: " + curr.type.toString());
		System.out.println(" Succ: " + curr.successors);
		tasks.add(curr);
		if(curr.type.equals(TaskType.SERVICE_TASK)) {
			
		} else if(curr.type.equals(TaskType.EXOR_SPLIT_GATE)) {
			
		} else if(curr.type.equals(TaskType.EXOR_JOIN_GATE)) {
			
		} else if(curr.type.equals(TaskType.PAR_SPLIT_GATE)) {
			
		} else if(curr.type.equals(TaskType.PAR_JOIN_GATE)) {
			
		} else if(curr.type.equals(TaskType.START_EVENT)) {
			
		} else { // abstract task
			
		}
	}
	
	public void addTask(String _name, TaskType _type, List<ProcessTask> succList) {
		ProcessTask curr = new ProcessTask(_name, _type, succList);
		tasks.add(curr);
	}
	
	public void addVertex(Vertex v) {
		vertices.add(v);
	}
	
	public void addEdge(Edge edge) {
		edges.add(edge);
	}
	
	public void addEdge(String _srcName, String _dstName, String _expression) {
		Edge edge = new Edge(_srcName, _dstName, _expression);
		edges.add(edge);
	}
	
	public void addDataType(DataType r) {
		types.add(r);
	}
	
	public Vertex getVertex(String name) {
		for (Vertex v : vertices) {
			if (v.name.equals(name)) {
				return v;
			}
		}
		return null;
	}
	
	public String generateDot() {
		StringBuilder b = new StringBuilder("digraph "+ name + " {\n");
		b.append("  rankdir = LR; nodesep=.25; sep=1;\n");
		for (Vertex v : vertices) {
			b.append("  ").append(v.name);
			b.append(" [shape=ellipse,label=\""+ v.name+ "\"];\n");
		}
		
		for(Edge edge: edges){
			b.append("  ").append(edge.srcName);
			b.append(" -> ").append(edge.dstName);
			b.append(" [label=\"");
			b.append(edge.expression).append("\"");
			b.append("]\n");
		}
		b.append("}\n");
		
		return b.toString();
	}
	
}
