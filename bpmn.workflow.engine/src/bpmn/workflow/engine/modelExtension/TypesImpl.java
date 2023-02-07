package bpmn.workflow.engine.modelExtension;

import static bpmn.workflow.engine.modelExtension.Constants.MAGIC_NS;
import static bpmn.workflow.engine.modelExtension.Constants.EXTENSION_TYPES;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.camunda.bpm.model.xml.type.child.ChildElementCollection;
import org.camunda.bpm.model.xml.type.child.SequenceBuilder;

public class TypesImpl extends BpmnModelElementInstanceImpl implements Types {
	
	protected static ChildElementCollection<Type> typeCollection;
	
	public TypesImpl(ModelTypeInstanceContext instanceContext) {
		super(instanceContext);
	}

	public static void registerType(ModelBuilder modelBuilder) {
		ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Types.class, EXTENSION_TYPES)
	      .namespaceUri(MAGIC_NS)
	      .instanceProvider(new ModelTypeInstanceProvider<Types>() {
	        public Types newInstance(ModelTypeInstanceContext instanceContext) {
	          return new TypesImpl(instanceContext);
	        }
	      });
		SequenceBuilder sequenceBuilder = typeBuilder.sequence();
		typeCollection = sequenceBuilder.elementCollection(Type.class).build();
	    typeBuilder.build();
	}

	@Override
	public Collection<Type> getType() {
		return typeCollection.get(this);
	}
}
