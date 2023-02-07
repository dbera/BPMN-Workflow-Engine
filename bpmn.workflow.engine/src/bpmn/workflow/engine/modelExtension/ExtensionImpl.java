package bpmn.workflow.engine.modelExtension;
import static bpmn.workflow.engine.modelExtension.Constants.ATTRIBUTE_KEY;
import static bpmn.workflow.engine.modelExtension.Constants.ATTRIBUTE_TYPE;
import static bpmn.workflow.engine.modelExtension.Constants.EXTENSION_EXTENSION;
import static bpmn.workflow.engine.modelExtension.Constants.MAGIC_NS;

import org.camunda.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.camunda.bpm.model.xml.type.attribute.Attribute;

public class ExtensionImpl extends BpmnModelElementInstanceImpl implements Extension {
	
	protected static Attribute<String> key;
	protected static Attribute<String> type;
	
	public ExtensionImpl(ModelTypeInstanceContext instanceContext) {
		super(instanceContext);
	}
	
	public static void registerType(ModelBuilder modelBuilder) {
		ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Extension.class, EXTENSION_EXTENSION)
	      .namespaceUri(MAGIC_NS)
	      .instanceProvider(new ModelTypeInstanceProvider<Extension>() {
	        public Extension newInstance(ModelTypeInstanceContext instanceContext) {
	          return new ExtensionImpl(instanceContext);
	        }
	      });
		key = typeBuilder.stringAttribute(ATTRIBUTE_KEY)
				.namespace(MAGIC_NS)
			    .required()
			    .build();
		type = typeBuilder.stringAttribute(ATTRIBUTE_TYPE)
				  .namespace(MAGIC_NS)
			      .required()
			      .build();
		typeBuilder.build();
	}

	@Override
	public String getKey() {
		return key.getValue(this);
	}

	@Override
	public String getType() {
		return type.getValue(this);
	}

}
