package org.makumba.providers.query.mql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.hql.antlr.HqlTokenTypes;
import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;
import org.makumba.MakumbaError;
import org.makumba.OQLParseError;
import org.makumba.ProgrammerError;
import org.makumba.commons.MakumbaJspAnalyzer;
import org.makumba.commons.NameResolver;
import org.makumba.commons.RegExpUtils;
import org.makumba.commons.NameResolver.TextList;
import org.makumba.providers.Configuration;
import org.makumba.providers.DataDefinitionProvider;
import org.makumba.providers.QueryAnalysis;
import org.makumba.providers.QueryProvider;
import org.makumba.providers.SQLQueryGenerator;

import antlr.RecognitionException;
import antlr.collections.AST;

/**
 * The hearth of the MQL query analysis and compilation. The query is first pre-processed and then the initial parsing takes place to produce the initial mql tree.
 * After this, the tree is transformed by the MqlSqlWalker for analysis and finally transformed again for sql query generation.
 * 
 * @author Cristian Bogdan
 * @author Manuel Gay
 * @version $Id: MqlQueryAnalysis.java,v 1.1 Apr 29, 2009 8:54:20 PM manu Exp $
 */
public class MqlQueryAnalysis implements QueryAnalysis, SQLQueryGenerator {

    public static final String MAKUMBA_PARAM = "param";

    private String query;

    protected DataDefinition insertIn;
    
    private List<String> parameterOrder = new ArrayList<String>();

    private DataDefinition proj;

    private boolean noFrom = false;

    private LinkedHashMap<String, DataDefinition> labels;

    private Hashtable<String, String> aliases;

    private DataDefinition paramInfo;
    
    private AST analyserTree;

    private TextList text;
    
    private MqlSqlWalker analyser;
    
    // all the labels that were generated by this analyser, kept to avoid collisions
    protected Vector<String> generatedLabels = new Vector<String>();
    

    static String formatQueryAndInsert(String query, String insertIn) {
        if(insertIn!=null && insertIn.length()>0)
            return  "###"+insertIn+"###"+query;
        else return query;
    }
    
    public MqlQueryAnalysis(String queryAndInsert, boolean optimizeJoins, boolean autoLeftJoin) {
        Date d = new Date();

        if(queryAndInsert.startsWith("###")){
            insertIn= DataDefinitionProvider.getInstance().getDataDefinition(queryAndInsert.substring(3, queryAndInsert.indexOf('#', 3)));
            query= queryAndInsert.substring(queryAndInsert.indexOf('#', 3)+3);
        } else {
            query=queryAndInsert;
        }

        query = preProcess(query);

        if (query.toLowerCase().indexOf("from") == -1) {
            noFrom = true;
            query += " FROM org.makumba.db.makumba.Catalog c";
        }
        
        AST parsed = null;
        
        if(!Configuration.getQueryInliner().equals("tree")) {
            HqlParser parser=null;
            try{
                parser = HqlParser.getInstance(query);
                parser.statement();
            }catch(Throwable t){
                doThrow(t, parser!=null?parser.getAST():null);
            }
            doThrow(parser.error, parser.getAST());
            
            parsed = parser.getAST();

        } else {
            parsed = FunctionInliner.inlineQueryTree(query);
        }
        
        // we need to do the transformation first so the second-pass parser will accept the query
        MqlQueryAnalysisProvider.transformOQLParameters(parsed, parameterOrder);
        MqlQueryAnalysisProvider.transformOQL(parsed);
                
        MqlSqlWalker mqlAnalyzer = new MqlSqlWalker(query, insertIn, optimizeJoins, autoLeftJoin, false);
        analyser = mqlAnalyzer;
        try {
            mqlAnalyzer.statement(parsed);
        } catch (Throwable e) {
            doThrow(e, parsed);
        }
        doThrow(mqlAnalyzer.error, parsed);
        
        analyserTree = mqlAnalyzer.getAST();
        labels = mqlAnalyzer.rootContext.labels;
        aliases = mqlAnalyzer.rootContext.aliases;
        paramInfo = DataDefinitionProvider.getInstance().getVirtualDataDefinition("Parameters for " + query);
        proj = DataDefinitionProvider.getInstance().getVirtualDataDefinition("Projections for " + query);
        mqlAnalyzer.setProjectionTypes(proj);
        
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
                paramInfo.addField(DataDefinitionProvider.getInstance().makeFieldWithName("param" + i, fd));
            } else {
                throw new MakumbaError("Panic: could not compute type of parameter at position " + i + " with name '" + parameterOrder.get(i) + "' of query " + getQuery());
            }
            
        }
        
        long diff = new java.util.Date().getTime() - d.getTime();
        java.util.logging.Logger.getLogger("org.makumba.db.query.compilation").fine("MQL analysis: " + diff + " ms: " + query);
       
    }

    protected void doThrow(Throwable t, AST debugTree) {
        if (t == null)
            return;
        if (t instanceof RuntimeException) {
            t.printStackTrace();
            throw (RuntimeException) t;
        }
        String errorLocation = "";
        String errorLocationNumber="";
        if (t instanceof RecognitionException) {
            RecognitionException re = (RecognitionException) t;
            if (re.getColumn() > 0) {
                errorLocationNumber= " column "+re.getColumn()+" of ";
                StringBuffer sb = new StringBuffer();
                sb.append("\r\n");

                for (int i = 0; i < re.getColumn(); i++) {
                    sb.append(' ');
                }
                sb.append('^');
                errorLocation = sb.toString();
            }
        }
        throw new OQLParseError("\r\nin "+errorLocationNumber+" query:\r\n" + query + errorLocation+errorLocation+errorLocation, t);
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
        return expandedParamInfo;
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

    public static final String regExpInSET = "in" + RegExpUtils.minOneWhitespace + "set" + RegExpUtils.whitespace
            + "\\(";

    public static final Pattern patternInSet = Pattern.compile(regExpInSET);

    public static String preProcess(String query) {
        // replace -> (subset separators) with __
        query = query.replaceAll("->", "__");

        // replace IN SET with IN.
        Matcher m = patternInSet.matcher(query.toLowerCase()); // find all occurrences of lower-case "in set"
        while (m.find()) {
            int start = m.start();
            int beginSet = m.group().indexOf("set"); // find location of "set" keyword
            // System.out.println(query);
            // composing query by concatenating the part before "set", 3 space and the part after "set"
            query = query.substring(0, start + beginSet) + "   " + query.substring(start + beginSet + 3);
            // System.out.println(query);
            // System.out.println();
        }
        query = query.replaceAll("IN SET", "IN    ");
        return query;
    }

    private int labelCounter = 0;
    
    public String createLabel() {
        String l = "_x_gen_" + labelCounter++;
        generatedLabels.add(l);
        return l;
    }
    
    
    
    
    
    
    
    
    
    
    
    /**
     * What follows is the implementation of the {@link SQLQueryGenerator}
     */
    
    private Map<String, Object> arguments;
    
    public void setArguments(java.util.Map<String, Object> arguments) {
        this.arguments = arguments;
        
        if(expandedParamInfo == null) {
            expandMultipleParameters();
        }
        
    }
    
    public int getSQLArgumentNumber() {
        if(expandedParamInfo == null) {
            throw new MakumbaError("Can't call this method without having set the arguments with setArguments!");
        } else {
            return expandedParamInfo.getFieldNames().size();
        }
    }
    
    public DataDefinition getSQLQueryArgumentTypes() {
        return expandedParamInfo;
    }
    
    DataDefinition expandedParamInfo = null;

    public String getSQLQuery(NameResolver nr) {
        
        // FIXME add the acceptColons thingie?
        
        if(arguments == null && expandedParamInfo != null) {
            throw new MakumbaError("Error: you cannot ask for the unexpanded SQL query after having already called it when providing arguments");
        }
        
        MqlSqlGenerator mg = new MqlSqlGenerator();
        try {
            mg.statement(analyserTree);
        } catch (Throwable e) {
            doThrow(e, analyserTree);
        }
        doThrow(mg.error, analyserTree);

        text = mg.text;

        
        // TODO: we can cache these SQL results by the key of the NameResolver
        // still we should first check if this is needed, maybe the generated SQL (or processing of it)
        // is cached already somewhere else
        String sql = text.toString(nr);
        if (noFrom)
            return sql.substring(0, sql.toLowerCase().indexOf("from")).trim();
        return sql;
    }

    
    
    public Object[] getSQLQueryArguments() {
        
        if(arguments == null) {
            throw new MakumbaError("Error: arguments should have been set before calling getSQLQueryArguments using setArguments");
        }

        ArrayList<Object> res = new ArrayList<Object>();

        for (Iterator<String> e = parameterOrder.iterator(); e.hasNext();) {
            
            Object o = getArgumentValue(e.next(), arguments);
            
            if (o instanceof List<?>) {
                List<?> v = (List<?>) o;
                for (int i = 1; i <= v.size(); i++)
                    res.add(v.get(i - 1));
            } else {
                res.add(o);
            }
        }
        return res.toArray();
    }
    
    
    /**
     * Expands multiple parameters, i.e. parameters that are vectors or lists. This is necessary for execution of the SQL query.
     * This method expands the analyser tree and multiplies the parameters according the size of the multiple parameters,
     * and sets the expandedParamInfo so that clients of the {@link SQLQueryGenerator} can use it to do type checking on the SQL query parameters.
     */
    private void expandMultipleParameters() throws ProgrammerError {
        
        expandedParamInfo = DataDefinitionProvider.getInstance().getVirtualDataDefinition("SQL parameters for " + query);
        
        ArrayList<AST> queryParams = findQueryParameters(analyserTree, new ArrayList<AST>());
        
       // expand multiple params (vectors, lists) into multiple parameter entries
        for(int i = 0; i < parameterOrder.size(); i++) {
            Object val = getArgumentValue(parameterOrder.get(i), arguments);

            // now expand the query tree from one list to a number of elements
            if (val instanceof List<?>) {
                List<?> v = (List<?>) val;
                AST qp = queryParams.get(i);
                AST next = qp.getNextSibling();
                
                // we have to append as n - 1 parameters to the tree
                for (int j = 0; j < v.size() - 1; j++) {
                    // expand tree
                    qp.setNextSibling(ASTUtil.create(analyser.fact, HqlSqlTokenTypes.NAMED_PARAM, "?"));
                    qp = qp.getNextSibling();
                    if(j == v.size() - 1) {
                        qp.setNextSibling(next);
                    }
                 }
                
                // build expanded parameter types definition
                for(int k = 0; k < v.size(); k++) {
                    FieldDefinition fd = paramInfo.getFieldDefinition(i);
                    expandedParamInfo.addField(DataDefinitionProvider.getInstance().makeFieldWithName(fd.getName() + "_" + k, fd));
                }
            } else {
                expandedParamInfo.addField(paramInfo.getFieldDefinition(i));
            }
        }
    }   
    
    /**
     * Gets the value of a given argument, applies name transformation if necessary, and checks if the value is not null
     */
    private Object getArgumentValue(String argumentName, Map<String, Object> arguments) throws ProgrammerError {
        
        if(arguments == null) {
            throw new MakumbaError("Empty arguments provided");
        }
        
        if(argumentName == null) {
            throw new MakumbaError("Empty argumentName provided");

        }
        
        // if we have a makumba parameter (translated by MqlAnalysisProvider#transformOQLParameters) we need to recover the original argument index to get it in the map
        // indeed in the map of arguments we get, unnamed parameters like $1, $2, ... are registered with their name
        if(argumentName.startsWith(MAKUMBA_PARAM)) {
            argumentName = argumentName.substring(MAKUMBA_PARAM.length());
            int n = Integer.parseInt(argumentName);
            argumentName = "" + (n+1);
        }
        if(argumentName.indexOf("###") > 0) {
            argumentName = argumentName.substring(0, argumentName.indexOf("###"));
        }
        
        Object val = arguments.get(argumentName);
        if(val == null) { 
            throw new ProgrammerError("The parameter '"+argumentName+"' should not be null");
        }
        return val;
    }
    
    /**
     * Find all the named parameters in the analysis tree and puts them in a list
     */
    private ArrayList<AST> findQueryParameters(AST tree, ArrayList<AST> result) {
        
        if(tree == null) {
            return result;
        }
        
        // we only look for named params since those are the ones MQL uses
        if(tree.getType() == HqlSqlTokenTypes.NAMED_PARAM) {
            result.add(tree);
        }

        // recursive-descent traversal, first the children, then the siblings
        findQueryParameters(tree.getFirstChild(), result);
        findQueryParameters(tree.getNextSibling(), result);
        
        return result;
    }

    
    public static void main(String[] args) {
        
        MqlQueryAnalysis qA = new MqlQueryAnalysis("SELECT i.name, $actor_test_Individual FROM test.Individual i WHERE i.surname=$surname and i.name = $2", false, true);
        
        Map<String, Object> arguments = new HashMap<String, Object>();
        
        Vector<String> test = new Vector<String>();
        test.add("la");
        test.add("la");
        test.add("la");
        arguments.put("actor_test_Individual", test);
        arguments.put("surname", "john");
        arguments.put("2", "stuff");
        
        qA.setArguments(arguments);
        
        String sql = qA.getSQLQuery(new NameResolver());
        System.out.println("QUERY: " + sql);
        
        Object[] arg = qA.getSQLQueryArguments();
        System.out.println("ARGS: " + Arrays.toString(arg));
        
        System.out.println("SIZE: " + qA.getSQLArgumentNumber());
        
        System.out.println("TYPES: + " + qA.getSQLQueryArgumentTypes());
        for(String n : qA.getSQLQueryArgumentTypes().getFieldNames()) {
          System.out.println(qA.getSQLQueryArgumentTypes().getFieldDefinition(n));
        }
        
    }
    
}
