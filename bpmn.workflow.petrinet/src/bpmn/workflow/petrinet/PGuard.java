package bpmn.workflow.petrinet;

import java.util.List;
import java.util.function.Function;

import bpmn.workflow.petrinet.PPlace.PPlaceType;

class PGuard {
	private String expression = "";
	private boolean inverse = false;
	
	enum PRepeatGuardType {MIN, MAX}
	private PRepeatGuardType repeatGuardType = null;
	private Long repeatGuardValue = null;
	
	PGuard(String expression) { this(expression, false); }
	
	PGuard(String expression, boolean inverse) {
		assert expression != "";
		this.expression = expression;
		this.inverse = inverse;
	}
	
	PGuard(PRepeatGuardType repeatGuardType, Long repeatGuardValue) {
		this.repeatGuardType = repeatGuardType;
		this.repeatGuardValue = repeatGuardValue;
	}
	
	String toSnakes(Function<String, String> variablePrefix, List<PInput> inputs) {
		if (expression != "") {
			Function<String, String> variablePrefix2 = (String variable) -> {
				boolean inTransition = inputs.stream().anyMatch(i -> i.place.type == PPlaceType.EDGE || i.place.type == PPlaceType.CLAUSE);
				String prefix = inTransition ? "gl." : "";
				return prefix + variablePrefix.apply(variable);
			};
			
			String expr = expression + variablePrefix2;
			if (inverse) expr = String.format("not (%s)", expr);
			return expr;
		} else {
			String operation = repeatGuardType == PRepeatGuardType.MAX ? "<" : ">=";
			return String.format("gl.r %s %d", operation, repeatGuardValue);
		}
	}
}
