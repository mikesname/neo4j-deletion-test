package io.github.mikesname;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.io.fs.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

class Neo4jDeletionTest {

    DatabaseManagementService managementService;
    GraphDatabaseService graphDb;
    Path databaseDirectory;

    static final Label LABEL = Label.label("Entity");
    static final String key = "key1";
    static final String key2 = "key2";

    @BeforeEach
    void setUp() throws Exception {
        databaseDirectory = Files.createTempDirectory("neo4j-deletion-test");
        managementService = new DatabaseManagementServiceBuilder(databaseDirectory).build();
        graphDb = managementService.database(DEFAULT_DATABASE_NAME);
    }

    void createAndDeleteNode() {
        try (Transaction tx = graphDb.beginTx()) {
            tx.schema().awaitIndexesOnline(1, TimeUnit.SECONDS);
            tx.commit();
        }

        try (Transaction tx = graphDb.beginTx()) {
            Node n = tx.createNode(LABEL);
            n.setProperty(key, "foo");
            n.setProperty(key2, "bar");

            n.delete();

            assertNull(tx.findNode(LABEL, key2, "bar"));
            assertNull(tx.findNode(LABEL, key, "foo"),
                    "node should have been deleted from unique index");
            tx.commit();
        }
    }

    @Test
    public void oneIndexTestWorksIn43x() {

        try (Transaction tx = graphDb.beginTx()) {
            tx.schema().constraintFor(LABEL)
                    .assertPropertyIsUnique(key)
                    .create();
            tx.commit();
        }

        createAndDeleteNode();
    }

    @Test
    public void twoIndexTestBrokenIn43x() {
        try (Transaction tx = graphDb.beginTx()) {
            tx.schema().constraintFor(LABEL)
                    .assertPropertyIsUnique(key)
                    .create();

            // Adding another index on key 2. This will cause
            // items referenced by the index to still be found
            // by the unique index after deletion
            tx.schema().indexFor(LABEL)
                    .on(key2)
                    .create();
            tx.commit();
        }

        createAndDeleteNode();
    }

    @AfterEach
    void tearDown() throws Exception {
        managementService.shutdown();
        FileUtils.deleteDirectory(databaseDirectory);
    }
}