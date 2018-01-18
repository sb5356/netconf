/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.rest.doc.model.builder;

import java.util.List;

import io.swagger.models.parameters.Parameter;

public final class Delete extends Method {

    public Delete(final String nodeName, final String description) {
        super(nodeName, description, MethodName.DELETE);
        loadResponse(ResponseKey.NO_CONTENT);
    }
    
    @Override
    public Delete pathParams(final List<Parameter> params) {
    	return (Delete) super.pathParams(params);
    }
}