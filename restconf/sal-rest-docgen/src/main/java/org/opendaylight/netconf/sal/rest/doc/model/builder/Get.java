/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.rest.doc.model.builder;

import java.util.List;

import io.swagger.models.Response;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.RefProperty;

public class Get extends Method {

    public Get(final String nodeName, final String description, final boolean isConfig) {
    	super((isConfig ? OperationBuilder.CONFIG : OperationBuilder.OPERATIONAL) + nodeName, description, MethodName.GET);
        final Response response = new Response();
        final RefProperty schema = new RefProperty();
        schema.set$ref((isConfig ? OperationBuilder.CONFIG : OperationBuilder.OPERATIONAL) + nodeName);
        response.setSchema(schema);
        loadResponse(ResponseKey.OK, response);
    }

    @Override
    public Get pathParams(final List<Parameter> params) {
    	return (Get) super.pathParams(params);
    }
}