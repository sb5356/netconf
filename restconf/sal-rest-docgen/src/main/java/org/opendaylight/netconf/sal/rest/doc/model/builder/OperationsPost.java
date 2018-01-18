package org.opendaylight.netconf.sal.rest.doc.model.builder;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;

import io.swagger.models.Operation;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.RefProperty;

public final class OperationsPost extends Post {

    public OperationsPost(final String nodeName, final String description,
            final DataNodeContainer requestDataNodeContainer, final DataNodeContainer responseDataNodeContainer) {
		super(nodeName, description, requestDataNodeContainer);
       
        final Operation operationSpec = new Operation();
        operationSpec.setOperationId(nodeName);
        final Response response = new Response();
        if (!responseDataNodeContainer.getChildNodes().isEmpty()) {
        	final RefProperty schema = new RefProperty();
        	schema.set$ref("(" + nodeName + ")output" + OperationBuilder.TOP);
        	response.setSchema(schema);
        	
        	response.setDescription(description);
        }
        loadResponse(ResponseKey.OK, response);
        bodyParams();
	} 

    public OperationsPost bodyParams() {
        final List<Parameter> parameters = new ArrayList<>();
        if (!getDataSchemaNodes().isEmpty()) {
            final BodyParameter payload = new BodyParameter();
            final RefModel model = new RefModel();
            model.set$ref("(" + nodeName + ")input" + OperationBuilder.TOP);
            payload.setName("**" + OperationBuilder.OPERATIONS + nodeName);
            payload.setSchema(model);
            parameters.add(payload);
        }
    	super.bodyParams(parameters);
        return this;
    }
}