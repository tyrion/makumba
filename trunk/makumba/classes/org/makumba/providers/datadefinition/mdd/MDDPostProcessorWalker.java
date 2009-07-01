package org.makumba.providers.datadefinition.mdd;

import java.util.HashMap;

import org.makumba.DataDefinition;
import org.makumba.DataDefinitionNotFoundError;
import org.makumba.MakumbaError;
import org.makumba.providers.datadefinition.mdd.validation.MultiUniquenessValidationRule;

import antlr.collections.AST;

/**
 * Build walker that glues all the contents of the MDD together.
 * 
 * @author Manuel Gay
 * @version $Id: MDDBuildWalker.java,v 1.1 May 3, 2009 10:13:05 PM manu Exp $
 */
public class MDDPostProcessorWalker extends MDDPostProcessorBaseWalker {
    
    private MDDFactory factory = null;
    
    private HashMap<String, FieldNode> typeShorthands;
    
    public MDDPostProcessorWalker(String typeName, MDDNode mdd, HashMap<String, FieldNode> typeShorthands, MDDFactory factory) {
        this.typeName = typeName;
        this.mdd = mdd;
        this.typeShorthands = typeShorthands;
        this.factory = factory;
    }
    
    @Override
    protected void processUnknownType(AST field) {
        FieldNode fieldNode = (FieldNode) field;
        FieldNode type = typeShorthands.get(fieldNode.unknownType);
        if(type == null) {
            factory.doThrow(this.typeName, "Unknown field type: "+fieldNode.unknownType, field);
        } else {
            fieldNode.makumbaType = type.makumbaType;
            if(fieldNode.makumbaType == FieldType.INTENUM) {
                fieldNode.intEnumValues = type.intEnumValues;
                fieldNode.intEnumValuesDeprecated = type.intEnumValuesDeprecated;
            }
        }
        
        field = fieldNode;
        
    }
    
    @Override
    protected void checkTitleField(AST titleField) {
        TitleFieldNode title = (TitleFieldNode) titleField;
        
        // titleField can be a field name or a function
        switch(title.titleType) {
            
            case MDDTokenTypes.FIELD:
                
                String t = title.getText();
                
                if(t.indexOf(".") > -1) {
                
                    while(t.indexOf(".") > -1) {
                        String field = t.substring(0, t.indexOf("."));
                        t = t.substring(t.indexOf(".") + 1);
                        String fieldInPointed = t.substring(0, t.indexOf("."));
                        FieldNode n = title.mdd.fields.get(field);
                        if(n == null) {
                            factory.doThrow(this.typeName, "Field " + field + " does not exist in type " + mdd.getName() , titleField);
                        } else {
                            // check if this is a pointer to another type
                            if(n.makumbaType != FieldType.PTRREL || n.makumbaType != FieldType.PTRONE) {
                                factory.doThrow(this.typeName, "Field " + field + " is not a pointer", titleField);
                            } else {
                                // if it's a pointer, let's check if we can make something out of it
                                try {
                                    DataDefinition pointed = MDDProvider.getMDD(n.pointedType);
                                    if(pointed.getFieldDefinition(fieldInPointed) == null) {
                                        factory.doThrow(this.typeName, "Field " + fieldInPointed + " does not exist in type " + pointed.getName(), titleField);
                                    }
                                    
                                } catch(DataDefinitionNotFoundError d) {
                                    factory.doThrow(this.typeName, "Could not find type " + n.pointedType, titleField);
                                }
                            }
                        }
                    }
                
                } else {
                    Object field = title.mdd.fields.get(title.getText());
                    if(field == null) {
                        factory.doThrow(this.typeName, "Field " + title.getText() + " does not exist in type " + mdd.getName() , titleField);
                    }
                }
                break;
                
                
            case FUNCTION:
                // TODO add support for calls with arguments
                if(title.functionArgs.size() > 0) {
                    factory.doThrow(this.typeName, "There's no support for function calls with arguments in the !title directive yet", titleField);
                } else {
                    if(mdd.functions.get(title.functionName) == null ) {
                        factory.doThrow(this.typeName, "Function " + title.getText() + " not defined in type " + typeName , titleField);
                    }
                }
                break;
            default:
                throw new MakumbaError("invalid title field type: " + title.titleType);
        }
        
    }
    
    @Override
    protected void processMultiUniqueValidationDefinitions(ValidationRuleNode v) {
        if(v instanceof MultiUniquenessValidationRule) {
            DataDefinition.MultipleUniqueKeyDefinition key = new DataDefinition.MultipleUniqueKeyDefinition(v.multiUniquenessFields.toArray(new String[] {}), v.message);
            mdd.addMultiUniqueKey(key);
        }
    }

}
