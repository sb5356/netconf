package org.opendaylight.netconf.sal.rest.doc.model.builder;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;

import io.swagger.models.RefModel;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;

public final class ConfigPost extends Post {

    public ConfigPost(final String nodeName, final String parentName, final String description,
            final DataNodeContainer dataNodeContainer) {
		super(nodeName, parentName, description, dataNodeContainer);
        loadResponse(ResponseKey.CONFLICT);
        loadResponse(ResponseKey.CREATED);
        bodyParams();
	} 

    public ConfigPost bodyParams() {
        final List<Parameter> parameters = new ArrayList<>();
        final BodyParameter payload = new BodyParameter();
        
        final RefModel schema = new RefModel();
        schema.set$ref(parentName + OperationBuilder.CONFIG + nodeName + OperationBuilder.TOP);    
        payload.setSchema(schema);
        payload.setName(OperationBuilder.CONFIG + nodeName + OperationBuilder.TOP);
        
        
        parameters.add(payload);
    	super.bodyParams(parameters);
        return this;
    }
}