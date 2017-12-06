package org.opendaylight.netconf.sal.rest.doc.model.builder;

public enum ResponseKey {
	BAD_REQUEST("400"), OK("200"), CREATED("201"), CONFLICT("409"), NO_CONTENT("204");
	
	private final String key;
	
	ResponseKey(final String code) {
		key = code;
	}
	
	public String value() {
		return key;
	}
}