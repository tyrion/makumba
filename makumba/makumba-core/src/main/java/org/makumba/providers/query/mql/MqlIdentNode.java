package org.makumba.providers.query.mql;

import java.util.ArrayList;

import org.makumba.DataDefinition;
import org.makumba.commons.TextList;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * This is a label used in the SELECT or WHERE of a query TODO issue a warning, and advice to select label.id
 * 
 * @author Cristian Bogdan
 * @version $Id: MqlIdentNode.java,v 1.1 Aug 5, 2008 5:35:48 PM cristi Exp $
 */
public class MqlIdentNode extends MqlNode {
    private static final long serialVersionUID = 1L;

    public String label;

    public MqlIdentNode() {
    }

    public void resolve() throws SemanticException {
        if (walker.error != null) {
            return;
        }
        if (label != null) {
            // we've analyzed already
            return;
        }
        label = getText();
        DataDefinition dd = walker.currentContext.findLabelType(label);
        if (dd == null) {
            MqlNode selectExpr = walker.currentContext.projectionLabelSearch.get(label);
            if (selectExpr != null) {
                checkAsIds = new ArrayList<String>();
                checkAsIds.add(label);
                // if we do this, we practically copy the select expression here
                // then we don't need to check a label any more
                // setTextList(selectExpr.text);
                setMakType(selectExpr.getMakType());
                return;
            }
            if (walker.knownLabels != null && walker.knownLabels.getFieldDefinition(label) != null) {
                setMakType(walker.knownLabels.getFieldDefinition(label));
                return;
            }

            throw new SemanticException("Unknown label: " + label, "", getLine(), getColumn());
        }

        setTextList(walker.currentContext.selectLabel(label, this));
    }

    @Override
    public void writeToHql(TextList tl) {
        // for some reason the label doesn't always make it
        tl.append(getText());
        if (getMakType().getType().startsWith("ptr")) {
            tl.append(".id");
        }
        if (getMakType().getType().endsWith("Enum")) {
            tl.append(".enum_");
        }
    }

    @Override
    public void initialize(AST t) {
        super.initialize(t);
        if (t instanceof MqlDotNode) {
            MqlDotNode n = (MqlDotNode) t;
            this.label = n.label;
        }
    }
}
