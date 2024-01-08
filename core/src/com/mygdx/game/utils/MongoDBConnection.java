package com.mygdx.game.utils;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.bson.conversions.Bson;

public class MongoDBConnection {

    private MongoClient mongoClient;
    private MongoDatabase database;
    private static String DATABASE_URL = "mongodb+srv://admin:admin@ni-imena.sygmxf2.mongodb.net/?retryWrites=true&w=majority";
    private static String DATABASE_NAME = "ni_imena";

    public MongoDBConnection() {
        // Connect to the MongoDB server
        mongoClient = MongoClients.create(DATABASE_URL);

        try {
            // Access the database
            database = mongoClient.getDatabase(DATABASE_NAME);
            database.runCommand(new Document("ping", 1));
            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
        }
        catch (MongoException e) {
            e.printStackTrace();
        }
    }

    // Add methods to interact with the database as needed
    public void insertDocument(Document document, String collectionName) {
        // Insert a document into the specified collection
        database.getCollection(collectionName).insertOne(document);
    }

    public FindIterable<Document> findDocuments(Bson filter, String collectionName) {
        // Find documents in the specified collection based on a filter
        return database.getCollection(collectionName).find(filter);
    }

    public void closeConnection() {
        // Close the connection when done
        mongoClient.close();
    }
}
