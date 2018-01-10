package org.opendaylight.netconf.sal.rest.doc.model.builder;

public final class OperationalGet extends Get {

    public OperationalGet(final String nodeName, final String description, final String parentName) {
		super(OperationBuilder.OPERATIONAL + nodeName, description, parentName);
	} 
}