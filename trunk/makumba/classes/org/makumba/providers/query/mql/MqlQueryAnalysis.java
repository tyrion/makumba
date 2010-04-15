package org.makumba.providers.query.mql;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.hibernate.hql.antlr.HqlTokenTypes;
import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;
import org.makumba.MakumbaError;
import org.makumba.commons.MakumbaJspAnalyzer;
import org.makumba.providers.DataDefinitionProvider;
import org.makumba.providers.QueryAnalysis;
import org.makumba.providers.QueryAnalysisProvider;
import org.makumba.providers.QueryProvider;

import antlr.ASTFactory;
import antlr.collections.AST;

/**
 * The hearth of the MQL query analysis and compilation. The query is first pre-processed and then the initial parsing takes place to produce the initial mql tree.
 * After this, the tree is transformed by the MqlSqlWalker for analysis and finally transformed again for sql query generation.
 * 
 * @author Cristian Bogdan
 * @author Manuel Gay
 * @version $Id: MqlQueryAnalysis.java,v 1.1 Apr 29, 2009 8:54:20 PM manu Exp $
 */
public class MqlQueryAnalysis implements QueryAnalysis {

    public static final String MAKUMBA_PARAM = "param";

    private String query;

    protected DataDefinition insertIn;
    
    private List<String> parameterOrder = new ArrayList<String>();

    private DataDefinition proj;

    private boolean noFrom = false;

    private LinkedHashMap<String, DataDefinition> labels;

    private Hashtable<String, String> aliases;

    private DataDefinition paramInfo;
    
    private AST analyserTreeOriginal;
    
    private MqlSqlWalker analyser;
    
    private AST pass1;
    
    // all the labels that were generated by this analyser, kept to avoid collisions
    protected Vector<String> generatedLabels = new Vector<String>();
    

    static String formatQueryAndInsert(String query, String insertIn) {
        if(insertIn!=null && insertIn.length()>0)
            return  "###"+insertIn+"###"+query;
        else return query;
    }
    
    public MqlQueryAnalysis(AST pass1){
        init(false, false, pass1, null);
        
    }
    public MqlQueryAnalysis(String queryAndInsert, boolean optimizeJoins, boolean autoLeftJoin){
        this(queryAndInsert, optimizeJoins, autoLeftJoin, null);
    }
    public MqlQueryAnalysis(String queryAndInsert, DataDefinition knownLabels){     
        this(queryAndInsert, false, false, knownLabels);       
    }

    MqlQueryAnalysis(String queryAndInsert, boolean optimizeJoins, boolean autoLeftJoin, DataDefinition knownLabels) {
        Date d = new Date();

        if(queryAndInsert.startsWith("###")){
            insertIn= DataDefinitionProvider.getInstance().getDataDefinition(queryAndInsert.substring(3, queryAndInsert.indexOf('#', 3)));
            query= queryAndInsert.substring(queryAndInsert.indexOf('#', 3)+3);
        } else {
            query=queryAndInsert;
        }

        query = QueryAnalysisProvider.checkForFrom(query);

        pass1= QueryAnalysisProvider.inlineFunctions(query);
        
        noFrom = QueryAnalysisProvider.reduceDummyFrom(pass1);

        /*
         * } else { if (!Configuration.getQ ueryInliner().equals("tree")) { HqlParser parser = null; try { parser =
         * HqlParser.getInstance(query); parser.statement(); } catch (Throwable t) { doThrow(t, parser != null ?
         * parser.getAST() : null); } doThrow(parser.error, parser.getAST()); parsed = parser.getAST(); } else { parsed
         * = FunctionInliner.inlineQueryTree(query); } }
         */

        // we need to do the transformation first so the second-pass parser will accept the query
        MqlQueryAnalysisProvider.transformOQLParameters(pass1, parameterOrder);
        MqlQueryAnalysisProvider.transformOQL(pass1);
                
        init(optimizeJoins, autoLeftJoin, pass1, knownLabels);
        
        long diff = new java.util.Date().getTime() - d.getTime();
        java.util.logging.Logger.getLogger("org.makumba.db.query.compilation").fine("MQL analysis: " + diff + " ms: " + query);
       
    }

    private void init(boolean optimizeJoins, boolean autoLeftJoin, AST parsed, DataDefinition knownLabels) throws MakumbaError {
        MqlSqlWalker mqlAnalyzer = new MqlSqlWalker(query, insertIn, optimizeJoins, autoLeftJoin, knownLabels);
        analyser = mqlAnalyzer;
        try {
            mqlAnalyzer.statement(parsed);
        } catch (Throwable e) {
            QueryAnalysisProvider.doThrow(query, e, parsed);
        }
        QueryAnalysisProvider.doThrow(query, mqlAnalyzer.error, parsed);

        analyserTreeOriginal = mqlAnalyzer.getAST();
        labels = mqlAnalyzer.rootContext.labels;
        aliases = mqlAnalyzer.rootContext.aliases;
        paramInfo = DataDefinitionProvider.getInstance().getVirtualDataDefinition("Parameters for " + query);
        proj = DataDefinitionProvider.getInstance().getVirtualDataDefinition("Projections for " + query);
        mqlAnalyzer.setProjectionTypes(proj);

        // TODO: nofrom queries should only have constant projections

        for (int i = 0; i < parameterOrder.size(); i++) {
            // first we check whether we have a parameter type for exactly this position
            FieldDefinition fd = mqlAnalyzer.paramInfoByPosition.getFieldDefinition("param" + i);
            
            // failing that, we see if we have a parameter with the same name on another position
            // but we consider that only if the parameter is not multi-type
            if (fd == null && ! mqlAnalyzer.multiTypeParams.contains(parameterOrder.get(i)))
                fd = mqlAnalyzer.paramInfoByName.getFieldDefinition(parameterOrder.get(i));

            // if we still don't know who this guy is, maybe it's an actor parameter that we generated on the fly
            if(parameterOrder.get(i).startsWith("actor_")) {
                // bingo
                String type = parameterOrder.get(i).substring(6).replaceAll("_", ".");
                fd = mqlAnalyzer.ddp.getDataDefinition(type).getFieldDefinition(0);
            }
            
            if (fd != null) {
                paramInfo.addField(DataDefinitionProvider.getInstance().makeFieldWithName("param" + i, fd, String.valueOf(mqlAnalyzer.multiTypeParams.contains(parameterOrder.get(i)))));
            } else {
                throw new MakumbaError("Panic: could not compute type of parameter at position " + i + " with name '" + parameterOrder.get(i) + "' of query " + getQuery());
            }
            
        }
    }

 

    public String getQuery() {
        return query;
    }

    public DataDefinition getLabelType(String labelName) {
        String s1 = (String) aliases.get(labelName);
        if (s1 != null)
            labelName = s1;
        return (DataDefinition) labels.get(labelName);
    }

    public Map<String, DataDefinition> getLabelTypes() {
        return labels;
    }

    public DataDefinition getParameterTypes() {
        return paramInfo;
    }

    public DataDefinition getProjectionType() {
        return proj;
    }
    
    // FIXME this is ugly, there is for sure a way to get the FROM from the tree
    private String getFrom() {

        String[] splitAtFrom = query.split("\\s[f|F][r|R][o|O][m|M]\\s");
        String[] splitAtWhere = splitAtFrom[1].split("\\s[w|W][h|H][e|E][r|R][e|E]\\s");

        return splitAtWhere[0];
    }

    // FIXME needed for relation miner, but should maybe be moved, it's dependent on the analysis per se
    public String getFieldOfExpr(String expr) {
        if (expr.indexOf(".") > -1)
            return expr.substring(expr.lastIndexOf(".") + 1);
        else
            return expr;
    }

    // FIXME needed for relation miner, but should be refactored or made more robust
    public DataDefinition getTypeOfExprField(String expr) {

        if (expr.indexOf(".") == -1) {
            return getLabelType(expr);
        } else {
            DataDefinition result;
            int lastDot = expr.lastIndexOf(".");
            String beforeLastDot = expr.substring(0, lastDot);
            if (beforeLastDot.indexOf(".") == -1) {
                result = getLabelType(beforeLastDot);
            } else {
                // compute dummy query for determining pointed type

                // FIXME will this work if the FROM contains subqueries?
                String dummyQuery = "SELECT " + beforeLastDot + " AS projection FROM " + getFrom();
                result = QueryProvider.getQueryAnalzyer(MakumbaJspAnalyzer.QL_OQL).getQueryAnalysis(dummyQuery).getProjectionType().getFieldDefinition(
                    "projection").getPointedType();
            }
            return result;

        }
    }

    public static String showAst(AST ast) {
        return ast.toStringTree();
    }

    static boolean isNil(AST a) {
        return a.getType() == HqlTokenTypes.IDENT && a.getText().toUpperCase().equals("NIL");
    }

    static void setNullTest(AST a) {
        if (a.getType() == HqlTokenTypes.EQ) {
            a.setType(HqlTokenTypes.IS_NULL);
            a.setText("is null");
        } else {
            a.setType(HqlTokenTypes.IS_NOT_NULL);
            a.setText("is not null");
        }
    }

    private int labelCounter = 0;
    
    public String createLabel() {
        String l = "_x_gen_" + labelCounter++;
        generatedLabels.add(l);
        return l;
    }
    
    public AST getAnalyserTree() {
        return analyser.fact.dupTree(analyserTreeOriginal);
    }
    
    public ASTFactory getAnalyserFactory() {
        return analyser.fact;
    }
    
    public boolean getNoFrom() {
        return noFrom;
    }
    
    public List<String> getParameterOrder() {
        return parameterOrder;
    }

    public static class ParamConstant {
        String paramName;

        public ParamConstant(String name) {
            paramName = name;
        }

        public String getParamName() {
            return paramName;
        }
    }

    public Map<String, Object> getConstantValues() {
        return analyser.constantValues;
    }

    public AST getPass1Tree() {
        return pass1;
    }
}
