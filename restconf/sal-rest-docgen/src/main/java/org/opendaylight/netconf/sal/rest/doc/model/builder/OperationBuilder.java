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
    public static final String TOP = "-TOP";

    public static final List<String> CONSUMES_PUT_POST = new ArrayList<>();

    static {
        CONSUMES_PUT_POST.add("application/json");
        CONSUMES_PUT_POST.add("application/xml");
    }
    
    public static Path post(Path path, String nodeName, String description, String parentName, DataNodeContainer node, List<Parameter> params) {
        final Post postBuilder = new Post(nodeName, parentName, description, node);
        postBuilder.pathParams(params);
    	path.post(postBuilder.build());
    	return path;
    }
    
    public static Path get(Path path, String nodeName, String description, Boolean isConfig, List<Parameter> params) {
        final Get getBuilder = new Get(nodeName, description, isConfig);
        getBuilder.pathParams(params);
    	path.get(getBuilder.build());
    	return path;
    }
    
	public static Path put(Path path, String nodeName, String description, String parentName, List<Parameter> params) {
        final Put putBuilder = new Put(nodeName, description, parentName);
        putBuilder.pathParams(params);
    	path.put(putBuilder.build());
    	return path;
	}
	
	public static Path delete(Path path, String nodeName, String description, List<Parameter> params) {
        final Delete deleteBuilder = new Delete(nodeName, description);
        deleteBuilder.pathParams(params);
    	path.delete(deleteBuilder.build());
    	return path;
	}
}