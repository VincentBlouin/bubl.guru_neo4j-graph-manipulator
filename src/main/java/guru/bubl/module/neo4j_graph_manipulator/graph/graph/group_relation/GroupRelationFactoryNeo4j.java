/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.group_relation;

import java.net.URI;

public interface GroupRelationFactoryNeo4j {
    GroupRelationOperatorNeo4j withUri(
            URI uri
    );
}
