package org.makumba.db.hibernate.hql;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.hql.ast.HqlParser;
import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;
import org.makumba.MakumbaSystem;
import org.makumba.OQLAnalyzer;
import org.makumba.abstr.FieldInfo;

import antlr.SemanticException;
import antlr.collections.AST;
import antlr.debug.misc.ASTFrame;

public class HqlAnalyzer implements OQLAnalyzer {
    
    private final static Map integerTypeMap = new HashMap();
    static {
        integerTypeMap.put(new Integer(FieldDefinition._ptr),"ptr");
        integerTypeMap.put(new Integer(FieldDefinition._ptrRel),"ptrRel");
        integerTypeMap.put(new Integer(FieldDefinition._ptrOne),"ptrOne");
        integerTypeMap.put(new Integer(FieldDefinition._ptrIndex),"ptrIndex");
        integerTypeMap.put(new Integer(FieldDefinition._int),"int");
        integerTypeMap.put(new Integer(FieldDefinition._intEnum),"intEnum");
        integerTypeMap.put(new Integer(FieldDefinition._char),"char");
        integerTypeMap.put(new Integer(FieldDefinition._charEnum),"charEnum");
        integerTypeMap.put(new Integer(FieldDefinition._text),"text");
        integerTypeMap.put(new Integer(FieldDefinition._date),"date");
        integerTypeMap.put(new Integer(
                FieldDefinition._dateCreate),"dateCreate");
        integerTypeMap.put(new Integer(
                FieldDefinition._dateModify),"dateModify");
        integerTypeMap.put(new Integer(FieldDefinition._set),"set");
        integerTypeMap.put(new Integer(
                FieldDefinition._setComplex),"setComplex");
        integerTypeMap.put(new Integer(FieldDefinition._nil),"nil");
        integerTypeMap.put(new Integer(FieldDefinition._real),"real");
        integerTypeMap.put(new Integer(
                FieldDefinition._setCharEnum),"setcharEnum");
        integerTypeMap.put(new Integer(
                FieldDefinition._setIntEnum),"setintEnum");
    }
    
    
    private String query;
    private HqlAnalyzeWalker walker;
    
    public HqlAnalyzer(String query) {
        this.query = query;
        
        HqlParser parser= HqlParser.getInstance(query);
        
        // Parse the input expression
        try{
          parser.statement();
          AST t1 = parser.getAST();
                     
          // Print the resulting tree out in LISP notation
          if(t1!=null)
          {
              //ASTFrame frame = new ASTFrame("tree", t1);
              //frame.setVisible(true);
              
              // pass here a session factory
              // HqlAnalyzerWalker passes itself (with the sf) to all object it creates
              walker= new HqlAnalyzeWalker();
              walker.objectTypeFactory= new ObjectTypeFactory(){
                  public ObjectType make(AST lhs, AST rhs, Map aliasTypes) throws SemanticException{
                      return new MddObjectTypeAST(lhs, rhs, aliasTypes);
                  }
              };
              walker.statement(t1);

              
              
              //print the tree
              AST t = walker.getAST();
              if(t!=null){
                  ASTFrame frame = new ASTFrame("analyzed", t);
                  frame.setVisible(true);
              }   
          }
        }
        catch(antlr.ANTLRException f){ 
            f.printStackTrace();
        }
    }

    public String getOQL() {
        return query;
    }

    public DataDefinition getProjectionType() {
        DataDefinition result = MakumbaSystem.getTemporaryDataDefinition("Projections for " + query);

        for(int i = 0; i < walker.getResult().size(); i++) {
            
            ExprTypeAST atom = (ExprTypeAST) walker.getResult().get(i);
            
            String name = atom.getIdentifier();
            if(atom.getObjectType() == null) {
                result.addField(FieldInfo.getFieldInfo(name, getTypeName(atom.getType()), false));
            } else{
                result.addField(FieldInfo.getFieldInfo(name, "ptr "+atom.getObjectType(), false));
            }
        }
        return result;
    }

    public DataDefinition getLabelType(String labelName) {
        return MakumbaSystem.getDataDefinition((String)walker.getLabelTypes().get(labelName));
    }

    public DataDefinition getParameterTypes() {
        // not implemented
        throw new UnsupportedOperationException("getParameterTypes");
    }

    public int parameterNumber() {
        throw new UnsupportedOperationException("getParameterNumber");
    }

    public int parameterAt(int index) {
        throw new UnsupportedOperationException("parameterAt");
    }
    
    
    String getTypeName(int i) {
        return (String) integerTypeMap.get(new Integer(i));
    }
    
    

}

    