package bpmn.workflow.engine.modelExtension;

import static bpmn.workflow.engine.modelExtension.Constants.ATTRIBUTE_NAME;
import static bpmn.workflow.engine.modelExtension.Constants.ATTRIBUTE_TYPE;
import static bpmn.workflow.engine.modelExtension.Constants.EXTENSION_TYPE;
import static bpmn.workflow.engine.modelExtension.Constants.MAGIC_NS;

import java.util.Collection;

import org.camunda.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.camunda.bpm.model.xml.type.attribute.Attribute;
import org.camunda.bpm.model.xml.type.child.ChildElement;
import org.camunda.bpm.model.xml.type.child.ChildElementCollection;
import org.camunda.bpm.model.xml.type.child.SequenceBuilder;
import org.camunda.bpm.model.xml.type.reference.AttributeReference;
public class TypeImpl extends BpmnModelElementInstanceImpl implements Type{

	protected static ChildElement<Extensions> extensions;
	protected static Attribute<String> type;
	protected static Attribute<String> name;
	public TypeImpl(ModelTypeInstanceContext instanceContext) {
		super(instanceContext);
	}
	
	public static void registerType(ModelBuilder modelBuilder) {
		ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Type.class, EXTENSION_TYPE)
	      .namespaceUri(MAGIC_NS)
	      .instanceProvider(new ModelTypeInstanceProvider<Type>() {
	        public Type newInstance(ModelTypeInstanceContext instanceContext) {
	          return new TypeImpl(instanceContext);
	        }
	      });
		name = typeBuilder.stringAttribute(ATTRIBUTE_NAME)
				  .namespace(MAGIC_NS)
			      .required()
			      .build();
		type = typeBuilder.stringAttribute(ATTRIBUTE_TYPE)
		  .namespace(MAGIC_NS)
	      .required()
	      .build();
		SequenceBuilder sequenceBuilder = typeBuilder.sequence();
		extensions = sequenceBuilder.element(Extensions.class).build();
	    typeBuilder.build();
	}

	@Override
	public Extensions getExtensions() {
		return extensions.getChild(this);
	}

	@Override
	public String getType() {
		return type.getValue(this);
	}

	@Override
	public String getName() {
		return name.getValue(this);
	}

}
