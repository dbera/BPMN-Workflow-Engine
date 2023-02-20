package bpmn.workflow.petrinet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import bpmn.workflow.taskgraph.Edge;
import bpmn.workflow.taskgraph.Vertex;

class PPlace {
	
	enum PPlaceType { STARTEVENT, EVENT, ENDEVENT, EDGE, CLAUSE, PARAMETERS }
	
	final String name;
	final PPlaceType type;
	static int index = 0;
	
	private final List<PToken> tokens = new ArrayList<PToken>();
	
	private PPlace(String name, PPlaceType type) {
		this.name = name;
		this.type = type;
	}
	
	private static String name(String prefix, String name) {
		return String.format("%s_%s", prefix, name);
	}
	
	static PPlace forVertex(Vertex v) {
		switch (v.getType()) {
		case START_EVENT :
			return new PPlace(v.getName(), PPlaceType.STARTEVENT);
		case MSG_EVENT:
			return new PPlace(v.getName(), PPlaceType.EVENT);
		case TIMER_EVENT:
			return new PPlace(v.getName(), PPlaceType.EVENT);//TODO: should be a transition in Twinscan
		case SIG_EVENT:
			return new PPlace(v.getName(), PPlaceType.EVENT);
		case END_EVENT:
			return new PPlace(v.getName(), PPlaceType.ENDEVENT);
		default:
			return new PPlace(v.getName(), PPlaceType.EVENT);
		}
	}
	
	static PPlace forEdge(Edge e) {
		String name = name("E_" + e.getSrcName(), e.getDstName());
		return new PPlace(name, PPlaceType.EDGE);
	}
	
	static PPlace forOutputEdge(Edge e) {
		String name = name("Task_Out", e.getDstName());
		index++;
		return new PPlace(name, PPlaceType.EDGE);
	}
	
	boolean isInitial() {
		return this.type == PPlaceType.STARTEVENT;
	}
	
	void addToken(PToken token) {
		this.tokens.add(token);
	}
	
	String toSnakes() {
		String token = "";
		if (!this.tokens.isEmpty()) {
			token = String.format(", [%s]", this.tokens.stream()
					.map(t -> t.toSnakes()).collect(Collectors.joining(", ")));
		}
		
		String meta = String.format("'type': '%s'", this.type.toString().toLowerCase());
		return String.format("add_place(Place('%s'%s), {%s})\n", name, token, meta);
	}
}
