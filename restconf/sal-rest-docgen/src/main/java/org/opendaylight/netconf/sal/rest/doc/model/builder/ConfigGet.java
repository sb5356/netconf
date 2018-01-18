package org.opendaylight.netconf.sal.rest.doc.model.builder;

import java.util.List;

import io.swagger.models.parameters.Parameter;

public final class ConfigGet extends Get {

    public ConfigGet(final String nodeName, final String description) {
		super(OperationBuilder.CONFIG + nodeName + OperationBuilder.TOP, description);
	} 

    @Override
    public ConfigGet pathParams(final List<Parameter> params) {
    	return (ConfigGet) super.pathParams(params);
    }
}