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
import io.swagger.models.Response;
import io.swagger.models.parameters.Parameter;

public abstract class Method {

    protected Operation operation;
    protected String nodeDescription;
    protected String nodeName;

    public Method(final String nodeName, final String nodeDescription, final MethodName method) {
    	this.nodeName = nodeName;
        this.nodeDescription = nodeDescription;
        operation = new Operation();
        operation.setDescription(nodeDescription);
        operation.setOperationId(method + "-" + nodeName);
        loadResponse(ResponseKey.BAD_REQUEST);
    }
    
    public Method pathParams(final List<Parameter> params) {
        final List<Parameter> pathParameters = new ArrayList<>(params);
        pathParameters.parallelStream().forEach(p -> p.setIn(ParameterIn.path.toString()));
        operation.getParameters().addAll(pathParameters);
        return this;
    }

    public Operation build() {
        return operation;
    }
    
    protected Method loadResponse(ResponseKey key) {
        return loadResponse(key, new Response());
    }
    
    protected Method loadResponse(ResponseKey key, Response response) {
    	if(key.equals(ResponseKey.OK)) {
    		if(nodeDescription != null) {
    			response.setDescription(nodeDescription);
    		} else {
    			response.setDescription(nodeName);
    		}
    	} else if(key.equals(ResponseKey.BAD_REQUEST)) {
    		response.setDescription("Internal error");
    	} else if(key.equals(ResponseKey.CREATED)) {
    		response.setDescription("Object created");
    	} else if(key.equals(ResponseKey.CONFLICT)) {
    		response.setDescription("Object already exists");
    	} else if(key.equals(ResponseKey.NO_CONTENT)) {
    		response.setDescription("Object modified");
    	}
        operation.addResponse(key.value(), response);
        return this;
    }
    
    protected Method bodyParams(final List<Parameter> params) {
        if(params.size() > 0) {
        	operation.setConsumes(OperationBuilder.CONSUMES_BODY);
        }
        operation.setParameters(params);
        return this;
    }
}