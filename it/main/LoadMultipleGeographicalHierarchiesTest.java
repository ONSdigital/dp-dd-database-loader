package main;

import org.scalatest.testng.TestNGSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import play.Logger;
import uk.co.onsdigital.discovery.model.DimensionalDataPoint;
import uk.co.onsdigital.discovery.model.GeographicArea;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static main.PostgresTest.AREA_TYPES;
import static main.PostgresTest._2011GPH;
import static main.PostgresTest._2013ADMIN;
import static org.testng.AssertJUnit.assertEquals;

public class LoadMultipleGeographicalHierarchiesTest extends TestNGSuite {

    static Logger.ALogger logger = Logger.of(LoadSingleDataPointToDatabaseTest.class);

    private EntityManagerFactory emf;
    private EntityManager em;
    private PostgresTest postgresTest = new PostgresTest();

    @BeforeClass
    public void setupDb() {
        logger.info("SETTING UP DB");
        emf = postgresTest.getEMFForEmptyTestDatabase();
        em = emf.createEntityManager();
    }

    @Test
    public void loadMultipleGeographicHierarchiesIntoSameDatabase() throws Exception {

        EntityTransaction tx = em.getTransaction();
        tx.begin();
        try {
            postgresTest.loadStandingData(em, Arrays.asList(AREA_TYPES));
            postgresTest.loadStandingData(em, Arrays.asList(_2011GPH));
            postgresTest.loadStandingData(em, Arrays.asList(_2013ADMIN));


            List<GeographicArea> results = em.createQuery("SELECT geoArea FROM GeographicArea geoArea WHERE geoArea.extCode = 'K04000001'", GeographicArea.class).getResultList();
            assertEquals(2, results.size());

            // TODO sort the results
            assertEquals(results.get(0).getGeographicAreaHierarchyBean().getGeographicAreaHierarchy(), "2013ADMIN");
            assertEquals(results.get(1).getGeographicAreaHierarchyBean().getGeographicAreaHierarchy(), "2011GPH");

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        } finally {
            tx.commit();
        }
    }

}
