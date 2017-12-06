package org.opendaylight.controller.sal.rest.doc.impl;

import java.io.IOException;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.models.Swagger;

public abstract class TestingUtils {
	
	private TestingUtils() {}
	
    private static final Logger LOG = LoggerFactory.getLogger(TestingUtils.class);

    public static void printSwagger(final Swagger swagger) throws IOException {
        if(swagger != null) {
            final StringWriter writer = new StringWriter();
            final ObjectMapper mapper = new ObjectMapper(new JsonFactory());
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.writeValue(writer, swagger);
            LOG.warn(writer.toString());
        } else {
            LOG.warn("TestingUtils.printSwagger: null swagger returned");
        }
    }
}