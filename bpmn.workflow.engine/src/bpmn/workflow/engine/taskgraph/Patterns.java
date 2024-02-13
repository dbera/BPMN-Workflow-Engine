package bpmn.workflow.engine.taskgraph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.camunda.bpm.model.bpmn.instance.FlowElement;


/*
 * TODO:
 * 6-2-2024 [] Manage events and other missing gates?
 * 6-2-2024 [] Some TODOS and FIXMES are inlined.
 * 6-2-2024 [] Cleanup code.
 * 7-2-2024 [] Check that model is valid.
 */

public class Patterns {
	public String name = "graph1";
	private List<ProcessTask> tasks = new ArrayList<ProcessTask>();
	public List<Edge> edges = new ArrayList<Edge>();
	public AbstractMap<String, Vertex> vertices = new HashMap<String,Vertex>();
	public List<DataType> types = new ArrayList<DataType>();
	public List<Component> components = new ArrayList<Component>();
	
	private static String UNIT_TYPE = "UNIT";
	
	
	public void setModelName(String _name) {
		name = _name;
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
	
	public String generateFabSpecTypes() {
		String typess = new String("");
		typess += "record UNIT {\n" + indent("int unit\n") + "}\n\n";
		typess += "enum Verdict { INSPEC OUTOFSPEC }\n\n";
		for (DataType d: types) { 
			String type = "record " + cleanName(d.name) + " {\n";
			String parameters = "";
			for (Entry<String, String> e: d.parameters.entrySet()) {
				String value = String.format("%s",e.getValue());
				String key = e.getKey();
				parameters += normalizeType(cleanName(value)) + "\t" + cleanName(e.getKey()) + "\n";
			}
			type += indent(parameters) + "}\n";
			typess += type + "\n";
		}
		return typess;
	}
	
	private String normalizeType(String type) {
		if (type.equals("String") || type.equals("Int")) {
			return type.toLowerCase();
		}else if (type.equals("Boolean")) {
			return "bool";
		}else {
			return type;
		}
	}
	
	public String generateFabSpec() {
		String TYPES_FILE_NAME = String.format("\"%s.types\"", name);
		StringBuilder strb = new StringBuilder(String.format("import %s\n", TYPES_FILE_NAME));
		strb.append("specification " +  name + "\n{\n");
		for (Component c: components) {
			String cname = normalizeName(c.name);
			String component = "system " + cname + "\n{\n";
			String inOut = fabSpecInputOutput(c);
			String local = fabSpecLocal(c);
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
		strb.append(indent("depth-limits 20\n"));
		strb.append("}\n");
		return strb.toString();
	}
	
	private String fabSpecInputOutput(Component c) {
		String inStr = "inputs\n";
		String outStr = "outputs\n";
		
		for (String src: c.incomings.keySet()) {
			String name = cleanName(src);
			String[] spl = src.split(":");
			String type = spl.length > 1 ? spl[1] : "UNKNOWN";
			inStr += type + "\t" + name + "\n";
		}
		
		for (String dst: c.outgoings.keySet()) {
			String name = cleanName(dst);
			String[] spl = dst.split(":");
			String type = spl.length > 1 ? spl[1] : "UNKNOWN";
			outStr += type + "\t" + name + "\n";
		}

		if (c.incomings.keySet().isEmpty()) { inStr = "//" + inStr; }
		if (c.outgoings.keySet().isEmpty()) { outStr = "//" + outStr; }
		return inStr + "\n" + outStr;
	}
	
	private String fabSpecLocal(Component c) {
		String locals = "local\n";
		//
		// All data vertices are places (if they are not inputs or outputs)
		//
		for (Vertex v: vertices.values()) {
			if( isDataVertex(v.getName()) && v.pname == c.name 
					&& !c.getIncomingEdges().keySet().contains(v.getName()) 
					&& !c.getOutgoingEdges().keySet().contains(v.getName())) {
				String t = cleanName(v.getDataType());
				t = cleanName(v.getDataType()) == "" ? "UNKNOWN" : t;
				locals += tabulate(t, cleanName(v.getName())) + "\n";
			}
		}
		//
		// XOR gates introduce a place
		//
		for (Vertex v: vertices.values()) {
			if (v.pname == c.name) {
				TaskType vtype = v.getType();
				if ( vtype == TaskType.EXOR_JOIN_GATE || vtype == TaskType.EXOR_SPLIT_GATE) {
					locals += tabulate(UNIT_TYPE, cleanName(v.getName())) + "\n";
				}
			}
		}
		//
		// Edges introduce places under certain conditions
		//
		for (Edge e: edges) {
			Vertex src = vertices.get(e.srcName);
			Vertex dst = vertices.get(e.dstName);
			if (src == null || dst == null) {
				// FIXME remove edges to subprocesses from <edges> if not needed, then this will not happen
				logInfo(String.format("WARNING: Cant find vertex %s or %s.", e.srcName, e.dstName));
			}else {
				if (src.pname == c.name 
						&& !isExclusiveGate(src) && !isDataVertex(e.srcName)
						&& !isExclusiveGate(dst) && !isDataVertex(e.dstName)){
					assert dst.pname == c.name;
					locals += tabulate(UNIT_TYPE, cleanName(e.srcName)+"2"+cleanName(e.dstName)) + "\n";
				}
			}
		}
		return locals;
	}
	

	private String fabSpecInit(String _cname) {
		// TODO
		return "// init\n";
	}
	
	
	private Boolean isAPlace (String name) {
		Vertex v = vertices.get(name);
		if (v != null) {
			TaskType t = v.getType();
			return t == TaskType.DATA_OBJECT || t == TaskType.CATCH_EVENT 
					|| t == TaskType.EXOR_SPLIT_GATE || t == TaskType.EXOR_JOIN_GATE;
		} else {
//			throw new Exception("No vertex named " +  name);
			logInfo("No vertex named " +  name);
			return false;
		}
		 
		
	}

	private String fabSpecDesc(String _compName) {
		ArrayList<String> desc = new ArrayList<String>();
		for (Vertex v: vertices.values()) {
			if (v.pname == _compName) {
				if(v.getType() == TaskType.TASK 
						|| v.getType() == TaskType.PAR_JOIN_GATE 
						|| v.getType() == TaskType.PAR_SPLIT_GATE) {
					String task = "";
					List<String> inputs = new ArrayList<String>();
					for(Edge e: v.getIncomingEdges()) {
						if (isAPlace(e.srcName)) {
							inputs.add(cleanName(e.srcName));
						} else {
							inputs.add(cleanName(e.srcName) + "2" + cleanName(v.getName()));
						}
					}
					task += "action\t\t\t" + normalizeName(v.getName()) + "\n";
					task += "case\t\t\t" + "default\n";
					task += "with-inputs\t\t" + String.join(", ", inputs) + "\n";
					for(Edge e: v.getOutgoingEdges()) {
						if (isAPlace(e.dstName)) {
							task += "produces-outputs\t" + cleanName(e.dstName) + "\n";
						} else {
							task += "produces-outputs\t" + cleanName(v.getName()) + "2" + cleanName(e.dstName) + "\n";	
						}
						
						task += e.expression != ""? "updates\n" + indent(e.expression) + "\n" : "";
					}
					desc.add(task);
				}
			}
		}
		// TODO: an edge between XOR gates introduces a transition (or collapse XOR gates)
		return "desc \"" + normalizeName(_compName) + "_Model\"\n\n" + String.join("\n", desc);
	}
	
	private String indent(String str) {
		return str.replaceAll("(?m)^", "    ");
	}
	
	private static String normalizeName(String name) {
		return name.replace(" ", "_").replace("+", "_plus_");
	}
	
	private String cleanName(String name) {
		return name == null ? "" : capAtColon(normalizeName(name));
	}

	public Boolean isDataVertex (String name) {
		Vertex v = vertices.get(name);
		if (v != null) {
			return v.type == TaskType.DATA_OBJECT || v.type == TaskType.CATCH_EVENT;
		} else {
//			throw new Exception("No vertex named " +  name);
			logInfo("No vertex named " +  name);
			return false;
		}
		
	}

	private Boolean isExclusiveGate(Vertex v) {
		return v.getType() == TaskType.EXOR_JOIN_GATE || v.getType() == TaskType.EXOR_SPLIT_GATE;
	}
	
	private String tabulate (String... strings) {
		return String.join("\t", strings);
	}
	
	private String capAtColon(String string) {
		return string == null ? "" : string.split(":")[0];
	}
	
	public static void logInfo(String str) { System.out.println(str); }		
	

	// Old code from Luna:
	
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

}
