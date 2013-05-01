package babel;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: dochaven
 * Date: 4/30/13
 * Time: 2:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class Table {
    public enum TableType {
        base,
        relationship,
        arrow,
        spider
    };

    public TableType Type;
    public ArrayList<Attribute> Attr;
    public String TableName;

    public Table(String name){
        TableName = name;
        Type = TableType.base;
        Attr = new ArrayList<Attribute>();
    }

    public void AddAttribute(Attribute a){
        if(a.Type == Attribute.AttributeType.foriegnkey){
            switch (Type){
                case base:
                    Type = TableType.arrow;
                    break;
                case relationship:
                    Type = TableType.spider;
                    break;
                case arrow:
                    Type = TableType.relationship;
                    break;
                case spider:
                    //do nothing
                    break;
            }
        }
        Attr.add(a);
    }
}
