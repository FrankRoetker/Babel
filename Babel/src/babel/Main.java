package babel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.shell.util.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        Converter c = Converter.GetInstance();

        String connectionURL = "jdbc:sqlserver://frank-server.reshall.rose-hulman.edu;database=URBEX;user=sa;password=TotallyMath!";
        c.SetSQLServerConnectionString(connectionURL);
        c.SetNeo4jConnectionString("http://babel.csse.rose-hulman.edu:7474/db/data/");
        Map<String, Table> tables = c.GrabSQLServerTableInfo();

        Iterator it = tables.entrySet().iterator();
        //try to get the base tables into the Neo4j database
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Table t = (Table) pairs.getValue();

            if (t.Type == Table.TableType.base) {
                c.ConvertBaseTable(t);
                it.remove();
            }
        }

        it = tables.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Table t = (Table) pairs.getValue();

            if (t.Type == Table.TableType.arrow) {
                c.ConvertArrowTable(t);
                it.remove();
            }
        }

        it = tables.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Table t = (Table) pairs.getValue();

            if (t.Type == Table.TableType.relationship) {
                c.ConvertRelationshipTable(t);
                it.remove();
            }
        }

        it = tables.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            Table t = (Table) pairs.getValue();

            if (t.Type == Table.TableType.spider) {
                c.ConvertSpiderTable(t);
                it.remove();
            }
        }

        System.out.println("Finished converting the database!");
    }

    //For Testing Purposes, Purging a Neo4j Database is done like this:
    //START n = node(*) MATCH n-[r?]-() WHERE ID(n)>0 DELETE n, r;
    //NOTE: DO NOT DELETE THE ROOT NODE

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it shuts down
        // nicely when the VM exits (even if you "Ctrl-C" the running example
        // before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }
}
