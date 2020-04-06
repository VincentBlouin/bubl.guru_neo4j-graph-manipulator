/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph;

import org.neo4j.graphdb.RelationshipType;

public enum Relationships implements RelationshipType {
    LABEL, //RDFS.LABEL
    SOURCE,
    DESTINATION,
    HAS_TYPE, //RDF.TYPE
    SAME_AS, //OWL2.sameAs
    DOMAIN, //RDFS.DOMAIN
    IDENTIFIED_TO,
    HAS_IMAGE //todo find common rdf property for image
}
