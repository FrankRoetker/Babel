package babel;

/**
 * Created with IntelliJ IDEA.
 * User: dochaven
 * Date: 4/30/13
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class Attribute {
    public enum AttributeType{
        base,
        foriegnkey,
    };

    public AttributeType Type;
    public String Name;
    public String fkTable;
    public String fkAttribute;

    public Attribute(String name, AttributeType type){
        Name = name;
        this.Type = type;
    }

    public void SetFKConstraint(String fktable, String fkattribute){
        fkTable = fktable;
        fkAttribute = fkattribute;
    }
}
