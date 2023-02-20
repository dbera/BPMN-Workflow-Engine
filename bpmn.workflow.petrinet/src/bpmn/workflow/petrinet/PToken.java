package bpmn.workflow.petrinet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class PToken {
	
	private final List<String> variables;
	private final Map<String, String> values;
	
	PToken() {
		this.variables = null;
		this.values = null;
	}
	
	PToken(List<String> variables) {
		this.variables = variables;
		this.values = null;
	}
	
	PToken(Map<String, String> values) {
		this.variables = null;
		this.values = values;
	}
	
	String toSnakes() {
		if (this.variables != null) {
			String variables = this.variables.stream().map(s -> String.format("'%s': %s", s, s))
					.collect(Collectors.joining(","));
			return String.format("Variables({%s})", variables);
		} else if (this.values != null) {
			String variables = this.values.entrySet().stream()
					.map(s -> String.format("'%s': %s", s.getKey(), s.getValue()))//To be checked
					.collect(Collectors.joining(","));
			return String.format("Variables({%s})", variables);
		} else {
			return "''";
		}
	}
}
