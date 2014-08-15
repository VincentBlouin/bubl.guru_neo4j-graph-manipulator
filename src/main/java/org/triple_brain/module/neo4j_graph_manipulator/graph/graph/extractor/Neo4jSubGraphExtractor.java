package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.graph.SubGraphPojo;
import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgePojo;
import org.triple_brain.module.model.graph.vertex.*;
import org.triple_brain.module.neo4j_graph_manipulator.graph.*;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexInSubGraphOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.image.Neo4jImages;

import java.net.URI;
import java.util.*;

import static org.neo4j.helpers.collection.MapUtil.map;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jSubGraphExtractor {
    Neo4jVertexFactory vertexFactory;
    Neo4jEdgeFactory edgeFactory;
    QueryEngine engine;
    URI centerVertexUri;
    Integer depth;
    private SubGraphPojo subGraph = SubGraphPojo.withVerticesAndEdges(
            new HashMap<URI, VertexInSubGraphPojo>(),
            new HashMap<URI, EdgePojo>()
    );

    @AssistedInject
    protected Neo4jSubGraphExtractor(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            QueryEngine engine,
            @Assisted URI centerVertexUri,
            @Assisted Integer depth
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.engine = engine;
        this.centerVertexUri = centerVertexUri;
        this.depth = depth;
    }

    public SubGraphPojo load() {
        QueryResult<Map<String, Object>> result = engine.query(
                queryToGetGraph(),
                map()
        );
        for (Map<String, Object> row : result) {
            if (isVertexFromRow(row)) {
                VertexInSubGraph vertexInSubGraph = addOrUpdateVertexUsingRow(
                        row
                );
                Integer distanceFromCenterVertex = (Integer) (
                        row.get("length(path)")
                );
                setDistanceFromCenterVertexToVertexIfApplicable(
                        vertexInSubGraph,
                        distanceFromCenterVertex / 2
                );
            } else {
                addOrUpdateEdgeUsingRow(
                        row
                );
            }
        }
        return subGraph;
    }

    private Boolean isVertexFromRow(Map<String, Object> row) {
        return row.get("type").toString().contains("vertex");
    }

    private void setDistanceFromCenterVertexToVertexIfApplicable(
            VertexInSubGraph vertexInSubGraph,
            Integer distance
    ) {
        if (vertexInSubGraph.minDistanceFromCenterVertex() == -1 || vertexInSubGraph.minDistanceFromCenterVertex() > distance) {
            vertexInSubGraph.setMinDistanceFromCenterVertex(distance);
        }
    }

    private VertexInSubGraph addOrUpdateVertexUsingRow(Map<String, Object> row) {
        URI uri = URI.create(
                row.get("in_path_node.uri").toString()
        );
        VertexInSubGraph vertex;
        if (subGraph.vertices().containsKey(uri)) {
            vertex = subGraph.vertexWithIdentifier(uri);
            new VertexFromExtractorQueryRow(row, "in_path_node").update(
                    (VertexInSubGraphPojo) vertex
            );
            return vertex;
        }
        vertex = new VertexFromExtractorQueryRow(row, "in_path_node").build();

        subGraph.addVertex(
                (VertexInSubGraphPojo) vertex
        );
        return vertex;
    }

    private String queryToGetGraph() {
        return "START start_node=node:node_auto_index(uri='" + centerVertexUri + "') " +
                "MATCH path=start_node<-[:" +
                Relationships.SOURCE_VERTEX +
                "|" + Relationships.DESTINATION_VERTEX + "*0.." + depth * 2 +
                "]->in_path_node " +
                "OPTIONAL MATCH (in_path_node)-[:HAS_INCLUDED_VERTEX]->(in_path_node_included_vertex) " +
                "OPTIONAL MATCH (in_path_node)-[:HAS_INCLUDED_EDGE]->(in_path_node_included_edge) " +
                "OPTIONAL MATCH (in_path_node_included_edge)-[:" + Relationships.SOURCE_VERTEX + "]->(in_path_node_included_edge_source_vertex) " +
                "OPTIONAL MATCH (in_path_node_included_edge)-[:" + Relationships.DESTINATION_VERTEX + "]->(in_path_node_included_edge_destination_vertex) " +
                "OPTIONAL MATCH (in_path_node)-[:" + Relationships.IDENTIFIED_TO + "]->(in_path_node_generic_identification) " +
                "OPTIONAL MATCH (in_path_node)-[:" + Relationships.TYPE + "]->(in_path_node_type) " +
                "OPTIONAL MATCH (in_path_node)-[:" + Relationships.SAME_AS + "]->(in_path_node_same_as) " +
                "RETURN " +
                vertexReturnQueryPart("in_path_node") +
                edgeReturnQueryPart("in_path_node") +
                "labels(in_path_node) as type, " +
                "length(path)";

    }

    private String edgeReturnQueryPart(String prefix) {
        return edgeSpecificPropertiesQueryPartUsingPrefix(prefix) +
                friendlyResourceReturnQueryPartUsingPrefix(prefix) +
                genericIdentificationReturnQueryPart(prefix) +
                typeReturnQueryPart(prefix) +
                sameAsReturnQueryPart(prefix);
    }

    private String vertexReturnQueryPart(String prefix) {
        return vertexSpecificPropertiesQueryPartUsingPrefix(prefix) +
                friendlyResourceReturnQueryPartUsingPrefix(prefix) +
                includedElementQueryPart(prefix + "_included_vertex") +
                includedEdgeQueryPart(prefix) +
                imageReturnQueryPart(prefix) +
                genericIdentificationReturnQueryPart(prefix) +
                typeReturnQueryPart(prefix) +
                sameAsReturnQueryPart(prefix);
    }

    private static String includedEdgeQueryPart(String prefix) {
        String key = prefix + "_included_edge";
        return includedElementQueryPart(
                key
        ) + includedElementQueryPart(
                key + "_source_vertex"
        ) + includedElementQueryPart(
                key + "_destination_vertex"
        );
    }

    public static String includedElementQueryPart(String key) {
        return getPropertyUsingContainerNameQueryPart(
                key,
                Neo4jUserGraph.URI_PROPERTY_NAME
        ) + getPropertyUsingContainerNameQueryPart(
                key,
                "`" + RDFS.label.getURI() + "`"
        );
    }

    private static String imageReturnQueryPart(String prefix) {
        return getPropertyUsingContainerNameQueryPart(
                prefix,
                Neo4jImages.props.images.name()
        );
    }

    private String typeReturnQueryPart(String prefix) {
        return identificationReturnQueryPart(
                prefix + "_type"
        );
    }

    private String sameAsReturnQueryPart(String prefix) {
        return identificationReturnQueryPart(
                prefix + "_same_as"
        );
    }

    private String genericIdentificationReturnQueryPart(String prefix) {
        return identificationReturnQueryPart(
                prefix + "_generic_identification"
        );
    }


    private String identificationReturnQueryPart(String prefix) {
        return getPropertyUsingContainerNameQueryPart(
                prefix, Neo4jIdentification.props.external_uri.name()
        ) +
                friendlyResourceReturnQueryPartUsingPrefix(
                        prefix
                ) + imageReturnQueryPart(prefix);
    }

    private String edgeSpecificPropertiesQueryPartUsingPrefix(String prefix) {
        return getPropertyUsingContainerNameQueryPart(
                prefix,
                Neo4jEdgeOperator.props.source_vertex_uri.toString()
        ) +
                getPropertyUsingContainerNameQueryPart(
                        prefix,
                        Neo4jEdgeOperator.props.destination_vertex_uri.toString()
                );
    }

    private String vertexSpecificPropertiesQueryPartUsingPrefix(String prefix) {
        return getPropertyUsingContainerNameQueryPart(
                prefix,
                Neo4jVertexInSubGraphOperator.props.number_of_connected_edges_property_name.toString()
        ) +
                getPropertyUsingContainerNameQueryPart(
                        prefix,
                        Neo4jVertexInSubGraphOperator.props.is_public.name()
                ) +
                getPropertyUsingContainerNameQueryPart(
                        prefix,
                        Neo4jVertexInSubGraphOperator.props.suggestions.name()
                );
    }

    public static String friendlyResourceReturnQueryPartUsingPrefix(String prefix) {
        return
                getPropertyUsingContainerNameQueryPart(
                        prefix,
                        Neo4jUserGraph.URI_PROPERTY_NAME
                ) +
                        getPropertyUsingContainerNameQueryPart(
                                prefix,
                                "`" + RDFS.label.getURI() + "`"
                        ) +
                        getPropertyUsingContainerNameQueryPart(
                                prefix,
                                "`" + RDFS.comment.getURI() + "`"
                        ) +
                        getPropertyUsingContainerNameQueryPart(
                                prefix,
                                Neo4jFriendlyResource.props.creation_date.name()
                        ) +
                        getPropertyUsingContainerNameQueryPart(
                                prefix,
                                Neo4jFriendlyResource.props.last_modification_date.name()
                        );
    }

    private static String getPropertyUsingContainerNameQueryPart(String containerName, String propertyName) {
        return containerName + "." + propertyName + ", ";
    }

    private Edge addOrUpdateEdgeUsingRow(Map<String, Object> row) {
        URI uri = URI.create(
                row.get(
                        "in_path_node." +
                                Neo4jUserGraph.URI_PROPERTY_NAME
                ).toString()
        );

        if (subGraph.hasEdgeWithUri(uri)) {
            return EdgeFromExtractorQueryRow.usingRow(row).update(
                    subGraph.edgeWithIdentifier(uri)
            );
        }
        EdgePojo edge = (EdgePojo) EdgeFromExtractorQueryRow.usingRow(
                row
        ).build();
        subGraph.addEdge(edge);
        return edge;
    }
}
