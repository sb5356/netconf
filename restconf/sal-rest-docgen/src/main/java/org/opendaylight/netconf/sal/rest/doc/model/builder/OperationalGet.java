package org.opendaylight.netconf.sal.rest.doc.model.builder;

public final class OperationalGet extends Get {

    public OperationalGet(final String nodeName, final String description) {
		super(OperationBuilder.OPERATIONAL + nodeName, description);
	} 
}