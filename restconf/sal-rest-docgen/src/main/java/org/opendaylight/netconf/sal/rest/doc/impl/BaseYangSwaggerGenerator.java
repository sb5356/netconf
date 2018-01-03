/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.rest.doc.impl;

import static org.opendaylight.netconf.sal.rest.doc.util.RestDocgenUtil.resolvePathArgumentsName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Preconditions;

import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.RefProperty;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.ws.rs.core.UriInfo;

import org.opendaylight.netconf.sal.rest.doc.model.builder.OperationBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypedSchemaNode;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseYangSwaggerGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(BaseYangSwaggerGenerator.class);

    protected static final String API_VERSION = "1.0.0";
    protected static final String SWAGGER_VERSION = "2.0";
    protected static final String RESTCONF_CONTEXT_ROOT = "restconf";
    private static final String RESTCONF_DRAFT = "18";
	private static final String SWAGGER_TITLE = "OpenDaylight RestConf API Documentation";

    static final String MODULE_NAME_SUFFIX = "_module";

    private final ModelGenerator modelConverter = new ModelGenerator();

    // private Map<String, ApiDeclaration> MODULE_DOC_CACHE = new HashMap<>()
    private final ObjectMapper mapper = new ObjectMapper();
    private static boolean newDraft;

    protected BaseYangSwaggerGenerator() {
        this.mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    /**
     * Return list of modules converted to swagger compliant resource list.
     */
    public Swagger getResourceListing(final UriInfo uriInfo, final SchemaContext schemaContext,
            final String context) {
        return getResourceListing(uriInfo, schemaContext, context, createInitialSwagger(uriInfo));
    }
    
    public Swagger getResourceListing(final UriInfo uriInfo, final SchemaContext schemaContext,
            final String context, Swagger swagger) {
    	final Set<Module> modules = getSortedModules(schemaContext);

        final Map<String, Path> resources = new HashMap<>(modules.size());

        LOG.debug("Modules found [{}]", modules.size());

        for (final Module module : modules) {
            final String revisionString = module.getQNameModule().getFormattedRevision();
            LOG.debug("Working on [{},{}]...", module.getName(), revisionString);
            final Swagger resource =
            		getApiDeclaration(module.getName(), revisionString, uriInfo, schemaContext, context);

            if (resource != null) {
            	for(Entry<String, Path> path : resource.getPaths().entrySet()) {
            		resources.put(generatePath(uriInfo, module.getName(), revisionString) + path.getKey(), path.getValue());
            	}
            } else {
                LOG.warn("Could not generate doc for {},{}", module.getName(), revisionString);
            }
        }

        swagger.setPaths(resources);

        return swagger;
    }

    protected Swagger createInitialSwagger(final UriInfo uriInfo) {
        final Swagger swagger = new Swagger();
        swagger.info(new Info());
        swagger.getInfo().setTitle(SWAGGER_TITLE);
        swagger.getInfo().setVersion(API_VERSION);
        swagger.setBasePath("/" + RESTCONF_CONTEXT_ROOT);
        swagger.setProduces(Arrays.asList("application/json", "application/xml"));
        swagger.setHost(createHostFromUriInfo(uriInfo));
        swagger.scheme(Scheme.forValue(uriInfo.getBaseUri().getScheme()));
        swagger.setPaths(new LinkedHashMap<String, Path>());
        return swagger;
    }

    protected String generatePath(final UriInfo uriInfo, final String name, final String revision) {
        return "/" + generateCacheKey(name, revision);
    }

    public Swagger getApiDeclaration(final String moduleName, final String revision, final UriInfo uriInfo,
            final SchemaContext schemaContext, final String context) {
        Date rev = null;

        try {
            if (revision != null && !SourceIdentifier.NOT_PRESENT_FORMATTED_REVISION.equals(revision)) {
                rev = SimpleDateFormatUtil.getRevisionFormat().parse(revision);
            }
        } catch (final ParseException e) {
            throw new IllegalArgumentException(e);
        }

        if (rev != null) {
            final Calendar cal = new GregorianCalendar();

            cal.setTime(rev);

            if (cal.get(Calendar.YEAR) < 1970) {
                rev = null;
            }
        }

        final Module module = schemaContext.findModuleByName(moduleName, rev);
        Preconditions.checkArgument(module != null,
                "Could not find module by name,revision: " + moduleName + "," + revision);

        return getApiDeclaration(module, rev, uriInfo, context, schemaContext);
    }

    public Swagger getApiDeclaration(final Module module, final Date revision, final UriInfo uriInfo,
            final String context, final SchemaContext schemaContext) {
        return getSwaggerDocSpec(module, uriInfo, context, schemaContext);
    }

    protected String createHostFromUriInfo(final UriInfo uriInfo) {
        String portPart = "";
        final int port = uriInfo.getBaseUri().getPort();
        if (port != -1) {
            portPart = ":" + port;
        }
        final String basePath =
                new StringBuilder(uriInfo.getBaseUri().getHost())
                        .append(portPart).toString();
        return basePath;
    }  
    
    public Swagger getSwaggerDocSpec(final Module module, final UriInfo uriInfo, final String context,
                                            final SchemaContext schemaContext, Swagger doc) {
        final Collection<DataSchemaNode> dataSchemaNodes = module.getChildNodes();
        LOG.debug("child nodes size [{}]", dataSchemaNodes.size());
        for (final DataSchemaNode node : dataSchemaNodes) {
            if ((node instanceof ListSchemaNode) || (node instanceof ContainerSchemaNode)) {
                LOG.debug("Is Configuration node [{}] [{}]", node.isConfiguration(), node.getQName().getLocalName());

                List<Parameter> pathParams = new ArrayList<>();
                String resourcePath;

                /*
                 * Only when the node's config statement is true, such apis as
                 * GET/PUT/POST/DELETE config are added for this node.
                 */
                if (node.isConfiguration()) { // This node's config statement is
                                              // true.
                    resourcePath = getDataStorePath("config", context);
                    addApis(node, doc, resourcePath, pathParams, schemaContext, true, module.getName(), "config");
                }
                pathParams = new ArrayList<>();
                resourcePath = getDataStorePath("operational", context);

                addApis(node, doc, resourcePath, pathParams, schemaContext, false, module.getName(), "operational");
            }
        }

        final Set<RpcDefinition> rpcs = module.getRpcs();
        for (final RpcDefinition rpcDefinition : rpcs) {
            final String resourcePath;
            resourcePath = getDataStorePath("operations", context);

            addRpcs(rpcDefinition, doc, resourcePath, schemaContext);
        }

        LOG.debug("Number of APIs found [{}]", doc.getPaths().size());

        if (!doc.getPaths().isEmpty()) {
            try {
            	doc.setDefinitions(this.modelConverter.convertToModels(module, schemaContext));
                if (LOG.isTraceEnabled()) {
                    LOG.trace(this.mapper.writeValueAsString(doc));
                }
            } catch (IOException e) {
                LOG.error("Exception occured in ModelGenerator", e);
            }

            return doc;
        }
        return doc;
    }
    
    public Swagger getSwaggerDocSpec(final Module module, final UriInfo uriInfo, final String context,
                                            final SchemaContext schemaContext) {
    	return getSwaggerDocSpec(module, uriInfo, context, schemaContext, createInitialSwagger(uriInfo));
    }

    protected String getDataStorePath(final String dataStore, final String context) {
        if (newDraft) {
            if ("config".contains(dataStore) || "operational".contains(dataStore)) {
                return "/" + RESTCONF_DRAFT + "/data" + context;
            }
            return "/" + RESTCONF_DRAFT + "/operations" + context;
        }

        return "/" + dataStore + context;
    }

    protected static String generateCacheKey(final String module, final String revision) {
        return module + "(" + revision + ")";
    }

    private void addApis(final DataSchemaNode node, final Swagger swagger, final String parentPath,
            final List<Parameter> parentPathParams, final SchemaContext schemaContext, final boolean addConfigApi,
            final String parentName, final String dataStore) {
        final List<Parameter> pathParams = new ArrayList<>(parentPathParams);

        final String resourcePath = parentPath + "/" + createPath(node, pathParams, schemaContext);
        LOG.debug("Adding path: [{}]", resourcePath);

        Iterable<DataSchemaNode> childSchemaNodes = Collections.<DataSchemaNode>emptySet();
        if ((node instanceof ListSchemaNode) || (node instanceof ContainerSchemaNode)) {
            final DataNodeContainer dataNodeContainer = (DataNodeContainer) node;
            childSchemaNodes = dataNodeContainer.getChildNodes();
        }
        final Path api = operation(node, pathParams, addConfigApi, childSchemaNodes, parentName);
        swagger.path(resourcePath.concat(getContent(dataStore)), api);

        for (final DataSchemaNode childNode : childSchemaNodes) {
            if ((childNode instanceof ListSchemaNode) || (childNode instanceof ContainerSchemaNode)) {
                // keep config and operation attributes separate.
                if (childNode.isConfiguration() == addConfigApi) {
                    final String newParent = parentName + "/" + node.getQName().getLocalName();
                    addApis(childNode, swagger, resourcePath, pathParams, schemaContext, addConfigApi, newParent,
                            dataStore);
                }
            }
        }
    }
    
    protected static String getContent(final String dataStore) {
        if (newDraft) {
            if ("operational".contains(dataStore)) {
                return "?content=nonconfig";
            } else if ("config".contains(dataStore)) {
                return "?content=config";
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    private static boolean containsListOrContainer(final Iterable<DataSchemaNode> nodes) {
        for (final DataSchemaNode child : nodes) {
            if (child instanceof ListSchemaNode || child instanceof ContainerSchemaNode) {
                return true;
            }
        }
        return false;
    }

    private static Path operation(final DataSchemaNode node, final List<Parameter> pathParams,
            final boolean isConfig, final Iterable<DataSchemaNode> childSchemaNodes, final String parentName) {
    	final Path path = new Path();

    	OperationBuilder.get(path, node.getQName().getLocalName(), node.getDescription(), isConfig, pathParams);

        if (isConfig) {
        	OperationBuilder.put(path, node.getQName().getLocalName(), node.getDescription(), parentName, pathParams);
        	OperationBuilder.delete(path, node.getQName().getLocalName(), node.getDescription(), pathParams);

            if (containsListOrContainer(childSchemaNodes)) {
            	operationPost(node.getQName().getLocalName(), node.getDescription(),
            			(DataNodeContainer)node, pathParams, isConfig, parentName + "/", path);
            }
        }
        
        return path;
    }

    private static void operationPost(final String name, final String description,
            final DataNodeContainer dataNodeContainer, final List<Parameter> pathParams, final boolean isConfig,
            final String parentName, Path path) {
        if (isConfig) {
        	OperationBuilder.post(path, parentName + name, description, parentName, dataNodeContainer, pathParams);
        }
    }

    private String createPath(final DataSchemaNode schemaNode, final List<Parameter> pathParams,
            final SchemaContext schemaContext) {
        final ArrayList<LeafSchemaNode> pathListParams = new ArrayList<>();
        final StringBuilder path = new StringBuilder();
        final String localName = resolvePathArgumentsName(schemaNode, schemaContext);
        path.append(localName);

        if ((schemaNode instanceof ListSchemaNode)) {
            final List<QName> listKeys = ((ListSchemaNode) schemaNode).getKeyDefinition();
            StringBuilder keyBuilder = null;
            if (newDraft) {
                keyBuilder = new StringBuilder("=");
            }

            for (final QName listKey : listKeys) {
                final DataSchemaNode dataChildByName = ((DataNodeContainer) schemaNode).getDataChildByName(listKey);
                pathListParams.add(((LeafSchemaNode) dataChildByName));
                final String pathParamIdentifier;
                if (newDraft) {
                    pathParamIdentifier = keyBuilder.append("{").append(listKey.getLocalName()).append("}").toString();
                } else {
                    pathParamIdentifier = "/{" + listKey.getLocalName() + "}";
                }
                path.append(pathParamIdentifier);

                final PathParameter pathParam = new PathParameter();
                pathParam.setName(listKey.getLocalName());
                pathParam.setDescription(dataChildByName.getDescription());
                pathParam.setRequired(true);
                pathParam.setType(modelConverter.processTypeDef(((TypedSchemaNode)dataChildByName).getType(), dataChildByName, schemaContext).getType());
                pathParams.add(pathParam);
                if (newDraft) {
                    keyBuilder = new StringBuilder(",");
                }
            }
        }
        return path.toString();
    }

    protected void addRpcs(final RpcDefinition rpcDefn, final Swagger swagger, final String parentPath,
            final SchemaContext schemaContext) {
        final Path rpc = new Path();
        final String resourcePath = parentPath + "/" + resolvePathArgumentsName(rpcDefn, schemaContext);

        final Operation operationSpec = new Operation();
        operationSpec.setDescription(rpcDefn.getDescription());
        operationSpec.setOperationId(rpcDefn.getQName().getLocalName());
        if (!rpcDefn.getOutput().getChildNodes().isEmpty()) {
        	final Response response = new Response();
        	final RefProperty property = new RefProperty();
        	property.set$ref("(" + rpcDefn.getQName().getLocalName() + ")output" + OperationBuilder.TOP);
        	response.setSchema(property);
        	response.setDescription(rpcDefn.getDescription());
            operationSpec.response(200, response);
        }
        if (!rpcDefn.getInput().getChildNodes().isEmpty()) {
            final BodyParameter payload = new BodyParameter();
            final RefModel model = new RefModel();
            model.set$ref("(" + rpcDefn.getQName().getLocalName() + ")input" + OperationBuilder.TOP);
            payload.setSchema(model);
            operationSpec.setParameters(Collections.singletonList(payload));
            operationSpec.setConsumes(OperationBuilder.CONSUMES_PUT_POST);
        }

        rpc.setPost(operationSpec);

        swagger.path(resourcePath, rpc);
    }

    protected SortedSet<Module> getSortedModules(final SchemaContext schemaContext) {
        if (schemaContext == null) {
            return new TreeSet<>();
        }

        final Set<Module> modules = schemaContext.getModules();

        final SortedSet<Module> sortedModules = new TreeSet<>((module1, module2) -> {
            int result = module1.getName().compareTo(module2.getName());
            if (result == 0) {
                final Date module1Revision = module1.getRevision() != null ? module1.getRevision() : new Date(0);
                final Date module2Revision = module2.getRevision() != null ? module2.getRevision() : new Date(0);
                result = module1Revision.compareTo(module2Revision);
            }
            if (result == 0) {
                result = module1.getNamespace().compareTo(module2.getNamespace());
            }
            return result;
        });
        for (final Module m : modules) {
            if (m != null) {
                sortedModules.add(m);
            }
        }
        return sortedModules;
    }

    public void setDraft(final boolean draft) {
        BaseYangSwaggerGenerator.newDraft = draft;
    }
}
