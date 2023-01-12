package bpmn.workflow.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataStoreReference;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.InclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;

public class BPMNElementsParser {

	public void logInfo(String str) { System.out.println(str); }
	
	public StartEvent getStartElement(BpmnModelInstance modelInst) {
		Collection<StartEvent> se = modelInst.getModelElementsByType(StartEvent.class);
		if(se.size() == 1) return se.iterator().next();
		else if(se.size() > 1) logInfo("[ERROR] More than one start element in BPMN model");
		return null;
	}
	
	public Collection<ServiceTask> getServiceTask(BpmnModelInstance modelInst) {
		return modelInst.getModelElementsByType(ServiceTask.class);
	}
	
	public Collection<Task> getTask(BpmnModelInstance modelInst) {
		return modelInst.getModelElementsByType(Task.class);
	}
	
	public Collection<EndEvent> getEndEvents(BpmnModelInstance modelInst) {
		return modelInst.getModelElementsByType(EndEvent.class);		
	}
	
	public Collection<ParallelGateway> getParallelGateWay(BpmnModelInstance modelInst) {
		return modelInst.getModelElementsByType(ParallelGateway.class);
	}
	
	public Collection<ExclusiveGateway> getExclusiveGateWay(BpmnModelInstance modelInst) {
		return modelInst.getModelElementsByType(ExclusiveGateway.class);
	}
	
	public Collection<InclusiveGateway> getInclusiveGateWay(BpmnModelInstance modelInst) {
		return modelInst.getModelElementsByType(InclusiveGateway.class);
	}
	
	public Collection<DataStoreReference> getDataStoreReference(BpmnModelInstance modelInst) {
		return modelInst.getModelElementsByType(DataStoreReference.class);
	}
	
	// given id, get data store ref name
	public String getDataStoreName(BpmnModelInstance modelInst, String id) {
		Collection<DataStoreReference> dsRefs = getDataStoreReference(modelInst);
		for(DataStoreReference ref : dsRefs) {
			if(ref.getId().equals(id))
				return ref.getName();
		}
		return new String();
	}
	
	public boolean checkBoundarySignal(BpmnModelInstance modelInst, String id) {
		Collection<BoundaryEvent> BEList = modelInst.getModelElementsByType(BoundaryEvent.class);
		for(BoundaryEvent be : BEList) {
			Collection<SequenceFlow> beOutgoing = be.getOutgoing();
			for(SequenceFlow beO : beOutgoing) {
				if(beO.getId().equals(id)) return true;
			}
			Collection<SequenceFlow> beIncoming = be.getIncoming();
			for(SequenceFlow beI : beIncoming) {
				if(beI.getId().equals(id)) return true;
			}
		}
		return false;
	}
	
	// given task, get outgoing signal event
	public List<String> getOutSignalEventOfTask(BpmnModelInstance modelInst, Task t) {
		List<String> outSignalList = new ArrayList<String>();
		
		Collection<SequenceFlow> sfOutgoing = t.getOutgoing();
		Collection<SequenceFlow> sfIncoming = t.getIncoming();
		
		for(SequenceFlow sf : sfOutgoing) {
			if(checkBoundarySignal(modelInst, sf.getId())) {
				logInfo("out signal of task " + t.getName() + " has source: " + sf.getSource().getName());
				logInfo("out signal of task " + t.getName() + "has target: " + sf.getTarget().getName());
			}
		}
		
		for(SequenceFlow sf : sfIncoming) {
			if(checkBoundarySignal(modelInst, sf.getId())) {
				
				logInfo("in signal of task " + t.getName() + " has source: " + sf.getSource());
				logInfo("in signal of task " + t.getName() + " has target: " + sf.getTarget());
			}
		}

		return outSignalList;
	}
	
	// given task, get incoming signal event
	public List<String> getInSignalEventOfTask(BpmnModelInstance modelInst, Task t) {
		List<String> inSignalList = new ArrayList<String>();
		
		return inSignalList;		
	}

	// Given service task, get input data stores
	public List<String> getInputDataStoresOfServiceTask(BpmnModelInstance modelInst, ServiceTask st) {
		List<String> dataStores = new ArrayList<String>();
		for(DataInputAssociation ia : st.getDataInputAssociations()) {
			for(ItemAwareElement iae : ia.getSources()) {
				dataStores.add(getDataStoreName(modelInst, iae.getId()));
			}
		}
		if(dataStores.size() > 0 ) 
			logInfo(" Service Task: " + st.getName() + 
				" has DATA STORES: " + dataStores);
		return dataStores;
	}
	
	// Given service task, get output data stores
	public List<String> getOutputDataStoresOfServiceTask(BpmnModelInstance modelInst, ServiceTask st) {
		List<String> dataStores = new ArrayList<String>();
		for(DataOutputAssociation ia : st.getDataOutputAssociations()) {
			for(ItemAwareElement iae : ia.getSources()) {
				dataStores.add(getDataStoreName(modelInst, iae.getId()));
			}
		}
		if(dataStores.size() > 0 ) 
			logInfo(" Service Task: " + st.getName() + 
				" has DATA STORES: " + dataStores);
		return dataStores;
	}
}
