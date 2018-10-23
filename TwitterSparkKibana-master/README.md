# Twitter -> Spark -> Kibana

Centos

Install Elasticsearch 
curl -L -O https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-1.4.5.tar.gz

unzip
 tar -xvf elasticsearch-1.4.5.tar.gz
and run
 ./elasticsearch-1.4.5/bin/elasticsearch


Install Kibana:
https://www.elastic.co/guide/en/kibana/4.0/setup.html
Choose version 4.0.0 https://www.elastic.co/downloads/past-releases
extract 
and start ./kibana-4.0.0-linux-x64/bin/kibana


Kibana http://localhost:5601



sudo service zookeeper-server start
sudo service kafka-server start
sudo service hbase-master start
sudo service hbase-regionserver start