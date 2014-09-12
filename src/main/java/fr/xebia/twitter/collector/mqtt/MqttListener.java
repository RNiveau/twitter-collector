package fr.xebia.twitter.collector.mqtt;

import fr.xebia.twitter.collector.Config;
import fr.xebia.twitter.collector.Context;
import fr.xebia.twitter.collector.json.SubscriptionJson;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Xebia on 03/09/2014.
 */
public class MqttListener implements AsyncClientListener {

    private static Logger logger = LoggerFactory.getLogger(MqttListener.class);

    @Override
    public void publishReceived(MqttClient client, PublishMessage message) {
        message.ack();
        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
        try {
            final SubscriptionJson subscription = mapper.readValue(message.getPayloadString(), SubscriptionJson.class);
            Context.getInstance().addSubscriptions(subscription);
            Context.getInstance().parseSubscription(subscription);
            getHistoric(subscription);
        } catch (IOException e) {
            logger.error("Error convert json {} {}", message.getPayloadString(), e);
            return;
        }

        logger.debug("Json received={}", message.getPayloadString());
    }

    private void getHistoric(SubscriptionJson subscription) {
        logger.debug("Get historic tweet for id {}", subscription.getId());
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(Config.consumerKey)
                .setOAuthConsumerSecret(Config.consumerSecret)
                .setOAuthAccessToken(Config.token)
                .setOAuthAccessTokenSecret(Config.secret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();
        subscription.getKeywords().stream().forEach(keyword -> {
            Query query = new Query(keyword);
            query.since("2014-08-01");
            query.count(100);
            query.lang("en");
            QueryResult result = null;
            try {
                logger.debug("Send historic msg for id={} and message={}", subscription.getId(), keyword);
                result = twitter.search(query);
                result.getTweets().stream().forEach(tweet -> {
                    logger.debug("Send historic");
                    JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
                    JsonObjectBuilder tweetJson = Json.createObjectBuilder();
                    tweetJson.add("text", tweet.getText());
                    objectBuilder.add("id", subscription.getId());
                    objectBuilder.add("tweet", tweetJson);
                    Context.getInstance().getMqttClient().publish(new PublishMessage("raw_data", QoS.AT_MOST_ONCE, objectBuilder.build().toString()));
                });
                int last = 0;
            } catch (TwitterException e) {
                logger.error("Error to get historic {}", e);
            }
        });
    }

    @Override
    public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {
        if (cause != null) {
            logger.error("Disconnected from the broker due to an exception.", cause);
        } else {
            logger.info("Disconnecting from the broker.");
        }

        if (reconnecting) {
            logger.info("Attempting to reconnect to the broker.");
        }
    }

    @Override
    public void connected(MqttClient client, ConnectReturnCode returnCode) {
        if (returnCode == null || returnCode != ConnectReturnCode.ACCEPTED) {
            logger.error("Unable to connect to the MQTT broker. Reason: " + returnCode);
            throw new RuntimeException();
        }
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        subscriptions.add(new Subscription("source_sub", QoS.AT_MOST_ONCE));
        client.subscribe(subscriptions);
    }

    @Override
    public void published(MqttClient client, PublishMessage message) {
    }

    @Override
    public void subscribed(MqttClient client, Subscription[] requestedSubscriptions, Subscription[]
            grantedSubscriptions, boolean requestsGranted) {
        if (!requestsGranted) {
            logger.error("Unable to subscribe to the following subscriptions: " + Arrays.toString(requestedSubscriptions));
        }

        logger.debug("Granted subscriptions: " + Arrays.toString(grantedSubscriptions));
    }

    @Override
    public void unsubscribed(MqttClient client, String[] topics) {
        client.unsubscribe(topics);
        logger.debug("Unsubscribed from the following topics: " + Arrays.toString(topics));
    }
}