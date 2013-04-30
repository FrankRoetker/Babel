package babel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.neo4j.shell.util.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dochaven
 * Date: 4/30/13
 * Time: 12:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class Converter {
    private static Converter _converter;
    private String SQLServerConnectionString;
    private String Neo4jConnectionString;

    private Converter() {
        SQLServerConnectionString = "";
        Neo4jConnectionString = "";
    }

    public static Converter GetInstance() {
        if (_converter == null)
            _converter = new Converter();
        return _converter;
    }

    public void SetSQLServerConnectionString(String sqlServerConnectionString) {
        SQLServerConnectionString = sqlServerConnectionString;
    }

    public void SetNeo4jConnectionString(String neo4jConnectionString) {
        Neo4jConnectionString = neo4jConnectionString;
    }

    public void GrabSQLServerData() {
        String query = "SELECT TABLE_NAME, COLUMN_NAME\n" +
                "FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "ORDER BY TABLE_NAME, ORDINAL_POSITION";

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(SQLServerConnectionString);
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                System.out.println(rs.getString("TABLE_NAME") + " : " + rs.getString("COLUMN_NAME"));
            }

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void AddNode(Map<String, String> Vals, ArrayList<String> Attr, String TableName) {
        //String path = "http://frank-server.reshall.rose-hulman.edu:7474/db/data/";
        String cypherUri = Neo4jConnectionString + "cypher";

        JSONObject jsonObject = new JSONObject();
        try {
            Map<String, String> params = new HashMap<String, String>();

            //CREATE (n:TableName = {Attr1 : Vals1, Attr2 : Vals2, ...})
            //RETURN n;
            String query = "CREATE (n:" + TableName + " = {";
            for (int i = 0; i < Attr.size(); i++) {
                query += Attr.get(i) + " : '" + Vals.get(Attr.get(i)) + "'";
                if(Attr.size() > i+1) query += ", ";
            }
            query += "}) RETURN n";

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
}
