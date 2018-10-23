package com.ognev.spark;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ognev.spark.pojo.Tweet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import java.text.SimpleDateFormat;
import java.util.*;


public class KafkaStreaming {

    public KafkaStreaming(String sparkAppName, List<String> topicList, int numberThreads) {
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);

        TwitterHbaseTable.createTableIfNotExists();
        SparkConf sparkConf = new SparkConf().
        		setAppName(sparkAppName).
        		setMaster("local[2]")
        .set("spark.serializer", KryoSerializer.class.getName())
        .set("es.nodes", "localhost:9200")
        .set("es.index.auto.create", "true");

        JavaStreamingContext jsc = new JavaStreamingContext(sparkConf, new Duration(3000));
        sparkStreaming(topicList, numberThreads, jsc);
    }

    private void sparkStreaming(List<String> topicList, int numberThreads, JavaStreamingContext jsc) {
        Map<String, Integer> topicMap = new HashMap<>();
        for (String topic : topicList) {
            topicMap.put(topic, numberThreads);
        }

        ObjectMapper mapper = new ObjectMapper();
   
        JavaPairReceiverInputDStream<String, String> messages =
                KafkaUtils.createStream(jsc, "localhost:2181", "tweetg", topicMap);
       
        SimpleDateFormat osf = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy"); //Mon Oct 22 03:26:55 +0000 2018

         messages.map(t -> {
        	 
        	 String jsonl = t._2.toString();
                String result = "";
                JsonObject rootObj = null;
                if(!jsonl.contains("limit")) {
                	JsonParser parser = new JsonParser();
                  rootObj = parser.parse(jsonl).getAsJsonObject();
                  Date date = osf.parse(rootObj.get("created_at").getAsString());
                  Tweet tweet = new Tweet(rootObj.get("user").getAsJsonObject().get("name").getAsString(), 
                		  rootObj.get("text").getAsString(),
                		  date , 
                		  rootObj.get("lang").getAsString() );
                  TwitterHbaseTable.insertTweetToHbase(tweet);
                  return mapper.writeValueAsString(tweet);
                }
                return "{ \"user\": \"BigDataTechnologies\", \"text\": \"BDT\", \"createdAt\": \"2018-10-22T03:20:20\", \"language\": \"en\"}";
             		            
         })
                .foreachRDD(tweets -> {
                	
                    tweets.collect().stream().forEach(t -> System.out.println(t));
                    JavaEsSpark.saveJsonToEs(tweets, "spark/tweets");
                    return null;
                });

        jsc.start();
        jsc.awaitTermination();
    }

    
}