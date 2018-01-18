/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.rest.doc.model.builder;

import java.util.ArrayList;
import java.util.List;

import io.swagger.models.Operation;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;

public class Put extends Method {

    public Put(final String nodeName, final String description) {
    	super(nodeName, description, MethodName.PUT);
        operation = new Operation();
        operation.setDescription(description);
        loadResponse(ResponseKey.CREATED);
        loadResponse(ResponseKey.NO_CONTENT);
        operation.setConsumes(OperationBuilder.CONSUMES_BODY);
        bodyParams();
    }

    public Put bodyParams() {
        final List<Parameter> parameters = new ArrayList<>();
        final BodyParameter payload = new BodyParameter();

        final RefModel schema = new RefModel();
        schema.set$ref(OperationBuilder.CONFIG + nodeName + OperationBuilder.TOP);    
        payload.setSchema(schema);
        payload.setName(OperationBuilder.CONFIG + nodeName);
        
        parameters.add(payload);
    	super.bodyParams(parameters);
        return this;
    }
}