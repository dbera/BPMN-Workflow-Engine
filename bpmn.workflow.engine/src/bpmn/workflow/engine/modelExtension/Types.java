package bpmn.workflow.engine.modelExtension;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;

public interface Types extends BpmnModelElementInstance {
	Collection<Type> getType();
}
