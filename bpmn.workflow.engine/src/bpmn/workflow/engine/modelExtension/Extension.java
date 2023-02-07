package bpmn.workflow.engine.modelExtension;

import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;

public interface Extension extends BpmnModelElementInstance {
	String getKey();
	String getType();
}
