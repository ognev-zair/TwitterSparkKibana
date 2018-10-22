package com.ognev.spark;


import java.util.ArrayList;
import java.util.List;

public class SparkStreamMain {
    public static void main(String[] args) throws Exception {

        ArrayList<String> topicList = new ArrayList<>();
        topicList.add("tweet");
        KafkaStreaming kafkaStream = new KafkaStreaming("KafkaSparkStreaming", topicList,2);
    }
}
