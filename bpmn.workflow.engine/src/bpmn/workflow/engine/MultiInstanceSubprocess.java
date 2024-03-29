package bpmn.workflow.engine;

import java.io.File;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SubProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MultiInstanceSubprocess {

  public static final String MULTI_INSTANCE_PROCESS = "myMultiInstanceProcess";

  // @see https://docs.camunda.org/manual/latest/user-guide/model-api/bpmn-model-api/fluent-builder-api/
  public static void main(String[] args) {

    BpmnModelInstance modelInst;
    Logger log = LoggerFactory.getLogger(MultiInstanceSubprocess.class);
    try {
      File file = new File("./src/main/resources/multiInstance.bpmn");
      modelInst = Bpmn.createProcess()
          .id("MyParentProcess")
          .executable()
          .startEvent("ProcessStarted")
          .subProcess(MULTI_INSTANCE_PROCESS)
          //first create sub process content
          .embeddedSubProcess()
          .startEvent("subProcessStartEvent")
          .userTask("UserTask1")
          .endEvent("subProcessEndEvent")
          .subProcessDone()
          .endEvent("ParentEnded").done();

      // Add multi-instance loop characteristics to embedded sub process
      SubProcess subProcess = modelInst.getModelElementById(MULTI_INSTANCE_PROCESS);
      subProcess.builder()
          .multiInstance()
          .camundaCollection("myCollection")
          .camundaElementVariable("myVar")
          .multiInstanceDone();

      log.info("Flow Elements - Name : Id : Type Name");
      modelInst.getModelElementsByType(FlowNode.class).forEach(e -> log.info("{} : {} : {}", e.getName(), e.getId(), e.getElementType().getTypeName()));

      Bpmn.writeModelToFile(file, modelInst);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
