/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.netconf.sal.rest.doc.mountpoints.MountPointSwagger;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class MountPointSwaggerTest {

    private static final String HTTP_URL = "http://localhost/path";
    private static final YangInstanceIdentifier INSTANCE_ID = YangInstanceIdentifier.builder()
            .node(QName.create("nodes"))
            .node(QName.create("node"))
            .nodeWithKey(QName.create("node"), QName.create("id"), "123").build();
    private static final String INSTANCE_URL = "/nodes/node/123/";
    private MountPointSwagger swagger;
    private DocGenTestHelper helper;

    @Before
    public void setUp() throws Exception {
        this.swagger = new MountPointSwagger();
        this.helper = new DocGenTestHelper();
        this.helper.setUp("/yang/toaster", "/yang/toaster2", "/yang/opflex", "/yang/toaster-augmented");
    }

    @Test()
    public void testGetResourceListBadIid() throws Exception {
        final UriInfo mockInfo = this.helper.createMockUriInfo(HTTP_URL);

        assertEquals(null, this.swagger.getResourceList(mockInfo, 1L));
    }

    @Test()
    public void getInstanceIdentifiers() throws Exception {
        setUpSwaggerForDocGeneration();

        assertEquals(0, this.swagger.getInstanceIdentifiers().size());
        this.swagger.onMountPointCreated(INSTANCE_ID); // add this ID into the list of
                                                 // mount points
        assertEquals(1, this.swagger.getInstanceIdentifiers().size());
        assertEquals((Long) 1L, this.swagger.getInstanceIdentifiers().entrySet().iterator().next()
                .getValue());
        assertEquals(INSTANCE_URL, this.swagger.getInstanceIdentifiers().entrySet().iterator().next()
                .getKey());
        this.swagger.onMountPointRemoved(INSTANCE_ID); // remove ID from list of mount
                                                 // points
        assertEquals(0, this.swagger.getInstanceIdentifiers().size());
    }

    @Test
    public void testGetResourceListGoodId() throws Exception {
        final UriInfo mockInfo = setUpSwaggerForDocGeneration();
        this.swagger.onMountPointCreated(INSTANCE_ID); // add this ID into the list of
                                                 // mount points
        final Swagger resourceList = this.swagger.getResourceList(mockInfo, 1L);
        TestingUtils.printSwagger(resourceList);
        Path dataStoreResource = null;
        for (final Entry<String, Path> r : resourceList.getPaths().entrySet()) {
            if (r.getKey().endsWith("/Datastores(-)")) {
                dataStoreResource = r.getValue();
            }
        }
        assertNotNull("Failed to find data store resource", dataStoreResource);
    }

    @Test
    public void testGetDataStoreApi() throws Exception {
        final UriInfo mockInfo = setUpSwaggerForDocGeneration();
        this.swagger.onMountPointCreated(INSTANCE_ID); // add this ID into the list of
                                                 // mount points

        final Swagger mountPointApi = this.swagger.getMountPointApi(mockInfo, 1L, "Datastores", "-");
        assertNotNull("failed to find Datastore API", mountPointApi);
        final Map<String, Path> apis = mountPointApi.getPaths();
        assertEquals("Unexpected api list size", 3, apis.size());

        final Set<String> actualApis = new TreeSet<>();
        for (final Entry<String, Path> api : apis.entrySet()) {
            actualApis.add(api.getKey());
            final List<Operation> operations = api.getValue().getOperations();
            assertEquals("unexpected operation size on " + api.getValue(), 1, operations.size());
            assertNotNull("expected non-null operation " + api.getValue().getGet(), api.getValue().getGet().getClass());
            assertNotNull("expected non-null desc on " + api.getValue(), operations.get(0)
                    .getDescription());
        }
        final Set<String> expectedApis = new TreeSet<>(Arrays.asList(new String[] {
            "/config" + INSTANCE_URL + "yang-ext:mount",
            "/operational" + INSTANCE_URL + "yang-ext:mount",
            "/operations" + INSTANCE_URL + "yang-ext:mount",}));
        assertEquals(expectedApis, actualApis);
    }

    protected UriInfo setUpSwaggerForDocGeneration() throws URISyntaxException {
        final UriInfo mockInfo = this.helper.createMockUriInfo(HTTP_URL);
        // We are sharing the global schema service and the mount schema service
        // in our test.
        // OK for testing - real thing would have seperate instances.
        final SchemaContext context = this.helper.createMockSchemaContext();
        final SchemaService schemaService = this.helper.createMockSchemaService(context);

        final DOMMountPoint mountPoint = mock(DOMMountPoint.class);
        when(mountPoint.getSchemaContext()).thenReturn(context);

        final DOMMountPointService service = mock(DOMMountPointService.class);
        when(service.getMountPoint(INSTANCE_ID)).thenReturn(Optional.of(mountPoint));
        this.swagger.setMountService(service);
        this.swagger.setGlobalSchema(schemaService);

        return mockInfo;
    }

}
