package org.wso2.cep.uima.demo;

import org.apache.log4j.PropertyConfigurator;
import org.wso2.cep.uima.demo.Util.TwitterConfiguration;
import org.wso2.cep.uima.demo.Util.TwitterConfigurationBuiler;
import org.xml.sax.SAXException;
import twitter4j.FilterQuery;
import twitter4j.Logger;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import javax.jms.JMSException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class TwitterStreamer {
	
    // API keys for the TwitterStreaming API
    private String consumerKey;
    private String consumerSecret;
    private String accessToken;
    private String accessTokenSecret;

    // Configuration parameters of the ActiveMQ Message Broker
    private String JMSUrl;

    private ConfigurationBuilder cb;
    private TwitterStream twitterStream;
    private FilterQuery filter;

    private long[] usersToFilter; // userIDs of the users to filter when the streaming tweets

    private static Logger logger = Logger.getLogger(TwitterStreamer.class);

    public static void main(String[] args) throws JMSException, IOException,
                                            SAXException, ParserConfigurationException {

        String JMSUrl = args[0];
        String queueName = args[1];

	if(JMSUrl.equals("") || queueName.equals("")){
		System.out.println("Usage ant -DjmsUrl=<JMS_URL> -DqueueName=<QUEUE_NAME>");
		System.exit(0);
	}

        TwitterConfiguration config = TwitterConfigurationBuiler.getTwitterConfiguration();

        // create a streamer object
        TwitterStreamer streamer = new TwitterStreamer(config.getFollowers(), JMSUrl);
        streamer.setConsumerSecret(config.getConsumerSecret());
        streamer.setConsumerKey(config.getConsumerKey());
        streamer.setAccessToken(config.getAccessToken());
        streamer.setAccessTokenSecret(config.getAccessTokenSecret());

        // create the status handler of the stream
        StatusHandler statusHandler = new StatusHandler(JMSUrl, queueName);

        // start streaming for tweets
        streamer.startStream(statusHandler);
    }

    /***
     *
     * @param users long[] userIDs of the users to filer the stream for
     * @param JMSUrl String url of the ActiveMQ Server
     */
    public TwitterStreamer(long[] users, String JMSUrl){
        this.JMSUrl = JMSUrl;
        this.usersToFilter = users;
        PropertyConfigurator.configure("src/main/resources/log4j.properties");
    }


    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }

    /***
     *  Method to set up the API keys for the configuration builder
     */
    private void buildConfiguration(){
        cb = new ConfigurationBuilder();
        Logger.getLogger(TwitterStreamer.class).debug("Building Configuration");

        if(consumerKey == null || consumerSecret == null || accessToken == null || accessTokenSecret == null)
            throw new NullPointerException("TWitter API Keys not set");

        cb.setDebugEnabled(true)
    	  .setOAuthConsumerKey(consumerKey)
    	  .setOAuthConsumerSecret(consumerSecret)
    	  .setOAuthAccessToken(accessToken)
    	  .setOAuthAccessTokenSecret(accessTokenSecret);	
    }


    /***
     *  Method to Set up the filter for the Streaming API
     */
    private void createFilter(){
    	if(usersToFilter == null)
    		throw new NullPointerException("User list to follow not set");
    	
    	filter = new FilterQuery();
    	filter.follow(usersToFilter);

    }

    /***
     * Method to start the streaming of Tweets
     * @param handler
     */
    public void startStream(StatusHandler handler){

        buildConfiguration();
        TwitterStreamFactory tf = new TwitterStreamFactory(cb.build());
        twitterStream = tf.getInstance();
        twitterStream.addListener(handler);
        createFilter();

    	if(filter == null){
    		throw new NullPointerException("Filter Not Set");
    	}
    	twitterStream.filter(filter);
    }
    

}
