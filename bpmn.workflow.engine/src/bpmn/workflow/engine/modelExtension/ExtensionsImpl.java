package bpmn.workflow.engine.modelExtension;

import static bpmn.workflow.engine.modelExtension.Constants.MAGIC_NS;
import static bpmn.workflow.engine.modelExtension.Constants.EXTENSION_EXTENSIONS;
import java.util.Collection;

import org.camunda.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import org.camunda.bpm.model.xml.ModelBuilder;
import org.camunda.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder;
import org.camunda.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.camunda.bpm.model.xml.type.child.ChildElementCollection;
import org.camunda.bpm.model.xml.type.child.SequenceBuilder;

public class ExtensionsImpl extends BpmnModelElementInstanceImpl implements Extensions {
	
	protected static ChildElementCollection<Extension> extensionCollection;
	
	public ExtensionsImpl(ModelTypeInstanceContext instanceContext) {
		super(instanceContext);
	}
	
	public static void registerType(ModelBuilder modelBuilder) {
		ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Extensions.class, EXTENSION_EXTENSIONS)
				.namespaceUri(MAGIC_NS)
				.instanceProvider(new ModelTypeInstanceProvider<Extensions>() {
					public Extensions newInstance(ModelTypeInstanceContext instanceContext) {
				      return new ExtensionsImpl(instanceContext);
				}
			});
		SequenceBuilder sequenceBuilder = typeBuilder.sequence();
		extensionCollection = sequenceBuilder.elementCollection(Extension.class).build();
		typeBuilder.build();
	}

	@Override
	public Collection<Extension> getExtension() {
		return extensionCollection.get(this);
	}

}
