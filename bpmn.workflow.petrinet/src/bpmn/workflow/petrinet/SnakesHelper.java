package bpmn.workflow.petrinet;

import java.util.ArrayList;
import java.util.List;

import bpmn.workflow.taskgraph.Edge;
import bpmn.workflow.taskgraph.TaskType;
import bpmn.workflow.taskgraph.Vertex;

class SnakesHelper {
	static String name(String prefix, String name) {
		return String.format("%s_%s", prefix, name);
	}
	
	static List<String> parameters() {
		
		return new ArrayList<String>();
	}
	
	static String nameGatewayTransition(Vertex v, String task) {
		switch (v.getType()) {
		case PAR_JOIN_GATE:
			return SnakesHelper.name("Join", v.getName());//Transition name: TJoin_NameOfGateway
		case PAR_SPLIT_GATE:
			return SnakesHelper.name("Fork", v.getName());//Transition name: TFork_NameOfGateway
		case EXOR_JOIN_GATE:
			return SnakesHelper.name(task, v.getName());//Transition name:T_NameOfFormerTask_NameOfGateway
		case EXOR_SPLIT_GATE:
			return SnakesHelper.name(v.getName(), task);//Transition name:T_NameOfGateway_NameOfLatterTask
		default:
			break;
		}
		return null;	
	}
}
