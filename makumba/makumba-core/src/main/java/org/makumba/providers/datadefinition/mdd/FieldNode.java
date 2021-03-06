package org.makumba.providers.datadefinition.mdd;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.makumba.ValidationRule;

import antlr.collections.AST;

/**
 * AST node that collects information about a MDD field
 * 
 * @author Manuel Bernhardt <manuel@makumba.org>
 * @version $Id: FieldNode.java,v 1.1 May 3, 2009 6:14:27 PM manu Exp $
 */
public class FieldNode extends MDDAST {

    private static final long serialVersionUID = 1844582254529895123L;

    // basic field info
    protected MDDNode mdd;

    protected String name;

    protected String description;

    // for unknown mak type, probably macro type
    protected String unknownType;

    // default value
    // FIXME where does this come from?
    // FIXME should this actually be transient?
    protected transient Object defaultValue;

    // modifiers
    protected boolean fixed;

    protected boolean notNull;

    protected boolean notEmpty;

    protected boolean unique;

    // native validation rule messages
    protected String notNullError;

    protected String NaNError;

    protected String uniqueError;

    protected String notEmptyError;

    protected String notIntError;

    protected String notRealError;

    protected String notBooleanError;

    // intEnum
    protected LinkedHashMap<Integer, String> intEnumValues = new LinkedHashMap<Integer, String>();

    protected LinkedHashMap<Integer, String> intEnumValuesDeprecated = new LinkedHashMap<Integer, String>();

    // charEnum
    protected Vector<String> charEnumValues = new Vector<String>();

    protected Vector<String> charEnumValuesDeprecated = new Vector<String>();

    // char length
    protected int charLength = 255;

    // pointed type
    protected String pointedType;

    // subfield - ptrOne, setComplex, file
    protected MDDNode subfield;

    // validation rules for this field
    protected Hashtable<String, ValidationRule> validationRules = new Hashtable<String, ValidationRule>();

    public FieldNode(MDDNode mdd, String name) {
        // AST
        setText(name);
        setType(MDDTokenTypes.FIELD);

        this.mdd = mdd;
        this.name = name;
    }

    /**
     * Constructor for FieldNode, originAST being an AST used for giving context to errors
     */
    public FieldNode(MDDNode mdd, String name, AST originAST) {
        initialize(originAST);

        // we need to overwrite the type after the initialisation
        setText(name);
        setType(MDDTokenTypes.FIELD);

        this.mdd = mdd;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void addIntEnumValue(int index, String text) {
        intEnumValues.put(index, text);
    }

    public void addIntEnumValueDeprecated(int index, String text) {
        intEnumValuesDeprecated.put(index, text);
    }

    public void addCharEnumValue(String text) {
        charEnumValues.add(text);
    }

    public void addCharEnumValueDeprecated(String text) {
        charEnumValuesDeprecated.add(text);
    }

    public MDDNode initSubfield() {
        if (this.subfield == null) {
            this.subfield = new MDDNode(mdd, this.name);
        }
        return this.subfield;
    }

    public void initSetSubfield() {
        initSubfield();
        this.subfield.name = this.mdd.getName();
        this.subfield.ptrSubfield = "->" + this.name;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("== Field name: " + name + " (line " + getLine() + ")\n");
        if (makumbaType != null) {
            sb.append("== Field type: " + makumbaType.getTypeName() + "\n");
        } else {
            sb.append("== Unknown field type: " + unknownType + "\n");
        }
        sb.append("== Modifiers: " + (fixed ? "fixed " : "") + (unique ? "unique " : "") + (notNull ? "not null " : "")
                + (notEmpty ? "not empty " : "") + "\n");
        if (description != null) {
            sb.append("== Description: " + description + "\n");
        }
        if (subfield != null) {
            sb.append("\n== Subfield detail" + "\n\n");
            sb.append(subfield.toString() + "\n");
        }
        return sb.toString();
    }
}