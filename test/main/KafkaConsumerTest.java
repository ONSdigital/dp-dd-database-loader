package main;

import au.com.bytecode.opencsv.CSVParser;
import models.DataResource;
import models.DimensionalDataSet;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import play.Logger;
import services.InputCSVParser;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.*;

import static junit.framework.Assert.fail;

public class KafkaConsumerTest {

    static EntityManagerFactory emf = Persistence.createEntityManagerFactory("OnslocalBOPU");
    static EntityManager em = emf.createEntityManager();
    static Logger.ALogger logger = Logger.of(SliceAndDiceTest.class);

    static String datasetId = "666";

    @BeforeClass
    public void initialise() throws Exception {
        PostgresTest postgresTest = new PostgresTest(em, datasetId, logger);
        postgresTest.createDatabase();
    }

    @Test(enabled = false)
    public void getAMessageFromKafka() throws Exception {

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", UUID.randomUUID().toString());
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        consumer.subscribe(Arrays.asList("test"));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(record.offset() + ": " + record.value());

                    JSONObject rowJson = new JSONObject(record.value());
                    String rowData = rowJson.getString("datapoint");


//                    String[] rowDataArray = rowData.split(",");
                    String[] rowDataArray = new CSVParser().parseLine(rowData);


                    logger.debug("rowDataArray: " + rowDataArray.toString());


                    DataResource dataResource = em.find(DataResource.class, "666");
                    DimensionalDataSet dimensionalDataSet = null;
                    if(dataResource == null) {
                        try {
                            EntityTransaction tx = em.getTransaction();
                            tx.begin();

                            dataResource = new DataResource(datasetId, "title");
                            dimensionalDataSet = new DimensionalDataSet("title", dataResource);
                            em.persist(dataResource);
                            em.persist(dimensionalDataSet);

                            tx.commit();
                        } catch (Exception e) {
                            e.printStackTrace();
                            fail();
                        }
                    } else {
                        try {
                            EntityTransaction tx = em.getTransaction();
                            tx.begin();

                            dimensionalDataSet = new DimensionalDataSet("title", dataResource);
                            em.persist(dimensionalDataSet);
                            tx.commit();

                        } catch (Exception e) {
                            e.printStackTrace();
                            fail();
                        }

                    }


                    try {
                        EntityTransaction tx = em.getTransaction();
                        tx.begin();

                        new InputCSVParser().parseRowdata2(em, rowDataArray, dimensionalDataSet);

                        tx.commit();
                    } catch (Exception e) {
                        e.printStackTrace();
                        fail();
                    }
                }
            }
        } finally {
            consumer.close();
        }
    }
}