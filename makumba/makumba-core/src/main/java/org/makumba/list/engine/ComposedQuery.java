///////////////////////////////
//  Makumba, Makumba tag library
//  Copyright (C) 2000-2003  http://www.makumba.org
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
//  -------------
//  $Id$
//  $Name$
/////////////////////////////////////

package org.makumba.list.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.makumba.DataDefinition;
import org.makumba.LogicException;
import org.makumba.UnauthorizedException;
import org.makumba.list.engine.ComposedQueryAuthorization.AuthorizationInfo;
import org.makumba.providers.QueryAnalysisProvider;
import org.makumba.providers.QueryParameters;
import org.makumba.providers.QueryProvider;

/**
 * An OQL query composed from various elements found in script pages. It can be enriched when a new element is found. It
 * has a prepared Qyuery correspondent in a makumba database It may be based on a super query.
 * 
 * @author Cristian Bogdan
 * @version $Id$
 */
public class ComposedQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Interface for an Evaluator which can evaluate expressions
     * 
     * @author Cristian Bogdan
     */
    public static interface Evaluator {

        /**
         * Evaluates the expression
         * 
         * @param s
         *            the expression to evaluate
         * @return The transformed expression after evaluation
         */
        String evaluate(String s);
    }

    public QueryAnalysisProvider qep = null;

    private boolean selectAllLabels;

    public ComposedQuery(String[] sections, String queryLanguage) {
        this(sections, queryLanguage, false, null);
    }

    public ComposedQuery(String[] sections, String queryLanguage, boolean selectAllLabels) {
        this(sections, queryLanguage, selectAllLabels, null);
    }

    /**
     * Default constructor
     * 
     * @param sections
     * @param selectAllLabels
     * @param usesHQL
     */
    public ComposedQuery(String[] sections, String queryLanguage, boolean selectAllLabels, List<String> authorization) {
        this.sections = sections;
        this.derivedSections = sections;
        this.qep = QueryProvider.getQueryAnalzyer(queryLanguage);
        this.selectAllLabels = selectAllLabels;
        this.authorization = authorization;
        if (authorization == null) {
            this.authorization = new ArrayList<String>();
        }
    }

    /** The subqueries of this query */
    int subqueries = 0;

    Projections basicProjections = new Projections();

    /** Standard index for the FROM query section */
    public static final int FROM = 0;

    /** Standard index for the WHERE query section */
    public static final int WHERE = 1;

    /** Standard index for the GROUPBY query section */
    public static final int GROUPBY = 2;

    /** Standard index for the ORDERBY query section */
    public static final int ORDERBY = 3;

    /** Standard index for the VARFROM query section */
    public static final int VARFROM = 4;

    /** Standard index for the STATIC_CONDITION query section */
    public static final int STATIC_WHERE = 5;

    /** Standard index for DISTINCT flag */
    public static final int DISTINCT = 6;

    /** Standard index for FILTERS flag */
    public static final int FILTERS = 7;

    private static final List<AuthorizationInfo> EMPTY_AUTH = new ArrayList<AuthorizationInfo>();

    /** Section texts, encoded with the standard indexes */
    String[] sections;

    List<String> authorization;

    /** Derived section texts, made from the sections of this query and the sections of its superqueries */
    String[] derivedSections;

    String typeAnalyzerOQL;

    String fromAnalyzerOQL;

    /**
     * The keyset defining the primary key for this query. Normally the primary key is made of the keys declared in
     * FROM, in this query and all the parent queries. Keys are kept as integers (indexes)
     */
    List<Integer> keyset;

    /** The keyset of all the parent queries */
    List<List<Integer>> previousKeyset;

    /** The labels of the keyset */
    List<String> keysetLabels;

    QueryParameters filterAttributes;

    /** A Vector containing an empty vector. Used for empty keysets */
    static List<List<Integer>> empty;
    static {
        empty = new ArrayList<List<Integer>>();
        empty.add(new ArrayList<Integer>());
    }

    /**
     * Gets the type of the result
     * 
     * @return The DataDefinition corresponding to the type of the result
     */
    public DataDefinition getResultType() {
        if (typeAnalyzerOQL == null) {
            return null;
        } else {
            return qep.getQueryAnalysis(typeAnalyzerOQL).getProjectionType();
        }
    }

    /**
     * Gets the type of a given label
     * 
     * @param s
     *            the name of the label
     * @return A DataDefinition corresponding to the type of the label
     */
    public DataDefinition getLabelType(String s) {
        if (typeAnalyzerOQL == null) {
            return null;
        } else {
            return qep.getQueryAnalysis(typeAnalyzerOQL).getLabelType(s);
        }
    }

    /**
     * Initializes the object. This is a template method
     */
    public void init() {
        initKeysets();
        fromAnalyzerOQL = "SELECT 1 ";
        if (getFromSection() != null) {
            fromAnalyzerOQL += "FROM " + getFromSection();
        }
        if (derivedSections.length > FILTERS && StringUtils.isNotBlank(derivedSections[FILTERS])) {
            StringTokenizer st = new StringTokenizer(derivedSections[FILTERS], ";");
            String filterAnalyzerOQL = "SELECT ";
            boolean filters = false;
            while (st.hasMoreTokens()) {
                filters = true;
                String cond = st.nextToken();
                filterAnalyzerOQL += cond;
                filterAnalyzerOQL += ", ";
            }

            if (!filters) {
                return;
            }
            filterAnalyzerOQL += " 1 ";
            if (getFromSection() != null) {
                filterAnalyzerOQL += "FROM " + getFromSection();
            }
            this.filterAttributes = qep.getQueryAnalysis(filterAnalyzerOQL).getQueryParameters();
        }

    }

    public QueryParameters getFilterAttributes() {
        return filterAttributes;
    }

    /**
     * Gets the FROM section
     * 
     * @return A String containing the FROM section of the query
     */
    public String getFromSection() {
        return derivedSections[FROM];
    }

    /**
     * Gets the GROUP BY section
     * 
     * @return A String containing the GROUP BY section of the query
     */
    public String getGroupBySection() {
        return derivedSections[GROUPBY];
    }

    /**
     * Initializes the keysets. previousKeyset is "empty"
     */
    protected void initKeysets() {
        previousKeyset = empty;
        keyset = new ArrayList<Integer>();
        keysetLabels = new ArrayList<String>();
    }

    /**
     * Adds a subquery to this query. Makes it aware that it has subqueries at all.
     * 
     * @param q
     *            the subquery
     */
    protected void addSubquery(ComposedSubquery q) {
        if (subqueries == 0) {
            prependFromToKeyset();
        }
        subqueries++;
    }

    /**
     * Adds all keys from the FROM section to the keyset, and their labels to the keyLabels. They are all added as
     * projections (this has to change)
     */
    public void prependFromToKeyset() {
        basicProjections.projectionExpr.clear();
        Iterator<String> e = new ArrayList<String>(basicProjections.projections).iterator();
        basicProjections.projections.clear();

        // add the previous keyset
        for (int i = 0; i < keyset.size(); i++) {
            checkProjectionInteger(e.next());
        }

        if (!isDistinct()) {
            for (StringTokenizer st = new StringTokenizer(sections[FROM] == null ? "" : sections[FROM], ","); st.hasMoreTokens();) {
                String label = st.nextToken().trim();
                int j = label.lastIndexOf(" ");
                if (j == -1) {
                    throw new RuntimeException("invalid FROM, looking for a label");
                }
                label = label.substring(j + 1).trim();

                label = qep.getPrimaryKeyNotation(label);

                keysetLabels.add(label);
                keyset.add(basicProjections.addProjection(label));
            }
            while (e.hasNext()) {
                checkProjectionInteger(e.next());
            }
        } else {
            while (e.hasNext()) {
                String expr = e.next();
                keysetLabels.add(expr);
                keyset.add(basicProjections.addProjection(expr));
            }
        }
    }

    public Integer checkProjectionInteger(String expr) {
        return basicProjections.checkProjectionInteger(expr);
    }

    /**
     * Computes the query from its sections
     * 
     * @param derivedSections
     *            the sections of this query
     * @param typeAnalysisOnly
     *            indicates whether this is only a type analysis
     * @return The computed OQL query
     */
    protected String computeQuery(Projections proj, String derivedSections[], boolean typeAnalysisOnly) {
        String groups = null;
        String orders = null;
        if (!typeAnalysisOnly) {
            groups = derivedSections[GROUPBY];
            orders = derivedSections[ORDERBY];
        }

        StringBuffer sb = new StringBuffer();
        sb.append("SELECT ");

        if (isDistinct()) {
            sb.append("DISTINCT ");
        }
        String sep = "";

        int i = 0;

        for (String string : proj.projections) {
            sb.append(sep);
            sep = ",";
            sb.append(string).append(" AS ").append(Projections.columnName(new Integer(i++)));
        }

        if (proj.projections.size() == 0) {
            sb.append("1");
        }

        Object o;

        if ((o = derivedSections[FROM]) != null) {
            sb.append(" FROM ");
            sb.append(o);

            // there can be no VARFROM without FROM
            // VARFROM is not part of type analysis
            // (i.e. projections don't know about it)
            if (!typeAnalysisOnly && derivedSections.length == 5 && derivedSections[VARFROM] != null
                    && derivedSections[VARFROM].trim().length() > 0) {
                sb.append(",").append(derivedSections[VARFROM]);
            }
        }
        if (!typeAnalysisOnly) {
            if (derivedSections[WHERE] != null && derivedSections[WHERE].trim().length() > 0) {
                sb.append(" WHERE ");
                sb.append(derivedSections[WHERE]);
            }
            if (groups != null && groups.trim().length() > 0) {
                sb.append(" GROUP BY ");
                sb.append(groups);
            }
            if (orders != null && orders.trim().length() > 0) {
                sb.append(" ORDER BY ");
                sb.append(orders);
            }
        }
        String ret = sb.toString();
        if (!typeAnalysisOnly) {
            return ret;
        }

        // replace names with numbers
        /*        ArgumentReplacer ar = new ArgumentReplacer(ret);
                Map<String, Object> d = new HashMap<String, Object>();
                int j = 1;
                for (Iterator<String> e = ar.getArgumentNames(); e.hasNext();)
                    d.put(e.next(), "$" + (j++));
                return ar.replaceValues(d);*/
        return ret;
    }

    private boolean isDistinct() {
        return sections.length > DISTINCT && "true".equals(sections[DISTINCT]);
    }

    // ------------
    /**
     * Executes the contained query in the given database
     * 
     * @param qep
     *            the database where the query should be ran
     * @param args
     *            the arguments we may need during the execution
     * @param v
     *            the evaluator evaluating the expressions
     * @param offset
     *            at which iteration this query should start
     * @param limit
     *            how many times should this query be ran
     * @throws LogicException
     */
    public Grouper execute(QueryProvider qep, Map<String, Object> args, Evaluator v, int offset, int limit)
            throws UnauthorizedException {
        analyze();
        String[] vars = new String[5];
        vars[0] = getFromSection();
        for (int i = 1; i < 5; i++) {
            vars[i] = derivedSections[i] == null ? null : v.evaluate(derivedSections[i]);
        }

        if (derivedSections.length > STATIC_WHERE && StringUtils.isNotBlank(derivedSections[STATIC_WHERE])) {
            // add the static condition to the WHERE part
            // first, check if the dynamic where condition is not blank; for this, we need to evaluate it

            addToWhere(vars, derivedSections[STATIC_WHERE]);
        }

        if (derivedSections.length > FILTERS && StringUtils.isNotBlank(derivedSections[FILTERS])) {
            StringTokenizer st = new StringTokenizer(derivedSections[FILTERS], ";");
            while (st.hasMoreTokens()) {
                String cond = st.nextToken();
                boolean allSet = true;
                int n, start = 0;
                while ((n = cond.indexOf('$', start)) != -1) {
                    start = n + 1;
                    while (start < cond.length() && Character.isJavaIdentifierPart(cond.charAt(start++))) {
                        ;
                    }
                    try {
                        Object val = qep.getContextAttributes().getAttribute(cond.substring(n + 1, start - 1));
                        if (val == null || new Vector<Object>().equals(val) || "".equals(val)) {
                            allSet = false;
                        }
                    } catch (LogicException e) {
                        allSet = false;
                    }
                }
                if (allSet) {
                    addToWhere(vars, cond);
                }
            }
        }

        Projections pr = basicProjections;
        List<AuthorizationInfo> authorize = EMPTY_AUTH;
        if (!authorization.isEmpty()) {
            ComposedQueryAuthorization cqa = ComposedQueryAuthorization.getAuthorization(this, vars[WHERE],
                computeQuery(basicProjections, vars, false));
            pr = cqa.getProjections();
            authorize = cqa.getAuthorizationInfos();
            vars[WHERE] = cqa.getWhere();
        }

        return new Grouper(previousKeyset, qep.execute(computeQuery(pr, vars, false), args, offset, limit).iterator(),
                authorize);
    }

    void addToWhere(String[] vars, String condition) {
        if (StringUtils.isNotBlank(vars[WHERE])) {
            vars[WHERE] += " AND " + condition;
        } else {
            vars[WHERE] = condition;
        }
    }

    public synchronized void analyze() {
        // if we have subqueries, we already did prepend.
        // if we don't but we were requested to select all labels, we prepend
        if (basicProjections.projections.isEmpty() || selectAllLabels && subqueries == 0) {
            prependFromToKeyset();
            // make sure that this doesn't repeat
            selectAllLabels = false;
        }
        if (typeAnalyzerOQL == null) {
            typeAnalyzerOQL = computeQuery(basicProjections, derivedSections, true);
        }
    }

    public Integer getProjectionIndex(String expr) {
        return basicProjections.getProjectionIndex(expr);
    }

    /**
     * allows to directly set a projection. Used for totalCount in
     * {@link QueryTag#doAnalyzedStartTag(org.makumba.analyser.PageCache)} to compose a query with 'count(*)' as the
     * only projection.
     */
    public void addProjectionDirectly(String s) {
        basicProjections.projections.add(s);
    }

    @Override
    public String toString() {
        return "Composed query: " + typeAnalyzerOQL;
    }

    public String getComputedQuery() {
        return computeQuery(basicProjections, derivedSections, false);
    }

    /**
     * Gets the query string.
     * 
     * @return the query string in a form that can be used by a type analyser, in the query language of the
     *         ComposedQuery
     */
    public String getTypeAnalyzerQuery() {
        return typeAnalyzerOQL;
    }

    /**
     * Gets the projections of this query
     * 
     * @return a {@link Vector} containing the projections of this ComposedQuery
     */
    public List<String> getProjections() {
        return basicProjections.projections;
    }

    /**
     * Gets the types of the labels in the FROM section
     * 
     * @return a Map containing the labels and their type
     */
    public Map<String, DataDefinition> getFromLabelTypes() {
        return qep.getQueryAnalysis(fromAnalyzerOQL).getLabelTypes();
    }

    public Object checkExprSetOrNullable(String expr) {
        return qep.checkExprSetOrNullable(getFromSection(), expr);
    }

    public String getProjectionAt(int i) {
        return basicProjections.getProjectionAt(i);
    }

}
