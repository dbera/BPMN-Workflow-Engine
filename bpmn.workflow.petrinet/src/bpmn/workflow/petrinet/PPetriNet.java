package bpmn.workflow.petrinet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import bpmn.workflow.taskgraph.Edge;
import bpmn.workflow.taskgraph.Patterns;
import bpmn.workflow.taskgraph.TaskType;
import bpmn.workflow.taskgraph.Vertex;

public class PPetriNet {
	private Patterns pattern;
	private Map<String, PPlace> places = new LinkedHashMap<>();
	private Map<String, PTransition> transitions = new LinkedHashMap<>();
	private ArrayList<PInput> inputs = new ArrayList<>();
	private ArrayList<POutput> outputs = new ArrayList<>();
	private List<String> globalVariables;
	private boolean build = false;
	private JsonObject executionResult = null;
	
	public PPetriNet(Patterns _pattern) {
		this.pattern = _pattern;
	}
	
	public PPetriNet build() {
		if (build) throw new RuntimeException("Already build");
		build = true;
		PTransition.resetIDCounter();
		for(Vertex v: pattern.vertices) {
			switch (v.getType()) {
			case START_EVENT:
				add(PPlace.forVertex(v));
				break;
			case END_EVENT:
				add(PPlace.forVertex(v));
				break;
			case TASK:
				add(new PTransition(v.getName()));
				break;
			case EXOR_JOIN_GATE:
				//OR: add two Transitions for two incoming edges
				for (Edge e : v.getIncomingEdge()) {
					add(new PTransition(SnakesHelper.nameGatewayTransition(v, e.getSrcName())));
				}
				break;
			case EXOR_SPLIT_GATE:
				//OR: add two Transitions for outgoing edges
				for (Edge e : v.getOutgoingEdge()) {
					add(new PTransition(SnakesHelper.nameGatewayTransition(v, e.getDstName())));
				}
				break;
			case PAR_JOIN_GATE:
				//Parallel: add one transition for incoming edges
				add(new PTransition(SnakesHelper.nameGatewayTransition(v, "")));
				break;
			case PAR_SPLIT_GATE:
				//Parallel: add one transition for outgoing edges
				add(new PTransition(SnakesHelper.nameGatewayTransition(v, "")));
				break;
			default:
				add(PPlace.forVertex(v));
				break;
			}
		}
		
		//add input/output arc, add place between two tasks
		for(Edge e: pattern.edges) {
			Vertex source = pattern.getVertex(e.getSrcName());
			Vertex target = pattern.getVertex(e.getDstName());
 			if (isTransition(source) && isTransition(target)) {
 				PTransition tranSrc = getTransition(source, target.getName());
 				PTransition tranDst = getTransition(target, source.getName());
				PPlace intermediate = add(PPlace.forEdge(e));
				outputs.add(new POutput(intermediate, tranSrc, e.getExpression()));
				//System.out.println("output: " + intermediate.name + tranSrc);
				inputs.add(new PInput(intermediate, tranDst, e.getExpression()));
				//System.out.println("input: " + intermediate.name + tranDst);
			}
 			
 			if (isTransition(source) && !isTransition(target)) {
 				PTransition tranSrc = getTransition(source, target.getName());
 				PPlace p = places.get(e.getDstName());
 				outputs.add(new POutput(p, tranSrc, e.getExpression()));
 				//System.out.println("output: " + p.name + tranSrc);
 			}
 			
 			if (!isTransition(source) && isTransition(target)) {
 				PPlace p = places.get(e.getSrcName());
 				PTransition tranDst = getTransition(target, source.getName());
 				inputs.add(new PInput(p, tranDst, e.getExpression()));
 				//System.out.println("input: " + p.name + tranDst);
 			}
			
		}
		
		return this;
	}
	
	private PTransition getTransition(Vertex source, String task) {
		PTransition trans = transitions.get("T"+source.getName());
		if (trans == null) {
			//gateway
			String name = SnakesHelper.nameGatewayTransition(source, task);
			trans = transitions.get("T"+name);
		}
		return trans;
	}
	
	private PPlace add(PPlace place) {
		if (places.containsKey(place.name)) {
			return places.get(place.name);
		} else {
			places.put(place.name, place);
			return place;
		}
	}
	
	private PTransition add(PTransition transition) {
		this.transitions.put(transition.name, transition);
		return transition;
	}

	private void throwIfNotBuild() {
		assert build : "Not build yet";
	}
	
	private boolean isTransition(Vertex v) {
		if (v.getType().equals(TaskType.TASK) ||
				v.getType().equals(TaskType.EXOR_SPLIT_GATE) ||
				v.getType().equals(TaskType.PAR_SPLIT_GATE) ||
				v.getType().equals(TaskType.EXOR_JOIN_GATE) ||
				v.getType().equals(TaskType.PAR_JOIN_GATE)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void printAll() {
		for (String key: places.keySet()) {
			System.out.println("Place " + places.get(key).name);
		}
		for (String key: transitions.keySet()) {
			System.out.println("Transition " + transitions.get(key).name);
		}
		for (PInput in: inputs) {
			System.out.println("Input arc "+ in.place.name + " to " + in.transition.name);
		}
		for (POutput out: outputs) {
			System.out.println("Output arc "+ out.transition.name + " to " + out.place.name);
		}
	}
	public String toSnakes() {
		throwIfNotBuild();
		StringBuilder builder = new StringBuilder();
		builder.append("from typing import Dict, Any\n"
				+ "import snakes.plugins, json\n"
				+ "snakes.plugins.load('gv', 'snakes.nets', 'nets')\n"
				+ "from nets import PetriNet, Place, Transition, Variable, Expression, MultiArc, Value\n"
				+ "\n"
				+ "class Variables:\n"
				+ "    def __init__(self, variables):\n"
				+ "        for key in variables.keys():\n"
				+ "            setattr(self, key, variables[key])\n"
				+ "\n"
				+ "    def copy(self):\n"
				+ "        if hasattr(self, \"g\"):\n"
				+ "            v = Variables({\"g\" : self.g.copy() if self.g != None else None, \n"
				+ "                \"l\": self.l.copy() if self.l != None else None})\n"
				+ "        else:\n"
				+ "            v = Variables(json.loads(json.dumps(vars(self))))\n"
				+ "\n"
				+ "        return v\n"
				+ "\n"
				+ "    def combine(self, g, l):\n"
				+ "        return Variables({\"g\" : g.copy(), \"l\": l.copy() if l != None else None})\n"
				+ "\n"
				+ "    def globals(self):\n"
				+ "        return self.g.copy()\n"
				+ "\n"
				+ "    def e(self, expr):\n"
				+ "        l = self.l.copy() if self.l != None else None\n"
				+ "        g = self.g.copy()\n"
				+ "        exec(expr)\n"
				+ "        return Variables({\"g\" : g, \"l\": l})\n"
				+ "\n"
				+ "    def eval(self, expr):\n"
				+ "        l = self.l.copy() if self.l != None else None\n"
				+ "        g = self.g.copy()\n"
				+ "        return eval(expr)\n"
				+ "\n"
				+ "    def er(self, expr):\n"
				+ "        v = Variables({\"g\" : self.g, \"l\": self.l})\n"
				+ "        if hasattr(self, 'r'): v.r = self.r\n"
				+ "        exec(\"v.r %s\" % expr)\n"
				+ "        return v\n"
				+ "\n"
				+ "    def __repr__(self):\n"
				+ "        v = vars(self)\n"
				+ "        return \"{%s}\" % ','.join([\"%s:%s\" % (k, v[k]) for k in v.keys()])\n"
				+ "\n"
				+ "    def __hash__(self):\n"
				+ "        return hash((self.g, self.l)) if hasattr(self, \"g\") else id(self)\n"
				+ "\n"
				+ "    def __eq__(self, obj):\n"
				+ "        return self.l == obj.l and self.g == obj.g if hasattr(self, \"g\") else super.__eq__(self, obj)\n"
				+ "\n"
				+ "def net():\n"
				+ "    n = PetriNet(\"N\")\n"
				+ "\n"
				+ "    def add_place(place: Place, meta: Dict[str, Any]):\n"
				+ "        n.add_place(place)\n"
				+ "        place.meta = meta\n"
				+ "\n"
				+ "    def add_transition(transition: Transition, meta: Dict[str, Any]):\n"
				+ "        n.add_transition(transition)\n"
				+ "        transition.meta = meta\n"
				+ "\n"
				+ "\n\n");
		
		Function<String, String> variablePrefix = (String variable) ->  globalVariables.contains(variable) ? "g." : "l.";
		
		builder.append("    # Variables\n");
		
		builder.append("\n    # Init\n");

		builder.append("\n    # Places\n");
		places.values().forEach(p -> builder.append("    " + p.toSnakes()));
		
		builder.append("\n    # Transitions\n");
		transitions.values().forEach(p -> builder.append("    " + p.toSnakes(variablePrefix, 
				this.inputs.stream().filter(i -> i.transition == p).collect(Collectors.toList()))));
		
		builder.append("\n    # Inputs \n");
		inputs.forEach(p -> builder.append("    " + p.toSnakes(variablePrefix)));
		
		builder.append("\n    # Outputs\n");
		outputs.forEach(p -> builder.append("    " + p.toSnakes(variablePrefix)));
		builder.append("    return n\n");
		builder.append("net().draw('test-gv-dot.png', engine='dot')");
		
		return builder.toString();
	}
	
	public PPetriNet outputScript() throws IOException {
		String script = this.toSnakes();
		FileWriter snakesWriter = new FileWriter("output\\petrinet.py");
		snakesWriter.write(script);
		//String json = PythonInterpreter.execute(script);
		//executionResult = new Gson().fromJson(json, JsonObject.class);
		snakesWriter.close();
		return this;
	}
}
