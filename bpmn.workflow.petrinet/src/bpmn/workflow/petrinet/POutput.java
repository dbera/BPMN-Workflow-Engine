package bpmn.workflow.petrinet;

import java.util.function.Function;

class POutput {
	enum RepeatAction { INIT, INCREASE, SET_1 }
	
	final PPlace place;
	final PTransition transition;
	String label;
	RepeatAction repeatAction = null;
	
	POutput(PPlace place, PTransition transition, String label) {
		this.place = place;
		this.transition = transition;
		this.label = label;
	}
	
	String toSnakes(Function<String, String> variablePrefix) {
		String var = "Variable('t')";
		if (!label.equals("")) {
			if (label.contains(",")) {
				String variables = "";
				for(String s: label.split(",")) {
					variables += "Variable('"+ s.trim()+ "'), ";
				}
				variables = variables.substring(0, variables.length()-2);
				var = "MultiArc(["+ variables +"])";
			} else {
				var = "Variable('"+ label + "')";
			}
		}
		String exec = "";
		
		if (repeatAction != null) {
			if (repeatAction == RepeatAction.INIT) exec += ".er('= 0')";
			else if (repeatAction == RepeatAction.INCREASE) exec += ".er('+= 1')";
			else if (repeatAction == RepeatAction.SET_1) exec += ".er('= 1')";
		}
		
//		if (place.type == PPlace.PPlaceType.EVENT && exec.equals("")) {
//			label = "Variable('l')";
//		} else if (place.type == PPlace.PPlaceType.EDGE && exec.equals("")) {
//			String locals = transition.event != null ? "l" : "None";
//			label = String.format("Expression('g.combine(g, %s)')", locals);
//		} /*else if (place.type == PPlace.PPlaceType.VARIABLES) {
//			label = String.format("Expression(\"gl%s.globals()\")", exec);
//		} else if (place.type == PPlace.PPlaceType.STATE) {
//			label = String.format("Expression(\"''\")", exec);
//		}*/else if (place.type == PPlace.PPlaceType.CLAUSE) {
//			if (!exec.equals("")) {
//				label = String.format("Expression(\"gl%s\")", exec);
//			} else {
//				label = "Variable('gl')";
//			}
//		} else {
//			assert false : "Should not happen";
//		}
		
		return String.format("n.add_output('%s', '%s', %s)\n", 
				place.name, transition.getSnakesName(variablePrefix), var);
	}
}
