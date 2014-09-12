package fr.xebia.twitter.collector;

import fr.xebia.twitter.collector.client.TwitterClient;
import fr.xebia.twitter.collector.json.SubscriptionJson;
import net.sf.xenqtt.client.AsyncMqttClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by Xebia on 03/09/2014.
 */
public class Context {

    final static private Context INSTANCE = new Context();

    final private List<SubscriptionJson> subscriptions = new ArrayList<>();

    final private List<TwitterClient> twitterClients = new ArrayList<>();

    final private ExecutorService executorService = Executors.newFixedThreadPool(100);

    private AsyncMqttClient mqttClient;

    public void addSubscriptions(SubscriptionJson subscription) {
        synchronized (subscriptions) {
            subscriptions.add(subscription);
        }
    }

    static public Context getInstance() {
        return INSTANCE;
    }

    public void parseSubscription(SubscriptionJson subscription) {
        synchronized (twitterClients) {
            subscription.getKeywords().stream().forEach(keyword -> {
                if (keyword.length() > 2) {
                    boolean present = twitterClients.stream().filter(e -> e.getKeyword().equalsIgnoreCase(keyword)).findFirst().isPresent();
                    if (!present) {
                        TwitterClient twitterClient = new TwitterClient(keyword);
                        twitterClients.add(twitterClient);
                        executorService.execute(twitterClient);
                    }
                }
            });
        }
    }

    public void setMqttClient(AsyncMqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public AsyncMqttClient getMqttClient() {
        return mqttClient;
    }

    public List<String> matchKeywordId(String keyword) {
        return subscriptions.stream().filter(s -> s.getKeywords().contains(keyword)).map(s -> s.getId()).collect(Collectors.toList());
    }
}
