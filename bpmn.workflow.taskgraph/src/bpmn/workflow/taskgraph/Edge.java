package bpmn.workflow.taskgraph;

public class Edge {
	String srcName = new String();
	String dstName = new String();
	String expression = new String();
	
	Edge(String _srcName, String _dstName, String _expression){
		srcName = _srcName;
		dstName = _dstName;
		expression = _expression;
	}
	
	public String getExpression() {
		return expression;
	}
	
	public String getSrcName() {
		return srcName;
	}
	
	public String getDstName() {
		return dstName;
	}
}
