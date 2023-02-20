package bpmn.workflow.petrinet;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class PTransition {
	private static int nextID = 0;
	static void resetIDCounter() { nextID = 0; }
	
	final String name;
	final Object event;
	
	private final PGuard guard;
	
	PTransition(String name) { this(name, null, null); }
	
	//PTransition(Object event) { this(event, null); }
	
	PTransition(String name, PGuard guard) { this(name, null, guard); }
	
	PTransition(String name, Object event, PGuard guard) {
		this.name = "T" + name;
		this.event = event;
		this.guard = guard;
	}
	
	String getSnakesName(Function<String, String> variablePrefix) {
		String name = this.name;
		if (this.event != null) {
			name += String.format("_event_%s", event);
		}
		return name;
	}
	
	String toSnakes(Function<String, String> variablePrefix, List<PInput> inputs) {
		String expr = "";
		if (guard != null) {
			expr = String.format(", Expression('%s')", guard.toSnakes(variablePrefix, inputs));
		}
		
		String eventMeta = "";
		if (this.event != null) {
			String type = "";
			String parameters = SnakesHelper.parameters().stream().map(p -> String.format("'%s'", p)).collect(Collectors.joining(","));
			
			
			eventMeta = String.format(",'event': {'name': '%s', 'type': '%s', 'parameters': [%s]}", name, type, parameters);
		}
		
		String typeMeta = String.format("'type': '%s'", this.event != null ? "event" : "none");
		String meta = String.format("{%s%s}", typeMeta, eventMeta);
		return String.format("add_transition(Transition('%s'%s), %s)\n", getSnakesName(variablePrefix), expr, meta);
	}
}
