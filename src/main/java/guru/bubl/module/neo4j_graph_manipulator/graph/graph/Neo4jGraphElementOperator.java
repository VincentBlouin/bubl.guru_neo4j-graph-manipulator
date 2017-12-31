/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.common.reflect.TypeToken;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NamedParameterStatement;
import guru.bubl.module.common_utils.NoEx;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.model.json.JsonUtils;
import guru.bubl.module.neo4j_graph_manipulator.graph.*;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.Neo4jImages;
import guru.bubl.module.neo4j_graph_manipulator.graph.meta.Neo4jIdentificationFactory;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;

public class Neo4jGraphElementOperator implements GraphElementOperator, Neo4jOperator {

    public enum props {
        identifications,
        sort_date,
        move_date
    }

    protected Node node;
    protected Neo4jFriendlyResource friendlyResource;
    protected Neo4jFriendlyResourceFactory friendlyResourceFactory;
    protected Connection connection;

    protected Neo4jIdentificationFactory identificationFactory;

    protected GraphElementOperatorFactory graphElementOperatorFactory;

    @AssistedInject
    protected Neo4jGraphElementOperator(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            Connection connection,
            Neo4jIdentificationFactory identificationFactory,
            GraphElementOperatorFactory graphElementOperatorFactory,
            @Assisted Node node
    ) {
        friendlyResource = friendlyResourceFactory.withNode(
                node
        );
        this.graphElementOperatorFactory = graphElementOperatorFactory;
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.identificationFactory = identificationFactory;
        this.connection = connection;
        this.node = node;
    }

    @AssistedInject
    protected Neo4jGraphElementOperator(
            Neo4jFriendlyResourceFactory friendlyResourceFactory,
            Connection connection,
            Neo4jIdentificationFactory identificationFactory,
            GraphElementOperatorFactory graphElementOperatorFactory,
            @Assisted URI uri
    ) {
        this.friendlyResource = friendlyResourceFactory.withUri(
                uri
        );
        this.identificationFactory = identificationFactory;
        this.connection = connection;
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.graphElementOperatorFactory = graphElementOperatorFactory;
    }

    @Override
    public Date creationDate() {
        return friendlyResource.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return friendlyResource.lastModificationDate();
    }

    public void updateLastModificationDate() {
        friendlyResource.updateLastModificationDate();
    }

    @Override
    public URI uri() {
        return friendlyResource.uri();
    }

    @Override
    public String label() {
        return friendlyResource.label();
    }

    @Override
    public void label(String label) {
        friendlyResource.label(
                label
        );
    }

    @Override
    public Set<Image> images() {
        return friendlyResource.images();
    }

    @Override
    public Boolean gotImages() {
        return friendlyResource.gotImages();
    }

    @Override
    public String comment() {
        return friendlyResource.comment();
    }

    @Override
    public void comment(String comment) {
        friendlyResource.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return friendlyResource.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        friendlyResource.addImages(
                images
        );
    }

    @Override
    public boolean hasLabel() {
        return friendlyResource.hasLabel();
    }

    @Override
    public void setSortDate(Date sortDate, Date moveDate) {
        String query = String.format(queryPrefix() +
                        "SET " +
                        "n.%s=@%s, " +
                        "n.%s=@%s ",
                props.sort_date,
                props.sort_date,
                props.move_date,
                props.move_date
        );
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    query
            );
            statement.setLong(
                    props.sort_date.name(),
                    sortDate.getTime()
            );
            statement.setLong(
                    props.move_date.name(),
                    moveDate.getTime()
            );
            return statement.execute();
        }).get();
    }

    @Override
    public Map<colorProps, String> getColors() {
        String query = queryPrefix() + "RETURN n.colors as colors";
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            String colorsStr = rs.getString("colors");
            if (colorsStr == null) {
                return new HashMap<colorProps, String>();
            }
            Map<colorProps, String> colors = JsonUtils.getGson().fromJson(
                    colorsStr,
                    new TypeToken<Map<colorProps, String>>() {
                    }.getType()
            );
            return colors;
        }).get();
    }

    @Override
    public void setColors(Map<colorProps, String> colors) {
        String query = queryPrefix()
                + "SET n.colors = @colors";
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection, query
            );
            statement.setString("colors", JsonUtils.getGson().toJson(colors));
            return statement.execute();
        }).get();
    }

    @Override
    public void setChildrenIndex(String childrenIndex) {
        String query = queryPrefix()
                + "SET n.childrenIndexes = @childrenIndexes";
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection, query
            );
            statement.setString("childrenIndexes", childrenIndex);
            return statement.execute();
        }).get();
    }

    @Override
    public String getChildrenIndex() {
        String query = queryPrefix() + "RETURN n.childrenIndexes as childrenIndexes";
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(
                    query
            );
            rs.next();
            String childrenIndexes = rs.getString("childrenIndexes");
            return childrenIndexes == null ? "" : childrenIndexes;
        }).get();
    }

    @Override
    public void create() {
        createUsingInitialValues(
                map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        friendlyResource.createUsingInitialValues(values);
    }

    @Override
    public Map<URI, IdentifierPojo> addMeta(
            Identifier identification
    ) {
        return addTagAndOriginalReferenceOnesOrNot(
                identification,
                true
        );
    }

    private Map<URI, IdentifierPojo> addTagAndOriginalReferenceOnesOrNot(Identifier tag, Boolean addOriginalReferenceTags) {
        IdentifierPojo identificationPojo;
        Boolean isIdentifyingToAnIdentification = UserUris.isUriOfAnIdentifier(
                tag.getExternalResourceUri()
        );
        if (isIdentifyingToAnIdentification) {
            identificationPojo = new IdentifierPojo(
                    identificationFactory.withUri(
                            tag.getExternalResourceUri()
                    ).getExternalResourceUri(),
                    new FriendlyResourcePojo(
                            tag.getExternalResourceUri()
                    )
            );
        } else {
            identificationPojo = new IdentifierPojo(
                    new UserUris(getOwnerUsername()).generateIdentificationUri(),
                    tag
            );
        }

        identificationPojo.setCreationDate(new Date().getTime());
        final Neo4jFriendlyResource neo4jFriendlyResource = friendlyResourceFactory.withUri(
                new UserUris(getOwnerUsername()).generateIdentificationUri()
        );
        Map<URI, IdentifierPojo> identifications = new HashMap<>();
        Date tagCreationDate = new Date();
        return NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    AddIdentificationQueryBuilder.usingIdentificationForGraphElement(
                            identificationPojo, this
                    ).build()
            );
            statement.setString(
                    "label",
                    tag.label()
            );
            statement.setString(
                    "comment",
                    tag.comment()
            );
            statement.setString(
                    Neo4jImages.props.images.name(),
                    ImageJson.toJsonArray(tag.images())
            );
            statement.setLong(
                    "creationDate",
                    tagCreationDate.getTime()
            );
            statement.setString(
                    "external_uri",
                    identificationPojo.getExternalResourceUri().toString()
            );
            statement.setString(
                    "type",
                    GraphElementType.meta.name()
            );
            statement.setString(
                    "relationExternalUri",
                    identificationPojo.getRelationExternalResourceUri().toString()
            );
            statement.setLong(
                    Neo4jFriendlyResource.props.last_modification_date.name(),
                    new Date().getTime()
            );
            neo4jFriendlyResource.setNamedCreationProperties(
                    statement
            );
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                URI externalUri = URI.create(
                        rs.getString("external_uri")
                );
                IdentifierPojo tagPojo = new IdentifierPojo(
                        externalUri,
                        new Integer(rs.getString("nbReferences")),
                        new FriendlyResourcePojo(
                                URI.create(
                                        rs.getString("uri")
                                ),
                                rs.getString("label") == null ?
                                        "" : rs.getString("label"),
                                rs.getString("images") == null ?
                                        new HashSet<>() : ImageJson.fromJson(rs.getString("images")),
                                rs.getString("comment") == null ? "" : rs.getString("comment"),

                                rs.getLong("creation_date"),
                                rs.getLong("last_modification_date")
                        )
                );
                Boolean isReference = tag.getExternalResourceUri().equals(
                        this.uri()
                );

                Boolean isOwnerOfExternalUri = UserUris.ownerUserNameFromUri(
                        tag.getExternalResourceUri()
                ).equals(getOwnerUsername());

                if (isOwnerOfExternalUri && !isReference && addOriginalReferenceTags) {
                    Map<URI, IdentifierPojo> existingTags = getIdentifications();
                    Map<URI, IdentifierPojo> referenceTags = graphElementOperatorFactory.withUri(
                            tag.getExternalResourceUri()
                    ).getIdentifications();
                    for (IdentifierPojo otherIdentifier : referenceTags.values()) {
                        if (!existingTags.containsKey(otherIdentifier.getExternalResourceUri())) {
                            otherIdentifier = addTagAndOriginalReferenceOnesOrNot(
                                    otherIdentifier,
                                    false
                            ).get(otherIdentifier.getExternalResourceUri());
                        }
                        identifications.put(otherIdentifier.getExternalResourceUri(), otherIdentifier);
                    }
                }
                Boolean justCreatedTag = tagCreationDate.equals(tagPojo.creationDate());
                if (!isReference && isOwnerOfExternalUri && justCreatedTag) {
                    Map<URI, IdentifierPojo> tags = graphElementOperatorFactory.withUri(
                            tag.getExternalResourceUri()
                    ).addMeta(
                            tagPojo
                    );
                    tagPojo = tags.get(tag.getExternalResourceUri());
                }
                identifications.put(
                        tag.getExternalResourceUri(),
                        tagPojo
                );
            }
            return identifications;
        }).get();
    }

    @Override
    public void removeIdentification(Identifier identification) {
        String query = String.format(
                "%s MATCH n-[r:%s]->(i {%s:'%s'}) " +
                        "DELETE r " +
                        "SET i.%s=i.%s -1, " +
                        Neo4jFriendlyResource.LAST_MODIFICATION_QUERY_PART +
                        "RETURN i.uri as uri",
                queryPrefix(),
                Relationships.IDENTIFIED_TO,
                Neo4jFriendlyResource.props.uri.name(),
                identification.uri().toString(),
                Neo4jIdentification.props.nb_references,
                Neo4jIdentification.props.nb_references
        );
        NoEx.wrap(() -> {
            NamedParameterStatement statement = new NamedParameterStatement(
                    connection,
                    query
            );
            statement.setLong(
                    Neo4jFriendlyResource.props.last_modification_date.name(),
                    new Date().getTime()
            );
            return statement.executeQuery();
        }).get();
    }

    @Override
    public void remove() {
        removeAllIdentifications();
        friendlyResource.remove();
    }

    @Override
    public boolean equals(Object graphElementToCompare) {
        return friendlyResource.equals(graphElementToCompare);
    }

    @Override
    public int hashCode() {
        return friendlyResource.hashCode();
    }

    @Override
    public String queryPrefix() {
        return friendlyResource.queryPrefix();
    }

    @Override
    public Node getNode() {
        if (null == node) {
            node = friendlyResource.getNode();
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return friendlyResource.addCreationProperties(
                map
        );
    }

    public GraphElementPojo pojoFromCreationProperties(Map<String, Object> creationProperties) {
        return new GraphElementPojo(
                friendlyResource.pojoFromCreationProperties(
                        creationProperties
                )
        );
    }

    @Override
    public void setNamedCreationProperties(NamedParameterStatement statement) throws SQLException {
        friendlyResource.setNamedCreationProperties(
                statement
        );
    }

    @Override
    public Map<URI, IdentifierPojo> getIdentifications() {
        String query = String.format(
                "%sMATCH n-[r:%s]->identification " +
                        "RETURN identification.uri as uri, " +
                        "identification.external_uri as external_uri, " +
                        "identification.%s as nbReferences, " +
                        "r.%s as r_x_u",
                queryPrefix(),
                Relationships.IDENTIFIED_TO,
                Neo4jIdentification.props.nb_references,
                Neo4jIdentification.props.relation_external_uri
        );
        Map<URI, IdentifierPojo> identifications = new HashMap<>();
        return NoEx.wrap(() -> {
            ResultSet rs = connection.createStatement().executeQuery(query);
            while (rs.next()) {
                URI uri = URI.create(
                        rs.getString("uri")
                );
                URI externalUri = URI.create(
                        rs.getString("external_uri")
                );
                IdentifierPojo identification = new IdentifierPojo(
                        externalUri,
                        new Integer(rs.getString("nbReferences")),
                        new FriendlyResourcePojo(
                                uri
                        )
                );
                String relationExternalUriString = rs.getString("r_x_u");
                identification.setRelationExternalResourceUri(
                        relationExternalUriString == null ? Identifier.DEFAULT_IDENTIFIER_RELATION_EXTERNAL_URI :
                                URI.create(
                                        relationExternalUriString
                                )
                );
                identifications.put(
                        externalUri,
                        identification
                );
            }
            return identifications;
        }).get();
    }

    public void removeAllIdentifications() {
        NoEx.wrap(() -> connection.createStatement().executeQuery(
                String.format(
                        "%s MATCH n-[r:%s]->i " +
                                "DELETE r " +
                                "SET i.%s=i.%s -1 ",
                        queryPrefix(),
                        Relationships.IDENTIFIED_TO,
                        Neo4jIdentification.props.nb_references,
                        Neo4jIdentification.props.nb_references
                )
        )).get();
    }

    public GraphElementOperator forkUsingCreationPropertiesAndCache(GraphElementOperator clone, Map<String, Object> additionalCreateValues, GraphElement cache) {
        FriendlyResourcePojo original = new FriendlyResourcePojo(
                uri(),
                cache.label()
        );
        original.setComment(
                cache.comment()
        );
        Map<String, Object> createValues = map(
                Neo4jFriendlyResource.props.label.name(), original.label(),
                Neo4jFriendlyResource.props.comment.name(), original.comment()
        );
        createValues.putAll(
                additionalCreateValues
        );
        clone.createUsingInitialValues(
                createValues
        );
        clone.addMeta(
                new IdentifierPojo(
                        this.uri(),
                        original
                )
        );
        cache.getIdentifications().values().forEach(
                clone::addMeta
        );
        return clone;
    }
}
