
package com.ognev.spark;


import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.ognev.spark.pojo.Tweet;

public class TwitterHbaseTable
{

	private static final String TABLE_NAME = "twitter";
	private static final String PERSONAL = "personal_details";
	private static final String COMMENT = "comment_details";

	
	public static void createTableIfNotExists(){

		Configuration config = HBaseConfiguration.create();

		// creating a table
		try (Connection connection = ConnectionFactory.createConnection(config);
				Admin admin = connection.getAdmin()) {

			HTableDescriptor tableDesc = new HTableDescriptor(
					TableName.valueOf(TABLE_NAME));


			tableDesc.addFamily(new HColumnDescriptor(PERSONAL));
			tableDesc.addFamily(new HColumnDescriptor(COMMENT));

			System.out.print("Creating table.... ");

			if (!admin.tableExists(tableDesc.getTableName())) {
			//	admin.disableTable(tableDesc.getTableName());
			//	admin.deleteTable(tableDesc.getTableName());
				admin.createTable(tableDesc);
			}
		//	admin.createTable(tableDesc);

			System.out.println(" Done!");
		}

	 catch (Exception e) {
		e.printStackTrace();
	}
		// Inserting Data

		
	}
	
	public static void insertTweetToHbase(Tweet tweet) {
		Configuration config = HBaseConfiguration.create();
		try (Connection connection = ConnectionFactory.createConnection(config);
				Table table = connection.getTable(TableName.valueOf(TABLE_NAME));) {

			SimpleDateFormat psf = new SimpleDateFormat("yyyy-mm-dd'T'HH:mm:ss");
			Put put = new Put(Bytes.toBytes(tweet.hashCode()));
			put.addColumn(Bytes.toBytes(PERSONAL), Bytes.toBytes("name"), Bytes.toBytes(tweet.getUser()));
			put.addColumn(Bytes.toBytes(COMMENT), Bytes.toBytes("text"), Bytes.toBytes(tweet.getText()));
			put.addColumn(Bytes.toBytes(COMMENT), Bytes.toBytes("createdAt"), Bytes.toBytes(psf.format(tweet.getCreatedAt())));
			put.addColumn(Bytes.toBytes(COMMENT), Bytes.toBytes("language"), Bytes.toBytes(tweet.getLanguage()));
	
			
			table.put(put);
		//	System.out.println("Data inserted!");
		

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	       
}
