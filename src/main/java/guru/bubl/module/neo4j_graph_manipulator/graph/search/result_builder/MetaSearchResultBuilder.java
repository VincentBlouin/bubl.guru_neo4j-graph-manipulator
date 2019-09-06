/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.search.result_builder;

import com.google.common.collect.ImmutableMap;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.model.search.GraphElementSearchResultPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;
import org.neo4j.driver.v1.Record;

import java.net.URI;

public class MetaSearchResultBuilder implements SearchResultBuilder {

    private Record row;
    private String prefix;

    public MetaSearchResultBuilder(Record row, String prefix) {
        this.row = row;
        this.prefix = prefix;
    }

    @Override
    public GraphElementSearchResult build() {
        FriendlyResourcePojo friendlyResourcePojo = FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                row,
                prefix
        ).build();
        IdentifierPojo identifierPojo = new IdentifierPojo(
                friendlyResourcePojo
        );

        identifierPojo.setExternalResourceUri(
                URI.create(
                        row.get("n.external_uri").asString()
                )
        );

        identifierPojo.setNbRefences(
                row.get("n.nb_references").asInt()
        );

        GraphElementPojo identifierAsGraphElement = new GraphElementPojo(
                identifierPojo.getFriendlyResource(),
                ImmutableMap.of(
                        identifierPojo.getExternalResourceUri(),
                        identifierPojo
                )
        );
        return new GraphElementSearchResultPojo(
                GraphElementType.Meta,
                identifierAsGraphElement,
                getContext()
        );
    }

    @Override
    public Record getRow() {
        return row;
    }
}