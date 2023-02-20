package bpmn.workflow.taskgraph;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataType {
	String name = new String();
	Map<String, String> parameters = new LinkedHashMap<String, String>();
	
	public DataType(String _name){
		name = _name;
	}
	
	public void addParameter(String key, String type) {
		parameters.put(key, type);
	}
}
