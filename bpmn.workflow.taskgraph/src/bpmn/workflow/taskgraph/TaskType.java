package bpmn.workflow.taskgraph;

public enum TaskType {
	TASK,
	SERVICE_TASK,
	SUB_PROCESS,
	EXOR_SPLIT_GATE,
	PAR_SPLIT_GATE,
	EXOR_JOIN_GATE,
	PAR_JOIN_GATE,
	MSG_EVENT,
	TIMER_EVENT,
	SIG_EVENT,
	START_EVENT,
	END_EVENT,
	BOUNDARY_EVENT,
	CATCH_EVENT,
	THROW_EVENT,
	DATA_STORE,
	DATA_OBJECT
}