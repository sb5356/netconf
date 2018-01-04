package org.opendaylight.netconf.sal.rest.doc.model.builder;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.RefProperty;

public final class ConfigPost extends Post {

    public ConfigPost(final String nodeName, final String parentName, final String description,
            final DataNodeContainer dataNodeContainer) {
		super(nodeName, parentName, description, dataNodeContainer);
        loadResponse(ResponseKey.CONFLICT);
        final Response response = new Response();
        final RefProperty schema = new RefProperty();
        schema.set$ref(OperationBuilder.CONFIG + nodeName + MethodName.POST);
        response.setSchema(schema);
        loadResponse(ResponseKey.OK, response);
        bodyParams();
	} 

    public ConfigPost bodyParams() {
        final List<Parameter> parameters = new ArrayList<>();
        for (final DataSchemaNode node : getDataSchemaNodes()) {
            if (node instanceof ListSchemaNode || node instanceof ContainerSchemaNode) {
                final BodyParameter payload = new BodyParameter();
                final RefModel schema = new RefModel();
                schema.set$ref(parentName + OperationBuilder.CONFIG + node.getQName().getLocalName() + OperationBuilder.TOP);
                payload.setSchema(schema);
                payload.setName("**" + OperationBuilder.CONFIG + node.getQName().getLocalName());
                parameters.add(payload);
            }
        }
    	super.bodyParams(parameters);
        return this;
    }
}