package fr.xebia.twitter.collector.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Xebia on 03/09/2014.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionJson implements Serializable {

    @JsonProperty
    private List<String> keywords;

    @JsonProperty
    private String id;

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
