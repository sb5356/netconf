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

import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;

import io.swagger.models.Path;
import io.swagger.models.parameters.Parameter;

public final class OperationBuilder {

	private OperationBuilder() {}
	
    public static final String OPERATIONAL = "(operational)";
    public static final String CONFIG = "(config)";
	public static final String OPERATIONS = "(operations)";
    public static final String TOP = "-TOP";

    public static final List<String> CONSUMES_BODY = new ArrayList<>();

    static {
        CONSUMES_BODY.add("application/json");
        CONSUMES_BODY.add("application/xml");
    }
    
    public static Path configPost(final Path path, final String nodeName, final String description, final String parentName, final DataNodeContainer node, final List<Parameter> params) {
        final Post postBuilder = new ConfigPost(nodeName, parentName, description, node);
        postBuilder.pathParams(params);
    	path.post(postBuilder.build());
    	return path;
    }
    
    public static Path operationalPost(final Path path, final String nodeName, final String description, final String parentName, final DataNodeContainer requestNode, final DataNodeContainer responseNode, final List<Parameter> params) {
        final Post postBuilder = new OperationsPost(nodeName, parentName, description, requestNode, responseNode);
        postBuilder.pathParams(params);
    	path.post(postBuilder.build());
    	return path;
    }
    
    public static Path operationalGet(final Path path, final String nodeName, final String description, final String parentName) {
        final Get getBuilder = new OperationalGet(nodeName, description, parentName);
    	path.get(getBuilder.build());
    	return path;
    }
    
    public static Path configGet(final Path path, final String nodeName, final String description, final String parentName, final List<Parameter> params) {
        final Get getBuilder = new ConfigGet(nodeName, description, parentName);
        getBuilder.pathParams(params);
    	path.get(getBuilder.build());
    	return path;
    }
    
	public static Path put(final Path path, final String nodeName, final String description, final String parentName, final List<Parameter> params) {
        final Put putBuilder = new Put(nodeName, description, parentName);
        putBuilder.pathParams(params);
    	path.put(putBuilder.build());
    	return path;
	}
	
	public static Path delete(final Path path, final String nodeName, final String description, final List<Parameter> params) {
        final Delete deleteBuilder = new Delete(nodeName, description);
        deleteBuilder.pathParams(params);
    	path.delete(deleteBuilder.build());
    	return path;
	}
}