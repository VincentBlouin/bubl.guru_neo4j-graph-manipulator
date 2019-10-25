/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element;

import com.google.gson.reflect.TypeToken;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.User;
import guru.bubl.module.model.center_graph_element.CenterGraphElementPojo;
import guru.bubl.module.model.center_graph_element.CenteredGraphElementsOperator;
import guru.bubl.module.model.friend.FriendStatus;
import guru.bubl.module.model.graph.FriendlyResourcePojo;
import guru.bubl.module.model.graph.GraphElementPojo;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.json.JsonUtils;
import org.neo4j.driver.v1.*;

import java.net.URI;
import java.util.*;

import static org.neo4j.driver.v1.Values.parameters;

public class CenterGraphElementsOperatorNeo4j implements CenteredGraphElementsOperator {

    private Driver driver;
    private Integer limit;
    private Integer skip;

    @AssistedInject
    protected CenterGraphElementsOperatorNeo4j(
            Driver driver
    ) {
        this(driver, 28, 0);
    }

    @AssistedInject
    protected CenterGraphElementsOperatorNeo4j(
            Driver driver,
            @Assisted("limit") Integer limit,
            @Assisted("skip") Integer skip
    ) {
        this.driver = driver;
        this.limit = limit;
        this.skip = skip;
    }

    private final static String GRAPH_ELEMENT_MATCH = "MATCH(n:GraphElement) WHERE";
    private final static String PATTERN_MATCH = "MATCH(n:Pattern) WHERE";
    private final static String ALL_FRIENDS_MATCH = "MATCH(user:User{username:$owner}), " +
            "(user)-[friendship:friend]-(friend) " +
            "WHERE friendship.status = '" + FriendStatus.confirmed.name() + "' " +
            "WITH collect(friend.username) AS friends " +
            "MATCH(n:GraphElement) " +
            "WHERE n.owner IN friends AND";

    @Override
    public List<CenterGraphElementPojo> getPublicAndPrivateForOwner(User owner) {
        return get(
                GRAPH_ELEMENT_MATCH,
                owner,
                true,
                true
        );
    }

    @Override
    public List<CenterGraphElementPojo> getAllPublic() {
        return get(
                GRAPH_ELEMENT_MATCH,
                null,
                false,
                false,
                ShareLevel.PUBLIC.getConfidentialityIndex()
        );
    }

    @Override
    public List<CenterGraphElementPojo> getPublicOfUser(User owner) {
        return get(
                GRAPH_ELEMENT_MATCH,
                owner,
                true,
                false,
                ShareLevel.PUBLIC.getConfidentialityIndex()
        );
    }

    @Override
    public List<CenterGraphElementPojo> getAllPatterns() {
        return get(
                PATTERN_MATCH,
                null,
                false,
                false,
                ShareLevel.PUBLIC.getConfidentialityIndex()
        );
    }

    @Override
    public List<CenterGraphElementPojo> getFriendsFeedForUser(User user) {
        return get(
                ALL_FRIENDS_MATCH,
                user,
                false,
                false,
                ShareLevel.FRIENDS.getConfidentialityIndex(),
                ShareLevel.PUBLIC.getConfidentialityIndex()
        );
    }

    @Override
    public List<CenterGraphElementPojo> getForAFriend(User friend) {
        return get(
                GRAPH_ELEMENT_MATCH,
                friend,
                true,
                false,
                ShareLevel.FRIENDS.getConfidentialityIndex(),
                ShareLevel.PUBLIC.getConfidentialityIndex()
        );
    }


    private List<CenterGraphElementPojo> get(String match, User user, Boolean filterOnUser, Boolean isPrivateContext, Integer... shareLevels) {
        List<CenterGraphElementPojo> centerGraphElements = new ArrayList<>();
        try (Session session = driver.session()) {
            StatementResult rs = session.run(
                    String.format(
                            match + " " +
                                    (filterOnUser ? "n.owner=$owner AND " : "") + "EXISTS(n.last_center_date)" +
                                    (shareLevels.length == 0 ? " " : "AND n.shareLevel IN {shareLevels} ") +
                                    "RETURN n.%s as context, n.number_of_visits as numberOfVisits, n.last_center_date as lastCenterDate, n.label as label, n.uri as uri, n.nb_references as nbReferences, n.colors as colors, n.shareLevel, 'Pattern' IN LABELS(n) as isPattern " +
                                    "ORDER BY n.last_center_date DESC " +
                                    "SKIP " + skip +
                                    " LIMIT " + limit,
                            (isPrivateContext ? "private_context" : "public_context")
                    ),
                    parameters(
                            "owner", user == null ? "" : user.username(),
                            "shareLevels", shareLevels
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                Date lastCenterDate = null == record.get("lastCenterDate").asObject() ?
                        null :
                        new Date(record.get("lastCenterDate").asLong());
                Integer numberOfVisits = null == record.get("numberOfVisits").asObject() ?
                        null :
                        record.get("numberOfVisits").asInt();
                Integer nbReferences = null == record.get("nbReferences").asObject() ?
                        null :
                        record.get("nbReferences").asInt();
                String colors = record.get("colors").asString();
                ShareLevel shareLevel = record.get("n.shareLevel").asObject() == null ? ShareLevel.PRIVATE : ShareLevel.get(
                        record.get("n.shareLevel").asInt()
                );
                GraphElementPojo graphElement = new GraphElementPojo(new FriendlyResourcePojo(
                        URI.create(record.get("uri").asString()),
                        record.get("label").asString()
                ));
                graphElement.setColors(colors);
                centerGraphElements.add(
                        new CenterGraphElementPojo(
                                numberOfVisits,
                                lastCenterDate,
                                graphElement,
                                getContextFromRow(record),
                                nbReferences,
                                shareLevel,
                                record.get("isPattern").asBoolean()
                        )
                );
            }
            return centerGraphElements;
        }
    }

    private Map<URI, String> getContextFromRow(Record record) {
        String contextStr = record.get("context").asString();
        if (null == contextStr) {
            return new HashMap<>();
        }
        return JsonUtils.getGson().fromJson(
                contextStr,
                new TypeToken<Map<URI, String>>() {
                }.getType()
        );
    }
}
