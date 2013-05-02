package babel;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
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

    public Map<String, Table> GrabSQLServerTableInfo() {
        String query = "SELECT INFO.TABLE_NAME, INFO.COLUMN_NAME, \n" +
                "       FK.FKTABLE_NAME, FK.FKCOLUMN_NAME \n" +
                "FROM\n" +
                "\n" +
                "(SELECT TABLE_NAME, COLUMN_NAME\n" +
                "        FROM INFORMATION_SCHEMA.COLUMNS) AS INFO\n" +
                "\n" +
                "LEFT OUTER JOIN\n" +
                "\n" +
                "(\n" +
                "  SELECT C.TABLE_NAME [TABLE_NAME], \n" +
                "         KCU.COLUMN_NAME [COLUMN_NAME],\n" +
                "         C2.TABLE_NAME [FKTABLE_NAME], \n" +
                "         KCU2.COLUMN_NAME [FKCOLUMN_NAME]\n" +
                "  FROM   INFORMATION_SCHEMA.TABLE_CONSTRAINTS C \n" +
                "         INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU \n" +
                "           ON C.CONSTRAINT_SCHEMA = KCU.CONSTRAINT_SCHEMA \n" +
                "              AND C.CONSTRAINT_NAME = KCU.CONSTRAINT_NAME \n" +
                "         INNER JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS RC \n" +
                "           ON C.CONSTRAINT_SCHEMA = RC.CONSTRAINT_SCHEMA \n" +
                "              AND C.CONSTRAINT_NAME = RC.CONSTRAINT_NAME \n" +
                "         INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS C2 \n" +
                "           ON RC.UNIQUE_CONSTRAINT_SCHEMA = C2.CONSTRAINT_SCHEMA \n" +
                "              AND RC.UNIQUE_CONSTRAINT_NAME = C2.CONSTRAINT_NAME \n" +
                "         INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU2 \n" +
                "           ON C2.CONSTRAINT_SCHEMA = KCU2.CONSTRAINT_SCHEMA \n" +
                "              AND C2.CONSTRAINT_NAME = KCU2.CONSTRAINT_NAME \n" +
                "              AND KCU.ORDINAL_POSITION = KCU2.ORDINAL_POSITION \n" +
                "  WHERE  C.CONSTRAINT_TYPE = 'FOREIGN KEY'\n" +
                ") AS FK\n" +
                "\n" +
                "ON FK.TABLE_NAME = INFO.TABLE_NAME\n" +
                "   AND FK.COLUMN_NAME = INFO.COLUMN_NAME\n" +
                "ORDER BY INFO.TABLE_NAME, INFO.COLUMN_NAME";
        Map<String, Table> tables = new HashMap<String, Table>();

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(SQLServerConnectionString);
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                String val = rs.getString("FKTABLE_NAME");
                boolean fk = rs.getString("FKTABLE_NAME") == null;
                Attribute Attr = new Attribute(rs.getString("COLUMN_NAME"),
                        fk ? Attribute.AttributeType.base : Attribute.AttributeType.foriegnkey);

                if(!fk){
                    Attr.SetFKConstraint(rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"));
                }

                if(!tables.containsKey(rs.getString("TABLE_NAME"))){
                    Table newTable = new Table(rs.getString("TABLE_NAME"));
                    newTable.AddAttribute(Attr);
                    tables.put(rs.getString("TABLE_NAME"), newTable);
                }
                else{
                    Table table = tables.get(rs.getString("TABLE_NAME"));
                    table.AddAttribute(Attr);
                }
            }

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return GrabSQLServerIndexes(tables);
    }

    public Map<String, Table> GrabSQLServerIndexes(Map<String, Table> tables) {
        String query = "SELECT tc.TABLE_NAME, COLUMN_NAME\n" +
                       "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc\n" +
                       "JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu \n" +
                       "ON tc.CONSTRAINT_NAME = ccu.Constraint_name\n" +
                       "WHERE tc.CONSTRAINT_TYPE = 'Primary Key'";
//        ArrayList<Table> t = new ArrayList<Table>(t.values());

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(SQLServerConnectionString);
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                if(!tables.containsKey(rs.getString("TABLE_NAME"))){
                    //There is something wrong!
                }
                else{
                    tables.get(rs.getString("TABLE_NAME")).Keys.add(rs.getString("COLUMN_NAME"));
                }
            }

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return tables;
    }

    public void ConvertBaseTable(Table table){
        System.out.println("Starting to convert: " + table.TableName);
        String query = "SELECT *\n" +
                       "FROM [" + table.TableName + "];";
        Map<String, String> Vals = new HashMap<String, String>();

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(SQLServerConnectionString);
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                for(Attribute a : table.Attr){
                    Vals.put(a.Name, rs.getString(a.Name));
                }
                AddNode(Vals, table.Attr, table.TableName);
                Vals = new HashMap<String, String>();
            }

            //Add Index:
            AddIndex(table);

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        System.out.println("Finished converting: " + table.TableName);
    }

    public void ConvertRelationshipTable(Table table){
        System.out.println("Starting to convert: " + table.TableName);
        String query = "SELECT *\n" +
                       "FROM [" + table.TableName + "];";
        Map<String, String> Vals = new HashMap<String, String>();

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(SQLServerConnectionString);
            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(query);

            while (rs.next()) {
                int i = 0;
                URI node[] = new URI[2];
                for(Attribute a : table.Attr){
                    if(table.Keys.contains(a.Name)) {
//                        START a=node(...), b=node(...)
//                        CREATE a-[r:CONNECTED_TO]-b
//                        SET r.att = val
                        final String nodeQuery = Neo4jConnectionString
                                + "label/" + a.fkTable.replace(' ', '_') + "/nodes?"
                                + a.fkAttribute + "=%22"
                                + rs.getString(a.Name).replace(' ', '+')
                                + "%22";

                        WebResource resource = Client.create()
                                .resource( nodeQuery );

                        ClientResponse response = resource.getRequestBuilder()
                                .get(ClientResponse.class);

                        node[i++]= response.getLocation();
                    }
                    Vals.put(a.Name, rs.getString(a.Name));
                }
//                AddNode(Vals, table.Attr, table.TableName);
                AddRelationship(Vals, table.Attr, table, node[0], node[1]);
                Vals = new HashMap<String, String>();
            }

            //Add Index:
            //AddIndex(table);

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        System.out.println("Finished converting: " + table.TableName);
    }

    public void AddRelationship(Map<String, String> Vals, ArrayList<Attribute> Attr,
                                Table table, URI node1, URI node2){
//        label/Person/nodes?name=%22bob+ross%22

    }

    public void AddNode(Map<String, String> Vals, ArrayList<Attribute> Attr, String TableName) {
        final String nodeEntryPointUri = Neo4jConnectionString + "node";

        WebResource resource = Client.create()
                .resource( nodeEntryPointUri );
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        for(int i = 0; i < Attr.size(); i++){
            Attribute property = Attr.get(i);
            sb.append("\"" + property.Name + "\" : \"" + Vals.get(property.Name) + "\"");
            if(i + 1 < Attr.size()) sb.append( ", " );
        }
//        sb.append(" }");
        sb.append(" }");

        ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( sb.toString() )
                .post( ClientResponse.class );

        final URI location = response.getLocation();
        AddLabel(TableName, location);
        System.out.println( String.format(
                "POST to [%s], status code [%d], location header [%s]",
                nodeEntryPointUri, response.getStatus(), location.toString() ) );
        response.close();
    }

    private void AddRelationship2(Table t, Attribute a1, Attribute a2, String key1, String key2,
                                  ArrayList<Attribute> Attr, Map<String, String> Vals){
        final String nodeEntryPointUri = Neo4jConnectionString + "cypher";

        WebResource resource = Client.create()
                .resource( nodeEntryPointUri );
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");

        sb.append("\"query\" : \"");
//                        START a=node(...), b=node(...)
//                        CREATE a-[r:CONNECTED_TO]-b
//                        SET r.att = val
        sb.append("START a=node:" + a1.fkTable.replace(' ', '_') + "(");
//        sb.append(a1.fkAttribute + "=" + );

        sb.append( "\"params\" : { " );

        for(int i = 0; i < Attr.size(); i++){
            Attribute property = Attr.get(i);
            sb.append("\"" + property.Name + "\" : \"" + Vals.get(property.Name) + "\"");
            if(i + 1 < Attr.size()) sb.append( ", " );
        }
        sb.append(" }");
        sb.append(" }");

        ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( sb.toString() )
                .post( ClientResponse.class );

        final URI location = response.getLocation();
        response.close();
    }

    private void AddIndex(Table table){
        final String nodeEntryPointUri = Neo4jConnectionString + "schema/index/" + table.TableName.replace(' ', '_');

        WebResource resource = Client.create()
                .resource( nodeEntryPointUri );

        StringBuilder sb = new StringBuilder();
        sb.append("{ \"property_keys\" : [ ");
        for(int i = 0; i < table.Keys.size(); i++){
            String property = table.Keys.get(i);
            sb.append("\"" + property + "\"" );
            if(i + 1 < table.Keys.size()) sb.append( ", " );
        }
        sb.append(" ] }");

        ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( sb.toString() )
                .post( ClientResponse.class );

        final URI location = response.getLocation();
        System.out.println( String.format(
                "POST to [%s], status code [%d]",
                nodeEntryPointUri, response.getStatus() ) );
        response.close();
    }

    private void AddLabel(String TableName, URI node){
        final String nodeEntryPointUri = node + "/labels";
        final String name = TableName.replace(' ', '_');

        WebResource resource = Client.create()
                .resource( nodeEntryPointUri );

        ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
                .type(MediaType.APPLICATION_JSON)
                .entity("\"" + name + "\"")
                .post(ClientResponse.class);

        final URI location = response.getLocation();
        response.close();
    }

    private static URI addRelationship( URI startNode, URI endNode,
                                        String relationshipType, String jsonAttributes )
            throws URISyntaxException
    {
        URI fromUri = new URI( startNode.toString() + "/relationships" );
        String relationshipJson = generateJsonRelationship( endNode,
                relationshipType, jsonAttributes );

        WebResource resource = Client.create()
                .resource( fromUri );
        // POST JSON to the relationships URI
        ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( relationshipJson )
                .post( ClientResponse.class );

        final URI location = response.getLocation();
        System.out.println( String.format(
                "POST to [%s], status code [%d], location header [%s]",
                fromUri, response.getStatus(), location.toString() ) );

        response.close();
        return location;
    }

    private static String generateJsonRelationship( URI endNode,
                                                    String relationshipType, String... jsonAttributes )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "{ \"to\" : \"" );
        sb.append( endNode.toString() );
        sb.append( "\", " );

        sb.append( "\"type\" : \"" );
        sb.append( relationshipType );
        if ( jsonAttributes == null || jsonAttributes.length < 1 )
        {
            sb.append( "\"" );
        }
        else
        {
            sb.append( "\", \"data\" : " );
            for ( int i = 0; i < jsonAttributes.length; i++ )
            {
                sb.append( jsonAttributes[i] );
                if ( i < jsonAttributes.length - 1 )
                { // Miss off the final comma
                    sb.append( ", " );
                }
            }
        }

        sb.append( " }" );
        return sb.toString();
    }

    private static void addMetadataToProperty( URI relationshipUri,
                                               String name, String value ) throws URISyntaxException
    {
        URI propertyUri = new URI( relationshipUri.toString() + "/properties" );
        String entity = toJsonNameValuePairCollection( name, value );
        WebResource resource = Client.create()
                .resource( propertyUri );
        ClientResponse response = resource.accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON )
                .entity( entity )
                .put( ClientResponse.class );

        System.out.println( String.format(
                "PUT [%s] to [%s], status code [%d]", entity, propertyUri,
                response.getStatus() ) );
        response.close();
    }

    private static String toJsonNameValuePairCollection( String name,
                                                         String value )
    {
        return String.format( "{ \"%s\" : \"%s\" }", name, value );
    }
}
