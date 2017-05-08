/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor;

import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraph;

public class IdentificationQueryBuilder {

    public static final String
            IDENTIFIER_QUERY_KEY = "id",
            IDENTIFICATION_RELATION_QUERY_KEY = "idr";

    public static String identificationReturnQueryPart() {
        return identificationReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
                IDENTIFIER_QUERY_KEY,
                IDENTIFICATION_RELATION_QUERY_KEY
        );
    }
    public static String identificationReturnQueryPartUsingKeysForIdentificationRelationAndAlias(
            String identificationKey,
            String relationKey
    ) {
        return "COLLECT([" +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        Neo4jIdentification.props.external_uri.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        Neo4jUserGraph.URI_PROPERTY_NAME
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        Neo4jFriendlyResource.props.label.toString()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        Neo4jFriendlyResource.props.comment.toString()
                ) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(
                        identificationKey
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        identificationKey,
                        Neo4jIdentification.props.nb_references.name()
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        relationKey,
                        Neo4jIdentification.props.relation_external_uri.name()
                ) +
                "]) as " + identificationKey + ", ";
    }
}
