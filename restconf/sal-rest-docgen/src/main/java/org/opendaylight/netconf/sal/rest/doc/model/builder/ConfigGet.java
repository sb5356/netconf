package org.opendaylight.netconf.sal.rest.doc.model.builder;

import java.util.List;

import io.swagger.models.parameters.Parameter;

public final class ConfigGet extends Get {

    public ConfigGet(final String nodeName, final String description, final String parentName) {
		super(OperationBuilder.CONFIG + nodeName + OperationBuilder.TOP, description, parentName);
	} 

    @Override
    public ConfigGet pathParams(final List<Parameter> params) {
    	return (ConfigGet) super.pathParams(params);
    }
}