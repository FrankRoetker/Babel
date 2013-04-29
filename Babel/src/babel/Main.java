package babel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.shell.util.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
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
