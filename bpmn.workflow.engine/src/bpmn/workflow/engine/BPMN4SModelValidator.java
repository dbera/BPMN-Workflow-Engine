package bpmn.workflow.engine;

import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataStoreReference;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.xml.ModelInstance;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;



public class BPMN4SModelValidator {
	
	private static Collection<Task> tasks;
	private static Collection<IntermediateCatchEvent> cEvents;
	private static Collection<DataStoreReference> dsRefs;
	private static Collection<DataObjectReference> doRefs;
	private static Collection<ExclusiveGateway> exGates;
	private static Collection<ParallelGateway> parGates;
	private static Collection<SubProcess> subProcess; 
	private static Map<String, SubProcess> components;

	public static boolean validate (BpmnModelInstance modelInst) {
		boolean veredict = true;
		BPMNParser parser = new BPMNParser();
		tasks = parser.getTask(modelInst);
		cEvents = parser.getCatchEvents(modelInst);
		dsRefs = parser.getDataStoreReference(modelInst);
		doRefs = parser.getDataObjectReference(modelInst);
		exGates = parser.getExclusiveGateWay(modelInst);
		parGates = parser.getParallelGateWay(modelInst);
		subProcess = parser.getSubProcesses(modelInst);
		
		components = new HashMap<String, SubProcess>();
		for(SubProcess sp: subProcess) {
			if(isTopLevel(sp)) {
				logInfo("Adding component " + sp.getName());
				components.put(sp.getName(), sp);
			}
		}
		
		veredict = veredict && validateNoDanglingInputOutput();
		return veredict;		
	}
	
	private static Boolean isTopLevel (ModelElementInstance elemInst) {
		return elemInst.getParentElement().getElementType().getTypeName() == "process";
	}
	
	public static void logInfo(String str) { System.out.println(str); }	
	
	public static void logError(String str) { System.out.println(str); }	

	/**
	 * [REQ-001]
	 * validateNoDanglingInputOutput
	 * At the top level of the model input and output data 
	 * as well as message queues should have both a source 
	 * and a target.
	 * @return true if validation passes, false otherwise.
	 */
	private static boolean validateNoDanglingInputOutput () {
		Set<String> inputs = new HashSet<String>();
		Set<String> outputs = new HashSet<String>();
		for(SubProcess comp: components.values()) {
			Collection<DataInputAssociation> diaList =  comp.getDataInputAssociations();
			Collection<DataOutputAssociation> doaList =  comp.getDataOutputAssociations();
			Collection<SequenceFlow> sfiList = comp.getIncoming();
			Collection<SequenceFlow> sfoList = comp.getOutgoing();
			for (DataInputAssociation dia: diaList) {
				for (ItemAwareElement src: dia.getSources()) {
					inputs.add(src.getName());
				}
			}
			for (DataOutputAssociation doa: doaList) {
				outputs.add(doa.getTarget().getName());
			}
			for (SequenceFlow sfi: sfiList) {
				if ( sfi.getSource() instanceof IntermediateCatchEvent ) {
					inputs.add(sfi.getSource().getName());
				}
			}
			for (SequenceFlow sfo: sfoList) {
				if ( sfo.getTarget() instanceof IntermediateCatchEvent ) {
					outputs.add(sfo.getTarget().getName());
				}
			}
		}
		Set<String> diff = new HashSet<String>(inputs);
		diff.removeAll(outputs);
		outputs.removeAll(inputs);
		diff.addAll(outputs);
		if (diff.isEmpty()) {
			return true;
		} else {
			logError("ERROR: Invalid Model. REQ-001 Dangling data elements: " + String.join(", ", diff) + ".");
			return false;
		}
	}
	
	
}
