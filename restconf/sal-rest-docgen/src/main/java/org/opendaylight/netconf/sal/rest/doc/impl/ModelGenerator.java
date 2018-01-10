/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netconf.sal.rest.doc.impl;

import static org.opendaylight.netconf.sal.rest.doc.util.RestDocgenUtil.resolveNodesName;

import com.mifmif.common.regex.Generex;

import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.BinaryProperty;
import io.swagger.models.properties.BooleanProperty;
import io.swagger.models.properties.ByteArrayProperty;
import io.swagger.models.properties.DecimalProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.concurrent.NotThreadSafe;

import org.opendaylight.netconf.sal.rest.doc.model.builder.MethodName;
import org.opendaylight.netconf.sal.rest.doc.model.builder.OperationBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RevisionAwareXPath;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.TypedSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.BinaryTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.BitsTypeDefinition.Bit;
import org.opendaylight.yangtools.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EmptyTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.LengthConstraint;
import org.opendaylight.yangtools.yang.model.api.type.PatternConstraint;
import org.opendaylight.yangtools.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnsignedIntegerTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.RevisionAwareXPathImpl;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates JSON Schema for data defined in YANG.
 */
@NotThreadSafe
public class ModelGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(ModelGenerator.class);

    private static final Pattern STRIP_PATTERN = Pattern.compile("\\[[^\\[\\]]*\\]");

    private Module topLevelModule;

    /**
     * Creates Json models from provided module according to swagger spec.
     *
     * @param module        - Yang module to be converted
     * @param schemaContext - SchemaContext of all Yang files used by Api Doc
     * @return ObjectNode containing data used for creating examples and models in Api Doc
     * @throws IOException if I/O operation fails
     */
    public Map<String, Model> convertToModels(final Module module,
                                          final SchemaContext schemaContext) {
        final Map<String, Model> models = new LinkedHashMap<String, Model>();
        topLevelModule = module;
        processModules(module, models, schemaContext);
        processContainersAndLists(module, models, schemaContext);
        processRPCs(module, models, schemaContext);
        processIdentities(module, models);
        return models;
    }

    private void processModules(final Module module, final Map<String, Model> models,
                                final SchemaContext schemaContext) {
        createConcreteModelForPost(models, module.getName(),
                createPropertiesForPost(module, schemaContext, module.getName()));
    }

    private void processContainersAndLists(final Module module, final Map<String, Model> models,
                                           final SchemaContext schemaContext) {
        for (final DataSchemaNode childNode : module.getChildNodes()) {
            // For every container and list in the module
            if (childNode instanceof ContainerSchemaNode || childNode instanceof ListSchemaNode) {
                processDataNodeContainer((DataNodeContainer) childNode, "", models, true, schemaContext, module);
                processDataNodeContainer((DataNodeContainer) childNode, "", models, false, schemaContext, module);
            }
        }
    }

    /**
     * Process the RPCs for a Module Spits out a file each of the name
     * {@code <rpcName>-input.json and <rpcName>-output.json}
     * for each RPC that contains input & output elements.
     *
     * @param module module
     * @throws IOException if I/O operation fails
     */
    private void processRPCs(final Module module, final Map<String, Model> models,
                             final SchemaContext schemaContext)  {
        final Set<RpcDefinition> rpcs = module.getRpcs();
        final String moduleName = module.getName();
        for (final RpcDefinition rpc : rpcs) {
            final ContainerSchemaNode input = rpc.getInput();
            if (!input.getChildNodes().isEmpty()) {
                final Map<String, Property> properties =
                        processChildren(input.getChildNodes(), moduleName, models, true, schemaContext, module);

                final String filename = "(" + rpc.getQName().getLocalName() + ")input";
                final ModelImpl childSchema = getOperationTemplate();
                childSchema.setType(ModelImpl.OBJECT);
                childSchema.setProperties(properties);
                childSchema.setName(filename);
                models.put(filename, childSchema);

                processTopData(filename, models, input);
            }

            final ContainerSchemaNode output = rpc.getOutput();
            if (!output.getChildNodes().isEmpty()) {
            	final Map<String, Property> properties =
                        processChildren(output.getChildNodes(), moduleName, models, true, schemaContext, module);
                final String filename = "(" + rpc.getQName().getLocalName() + ")output";
                final ModelImpl childSchema = getOperationTemplate();
                childSchema.setType(ModelImpl.OBJECT);
                childSchema.setProperties(properties);
                childSchema.setName(filename);
                models.put(filename, childSchema);

                processTopData(filename, models, output);
            }
        }
    }

    private Property processTopData(final String filename, final Map<String, Model> models, final SchemaNode schemaNode) {
        return schemaNode instanceof ListSchemaNode ? 
        		processTopData(filename, models, (ListSchemaNode) schemaNode, new ArrayProperty()) : 
        			processTopData(filename, models, schemaNode, new ObjectProperty());
    }
    
    private Property processTopData(final String filename, final Map<String, Model> models, final ListSchemaNode schemaNode,  final ArrayProperty dataNodeProperties) {
        final RefProperty items = new RefProperty();

        items.set$ref(filename);
        dataNodeProperties.setItems(items);

        putDescriptionIfNonNull(dataNodeProperties, schemaNode.getDescription());
        final Map<String, Property> properties = new HashMap<String, Property>();;
        properties.put(topLevelModule.getName() + ":" + schemaNode.getQName().getLocalName(), dataNodeProperties);
        final ModelImpl finalChildSchema = getOperationTemplate();
        finalChildSchema.setType(ModelImpl.OBJECT);
        finalChildSchema.setProperties(properties);
        finalChildSchema.setName(filename + OperationBuilder.TOP);
        models.put(filename + OperationBuilder.TOP, finalChildSchema);

        return dataNodeProperties;
    }
    
    private Property processTopData(final String filename, final Map<String, Model> models, final SchemaNode schemaNode,  final ObjectProperty dataNodeProperties) {
        final RefProperty items = new RefProperty();

        items.set$ref(filename);
        final Map<String, Property> objectProperties = new HashMap<String, Property>();
        objectProperties.put("items", items);
        dataNodeProperties.setProperties(objectProperties);

        putDescriptionIfNonNull(dataNodeProperties, schemaNode.getDescription());
        final Map<String, Property> properties = new HashMap<String, Property>();
        properties.put(topLevelModule.getName() + ":" + schemaNode.getQName().getLocalName(), dataNodeProperties);
        final ModelImpl finalChildSchema = getOperationTemplate();
        finalChildSchema.setType(ModelImpl.OBJECT);
        finalChildSchema.setProperties(properties);
        finalChildSchema.setName(filename + OperationBuilder.TOP);
        models.put(filename + OperationBuilder.TOP, finalChildSchema);

        return dataNodeProperties;
    }

    /**
     * Processes the 'identity' statement in a yang model and maps it to a 'model' in the Swagger JSON spec.
     *
     * @param module The module from which the identity stmt will be processed
     * @param models The ObjectNode in which the parsed identity will be put as a 'model' obj
     */
    private static void processIdentities(final Module module, final Map<String, Model> models) {
    	//TODO i dont think this method is done right
        final String moduleName = module.getName();
        final Set<IdentitySchemaNode> idNodes = module.getIdentities();
        LOG.debug("Processing Identities for module {} . Found {} identity statements", moduleName, idNodes.size());

        for (final IdentitySchemaNode idNode : idNodes) {
        	processIdentity(idNode, models);
        }
    }

    /**
     * Process one item in the dientity statement
     *
     * @param module The module from which the identity stmt will be processed
     * @param models The ObjectNode in which the parsed identity will be put as a 'model' obj
     */
    private static void processIdentity(final IdentitySchemaNode idNode, final Map<String, Model> models) {
    	final Set<IdentitySchemaNode> baseIds = idNode.getBaseIdentities();
    	
        final Model identityObj;
        final String identityName = idNode.getQName().getLocalName();
        LOG.debug("Processing Identity: {}", identityName);

        final RefProperty props = new RefProperty();

        if (baseIds != null && baseIds.size() > 0) {
        	identityObj = new ComposedModel();
            /*
             * This is a derived entity. So lets see if it has sub types. If it does, then add them to the model
             * definition.
             */
        	final List<Model> parentTypes = new ArrayList<Model>();
        	for(IdentitySchemaNode baseId:  baseIds) {
	        	parentTypes.add(new RefModel(baseId.getQName().getLocalName()));
        	}
        	((ComposedModel)identityObj).setAllOf(parentTypes);
        	
            // Add the properties here so for a base type, this will be an empty object as required by the Swagger spec.
            Map<String, Property> propMap = new HashMap<>();
            propMap.put(identityName,props);
            identityObj.setProperties(propMap);
        } else {
        	// not sure if this is an object
        	final ModelImpl impl = new ModelImpl();
        	impl.setType(ModelImpl.OBJECT);
            /*
             * This is a base identity. Add it's base type & move on.
             */
        	identityObj = impl;
        }

        identityObj.setTitle(identityName);
        putDescriptionIfNonNull(identityObj, idNode.getDescription());

        models.put(identityName, identityObj);
    }
    
    private Property processDataNodeContainer(
            final DataNodeContainer dataNode, final String parentName, final Map<String, Model> models, final boolean isConfig,
            final SchemaContext schemaContext, final Module module)  {
        if (dataNode instanceof ListSchemaNode || dataNode instanceof ContainerSchemaNode) {
            final Iterable<DataSchemaNode> containerChildren = dataNode.getChildNodes();
            final String localName = ((SchemaNode) dataNode).getQName().getLocalName();
            final String nodeName;
            if(parentName != "") {
            	nodeName = parentName + "/" + resolveNodesName(((SchemaNode) dataNode), module, schemaContext);
            } else {
            	nodeName = (isConfig ? OperationBuilder.CONFIG : OperationBuilder.OPERATIONAL) + resolveNodesName(((SchemaNode) dataNode), module, schemaContext);
            }

            final Map<String, Property> properties =
                    processChildren(containerChildren, nodeName, models, isConfig, schemaContext, module);

            final ModelImpl childSchema = getOperationTemplate();
            childSchema.setType(ModelImpl.OBJECT);
            childSchema.setProperties(properties);

            childSchema.setName(nodeName);
            models.put(nodeName, childSchema);

            if (isConfig) {
                createConcreteModelForPost(models, localName,
                        createPropertiesForPost(dataNode, schemaContext, nodeName));
            }

            return processTopData(nodeName, models, (SchemaNode) dataNode);
        }
        return null;
    }

    private static void createConcreteModelForPost(final Map<String, Model> models, final String localName,
                                                   final Map<String, Property> properties) {
        final String nodePostName = OperationBuilder.CONFIG + localName + OperationBuilder.TOP;
        final ModelImpl postSchema = getOperationTemplate();
        postSchema.setType(ModelImpl.OBJECT);
        postSchema.setProperties(properties);
        models.put(nodePostName, postSchema);
    }

    private Map<String, Property> createPropertiesForPost(final DataNodeContainer dataNodeContainer,
                                               final SchemaContext schemaContext, final String parentName) {
        final Map<String, Property> properties = new LinkedHashMap<String, Property>();
        for (final DataSchemaNode childNode : dataNodeContainer.getChildNodes()) {
            if (childNode instanceof ListSchemaNode || childNode instanceof ContainerSchemaNode) {
                final RefProperty items = new RefProperty();
                items.set$ref(parentName + OperationBuilder.CONFIG + childNode.getQName().getLocalName());
                if(childNode instanceof ListSchemaNode) {
	                final ArrayProperty property = new ArrayProperty();
	                property.setItems(items);
	                properties.put(childNode.getQName().getLocalName(), property);
                } else {
	                final ObjectProperty property = new ObjectProperty();
	                final Map<String, Property> objectProperties = new HashMap<String, Property>();
	                objectProperties.put("items", items);
	                property.setProperties(objectProperties);
	                properties.put(childNode.getQName().getLocalName(), property);
                }
            } else if (childNode instanceof LeafSchemaNode) {
                final Property property = processLeafNode((LeafSchemaNode) childNode, schemaContext);
                properties.put(childNode.getQName().getLocalName(), property);
            }
        }
        return properties;
    }

    /**
     * Processes the nodes.
     */
    private Map<String, Property> processChildren(
            final Iterable<DataSchemaNode> nodes, final String parentName, final Map<String, Model> models,
            final boolean isConfig, final SchemaContext schemaContext, final Module module) {
        final Map<String, Property> properties = new HashMap<String, Property>();
        for (final DataSchemaNode node : nodes) {
            if (node.isConfiguration() == isConfig) {
                final String name = resolveNodesName(node, topLevelModule, schemaContext);
                final Property property;
                if (node instanceof LeafSchemaNode) {
                    property = processLeafNode((LeafSchemaNode) node, schemaContext);

                } else if (node instanceof ListSchemaNode) {
                    property = processDataNodeContainer((ListSchemaNode) node, parentName, models, isConfig,
                            schemaContext, module);

                } else if (node instanceof LeafListSchemaNode) {
                    property = processLeafListNode((LeafListSchemaNode) node, schemaContext);

                } else if (node instanceof ChoiceSchemaNode) {
                    if (((ChoiceSchemaNode) node).getCases().iterator().hasNext()) {
                        processChoiceNode(((ChoiceSchemaNode) node).getCases().iterator().next().getChildNodes(),
                                parentName, models, schemaContext, isConfig, properties, module);
                    }
                    continue;

                } else if (node instanceof AnyXmlSchemaNode) {
                    property = processAnyXMLNode((AnyXmlSchemaNode) node);

                } else if (node instanceof ContainerSchemaNode) {
                    property = processDataNodeContainer((ContainerSchemaNode) node, parentName, models, isConfig,
                            schemaContext, module);

                } else {
                    throw new IllegalArgumentException("Unknown DataSchemaNode type: " + node.getClass());
                }
                putDescriptionIfNonNull(property, node.getDescription());
                properties.put(name, property);
            }
        }
        return properties;
    }

    private Property processLeafListNode(final LeafListSchemaNode listNode,
                                           final SchemaContext schemaContext) {
        final ArrayProperty props = new ArrayProperty();

        final ConstraintDefinition constraints = listNode.getConstraints();
        final Property itemsVal = processTypeDef(listNode.getType(), listNode, schemaContext);
        props.setItems(itemsVal);

        processConstraints(constraints, props);

        return props;
    }

    private void processChoiceNode(
            final Iterable<DataSchemaNode> nodes, final String moduleName, final Map<String, Model> models,
            final SchemaContext schemaContext, final boolean isConfig, final Map<String, Property> properties, final Module module)
          {
        for (final DataSchemaNode node : nodes) {
            final String name = resolveNodesName(node, topLevelModule, schemaContext);
            final Property property;

            if (node instanceof LeafSchemaNode) {
                property = processLeafNode((LeafSchemaNode) node, schemaContext);

            } else if (node instanceof ListSchemaNode) {
                property = processDataNodeContainer((ListSchemaNode) node, moduleName, models, isConfig,
                        schemaContext, module);

            } else if (node instanceof LeafListSchemaNode) {
                property = processLeafListNode((LeafListSchemaNode) node, schemaContext);

            } else if (node instanceof ChoiceSchemaNode) {
                if (((ChoiceSchemaNode) node).getCases().iterator().hasNext()) {
                    processChoiceNode(((ChoiceSchemaNode) node).getCases().iterator().next().getChildNodes(),
                    		moduleName, models, schemaContext, isConfig, properties, module);
                }
                continue;

            } else if (node instanceof AnyXmlSchemaNode) {
                property = processAnyXMLNode((AnyXmlSchemaNode) node);

            } else if (node instanceof ContainerSchemaNode) {
                property = processDataNodeContainer((ContainerSchemaNode) node, module.getName(), models, isConfig,
                        schemaContext, module);

            } else {
                throw new IllegalArgumentException("Unknown DataSchemaNode type: " + node.getClass());
            }

            putDescriptionIfNonNull(property, node.getDescription());
            properties.put(name, property);
        }
    }

    private static void processConstraints(final ConstraintDefinition constraints,
                                           final Property props) {
        final boolean isMandatory = constraints.isMandatory();
        props.setRequired(isMandatory);

        final Integer minElements = constraints.getMinElements();
        final Integer maxElements = constraints.getMaxElements();
        if (minElements != null) {
            ((ArrayProperty)props).setMinItems(minElements);
        }
        if (maxElements != null) {
            ((ArrayProperty)props).setMaxItems(maxElements);
        }
    }

    private Property processLeafNode(final LeafSchemaNode leafNode,
                                       final SchemaContext schemaContext) {
        final String leafDescription = leafNode.getDescription();
        final Property property = processTypeDef(leafNode.getType(), leafNode, schemaContext);
        putDescriptionIfNonNull(property, leafDescription);
        processConstraints(leafNode.getConstraints(), property);
        return property;
    }

    private static Property processAnyXMLNode(final AnyXmlSchemaNode leafNode) {
        final Property property = new StringProperty();

        final String leafDescription = leafNode.getDescription();
        putDescriptionIfNonNull(property, leafDescription);

        processConstraints(leafNode.getConstraints(), property);
        final String localName = leafNode.getQName().getLocalName();
        property.setExample((Object)("example of anyxml " + localName));
        property.setName(localName);

        return property;
    }

    public Property processTypeDef(final TypeDefinition<?> leafTypeDef, final DataSchemaNode node,
                                  final SchemaContext schemaContext) {
    	final Property property;
        if (leafTypeDef instanceof BinaryTypeDefinition) {
        	property = processBinaryType();
        	
        } else if (leafTypeDef instanceof BitsTypeDefinition) {
        	property = processBitsType((BitsTypeDefinition) leafTypeDef);

        } else if (leafTypeDef instanceof EnumTypeDefinition) {
        	property = processEnumType((EnumTypeDefinition) leafTypeDef);

        } else if (leafTypeDef instanceof IdentityrefTypeDefinition) {
        	property = processIdentityrefType(node, schemaContext, (IdentityrefTypeDefinition) leafTypeDef);
        	
        } else if (leafTypeDef instanceof StringTypeDefinition) {
        	property = processStringType(leafTypeDef, node.getQName().getLocalName());

        } else if (leafTypeDef instanceof UnionTypeDefinition) {
        	property = processUnionType((UnionTypeDefinition) leafTypeDef, schemaContext, node);

        } else if (leafTypeDef instanceof EmptyTypeDefinition) {
        	property = processEmptyType();

        } else if (leafTypeDef instanceof LeafrefTypeDefinition) {
        	property = processLeafRef(node, schemaContext, (LeafrefTypeDefinition) leafTypeDef);

        } else if (leafTypeDef instanceof BooleanTypeDefinition) {
        	property = processBooleanType();

        } else if (leafTypeDef instanceof DecimalTypeDefinition) {
        	property = processDecimalType((DecimalTypeDefinition)leafTypeDef);
        	
        } else if (leafTypeDef instanceof IntegerTypeDefinition) {
        	property = processIntegerType((IntegerTypeDefinition)leafTypeDef);
        	
        } else if (leafTypeDef instanceof UnsignedIntegerTypeDefinition) {
        	property = processUnsignedIntegerType((UnsignedIntegerTypeDefinition)leafTypeDef);
        } else {
        	property = processObjectType();
        }
        return property;
    }

    private ObjectProperty processObjectType() {
    	final ObjectProperty property = new ObjectProperty();
		return property;
	}

	private IntegerProperty processUnsignedIntegerType(UnsignedIntegerTypeDefinition leafTypeDef) {
    	final IntegerProperty property = new IntegerProperty();
    	property.setExample((Object)String.valueOf(leafTypeDef.getRangeConstraints()
                .iterator().next().getMin()));
		return property;
	}
    
    private IntegerProperty processIntegerType(IntegerTypeDefinition leafTypeDef) {
    	final IntegerProperty property = new IntegerProperty();
    	property.setExample((Object)String.valueOf(leafTypeDef.getRangeConstraints()
                .iterator().next().getMin()));
		return property;
	}
    
    private DecimalProperty processDecimalType(DecimalTypeDefinition leafTypeDef) {
    	final DecimalProperty property = new DecimalProperty();
    	property.setExample((Object)String.valueOf(leafTypeDef.getRangeConstraints()
                .iterator().next().getMin()));
		return property;
	}
    
    private BooleanProperty processBooleanType() {
    	final BooleanProperty property = new BooleanProperty();
    	property.setExample((Object)"true");
		return property;
	}

	//TODO not sure if this is what we want sent
	private StringProperty processEmptyType() {
        final StringProperty property = new StringProperty();
        final List<String> enums = new ArrayList<>();
        enums.add("");
        property.setEnum(enums);
		return property;
	}

	private RefProperty processIdentityrefType(final DataSchemaNode node, final SchemaContext schemaContext, 
			final IdentityrefTypeDefinition leafTypeDef) {
		final RefProperty property = new RefProperty();
		final Module module = findModule(schemaContext, leafTypeDef.getQName());
        final String name;
        if(topLevelModule.getName().equals(module.getName())) {
        	//if the module is the same as the topLevelModule its in the current namespace and doesnt need the module name
        	name = "";
        } else {
        	name = module.getName() + ":";
        }
    	property.set$ref(name + ((IdentityrefTypeDefinition) leafTypeDef).getIdentity().getQName().getLocalName());
		return property;
	}

    private Property processLeafRef(final DataSchemaNode node, final SchemaContext schemaContext, 
    										final LeafrefTypeDefinition leafTypeDef) {
        RevisionAwareXPath xpath = leafTypeDef.getPathStatement();
        final SchemaNode schemaNode;

        final String xPathString = STRIP_PATTERN.matcher(xpath.toString()).replaceAll("");
        xpath = new RevisionAwareXPathImpl(xPathString, xpath.isAbsolute());

        final Module module;
        if (xpath.isAbsolute()) {
            module = findModule(schemaContext, leafTypeDef.getQName());
            schemaNode = SchemaContextUtil.findDataSchemaNode(schemaContext, module, xpath);
        } else {
            module = findModule(schemaContext, node.getQName());
            schemaNode = SchemaContextUtil.findDataSchemaNodeForRelativeXPath(schemaContext, module, node, xpath);
        }

        return processTypeDef(((TypedSchemaNode) schemaNode).getType(), (DataSchemaNode) schemaNode,
                schemaContext);
    }

    private static Module findModule(final SchemaContext schemaContext, final QName qualifiedName) {
        return schemaContext
                .findModuleByNamespaceAndRevision(qualifiedName.getNamespace(), qualifiedName.getRevision());
    }

    private static ByteArrayProperty processBinaryType() {
    	final ByteArrayProperty property = new ByteArrayProperty();
    	property.setExample((Object)("bin1 bin2"));
        return property;
    }

    private static StringProperty processEnumType(final EnumTypeDefinition enumLeafType) {
        final StringProperty property = new StringProperty();
        final List<EnumPair> enumPairs = enumLeafType.getValues();
        List<String> enumNames = new ArrayList<>();
        for (final EnumPair enumPair : enumPairs) {
            enumNames.add(enumPair.getName());
        }
        property.setEnum(enumNames);
    	property.setExample((Object)(enumLeafType.getValues().iterator().next().getName()));
        return property;
    }

    private static BinaryProperty processBitsType(final BitsTypeDefinition bitsType) {
    	final BinaryProperty property = new BinaryProperty();
    	property.setMinLength(0);
    	final List<String> enumNames = new ArrayList<>();
        final List<Bit> bits = bitsType.getBits();
        for (final Bit bit : bits) {
            enumNames.add(new String(bit.getName()));
        }
        property.setEnum(enumNames);
    	property.setExample((Object)(enumNames.iterator().next() + " " + enumNames.get(enumNames.size() - 1)));
        return property;
    }

    private static StringProperty processStringType(final TypeDefinition<?> stringType,
                                            final String nodeName) {
    	final StringProperty property = new StringProperty();
        StringTypeDefinition type = (StringTypeDefinition) stringType;
        List<LengthConstraint> lengthConstraints = ((StringTypeDefinition) stringType).getLengthConstraints();
        while (lengthConstraints.isEmpty() && type.getBaseType() != null) {
            type = type.getBaseType();
            lengthConstraints = type.getLengthConstraints();
        }

        final List<Integer> mins = new ArrayList<>();
        final List<Integer> maxs = new ArrayList<>();
        for (final LengthConstraint lengthConstraint : lengthConstraints) {
            // whats a non integer string length?
			final Integer min = (Integer) lengthConstraint.getMin();
            // whats a non integer string length?
			final Integer max = (Integer) lengthConstraint.getMax();
            if(min != null) {
            	mins.add(min);
            }
            if(max != null) {
            	maxs.add(max);
            }
        }
        if(mins.size() > 0) {
        	property.setMinLength(Collections.min(mins));
        }
        if(maxs.size() > 0) {
        	property.setMaxLength(Collections.max(maxs));
        }
        if (type.getPatternConstraints().iterator().hasNext()) {
            final PatternConstraint pattern = type.getPatternConstraints().iterator().next();
            String regex = pattern.getRegularExpression();
            regex = regex.substring(1, regex.length() - 1);
        	property.setPattern(regex);
            final Generex generex = new Generex(regex);
            property.setExample(generex.random());
        } else {
        	property.setExample("Some " + nodeName);
        }
        return property;
    }

    //TODO this can not really be expressed in json schema
    private StringProperty processUnionType(final UnionTypeDefinition unionType, final SchemaContext schemaContext,  
                                    final DataSchemaNode node) {
    	final StringProperty property = new StringProperty();
        final StringBuilder unionNames = new StringBuilder();
        for (final TypeDefinition<?> typeDef : unionType.getTypes()) {
            unionNames.append(processTypeDef(typeDef, node, schemaContext).getType());
        }
    	property.setFormat(unionNames.toString());
    	final String example = processTypeDef(unionType.getTypes().iterator().next(), node, schemaContext).getType();
    	property.setExample(example);
        return property;
    }

    private static ModelImpl getOperationTemplate() {
        final ModelImpl operation = new ModelImpl();
        return operation;
    }

    private static void putDescriptionIfNonNull(Property property, String desc) {
    	if (property != null && desc != null) {
    		property.setDescription(desc);
        }
    }
    
    private static void putDescriptionIfNonNull(Model model, String desc) {
    	if (model != null && desc != null) {
    		model.setDescription(desc);
        }
    }
}