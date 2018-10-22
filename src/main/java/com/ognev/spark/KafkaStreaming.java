package com.ognev.spark;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ognev.spark.pojo.Tweet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.serializer.KryoSerializer;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.apache.spark.streaming.twitter.TwitterUtils;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import scala.Tuple2;

import javax.swing.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.SimpleDateFormat;
import java.util.*;



public class KafkaStreaming {

    public KafkaStreaming(String sparkAppName, List<String> topicList, int numberThreads) {
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);

        SparkConf sparkConf = new SparkConf().
        		setAppName(sparkAppName).
        		setMaster("local[2]")
        .set("spark.serializer", KryoSerializer.class.getName())
        .set("es.nodes", "localhost:9200")
        .set("es.index.auto.create", "true");
    //    sparkConf.set("spark.cassandra.connection.host", "127.0.0.1");
        JavaStreamingContext jsc = new JavaStreamingContext(sparkConf, new Duration(3000));
        sparkStreaming(topicList, numberThreads, jsc);
    }

    private void sparkStreaming(List<String> topicList, int numberThreads, JavaStreamingContext jsc) {
        Map<String, Integer> topicMap = new HashMap<>();
        for (String topic : topicList) {
            topicMap.put(topic, numberThreads);
        }
        
        
     //   String[] filters = { "#Trump" };
        ObjectMapper mapper = new ObjectMapper();
   
        JavaPairReceiverInputDStream<String, String> messages =
                KafkaUtils.createStream(jsc, "localhost:2181", "tweetg", topicMap);
       
     
       // JavaDStream<String> line = messages.map(new StreamMessages());
        SimpleDateFormat osf = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy"); //Mon Oct 22 03:26:55 +0000 2018
        SimpleDateFormat psf = new SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss");
         messages.map(t -> {
        	 
        	 String jsonl = t._2.toString();
        	// System.out.println("JSONL: " + jsonl);
        	 
        	// return "none";
        	   //char c = jsonl.charAt(0);
                String result = "";
                JsonObject rootObj = null;
                if(!jsonl.contains("limit")) {
                	JsonParser parser = new JsonParser();
                  rootObj = parser.parse(jsonl).getAsJsonObject();
               //   psf.format(osf.parse(rootObj.get("created_at").getAsString()));
                  Tweet tweet = new Tweet(rootObj.get("user").getAsJsonObject().get("name").getAsString(), 
                		  rootObj.get("text").getAsString(),
                		  osf.parse(rootObj.get("created_at").getAsString()), 
                		  rootObj.get("lang").getAsString() );
                
                  return mapper.writeValueAsString(tweet);
                }
                return null;
           //  return  "{ \"user\": \"VoteNovember6th\", \"text\": \"nOur country is in crisis. \\n\\n#Democracy is being murdered in b…\", \"createdAt\": \"2018-10-22T03:20:20\", \"language\": \"en\"}";
            		 
             
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
