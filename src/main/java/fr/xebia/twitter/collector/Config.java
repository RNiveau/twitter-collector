package fr.xebia.twitter.collector;

import java.io.IOException;

/**
 * Created by Xebia on 03/09/2014.
 */
public class Config {

    public static String consumerKey;
    public static String consumerSecret;
    public static String token;
    public static String secret;
    public static String mqttHost;
    public static String mqttPort;

    public static void setupCreds() throws IOException {
//        Properties properties = new Properties();
//        FileInputStream fis = new FileInputStream(ClassLoader.getSystemResource("twitter.properties").getPath());
//        properties.load(fis);
//        consumerKey = (String) properties.get("consumerKey");
//        consumerSecret = (String) properties.get("consumerSecret");
//        token = (String) properties.get("token");
//        secret = (String) properties.get("secret");
//        mqttHost = (String) properties.get("mqtt.host");
//        mqttPort = (String) properties.get("mqtt.port");
        consumerKey = "";
        consumerSecret = "";
        token="";
        secret="";

        mqttHost="ec2-54-186-153-191.us-west-2.compute.amazonaws.com";
        mqttPort="1883";

    }
}
