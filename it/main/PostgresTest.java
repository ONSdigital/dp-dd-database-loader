package main;

import au.com.bytecode.opencsv.CSVParser;
import com.google.common.util.concurrent.UncheckedExecutionException;
import configuration.Configuration;
import configuration.DbMigrator;
import exceptions.DatapointMappingException;
import org.eclipse.persistence.platform.database.H2Platform;
import org.flywaydb.core.api.MigrationVersion;
import play.Logger;
import services.InputCSVParserV3;
import uk.co.onsdigital.discovery.model.*;

import javax.persistence.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.eclipse.persistence.config.PersistenceUnitProperties.*;
import static org.junit.Assert.fail;
import static org.testng.Assert.assertEquals;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;


public class PostgresTest {

    static Logger.ALogger logger = Logger.of(PostgresTest.class);

    public static final MigrationVersion EMPTY_DB_VERSION = MigrationVersion.fromVersion("01.001");

    static String TIME = "../classification/time.sql";

    static String _2011STATH_small = "../geo/2011STATH_small.sql";

    static String COICOP = "/classification/COICOP_test.sql";
    static String COICOP2 = "/classification/COICOP_test2.sql";
    static String NACE = "../classification/CL_0001480_NACE.sql";
    static String PRODCOM_ELEMENTS = "../classification/CL_0000737_Prodcom_Elements.sql";



    public EntityManagerFactory getEMFForProductionLikeDatabase() {
        DbMigrator migrator = DbMigrator.getMigrator();
        migrator.getFlyway().setTarget(MigrationVersion.LATEST);
        migrator.migrate();
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("data_discovery", Configuration.getDatabaseParameters());
        return emf;
    }

    public EntityManagerFactory getEMFForEmptyTestDatabase() {
        Map<String, String> databaseParameters = new HashMap<String, String>() {{
            put(JDBC_URL, "jdbc:h2:mem:test");
            put(JDBC_USER, "SA");
            put(JDBC_PASSWORD, "");
            put(JDBC_DRIVER, "org.h2.Driver");
            put(DDL_GENERATION, DROP_AND_CREATE);
            put(DDL_GENERATION_MODE, DDL_DATABASE_GENERATION);
            put(TARGET_DATABASE, H2Platform.class.getName());
        }};
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("data_discovery", databaseParameters);
        return emf;
    }

    public void loadStandingData(EntityManager em, List<String> fileList) throws Exception {
        for (String file : fileList) {
            loadSomeData(em, file);
        }
    }

    private void loadSomeData(EntityManager em, String filename) throws Exception {
        logger.info("Loading data file {}", filename);
        File inputFile = new File(getClass().getResource(filename).getPath());

        // Raw JDBC is significantly faster than EclipseLink for bulk loading data, so unwrap the connection
        final Connection connection = em.unwrap(Connection.class);
        final Statement statement = connection.createStatement();
        final AtomicLong rows = new AtomicLong();
        final long batchSize = 5000L;

        Files.lines(inputFile.toPath()).forEach(line -> {
            try {
                statement.addBatch(line);
                if (rows.incrementAndGet() % batchSize == 0) {
                    logger.info("Processed {} rows of {}", rows.get(), filename);
                    statement.executeBatch();
                }

            } catch (SQLException e) {
                throw new UncheckedExecutionException(e);
            }
        });

        statement.executeBatch();
        logger.info("Finished loading {} rows of {}", rows.get(), filename);
    }

    public DimensionalDataSet createEmptyDataset(EntityManager em, String id, String title) {
        // todo this belongs as part of the csv 'import' function
        DimensionalDataSet dimensionalDataSet = em.find(DimensionalDataSet.class, UUID.fromString(id));
        if (dimensionalDataSet == null) {
            DataResource resource = new DataResource(id, "title");
            em.persist(resource);
            dimensionalDataSet = new DimensionalDataSet(title, resource);
            dimensionalDataSet.setId(UUID.fromString(id));
            em.persist(dimensionalDataSet);
        }
        return dimensionalDataSet;
    }

    public void loadEachLineInV3File(EntityManager em, String inputFileName, DimensionalDataSet dimensionalDataSet) throws IOException, DatapointMappingException {
        String rowData[];
        InputCSVParserV3 parser = new InputCSVParserV3();
        InputStream inputFileAsStream = getClass().getResourceAsStream(inputFileName);
        if(inputFileAsStream == null) {
            throw new RuntimeException("Input file not found!");
        }
        try (BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputFileAsStream, "UTF-8"), 32768)) {
            CSVParser csvParser = new CSVParser();
            csvReader.readLine();
            while (csvReader.ready() && (rowData = csvParser.parseLine(csvReader.readLine())) != null) {
                parser.parseRowdataDirectToTables(em, rowData, dimensionalDataSet);
            }
        }
    }


    public void loadFileAndCheckDimensionCount(EntityManager em, UUID datasetId, String inputFileName, int expectedNumberOfDimensions) {
        running(fakeApplication(), () -> {

            EntityTransaction tx = em.getTransaction();
            tx.begin();
            try {
                logger.debug("Loading file " + inputFileName + " ...");
                loadEachLineInV3File(em, inputFileName, createEmptyDataset(em, datasetId.toString(), "dataset"));

                List<DimensionValue> dimensionValues= em.createQuery("SELECT dim from DimensionValue dim where dim.dimension.dataSet.id = :datasetId", DimensionValue.class)
                        .setParameter("datasetId", datasetId)
                        .getResultList();

                assertEquals(dimensionValues.size(), expectedNumberOfDimensions);

            } catch (Exception e) {
                e.printStackTrace();
                fail();
            } finally {
                tx.rollback();
            }
        });
    }

}
