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
    protected String parentName;

    public Put(final String nodeName, final String description, final String parentName) {
    	super(nodeName, description, MethodName.PUT);
        this.parentName = parentName;
        operation = new Operation();
        operation.setDescription(description);
        loadResponse(ResponseKey.CREATED);
        loadResponse(ResponseKey.NO_CONTENT);
        operation.setConsumes(OperationBuilder.CONSUMES_PUT_POST);
    }

    @Override
    public Put pathParams(final List<Parameter> params) {
        final List<Parameter> parameters = new ArrayList<>(params);
        final BodyParameter payload = new BodyParameter();

        final RefModel schema = new RefModel();
        schema.set$ref(parentName + OperationBuilder.CONFIG + nodeName + OperationBuilder.TOP);    
        payload.setSchema(schema);
        payload.setName(OperationBuilder.CONFIG + nodeName);
        
        parameters.add(payload);
        operation.setParameters(parameters);
        return this;
    }
}