package bpmn.workflow.engine;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.Query;
import org.camunda.bpm.model.bpmn.instance.Activity;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObjectReference;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataStoreReference;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.ItemAwareElement;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import bpmn.workflow.engine.modelExtension.Extension;
import bpmn.workflow.engine.modelExtension.ExtensionImpl;
import bpmn.workflow.engine.modelExtension.Extensions;
import bpmn.workflow.engine.modelExtension.ExtensionsImpl;
import bpmn.workflow.engine.modelExtension.Type;
import bpmn.workflow.engine.modelExtension.TypeImpl;
import bpmn.workflow.engine.modelExtension.Types;
import bpmn.workflow.engine.modelExtension.TypesImpl;
import bpmn.workflow.engine.taskgraph.Component;
import bpmn.workflow.engine.taskgraph.DataType;
import bpmn.workflow.engine.taskgraph.Patterns;
import bpmn.workflow.engine.taskgraph.TaskType;
import bpmn.workflow.engine.taskgraph.Vertex;
public class DemoBPMNParser {
	
	static List<Patterns> patternList = new ArrayList<Patterns>();
	static boolean flat = true;//if true then all layers are printed to one task graph, false(print for each layer)

	public static void logInfo(String str) { System.out.println(str); }

	public static void main(String[] args) {
		
        BpmnModelInstance modelInst;
        try {
        	URL resource = DemoBPMNParser.class.getClassLoader().getResource("simpleDiagram.bpmn");
        	File file = new File(resource.toURI());
        	modelInst = Bpmn.readModelFromFile(file);
        	logInfo(modelInst.getModel().getModelName());
        	doRegister();
        	parseBPMN(modelInst);
        	for(Patterns p : patternList) {
        		String fab = p.generateFabSpec();
        		String types = p.generateFabSpecTypes();
        		logInfo(fab);
        		logInfo(types);
        	}

        } catch (Exception e) { e.printStackTrace(); }
	}
	
	private static void doRegister() {
		TypesImpl.registerType(Bpmn.INSTANCE.getBpmnModelBuilder());
		TypeImpl.registerType(Bpmn.INSTANCE.getBpmnModelBuilder());
		ExtensionsImpl.registerType(Bpmn.INSTANCE.getBpmnModelBuilder());
		ExtensionImpl.registerType(Bpmn.INSTANCE.getBpmnModelBuilder());
	}
	
	public static void parseBPMN(BpmnModelInstance modelInst) {
		BPMNParser parser = new BPMNParser();
   		if(flat) {
   			getFlatPatterns(modelInst);
   		} else {
   			getMultiLayerPatterns(modelInst);
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
		Collection<SubProcess> subProcessList = sp.getChildElementsByType(SubProcess.class);
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
		
		for(SubProcess subp: subProcessList) {
			if(subp.getName() != null) {
				parseSubProcess(subp, p);
			}
		}
		for(ExclusiveGateway eg : exGateList) {
			parseGateway(eg, p);
		}
		
		for(ParallelGateway pg : parGateList) {
			parseGateway(pg, p);
		}
		
		for(BoundaryEvent be : beList) {
			parseBoundaryEvent(be, p);
		}
		
		for(IntermediateCatchEvent ce: ceList) {
			parseEvent(ce, p);
		}
		
		for(IntermediateThrowEvent te: teList) {
			parseEvent(te, p);
		}
		
		for(DataStoreReference dsRef: dsRefs) {
			parseDataStoreRef(dsRef, p);
		}
		for(DataObjectReference doRef: doRefs) {
			parseDataObjectRef(doRef, p);
		}
		patternList.add(p);
	}
	
	private static void getMultiLayerPatterns(BpmnModelInstance modelInst) {
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
		Collection<SubProcess> subProcessList = parser.getSubProcesses(modelInst);
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
		for(SubProcess subp: subProcessList) {
			if(subp.getName() != null && subp.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseSubProcess(subp, p);
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
				parseBoundaryEvent(be, p);
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
		for(DataStoreReference dsRef: dsRefs) {
			if(dsRef.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseDataStoreRef(dsRef, p);
			}
		}
		for(DataObjectReference doRef: doRefs) {
			if(doRef.getParentElement().getElementType().getTypeName() != "subProcess") {
				parseDataObjectRef(doRef, p);
			}
		}
		patternList.add(p);
	}

	private static void getFlatPatterns(BpmnModelInstance modelInst) {
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
		Collection<SubProcess> subProcessList = parser.getSubProcesses(modelInst);
		Collection<StartEvent> startEvts = parser.getStartEvents(modelInst);
		Collection<EndEvent> endEvts = parser.getEndEvents(modelInst);
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
		
		for(SubProcess subp: subProcessList) {
			if(subp.getName() != null) {
				if (!containProcedure(subp)) {
					parseSubProcess(subp, p);
				} else {
					flatSubProcess(subp, p);
				}
			}
		}
		
		for(ExclusiveGateway eg : exGateList) {
			parseGateway(eg, p);
		}
		
		for(ParallelGateway pg : parGateList) {
			parseGateway(pg, p);
		}
		
		for(BoundaryEvent be : beList) {
			parseBoundaryEvent(be, p);
		}
		
		for(IntermediateCatchEvent ce: ceList) {
			parseEvent(ce, p);
		}
		
		for(IntermediateThrowEvent te: teList) {
			parseEvent(te, p);
		}
		
		for(DataStoreReference dsRef: dsRefs) {
			parseDataStoreRef(dsRef, p);
		}
		
		for(DataObjectReference doRef: doRefs) {
			parseDataObjectRef(doRef, p);
		}
		
		for(StartEvent se: startEvts) {
			parseEvent(se, p);
		}
		
		for(EndEvent ee: endEvts) {
			parseEvent(ee, p);
		}
		
		patternList.add(p);
	}
	
	private static void flatSubProcess(SubProcess subp, Patterns p) {
		Collection<SequenceFlow> sfiList = subp.getIncoming();
		Collection<SequenceFlow> sfoList = subp.getOutgoing();		
		Collection<DataInputAssociation> diaList =  subp.getDataInputAssociations();
		Collection<DataOutputAssociation> doaList =  subp.getDataOutputAssociations();
		
		for (SequenceFlow sf: sfiList) {
			if (sf.getSource() instanceof Task || sf.getSource() instanceof ServiceTask 
					|| sf.getSource() instanceof Gateway || sf.getSource() instanceof StartEvent) {
				Collection<StartEvent> seList = subp.getChildElementsByType(StartEvent.class);
				String expression = sf.getName()==null ? "": sf.getName();
				for(StartEvent se : seList) {
					p.addEdge(getElementName(sf.getSource()), getElementName(se), expression);
				}
			}
			
			if (sf.getSource() instanceof SubProcess) {
				Collection<EndEvent> eeList = sf.getSource().getChildElementsByType(EndEvent.class);
				Collection<StartEvent> seList = subp.getChildElementsByType(StartEvent.class);
				String expression = sf.getName()==null ? "": sf.getName();
				for(EndEvent ee : eeList) {
					for(StartEvent se : seList) {
						p.addEdge(getElementName(ee), getElementName(se), expression);
					}
				}
			}
		}
		
		for (SequenceFlow sf: sfoList) {
			if (sf.getTarget() instanceof Task || sf.getTarget() instanceof ServiceTask
					|| sf.getTarget() instanceof Gateway || sf.getTarget() instanceof EndEvent
					|| (isTopLevel(subp) && sf.getTarget() instanceof IntermediateCatchEvent)) {
				Collection<EndEvent> eeList = subp.getChildElementsByType(EndEvent.class);
				String expression = sf.getName()==null ? "": sf.getName();
				for(EndEvent eevt: eeList) {
					p.addEdge(getElementName(eevt), getElementName(sf.getTarget()), expression);
				}
			}
		}
		
		if (isTopLevel(subp)) { // add info about components
			Component tlcomp = new Component(subp.getName());
			for (SequenceFlow sf: sfiList) {
				String expression = "";
				if (sf.getSource() instanceof IntermediateCatchEvent) {
					if (sf.getAttributeValueNs("http://magic", "expression") != null) {
						expression = sf.getAttributeValueNs("http://magic", "expression");
					}
					p.addEdge(getElementName(sf.getSource()), getElementName(subp), expression);
					tlcomp.addIncomingEdge(getElementName(sf.getSource()), expression);
				}
			}
			for (SequenceFlow sf: sfoList) {
				String expression = "";
				if (sf.getTarget() instanceof IntermediateCatchEvent) {
					if (sf.getAttributeValueNs("http://magic", "expression") != null) {
						expression = sf.getAttributeValueNs("http://magic", "expression");
					}
					p.addEdge(getElementName(subp), getElementName(sf.getTarget()), expression);
					tlcomp.addOutgoingEdge(getElementName(sf.getTarget()), expression);
				}
			}
			for(DataInputAssociation dia : diaList) {
				String expression = "";
				if (dia.getAttributeValueNs("http://magic", "expression") != null) {
					expression = dia.getAttributeValueNs("http://magic", "expression");
				}
				for(ItemAwareElement elm : dia.getSources()) {
					tlcomp.addIncomingEdge(getDataName(elm), expression);
					p.addEdge(getDataName(elm), getElementName(subp), expression);
				}
			}
			for(DataOutputAssociation doa : doaList) {
				String expression = "";
				if (doa.getAttributeValueNs("http://magic", "expression") != null) {
					expression = doa.getAttributeValueNs("http://magic", "expression");
				}
				tlcomp.addOutgoingEdge(getDataName(doa.getTarget()), expression);
				p.addEdge(subp.getName(), getDataName(doa.getTarget()), expression);
			}
			p.components.add(tlcomp);
		}
	}

	private static void parseDataStoreRef(DataStoreReference dsRef, Patterns p) {
		Collection<Types> types = dsRef.getExtensionElements().getChildElementsByType(Types.class);
		for(Types typeDef : types) {
			Collection<Type> t = typeDef.getChildElementsByType(Type.class);
			for (Type type : t) {
				if (type.getType().equals("Record")) {
					Extensions exts = type.getExtensions();
					Collection<Extension> extCollection = exts.getExtension();
					DataType dataType = new DataType(type.getName());
					for(Extension extension : extCollection) {
						dataType.addParameter(extension.getKey(), extension.getType());
					}
					p.addDataType(dataType);
				}
			}
		}
	}
	
	private static void parseDataObjectRef(DataObjectReference doRef, Patterns p) {
		String name = "dataObject";
		if(doRef.getName() != null) {
			name = getElementName(doRef);
		} else {
			name = doRef.getId();
		}
		Vertex v = new Vertex(name);
		v.setType(TaskType.DATA_OBJECT);
		v.setDataType(doRef.getAttributeValueNs("http://magic", "objectType"));
		v.setPname(getTopLevelParentName(doRef));
		p.addVertex(v);
	}
	
	private static Boolean isSplit(Gateway gw) {
		return gw.getIncoming().size() == 1;
	}
	
	private static void parseGateway(Gateway gw, Patterns p) {
		String name = "gateway";
		if(gw.getName() != null) {
			name = getElementName(gw);
		} else {
			name = gw.getId();
		}
		Vertex v = new Vertex(name);
		if (gw instanceof ExclusiveGateway) {
			if (isSplit(gw)) {
				v.setType(TaskType.EXOR_SPLIT_GATE);
			} else {
				v.setType(TaskType.EXOR_JOIN_GATE);
			}
		} else if (gw instanceof ParallelGateway) {
			if (isSplit(gw)) {
				v.setType(TaskType.PAR_SPLIT_GATE);
			} else {
				v.setType(TaskType.PAR_JOIN_GATE);
			}
		}		
		Collection<SequenceFlow> sfiList = gw.getIncoming();
		Collection<SequenceFlow> sfoList = gw.getOutgoing();
		
		for (SequenceFlow sf: sfiList) {
			String expression = sf.getAttributeValueNs("http://magic", "expression");
			expression = expression == null ? "" : expression;
			if (flat) {
				if (!containProcedure(sf.getSource())) {
					v.addIncomingEdge(getElementName(sf.getSource()), expression);
					p.addEdge(getElementName(sf.getSource()), name, expression);
				}
			} else {
				v.addIncomingEdge(getElementName(sf.getSource()), expression);
				p.addEdge(getElementName(sf.getSource()), name, expression);
			}
		}
		
		for (SequenceFlow sf: sfoList) {
			String expression = sf.getAttributeValueNs("http://magic", "expression");
			expression = expression == null ? "" : expression;
			if (flat) {
				if (!containProcedure(sf.getTarget())) {
					v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
				}
			} else {
				v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
			}
		}
		v.setPname(getTopLevelParentName(gw));
		p.addVertex(v);
	}
	
	private static void parseBoundaryEvent(BoundaryEvent evt, Patterns p) {
		String name = "boundaryEvent";
		if(evt.getName() != null) {
			name = getElementName(evt);
		} else {
			name = evt.getId();
		}
		Vertex v = new Vertex(name);
		v.setType(TaskType.BOUNDARY_EVENT);
		
		Collection<SequenceFlow> sfiList = evt.getIncoming();
		Collection<SequenceFlow> sfoList = evt.getOutgoing();
		
		for (SequenceFlow sf: sfiList) {
			if (flat) {
				if (!containProcedure(sf.getSource())) {
					String expression = sf.getName()==null ? "": sf.getName();
					v.addIncomingEdge(getElementName(sf.getSource()), expression);
					p.addEdge(getElementName(sf.getSource()), name, expression);
				}
			} else {
				String expression = sf.getName()==null ? "": sf.getName();
				v.addIncomingEdge(getElementName(sf.getSource()), expression);
				p.addEdge(getElementName(sf.getSource()), name, expression);
			}
		}
		
		for (SequenceFlow sf: sfoList) {
			if (flat) {
				if (!containProcedure(sf.getTarget())) {
					String expression = sf.getName()==null ? "": sf.getName();
					v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
				}
			} else {
				String expression = sf.getName()==null ? "": sf.getName();
				v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
			}
			
		}
		Activity act = evt.getAttachedTo();
		v.addIncomingEdge(getElementName(act), "");
		p.addEdge(getElementName(act), name, "");
		p.addVertex(v);
		
	}
	
	private static void parseEvent(Event evt, Patterns p) {
		String name = "event";
		if(evt.getName() != null) {
			name = getElementName(evt);
		} else {
			name = evt.getId();
		}
		Vertex v = new Vertex(name);
		if (evt instanceof IntermediateCatchEvent) {
			v.setType(TaskType.CATCH_EVENT);
		} else if (evt instanceof IntermediateThrowEvent) {
			v.setType(TaskType.THROW_EVENT);
		} else if (evt instanceof StartEvent){
			v.setType(TaskType.START_EVENT);
		} else if (evt instanceof EndEvent) {
			v.setType(TaskType.END_EVENT);
		}
		
		Collection<SequenceFlow> sfiList = evt.getIncoming();
		Collection<SequenceFlow> sfoList = evt.getOutgoing();
		
		for (SequenceFlow sf: sfiList) {
			String expression = sf.getName()==null ? "": sf.getName();
			if (flat) {
				if (!containProcedure(sf.getSource())) {
					v.addIncomingEdge(getElementName(sf.getSource()), expression);
					p.addEdge(getElementName(sf.getSource()), name, expression);
				}
			} else {
				v.addIncomingEdge(getElementName(sf.getSource()), expression);
				p.addEdge(getElementName(sf.getSource()), name, expression);
			}
		}
		
		for (SequenceFlow sf: sfoList) {
			String expression = sf.getName()==null ? "": sf.getName();
			if (flat) {
				if (!containProcedure(sf.getTarget())) {
					v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
				}
			} else {
				v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
			}
		}
		v.setDataType(evt.getAttributeValueNs("http://magic", "objectType"));
		v.setPname(getTopLevelParentName(evt));
		p.addVertex(v);
	}
	
	private static void parseServiceTask(ServiceTask t, Patterns p) {
		Vertex v = new Vertex(getElementName(t), TaskType.SERVICE_TASK);
		Collection<SequenceFlow> sfiList = t.getIncoming();
		Collection<SequenceFlow> sfoList = t.getOutgoing();
		Collection<DataInputAssociation> diaList =  t.getDataInputAssociations();
		Collection<DataOutputAssociation> doaList =  t.getDataOutputAssociations();
		
		for (SequenceFlow sf: sfiList) {
			String expression = sf.getName()==null ? "": sf.getName();
			if(flat) {
				if(!containProcedure(sf.getSource())) {
					v.addIncomingEdge(getElementName(sf.getSource()), expression);
					p.addEdge(getElementName(sf.getSource()), getElementName(t), expression);
				}
			} else {
				v.addIncomingEdge(getElementName(sf.getSource()), expression);
				p.addEdge(getElementName(sf.getSource()), getElementName(t), expression);
			}
		}
		
		for (SequenceFlow sf: sfoList) {
			String expression = sf.getName()==null ? "": sf.getName();
			if(flat) {
				if(!containProcedure(sf.getTarget())) {
					v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
				}
			} else {
				v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
			}
		}
		
		for(DataInputAssociation dia : diaList) {
			String expression = "";
			if (dia.getAttributeValueNs("http://magic", "expression") != null) {
				expression = dia.getAttributeValueNs("http://magic", "expression");
			}
			for(ItemAwareElement elm : dia.getSources()) {
				v.addIncomingEdge(getDataName(elm), expression);
				p.addEdge(getDataName(elm), getElementName(t), expression);
			}
		}
		for(DataOutputAssociation doa : doaList) {
			String expression = "";
			if (doa.getAttributeValueNs("http://magic", "expression") != null) {
				expression = doa.getAttributeValueNs("http://magic", "expression");
			}
			v.addOutgoingEdge(doa.getTarget().getId(), expression);
			//p.addEdge(t.getName(), doa.getTarget().getId(), expression);
		}
		p.addVertex(v);
	}
	
	private static void parseTasks(Task t, Patterns p) {
		Vertex v = new Vertex(getElementName(t), TaskType.TASK);
		Collection<SequenceFlow> sfiList = t.getIncoming();
		Collection<SequenceFlow> sfoList = t.getOutgoing();
		Collection<DataInputAssociation> diaList =  t.getDataInputAssociations();
		Collection<DataOutputAssociation> doaList =  t.getDataOutputAssociations();
				
		for (SequenceFlow sf: sfiList) {
			String expression = sf.getName()==null ? "": sf.getName();
			if(flat) {
				if(!containProcedure(sf.getSource())) {
					v.addIncomingEdge(getElementName(sf.getSource()), expression);
					p.addEdge(getElementName(sf.getSource()), getElementName(t), expression);
				}
			} else {
				v.addIncomingEdge(getElementName(sf.getSource()), expression);
				p.addEdge(getElementName(sf.getSource()), getElementName(t), expression);
			}
		}
		
		for (SequenceFlow sf: sfoList) {
			String expression = sf.getName()==null ? "": sf.getName();
			if(flat) {
				if(!containProcedure(sf.getTarget())) {
					v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
				}
			} else {
				v.addOutgoingEdge(getElementName(sf.getTarget()), expression);
			}
		}
		
		for(DataInputAssociation dia : diaList) {
			String expression = "";
			if (dia.getAttributeValueNs("http://magic", "expression") != null) {
				expression = dia.getAttributeValueNs("http://magic", "expression");
			}
			for(ItemAwareElement elm : dia.getSources()) {
				v.addIncomingEdge(getDataName(elm), expression);
				p.addEdge(getDataName(elm), getElementName(t), expression);
			}
		}
		for(DataOutputAssociation doa : doaList) {
			String expression = "";
			if (doa.getAttributeValueNs("http://magic", "expression") != null) {
				expression = doa.getAttributeValueNs("http://magic", "expression");
			}
			v.addOutgoingEdge(getDataName(doa.getTarget()), expression);
			p.addEdge(getElementName(t), getDataName(doa.getTarget()), expression);
		}
		
		v.setPname(getTopLevelParentName(t));
		p.addVertex(v);
	}
	
	private static String getTopLevelParentName (BpmnModelElementInstance elemInst) {
		ModelElementInstance parent = elemInst.getParentElement();
		while (parent.getElementType().getTypeName() != "process") {
			if (isTopLevel(parent)) {
				return parent.getAttributeValue("name");
			}
			parent = parent.getParentElement();
		}
		return null;
	}
	
	private static Boolean isTopLevel (ModelElementInstance elemInst) {
		return elemInst.getParentElement().getElementType().getTypeName() == "process";
	}
	
	private static void parseSubProcess(SubProcess subp, Patterns p) {
		Vertex v = new Vertex(getElementName(subp), TaskType.SUB_PROCESS);
		Collection<SequenceFlow> sfiList = subp.getIncoming();
		Collection<SequenceFlow> sfoList = subp.getOutgoing();
		Collection<DataInputAssociation> diaList =  subp.getDataInputAssociations();
		Collection<DataOutputAssociation> doaList =  subp.getDataOutputAssociations();
		
		for (SequenceFlow sf: sfiList) {
			v.addIncomingEdge(getElementName(sf.getSource()), "");
			p.addEdge(getElementName(sf.getSource()), getElementName(subp), "");
		}
		
		for (SequenceFlow sf: sfoList) {
			v.addOutgoingEdge(getElementName(sf.getTarget()), "");
			//p.addEdge(t.getName(), sf.getTarget().getName(), sf.getName());
		}
		
		for(DataInputAssociation dia : diaList) {
			String expression = "";
			if (dia.getAttributeValueNs("http://magic", "expression") != null) {
				expression = dia.getAttributeValueNs("http://magic", "expression");
			}
			for(ItemAwareElement elm : dia.getSources()) {
				v.addIncomingEdge(getDataName(elm), expression);
				p.addEdge(getDataName(elm), getElementName(subp), expression);
			}
		}
		for(DataOutputAssociation doa : doaList) {

			String expression = "";
			if (doa.getAttributeValueNs("http://magic", "expression") != null) {
				expression = doa.getAttributeValueNs("http://magic", "expression");
			}
			v.addOutgoingEdge(getDataName(doa.getTarget()), expression);
			p.addEdge(getElementName(subp), getDataName(doa.getTarget()), expression);
		}
		
		if (isTopLevel(subp)) {
			p.components.add(new Component(subp.getName()));
		}
		
		p.addVertex(v);
	}
	
	private static boolean containProcedure(FlowNode subp) {
		if (subp.getChildElementsByType(Task.class).size() == 0
				&& subp.getChildElementsByType(SubProcess.class).size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	private static String getDataName(ItemAwareElement elm) {
		if (elm.getName() != null) {
			return elm.getName().replace(" ", "_").replace("/", "Or");
		} else {
			return elm.getId();
		}
	}
	
	private static String getElementName(FlowElement fele) {
		if(fele.getName() != null) {
			return fele.getName();
		} else {
			return fele.getId();
		}
	}
}