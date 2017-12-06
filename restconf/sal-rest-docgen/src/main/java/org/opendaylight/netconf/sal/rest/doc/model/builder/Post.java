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

import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.RefProperty;

public final class Post extends Method {
    protected String parentName;
    private final DataNodeContainer dataNodeContainer;

    public Post(final String nodeName, final String parentName, final String description,
                final DataNodeContainer dataNodeContainer) {
        super(nodeName, description, MethodName.POST);
        this.parentName = parentName;
        this.dataNodeContainer = dataNodeContainer;
        final Response response = new Response();
        final RefProperty schema = new RefProperty();
        schema.set$ref(OperationBuilder.CONFIG + nodeName + MethodName.POST);
        response.setSchema(schema);
        loadResponse(ResponseKey.OK, response);
        loadResponse(ResponseKey.CONFLICT);
        operation.setConsumes(OperationBuilder.CONSUMES_PUT_POST);
    }

    @Override
    public Post pathParams(final List<Parameter> params) {
        final List<Parameter> parameters = new ArrayList<>(params);
        for (final DataSchemaNode node : dataNodeContainer.getChildNodes()) {
            if (node instanceof ListSchemaNode || node instanceof ContainerSchemaNode) {
                final BodyParameter payload = new BodyParameter();
                final RefModel schema = new RefModel();
                schema.set$ref(parentName + OperationBuilder.CONFIG + node.getQName().getLocalName() + OperationBuilder.TOP);
                payload.setSchema(schema);
                payload.setName("**" + OperationBuilder.CONFIG + node.getQName().getLocalName());
                parameters.add(payload);
            }
        }
        operation.setParameters(parameters);
        return this;
    }
}