package bpmn.workflow.engine.modelExtension;

import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;

public interface Type extends BpmnModelElementInstance {
	Extensions getExtensions();
	String getType();
	String getName();
}
