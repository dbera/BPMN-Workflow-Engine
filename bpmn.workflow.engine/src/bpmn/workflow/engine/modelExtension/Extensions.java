package bpmn.workflow.engine.modelExtension;

import java.util.Collection;

import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;

public interface Extensions extends BpmnModelElementInstance {
	Collection<Extension> getExtension();
}
