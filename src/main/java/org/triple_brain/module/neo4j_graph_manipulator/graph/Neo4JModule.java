package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.neo4j.cypher.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSetting;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.kernel.logging.BufferingLogger;
import org.triple_brain.module.model.FriendlyResource;
import org.triple_brain.module.model.FriendlyResourceFactory;
import org.triple_brain.module.model.GraphTransaction;
import org.triple_brain.module.model.WholeGraph;
import org.triple_brain.module.model.graph.GraphFactory;
import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgeFactory;
import org.triple_brain.module.model.graph.vertex.VertexFactory;
import org.triple_brain.module.model.graph.vertex.VertexInSubGraphOperator;
import org.triple_brain.module.model.suggestion.Suggestion;
import org.triple_brain.module.model.suggestion.SuggestionFactory;

import javax.inject.Singleton;
import javax.naming.InitialContext;
import java.io.File;
import java.io.IOException;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JModule extends AbstractModule {

    public static final String DB_PATH = "/var/lib/triple_brain/neo4j/db";
    public static final String DB_PATH_FOR_TESTS = "/tmp/triple_brain/neo4j/db";

    private static Boolean isTesting;

    @Override
    protected void configure() {
        try {
            final InitialContext jndiContext = new InitialContext();
            String isTestingStr = (String) jndiContext.lookup("is_testing");
            isTesting = "yes".equals(isTestingStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        bind(GraphTransaction.class).to(Neo4JGraphTransaction.class)
                .in(Singleton.class);

        bind(WholeGraph.class).to(Neo4JWholeGraph.class);

        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(
                isTesting ? DB_PATH_FOR_TESTS : DB_PATH
        )
                .setConfig(
                        GraphDatabaseSettings.node_keys_indexable,
                        Neo4JUserGraph.URI_PROPERTY_NAME
                ).setConfig(
                        GraphDatabaseSettings.node_auto_indexing,
                        GraphDatabaseSetting.TRUE
                ).setConfig(
                        GraphDatabaseSettings.relationship_keys_indexable,
                        Neo4JUserGraph.URI_PROPERTY_NAME
                ).setConfig(
                        GraphDatabaseSettings.relationship_auto_indexing,
                        GraphDatabaseSetting.TRUE
                ).newGraphDatabase();

        registerShutdownHook(graphDb);

        bind(GraphDatabaseService.class).toInstance(
                graphDb
        );
        bind(ExecutionEngine.class).toInstance(
                new ExecutionEngine(graphDb, new BufferingLogger())
        );

        FactoryModuleBuilder factoryModuleBuilder = new FactoryModuleBuilder();

        install(factoryModuleBuilder
                .build(Neo4JEdgeFactory.class));

        install(factoryModuleBuilder
                .build(Neo4JUserGraphFactory.class));

        install(factoryModuleBuilder
                .implement(VertexInSubGraphOperator.class, Neo4JVertexInSubGraphOperator.class)
                .build(VertexFactory.class));

        install(factoryModuleBuilder
                .build(Neo4JVertexFactory.class));

        install(factoryModuleBuilder
                .build(Neo4JSubGraphExtractorFactory.class));

        install(factoryModuleBuilder
                .implement(Edge.class, Neo4JEdgeOperator.class)
                .build(EdgeFactory.class));

        install(factoryModuleBuilder
                .build(Neo4JGraphElementFactory.class));

        install(factoryModuleBuilder
                .implement(FriendlyResource.class, Neo4JFriendlyResource.class)
                .build(FriendlyResourceFactory.class)
        );
        install(factoryModuleBuilder
                .build(Neo4JFriendlyResourceFactory.class)
        );
        install(factoryModuleBuilder
                .implement(Suggestion.class, Neo4JSuggestion.class)
                .build(SuggestionFactory.class)
        );
        install(factoryModuleBuilder
                .build(Neo4JSuggestionFactory.class)
        );
        install(factoryModuleBuilder
                .build(Neo4JSuggestionOriginFactory.class)
        );
        bind(new TypeLiteral<ReadableIndex<Node>>() {
        }).toInstance(
                graphDb.index()
                        .getNodeAutoIndexer()
                        .getAutoIndex()
        );

        bind(new TypeLiteral<ReadableIndex<Relationship>>() {
        }).toInstance(
                graphDb.index()
                        .getRelationshipAutoIndexer()
                        .getAutoIndex()
        );

        bind(GraphFactory.class).to(Neo4JGraphFactory.class).in(Singleton.class);

        requireBinding(Neo4JUtils.class);
    }

    private void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
                if (isTesting) {
                    clearDb();
                }
            }
        });
    }

    public static void clearDb() {
        try {
            FileUtils.deleteRecursively(new File(
                    DB_PATH_FOR_TESTS
            ));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
