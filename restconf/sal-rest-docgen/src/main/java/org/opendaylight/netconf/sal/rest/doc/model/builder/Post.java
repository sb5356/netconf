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

import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;


public class Post extends Method {
    protected final DataNodeContainer dataNodeContainer;

    public Post(final String nodeName, final String description,
                final DataNodeContainer dataNodeContainer) {
        super(nodeName, description, MethodName.POST);
        this.dataNodeContainer = dataNodeContainer;
    }
        
    protected Collection<DataSchemaNode> getDataSchemaNodes() {
    	if(dataNodeContainer != null) {
    		return dataNodeContainer.getChildNodes();
    	} else {
    		return Collections.emptyList();
    	}
    }
}