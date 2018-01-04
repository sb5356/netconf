/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.rest.doc.model.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

import io.swagger.models.parameters.Parameter;

public class Post extends Method {
    protected String parentName;
    protected final DataNodeContainer dataNodeContainer;

    public Post(final String nodeName, final String parentName, final String description,
                final DataNodeContainer dataNodeContainer) {
        super(nodeName, description, MethodName.POST);
        this.parentName = parentName;
        this.dataNodeContainer = dataNodeContainer;
    }

    protected Post bodyParams(final List<Parameter> params) {
        if(params.size() > 0) {
        	operation.setConsumes(OperationBuilder.CONSUMES_PUT_POST);
        }
        operation.setParameters(params);
        return this;
    }
        
    protected Collection<DataSchemaNode> getDataSchemaNodes() {
    	if(dataNodeContainer != null) {
    		return dataNodeContainer.getChildNodes();
    	} else {
    		return Collections.emptyList();
    	}
    }
}