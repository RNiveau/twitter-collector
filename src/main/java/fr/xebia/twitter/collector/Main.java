package fr.xebia.twitter.collector;

import fr.xebia.twitter.collector.mqtt.MqttListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.message.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.TwitterException;

import java.io.IOException;

public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String args[]) throws InterruptedException, IOException, TwitterException {
        Config.setupCreds();

        final AsyncMqttClient client = new AsyncMqttClient("tcp://" + Config.mqttHost + ":" + Config.mqttPort, new MqttListener(), 5);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    // Connect to the broker with a specific client ID. Only if the broker accepted the connection shall we proceed.
                    logger.debug("Try to connect to mqtt");
                    client.connect("twitter", true);
                    logger.debug("Connected to mqtt");
                    client.publish(new PublishMessage("raw_data", QoS.AT_MOST_ONCE, "On Time"));
                } catch (Exception ex) {
                    logger.error("An unexpected exception has occurred.", ex);
                }
            }
        };
        Context.getInstance().setMqttClient(client);
        thread.start();
        thread.join();
    }
}
