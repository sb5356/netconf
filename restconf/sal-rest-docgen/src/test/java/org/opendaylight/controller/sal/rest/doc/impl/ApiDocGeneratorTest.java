/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.doc.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Preconditions;

import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;

import java.net.URI;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.core.UriInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.netconf.sal.rest.doc.impl.ApiDocGenerator;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ApiDocGeneratorTest {
	

    public static final String HTTP_HOST = "http://host";
    private static final String NAMESPACE = "http://netconfcentral.org/ns/toaster2";
    private static final String STRING_DATE = "2009-11-20";
    private static final Date DATE = Date.valueOf(STRING_DATE);
    private static final String NAMESPACE_2 = "http://netconfcentral.org/ns/toaster";
    private static final Date REVISION_2 = Date.valueOf(STRING_DATE);
    private ApiDocGenerator generator;
    private DocGenTestHelper helper;
    private SchemaContext schemaContext;
    
    @Mock
    private UriInfo info;

    @Before
    public void setUp() throws Exception {
    	MockitoAnnotations.initMocks(this);
        this.generator = new ApiDocGenerator();
        generator.setDraft(false);
        this.helper = new DocGenTestHelper();
        this.helper.setUp();

        this.schemaContext = this.helper.getSchemaContext();
        
        Mockito.when(info.getBaseUri()).thenReturn(new URI("http://localhost:8080/restconf"));
    } 

    @After
    public void after() throws Exception {
    	
    }
    
    /**
     * Method: getApiDeclaration(String module, String revision, UriInfo uriInfo).
     */
    @Test
    public void testGetModuleDoc() throws Exception {
        Preconditions.checkArgument(this.helper.getModules() != null, "No modules found");

        for (final Module m : this.helper.getSchemaContext().getModules()) {
            if (m.getQNameModule().getNamespace().toString().equals(NAMESPACE)
                    && m.getQNameModule().getRevision().equals(DATE)) {
                final Swagger doc = this.generator.getSwaggerDocSpec(m, info, "",
                        this.schemaContext);
                TestingUtils.printSwagger(doc);
                validateToaster(doc);
                validateTosterDocContainsModulePrefixes(doc);
                validateSwaggerModules(doc);
                validateSwaggerApisForPost(doc);
            }
        }
    }

    /**
     * Validate whether ApiDelcaration contains Apis with concrete path and whether this Apis contain specified POST
     * operations.
     */
    private void validateSwaggerApisForPost(final Swagger doc) {
        // two POST URI with concrete schema name in summary
        final Path lstApi = findApi("/config/toaster2:lst", doc);
        assertNotNull("Api /config/toaster2:lst wasn't found", lstApi);
        assertTrue("POST for cont1 in lst is missing",
                findOperation(lstApi.getPost(), "toaster2/lst(config)lst1-TOP",
                        "toaster2/lst(config)cont1-TOP"));

        final Path cont1Api = findApi("/config/toaster2:lst/cont1", doc);
        assertNotNull("Api /config/toaster2:lst/cont1 wasn't found", cont1Api);
        assertTrue("POST for cont11 in cont1 is missing",
            findOperation(cont1Api.getPost(), "toaster2/lst/cont1(config)cont11-TOP",
                    "toaster2/lst/cont1(config)lst11-TOP"));

        // no POST URI
        final Path cont11Api = findApi("/config/toaster2:lst/cont1/cont11", doc);
        assertNotNull("Api /config/toaster2:lst/cont1/cont11 wasn't found", cont11Api);
        assertTrue("POST operation shouldn't be present.", null == cont11Api.getPost());

    }

    /**
     * Tries to find operation with name {@code operationName} and with summary {@code summary}.
     */
    private boolean findOperation(final Operation operation, final String type,
                                  final String... searchedParameters) {
        if (operation != null) {
            if (operation.getResponses().containsKey(type)) {
                final List<Parameter> parameters = operation.getParameters();
                return containAllParameters(parameters, searchedParameters);
            }
        }
        return false;
    }

    private boolean containAllParameters(final List<Parameter> searchedIns, final String[] searchedWhats) {
        for (final String searchedWhat : searchedWhats) {
            boolean parameterFound = false;
            for (final Parameter searchedIn : searchedIns) {
                if (searchedIn.getName().equals(searchedWhat)) {
                    parameterFound = true;
                }
            }
            if (!parameterFound) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to find {@code Api} with path {@code path}.
     */
    private Path findApi(final String path, final Swagger doc) {
        return doc.getPath(path);
    }

    /**
     * Validates whether doc {@code doc} contains concrete specified models.
     */
    private void validateSwaggerModules(final Swagger doc) {
        final Map<String, Model> models = doc.getDefinitions();
        assertNotNull(models);

        final Model configLstTop = models.get("toaster2(config)lst-TOP");
        assertNotNull(configLstTop);

        containsReferences(configLstTop, "toaster2:lst", "toaster2(config)");

        final Model configLst = models.get("toaster2(config)lst");
        assertNotNull(configLst);

        containsReferences(configLst, "toaster2:lst1", "toaster2/lst(config)");
        containsReferences(configLst, "toaster2:cont1", "toaster2/lst(config)");

        final Model configLst1Top = models.get("toaster2/lst(config)lst1-TOP");
        assertNotNull(configLst1Top);

        containsReferences(configLst1Top, "toaster2:lst1", "toaster2/lst(config)");

        final Model configLst1 = models.get("toaster2/lst(config)lst1");
        assertNotNull(configLst1);

        final Model configCont1Top = models.get("toaster2/lst(config)cont1-TOP");
        assertNotNull(configCont1Top);

        containsReferences(configCont1Top, "toaster2:cont1", "toaster2/lst(config)");
        final Model configCont1 = models.get("toaster2/lst(config)cont1");
        assertNotNull(configCont1);

        containsReferences(configCont1, "toaster2:cont11", "toaster2/lst/cont1(config)");
        containsReferences(configCont1, "toaster2:lst11", "toaster2/lst/cont1(config)");

        final Model configCont11Top = models.get("toaster2/lst/cont1(config)cont11-TOP");
        assertNotNull(configCont11Top);

        containsReferences(configCont11Top, "toaster2:cont11", "toaster2/lst/cont1(config)");
        final Model configCont11 = models.get("toaster2/lst/cont1(config)cont11");
        assertNotNull(configCont11);

        final Model configlst11Top = models.get("toaster2/lst/cont1(config)lst11-TOP");
        assertNotNull(configlst11Top);

        containsReferences(configlst11Top, "toaster2:lst11", "toaster2/lst/cont1(config)");
        final Model configLst11 = models.get("toaster2/lst/cont1(config)lst11");
        assertNotNull(configLst11);
    }

    /**
     * Checks whether object {@code mainObject} contains in properties/items key $ref with concrete value.
     */
    private void containsReferences(final Model mainObject, final String childObject, final String prefix) {
        final Map<String, Property> properties = mainObject.getProperties();
        assertNotNull(properties);

        final Property nodeInProperties = properties.get(childObject);
        assertNotNull(nodeInProperties);

        if(nodeInProperties instanceof RefProperty) {
            final String itemRef = ((RefProperty) nodeInProperties).get$ref();
            assertEquals(prefix + childObject.split(":")[1], itemRef);
        } else if(nodeInProperties instanceof ArrayProperty) {
            final Property itemsInNodeInProperties = ((ArrayProperty) nodeInProperties).getItems();
            assertNotNull(itemsInNodeInProperties);
        }
    }

    @Test
    public void testEdgeCases() throws Exception {
        Preconditions.checkArgument(this.helper.getModules() != null, "No modules found");

        for (final Module m : this.helper.getModules()) {
            if (m.getQNameModule().getNamespace().toString().equals(NAMESPACE_2)
                    && m.getQNameModule().getRevision().equals(REVISION_2)) {
                final Swagger doc = this.generator.getSwaggerDocSpec(m, info, "",
                        this.schemaContext);
                assertNotNull(doc);

                // testing bugs.opendaylight.org bug 1290. UnionType model type.
                final Map<String, Model> jsonString = doc.getDefinitions();
                assertEquals("", jsonString.get("testUnion"));
            }
        }
    }

    @Test
    public void testRPCsModel() throws Exception {
        Preconditions.checkArgument(this.helper.getModules() != null, "No modules found");

        for (final Module m : this.helper.getModules()) {
            if (m.getQNameModule().getNamespace().toString().equals(NAMESPACE_2)
                    && m.getQNameModule().getRevision().equals(REVISION_2)) {
                final Swagger doc = this.generator.getSwaggerDocSpec(m, info, "",
                        this.schemaContext);
                assertNotNull(doc);

                final Map<String, Model> models = doc.getDefinitions();
                final Model inputTop = models.get("(make-toast)input-TOP");
                Property prop = inputTop.getProperties().get("toaster:input");
                assertNotNull(prop);
                assertEquals("object", prop.getType());
                assertEquals("#/definitions/(make-toast)input", ((RefProperty)((ObjectProperty)prop).getProperties().get("items")).get$ref());
                final Model input = models.get("(make-toast)input");
                final Map<String, Property> properties = input.getProperties();
                assertTrue(properties.containsKey("toaster:toasterDoneness"));
                assertTrue(properties.containsKey("toaster:toasterToastType"));
            }
        }
    }

    /**
     * Tests whether from yang files are generated all required paths for HTTP operations (GET, DELETE, PUT, POST)
     *
     * <p>
     * If container | list is augmented then in path there should be specified module name followed with collon (e. g.
     * "/config/module1:element1/element2/module2:element3")
     *
     * @param doc Api declaration
     * @throws Exception if operation fails
     */
    private void validateToaster(final Swagger doc) throws Exception {
        final Set<String> expectedUrls = new TreeSet<>(Arrays.asList(new String[]{"/config/toaster2:toaster",
            "/operational/toaster2:toaster", "/operations/toaster2:cancel-toast",
            "/operations/toaster2:make-toast", "/operations/toaster2:restock-toaster",
            "/config/toaster2:toaster/toasterSlot/{slotId}/toaster-augmented:slotInfo"}));

        final Set<String> actualUrls = new TreeSet<>();

        Path configApi = null;
        for (final Entry<String, Path> api : doc.getPaths().entrySet()) {
            actualUrls.add(api.getKey());
            if (api.getKey().contains("/config/toaster2:toaster/")) {
                configApi = api.getValue();
            }
        }

        boolean containsAll = actualUrls.containsAll(expectedUrls);
        if (!containsAll) {
            expectedUrls.removeAll(actualUrls);
            fail("Missing expected urls: " + expectedUrls);
        }

        final Set<String> expectedConfigMethods = new TreeSet<>(Arrays.asList(new String[] { "GET", "PUT", "DELETE" }));
        final Set<String> actualConfigMethods = new TreeSet<>();
        
        if(configApi.getGet() != null) {
            actualConfigMethods.add("GET");
        }
        if(configApi.getPut() != null) {
            actualConfigMethods.add("PUT");
        }
        if(configApi.getDelete() != null) {
            actualConfigMethods.add("DELETE");
        }
        
        containsAll = actualConfigMethods.containsAll(expectedConfigMethods);
        if (!containsAll) {
            expectedConfigMethods.removeAll(actualConfigMethods);
            fail("Missing expected method on config API: " + expectedConfigMethods);
        }

        // TODO: we should really do some more validation of the
        // documentation...
        /*
         * Missing validation: Explicit validation of URLs, and their methods Input / output models.
         */
    }

    @Test
    public void testGetResourceListing() throws Exception {
        final UriInfo info = this.helper.createMockUriInfo(HTTP_HOST);
        final SchemaService mockSchemaService = this.helper.createMockSchemaService(this.schemaContext);

        this.generator.setSchemaService(mockSchemaService);

        final Swagger resourceListing = this.generator.getResourceListing(info, this.schemaContext, "");

        String toaster = null;
        String toaster2 = null;
        for (final Entry<String, Path> r : resourceListing.getPaths().entrySet()) {
            final String path = r.getKey();
            if (path.contains("toaster2")) {
                toaster2 = r.getKey();
            } else if (path.contains("toaster")) {
                toaster = r.getKey();
            }
        }

        assertNotNull(toaster2);
        assertNotNull(toaster);

        assertEquals(HTTP_HOST + "/toaster(2009-11-20)", toaster);
        assertEquals(HTTP_HOST + "/toaster2(2009-11-20)", toaster2);
    }

    private void validateTosterDocContainsModulePrefixes(final Swagger doc) {
        final Map<String, Model> topLevelJson = doc.getDefinitions();

        final Model configToaster = topLevelJson.get("toaster2(config)toaster");
        assertNotNull("(config)toaster JSON object missing", configToaster);
        // without module prefix
        containsProperties(configToaster, "toaster2:toasterSlot");

        final Model toasterSlot = topLevelJson.get("toaster2/toaster(config)toasterSlot");
        assertNotNull("(config)toasterSlot JSON object missing", toasterSlot);
        // with module prefix
        containsProperties(toasterSlot, "toaster2:toaster-augmented:slotInfo");
    }

    private void containsProperties(final Model jsonObject, final String... properties) {
        for (final String property : properties) {
            final Property concretePropertyObject = jsonObject.getProperties().get(property);
            assertNotNull(property + " is missing", concretePropertyObject);
        }
    }
}