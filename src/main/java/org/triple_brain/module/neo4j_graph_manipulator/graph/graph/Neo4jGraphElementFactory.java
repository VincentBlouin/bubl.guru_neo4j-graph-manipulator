package org.triple_brain.module.neo4j_graph_manipulator.graph.graph;

import org.neo4j.graphdb.Node;
import org.triple_brain.module.model.graph.GraphElementOperatorFactory;

import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public interface Neo4jGraphElementFactory extends GraphElementOperatorFactory{
    Neo4jGraphElementOperator withNode(
            Node node
    );
    @Override
    Neo4jGraphElementOperator withUri(URI uri);
}