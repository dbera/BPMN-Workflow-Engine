package bpmn.workflow.petrinet;

import java.util.function.Function;

class PInput {
	
	final PPlace place;
	final PTransition transition;
	String label;
	
	PInput(PPlace place, PTransition transition, String label) {
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
//		if (place.type == PPlace.PPlaceType.EVENT) label = "Variable('t')";
//		else if (place.type == PPlace.PPlaceType.EVENT) label = "Variable('g')";
//		else if (place.type == PPlace.PPlaceType.ENDEVENT) label = "Variable('l')";
//		else if (place.type == PPlace.PPlaceType.EDGE || place.type == PPlace.PPlaceType.STARTEVENT) 
//			label = "Variable('gl')";
//		else throw new RuntimeException("Should not happen");
		
		return String.format("n.add_input('%s', '%s', %s)\n", 
				place.name, transition.getSnakesName(variablePrefix), var);
	}
}
