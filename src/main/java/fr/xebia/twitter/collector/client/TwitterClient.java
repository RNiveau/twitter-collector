package fr.xebia.twitter.collector.client;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import fr.xebia.twitter.collector.Config;
import fr.xebia.twitter.collector.Context;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.message.QoS;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Xebia on 03/09/2014.
 */
public class TwitterClient extends Thread {

    private static Logger logger = LoggerFactory.getLogger(TwitterClient.class);

    private String keyword;

    private Client client;

    public TwitterClient(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public void run() {
        logger.debug("Call twitterClient {}", keyword);

        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
        BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);
        Authentication hosebirdAuth = new OAuth1(Config.consumerKey, Config.consumerSecret, Config.token, Config.secret);
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        final StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        final ArrayList<String> terms = Lists.newArrayList(keyword);
        hosebirdEndpoint.trackTerms(terms);
        hosebirdEndpoint.languages(Lists.newArrayList("en"));


        ClientBuilder builder = new ClientBuilder()
                .name(keyword + "-client")
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue))
                .eventMessageQueue(eventQueue);

        client = builder.build();

        client.connect();
        logger.debug("TwitterClient connected {}", keyword);
        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.take();
                if (StringUtils.isNotBlank(msg)) {
                    JsonReader reader = Json.createReader(new StringReader(msg));
                    JsonObject jsonObject = reader.readObject();
                    reader.close();
                    List<String> ids = Context.getInstance().matchKeywordId(keyword);
                    ids.forEach(id -> {
                        logger.debug("Send msg for id={} and keyword={}", id, keyword);
                        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
                        objectBuilder.add("tweet", jsonObject);
                        objectBuilder.add("id", id);
                        Context.getInstance().getMqttClient().publish(new PublishMessage("raw_data", QoS.AT_MOST_ONCE, objectBuilder.build().toString()));
                    });

                }
            } catch (InterruptedException e) {
                logger.error("Error when received message {}", e.getMessage());
            }
        }
    }

    public String getKeyword() {
        return keyword;
    }

    public void disconnect() {
        client.stop();
    }

}
