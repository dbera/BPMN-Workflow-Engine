# bpmn-creator

This project illustrates how to use the [CAMUNDA Model Builder API](https://docs.camunda.org/manual/latest/user-guide/model-api/bpmn-model-api/) to create or modify process models programmatically.


# Running this project in eclipse

First go to Help -> "Install new software" and search in all your sources for "maven integration for eclipse" and install it all.

Then you should be able to see the following locations in your target platform file (in the bpmn.workflow.target project):

ch.qos.logback:logback-classic (1.2.3) 2 plug-ins available
https://projectlombok.org/p2 1 plug-ins available (MAYBE NOT THIS ONE)
junit:junit (4.12) 2 plug-ins available
org.assertj:assertj-core (3.23.1) 2 plug-ins available
org.projectlombok:lombok (1.18.26) 2 plug-ins available
org.slf4j:slf4j-api (2.0.3) 2 plug-ins available

Then just clean all projects and build again. Then run DemoBMPNParser.java as a java app from the project bpmn.workflow.engine.
