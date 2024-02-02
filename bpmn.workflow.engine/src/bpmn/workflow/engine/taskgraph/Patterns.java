package bpmn.workflow.engine.taskgraph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.camunda.bpm.model.bpmn.instance.FlowElement;

public class Patterns {
	public String name = "graph1";
	private List<ProcessTask> tasks = new ArrayList<ProcessTask>();
	public List<Edge> edges = new ArrayList<Edge>();
	public AbstractMap<String, Vertex> vertices = new HashMap<String,Vertex>();
	public List<DataType> types = new ArrayList<DataType>();
	public List<Component> components = new ArrayList<Component>();
	
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
		vertices.put(v.name, v);
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
	

	
	public void toPPetriNet() {
		
	}
	
	public void generateSnakes() {
		int idx = 0;
		try {
			FileWriter snakesWriter = new FileWriter("output\\petrinet.py");
			snakesWriter.write("from snakes.nets import *\n");
			snakesWriter.write("from pyrecord import Record\n");
			snakesWriter.write("n = PetriNet('First net')\n"); 
			for (Vertex v : vertices.values()) {
				if (v.type.equals(TaskType.START_EVENT)) {
					snakesWriter.write("n.add_place(Place('"+ v.name + "', [0]))\n");
				} else {
					snakesWriter.write("n.add_place(Place('"+ v.name + "'))\n");
				}
			}
			for(Edge edge: edges){
				snakesWriter.write("n.add_transition(Transition('t"+idx+"', Expression('"+edge.expression +"')))\n");
				idx++;
			}
			for(DataType dataType : types) {
				snakesWriter.write(dataType.name + " = Record.create_type(\"" +dataType.name + "\"");
				for (String key : dataType.parameters.keySet()) {
					snakesWriter.write(", \""+key+"\"");
				}
				snakesWriter.write(")\n");
			}
			snakesWriter.close();
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}
	
	public String generateDot() {
		StringBuilder b = new StringBuilder("digraph "+ name + " {\n");
		b.append("  rankdir = LR; nodesep=.25; sep=1;\n");
		for (Vertex v : vertices.values()) {
			b.append("  ").append(v.name);
			b.append(" [shape=ellipse,label=\""+ v.name + v.type.toString() + v.pname + "\"];\n");
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
	
	public String generateFabSpecTypes() {
		String typess = new String("");
		typess += "record CTX {\n" + indent("int ctx\n") + "}\n\n";
		typess += "enum Verdict { INSPEC OUTOFSPEC }\n\n";
		for (DataType d: types) { 
			String type = "record " + d.name + " {\n";
			String parameters = "";
			for (Entry<String, String> e: d.parameters.entrySet()) {
				String value = String.format("%s",e.getValue());
				String key = e.getKey();
				parameters += value + "\t" + e.getKey() + "\n";
			}
			type += indent(parameters) + "}\n";
			typess += type + "\n";
		}
		return typess;
	}
	
	public String generateFabSpec() {
		String TYPES_FILE_NAME = "data.types";
		StringBuilder strb = new StringBuilder(String.format("import %s;\n", TYPES_FILE_NAME));
		strb.append("Product-Spec " +  name + "\n{\n");
		for (Component c: components) {
			String cname = normalizeName(c.name);
			String component = cname + "\n{\n";
			String inOut = fabSpecInputOutput(c.name);
			String local = fabSpecLocal(c.name);
			String init = fabSpecInit(c.name);
			String desc = fabSpecDesc(c.name); 
			component += indent(inOut);
			component += "\n";
			component += indent(local);
			component += "\n";
			component += indent(init);
			component += "\n";
			component += indent(desc);
			component += "}\n\n";
			strb.append(indent(component));			
		}
		strb.append("}\n");
		return strb.toString();
	}

	private String fabSpecInputOutput(String compName) {
		String inStr = "inputs\n";
		String outStr = "outputs\n";
		for (Vertex v: vertices.values()) {
			if(v.getType() == TaskType.TASK && v.pname == compName) {
				for (Edge e: edges) {
					if (isDataVertex(e.dstName) && e.srcName == v.name) {
						Vertex d = vertices.get(e.dstName);
						String name = d.name != null ? d.name.split(":")[0] : "null";
						String type = d.dataType != null ? d.dataType.split(":")[0] : "null";
						outStr += type + "\t" + name + "\n";
					}
					if (isDataVertex(e.srcName) && e.dstName == v.name) {
						Vertex d = vertices.get(e.srcName);
						String name = d.name != null ? d.name.split(":")[0] : "null";
						String type = d.dataType != null ? d.dataType.split(":")[0] : "null";
						inStr += type + "\t" + name + "\n";
					}
				}
			}
		}
		return inStr + "\n" + outStr;
	}
	
	private String fabSpecLocal(String _compName) {
		String compName = normalizeName(_compName);
		String locals = "local\n";
		//
		// XOR gates introduce a place
		//
		for (Vertex v: vertices.values()) {
			if (v.pname == _compName) {
				TaskType vtype = v.getType();
				if ( vtype == TaskType.EXOR_JOIN_GATE || vtype == TaskType.EXOR_SPLIT_GATE) {
					locals += tabulate("SOME_TYPE_TODO", capAtColon(v.getName())) + "\n";
				}
			}
		}
		//
		// Edges between tasks, gates, except for XOR gates, introduce places 
		//
		for (Edge e: edges) {
			Vertex src = vertices.get(e.srcName);
			Vertex dst = vertices.get(e.dstName);
			if (src == null || dst == null) {
				logInfo(String.format("ERROR: Cant find vertex %s or %s.", e.srcName, e.dstName));
			}else {
				if (!isExclusiveGate(src) && !isDataVertex(e.srcName)
						&& !isExclusiveGate(dst) && !isDataVertex(e.dstName)){
					locals += tabulate("SOME_TYPE_TODO", capAtColon(e.srcName)+"2"+capAtColon(e.dstName)) + "\n";
				}
			}
		}
		return locals;
	}
	
	private Boolean isExclusiveGate(Vertex v) {
		return v.getType() != TaskType.EXOR_JOIN_GATE && v.getType() != TaskType.EXOR_SPLIT_GATE;
	}
	
	private String tabulate (String... strings) {
		return String.join("\t", strings);
	}
	
	private String capAtColon(String string) {
		return string == null ? "" : string.split(":")[0];
	}
	
	private String fabSpecInit(String _cname) {
		return "init \\\\TODO\n";
	}

	private String fabSpecDesc(String _compName) {
		String desc = "desc \"" + normalizeName(_compName) + "_Model\"\n\n";
		for (Vertex v: vertices.values()) {
			if(v.getType() == TaskType.TASK && v.pname == _compName) {
				List<String> inputs = new ArrayList<String>();
				List<String> outputs = new ArrayList<String>();
				for(Edge e: v.getIncomingEdges()) {
					inputs.add(e.srcName);
				}
				for(Edge e: v.getOutgoingEdges()) {
					outputs.add(e.dstName);
				}	
				desc += "action\t\t\t" + normalizeName(v.getName()) + "\n";
				desc += "case\t\t\t" + "default\n";
				desc += "with-inputs\t\t" + String.join(", ", inputs) + "\n";
				desc += "produces-outputs\t" + String.join(", ", outputs) + "\n";
			}
		}
		return desc;
	}
	
	private String indent(String str) {
		return str.replaceAll("(?m)^", "    ");
	}
	
	private static String normalizeName(String name) {
		return name.replace(" ", "_");
	}

	public Boolean isDataVertex (String name) {
		Vertex v = vertices.get(name);
		if (v != null) {
			return v.type == TaskType.DATA_OBJECT || v.type == TaskType.CATCH_EVENT;
		}
		return false;
	}
	
	public static void logInfo(String str) { System.out.println(str); }		
	
}
