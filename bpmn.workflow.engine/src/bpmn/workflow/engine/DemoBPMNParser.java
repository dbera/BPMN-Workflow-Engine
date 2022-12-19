package bpmn.workflow.engine;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataStoreReference;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.Task;

import bpmn.workflow.engine.taskgraph.Patterns;
import bpmn.workflow.engine.taskgraph.TaskType;
import bpmn.workflow.engine.taskgraph.Vertex;
public class DemoBPMNParser {
	
	static List<Patterns> patternList = new ArrayList<Patterns>();
	static boolean expanded = false;//if true then all layers are printed to one task graph, false(print for each layer)

	public static void logInfo(String str) { System.out.println(str); }

	public static void main(String[] args) {
		
		BPMNElementsParser parser = new BPMNElementsParser();
        BpmnModelInstance modelInst;
        try {
        	URL resource = DemoBPMNParser.class.getClassLoader().getResource("diagram_subP.bpmn");
        	File file = new File(resource.toURI());
        	modelInst = Bpmn.readModelFromFile(file);
        	logInfo(modelInst.getModel().getModelName());
        	
        	parseBPMN(modelInst);
        	for(Patterns p : patternList) {
        		String dot = p.generateDot();
            	logInfo(dot);
        	}
        	/*StartEvent se = parser.getStartElement(modelInst);
       		if(se != null) logInfo("StartEvent: " + se.getName());
       		
       		Collection<Task> tList = parser.getTask(modelInst);
       		tList.forEach(st -> logInfo("Task: " + st.getName()));
       		for(Task st: tList) {
       			parser.getOutSignalEventOfTask(modelInst, st);
       			//parser.getInputDataStoresOfServiceTask(modelInst, st);
       			//parser.getOutputDataStoresOfServiceTask(modelInst, st);
       		}
       		
       		Collection<ServiceTask> stList = parser.getServiceTask(modelInst);
       		stList.forEach(st -> logInfo("ServiceTask: " + st.getName()));
       		for(ServiceTask st: stList) {
       			parser.getInputDataStoresOfServiceTask(modelInst, st);
       			parser.getOutputDataStoresOfServiceTask(modelInst, st);
       		}
       		
       		Collection<InclusiveGateway> iGList = parser.getInclusiveGateWay(modelInst);
       		iGList.forEach(ig -> logInfo("Inclusive Gateway: " + ig.getName()));
       		for(InclusiveGateway ig : iGList) {
       			ig.getSucceedingNodes().list().forEach(elm -> logInfo(" Successor: " + elm.getName()));
       		}*/
        } catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void parseBPMN(BpmnModelInstance modelInst) {
		BPMNParser parser = new BPMNParser();
   		if(expanded) {
   			getExpandedPatterns(modelInst);
   		} else {
   			getCollapsedPatterns(modelInst);
   			Collection<SubProcess> subProcessList = parser.getSubProcesses(modelInst);
   			for(SubProcess sp: subProcessList) {
   				getSubPatterns(sp);
   			}
   		}
	}
	
	private static void getSubPatterns(SubProcess sp) {
		Patterns p = new Patterns();
		p.name = sp.getName().replace(" ", "_");
		Collection<Task> taskList = sp.getChildElementsByType(Task.class);
		Collection<ServiceTask> serviceTaskList =  sp.getChildElementsByType(ServiceTask.class);
		Collection<BoundaryEvent> beList = sp.getChildElementsByType(BoundaryEvent.class);
		Collection<IntermediateCatchEvent> ceList = sp.getChildElementsByType(IntermediateCatchEvent.class);
		Collection<IntermediateThrowEvent> teList = sp.getChildElementsByType(IntermediateThrowEvent.class);
		Collection<DataStoreReference> dsRefs = sp.getChildElementsByType(DataStoreReference.class);
		Collection<DataObjectReference> doRefs = sp.getChildElementsByType(DataObjectReference.class);
		Collection<ExclusiveGateway> exGateList = sp.getChildElementsByType(ExclusiveGateway.class);
		Collection<ParallelGateway> parGateList = sp.getChildElementsByType(ParallelGateway.class);
		
		//add vertices of type task (activity, gateway, timer/signals/messages), data reference
		for(Task t : taskList) {
			if(t.getName() != null) {
				parseTasks(t, p);
			}
		}
		
		for(ServiceTask t : serviceTaskList) {
			if(t.getName() != null) {
				parseServiceTask(t, p);
			}
		}
		for(ExclusiveGateway eg : exGateList) {
			parseGateway(eg, p);
		}
		
		for(ParallelGateway pg : parGateList) {
			parseGateway(pg, p);
		}
		
		for(BoundaryEvent be : beList) {
			parseEvent(be, p);
		}
		
		for(IntermediateCatchEvent ce: ceList) {
			parseEvent(ce, p);
		}
		
		for(IntermediateThrowEvent te: teList) {
			parseEvent(te, p);
		}
		patternList.add(p);
	}
	
	private static void getCollapsedPatterns(BpmnModelInstance modelInst) {
		BPMNParser parser = new BPMNParser();
		Patterns p = new Patterns();
		Collection<Task> taskList = parser.getTask(modelInst);
		Collection<ServiceTask> serviceTaskList = parser.getServiceTask(modelInst);
		Collection<BoundaryEvent> beList = parser.getBoundaryEvents(modelInst);
		Collection<IntermediateCatchEvent> ceList = parser.getCatchEvents(modelInst);
		Collection<IntermediateThrowEvent> teList = parser.getThrowEvents(modelInst);
		Collection<DataStoreReference> dsRefs = parser.getDataStoreReference(modelInst);
		Collection<DataObjectReference> doRefs = parser.getDataObjectReference(modelInst);
		Collection<ExclusiveGateway> exGateList = parser.getExclusiveGateWay(modelInst);
		Collection<ParallelGateway> parGateList = parser.getParallelGateWay(modelInst);
		//add vertices of type task (activity, gateway, timer/signals/messages), data reference
		for(Task t : taskList) {
			if(t.getName() != null && t.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseTasks(t, p);
			}
		}
		
		for(ServiceTask t : serviceTaskList) {
			if(t.getName() != null && t.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseServiceTask(t, p);
			}
		}
		for(ExclusiveGateway eg : exGateList) {
			if(eg.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseGateway(eg, p);
			}
		}
		
		for(ParallelGateway pg : parGateList) {
			if(pg.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseGateway(pg, p);
			}
		}
		
		for(BoundaryEvent be : beList) {
			if(be.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseEvent(be, p);
			}
		}
		
		for(IntermediateCatchEvent ce: ceList) {
			if(ce.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseEvent(ce, p);
			}
		}
		
		for(IntermediateThrowEvent te: teList) {
			if(te.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseEvent(te, p);
			}
		}
		patternList.add(p);
	}

	private static void getExpandedPatterns(BpmnModelInstance modelInst) {
		BPMNParser parser = new BPMNParser();
		Patterns p = new Patterns();
		Collection<Task> taskList = parser.getTask(modelInst);
		Collection<ServiceTask> serviceTaskList = parser.getServiceTask(modelInst);
		Collection<BoundaryEvent> beList = parser.getBoundaryEvents(modelInst);
		Collection<IntermediateCatchEvent> ceList = parser.getCatchEvents(modelInst);
		Collection<IntermediateThrowEvent> teList = parser.getThrowEvents(modelInst);
		Collection<DataStoreReference> dsRefs = parser.getDataStoreReference(modelInst);
		Collection<DataObjectReference> doRefs = parser.getDataObjectReference(modelInst);
		Collection<ExclusiveGateway> exGateList = parser.getExclusiveGateWay(modelInst);
		Collection<ParallelGateway> parGateList = parser.getParallelGateWay(modelInst);
		//add vertices of type task (activity, gateway, timer/signals/messages), data reference
		for(Task t : taskList) {
			if(t.getName() != null) {
				parseTasks(t, p);
			}
		}
		
		for(ServiceTask t : serviceTaskList) {
			if(t.getName() != null) {
				parseServiceTask(t, p);
			}
		}
		for(ExclusiveGateway eg : exGateList) {
			parseGateway(eg, p);
		}
		
		for(ParallelGateway pg : parGateList) {
			parseGateway(pg, p);
		}
		
		for(BoundaryEvent be : beList) {
			parseEvent(be, p);
		}
		
		for(IntermediateCatchEvent ce: ceList) {
			parseEvent(ce, p);
		}
		
		for(IntermediateThrowEvent te: teList) {
			parseEvent(te, p);
		}
		patternList.add(p);
	}
	
	private static void parseGateway(Gateway gw, Patterns p) {
		String name = "gateway";
		if(gw.getName() != null) {
			name = gw.getName();
		} else {
			name = gw.getId();
		}
		Vertex v = new Vertex(name);
		if (gw instanceof ExclusiveGateway) {
			v.setType(TaskType.EXOR_JOIN_GATE);
		} else if (gw instanceof ParallelGateway) {
			v.setType(TaskType.PAR_JOIN_GATE);
		}
		
		Collection<SequenceFlow> sfiList = gw.getIncoming();
		Collection<SequenceFlow> sfoList = gw.getOutgoing();
		
		for (SequenceFlow sf: sfiList) {
			v.addIncomingEdge(getElementName(sf.getSource()), "");
			p.addEdge(getElementName(sf.getSource()), name, "");
		}
		
		for (SequenceFlow sf: sfoList) {
			v.addOutgoingEdge(getElementName(sf.getTarget()), "");
			//p.addEdge(gw.getName(), sf.getTarget().getName(), sf.getName());
		}
		p.addVertex(v);
	}
	
	private static void parseEvent(Event evt, Patterns p) {
		String name = "event";
		if(evt.getName() != null) {
			name = evt.getName();
		} else {
			name = evt.getId();
		}
		Vertex v = new Vertex(name);
		if(evt instanceof BoundaryEvent) {
			v.setType(TaskType.BOUNDARY_EVENT);
		} else if (evt instanceof IntermediateCatchEvent) {
			v.setType(TaskType.CATCH_EVENT);
		} else if (evt instanceof IntermediateThrowEvent) {
			v.setType(TaskType.THROW_EVENT);
		}
		Collection<SequenceFlow> sfiList = evt.getIncoming();
		Collection<SequenceFlow> sfoList = evt.getOutgoing();
		
		for (SequenceFlow sf: sfiList) {
			v.addIncomingEdge(getElementName(sf.getSource()), "");
			p.addEdge(getElementName(sf.getSource()), name, "");
		}
		
		for (SequenceFlow sf: sfoList) {
			v.addOutgoingEdge(getElementName(sf.getTarget()), "");
			//p.addEdge(evt.getName(), sf.getTarget().getName(), "");
		}
		p.addVertex(v);
	}
	
	private static void parseServiceTask(ServiceTask t, Patterns p) {
		Vertex v = new Vertex(t.getName().replace(" ", "_"), TaskType.SERVICE_TASK);
		Collection<SequenceFlow> sfiList = t.getIncoming();
		Collection<SequenceFlow> sfoList = t.getOutgoing();
		Collection<DataInputAssociation> diaList =  t.getDataInputAssociations();
		Collection<DataOutputAssociation> doaList =  t.getDataOutputAssociations();
		
		for (SequenceFlow sf: sfiList) {
			v.addIncomingEdge(getElementName(sf.getSource()), "");
			p.addEdge(getElementName(sf.getSource()), t.getName().replace(" ", "_"), "");
		}
		
		for (SequenceFlow sf: sfoList) {
			v.addOutgoingEdge(getElementName(sf.getTarget()), "");
			//p.addEdge(t.getName(), sf.getTarget().getName(), sf.getName());
		}
		
		for(DataInputAssociation dia : diaList) {
			String expression = "";
			if (dia.getAttributeValue("expression") != null) {
				expression = dia.getAttributeValue("expression");
			}
			for(ItemAwareElement elm : dia.getSources()) {
				v.addIncomingEdge(elm.getId(), expression);
				p.addEdge(elm.getId(), t.getName().replace(" ", "_"), expression);
				logInfo(" DIA: " + elm.getId());
			}
		}
		for(DataOutputAssociation doa : doaList) {
			String expression = "";
			if (doa.getAttributeValue("expression") != null) {
				expression = doa.getAttributeValue("expression");
			}
			v.addOutgoingEdge(doa.getTarget().getId(), expression);
			//p.addEdge(t.getName(), doa.getTarget().getId(), expression);
			logInfo(" DOA: " + doa.getTarget().getId());
		}
		p.addVertex(v);
	}
	
	private static void parseTasks(Task t, Patterns p) {
		Vertex v = new Vertex(t.getName().replace(" ", "_"), TaskType.TASK);
		Collection<SequenceFlow> sfiList = t.getIncoming();
		Collection<SequenceFlow> sfoList = t.getOutgoing();
		Collection<DataInputAssociation> diaList =  t.getDataInputAssociations();
		Collection<DataOutputAssociation> doaList =  t.getDataOutputAssociations();
		
		for (SequenceFlow sf: sfiList) {
			v.addIncomingEdge(getElementName(sf.getSource()), "");
			p.addEdge(getElementName(sf.getSource()), t.getName().replace(" ", "_"), "");
		}
		
		for (SequenceFlow sf: sfoList) {
			v.addOutgoingEdge(getElementName(sf.getTarget()), "");
			//p.addEdge(t.getName(), sf.getTarget().getName(), sf.getName());
		}
		
		for(DataInputAssociation dia : diaList) {
			String expression = "";
			if (dia.getAttributeValue("expression") != null) {
				expression = dia.getAttributeValue("expression");
			}
			for(ItemAwareElement elm : dia.getSources()) {
				v.addIncomingEdge(elm.getName(), expression);
				p.addEdge(elm.getName(), t.getName().replace(" ", "_"), expression);
				logInfo(" DIA: " + elm.getId());
			}
		}
		for(DataOutputAssociation doa : doaList) {
			String expression = "";
			if (doa.getAttributeValue("expression") != null) {
				expression = doa.getAttributeValue("expression");
			}
			v.addOutgoingEdge(doa.getTarget().getName(), expression);
			p.addEdge(t.getName().replace(" ", "_"), doa.getTarget().getName(), expression);
			logInfo(" DOA: " + doa.getTarget().getId());
		}
		p.addVertex(v);
	}
	
	private static String getElementName(FlowElement fele) {
		if(fele.getName() != null) {
			return fele.getName().replace(" ", "_");
		} else {
			return fele.getId();
		}
	}
}
