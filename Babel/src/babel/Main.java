package babel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.shell.util.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        sqlTesting();
    }

    private static void sqlTesting()
    {
        String connectionURL = "jdbc:sqlserver://frank-server.reshall.rose-hulman.edu;database=URBEX;user=sa;password=TotallyMath!";
        String query = "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS ORDER BY TABLE_NAME, ORDINAL_POSITION";
        try {

            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(connectionURL);
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(query);

            while (rs.next())
            {
                System.out.println(rs.getString("TABLE_NAME") + " : " + rs.getString("COLUMN_NAME"));
            }

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static void neoTesting()
    {
        String path = "http://frank-server.reshall.rose-hulman.edu:7474/db/data/";
        String cypherUri = path + "cypher";

        JSONObject jsonObject = new JSONObject();
        try {
            Map<String, String> params = new HashMap<String, String>();
            String query = "start doc=node:characters(character=\"Doctor\")" +
                    "match doc-[:OWNS]->item" +
                    "return item.thing";
            jsonObject.put("query", query);
            jsonObject.put("params", params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        WebResource resource = Client.create().resource(cypherUri);

        ClientResponse post = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(jsonObject.toString())
                .post(ClientResponse.class);

        System.out.println(String.format("Accept on [%s], status code [%d]", cypherUri, post.getStatus()));
        post.close();
    }

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it shuts down
        // nicely when the VM exits (even if you "Ctrl-C" the running example
        // before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        });
    }
}
