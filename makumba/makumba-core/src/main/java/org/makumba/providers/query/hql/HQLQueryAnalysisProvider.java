package org.makumba.providers.query.hql;

import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;
import org.makumba.commons.NamedResourceFactory;
import org.makumba.commons.NamedResources;
import org.makumba.providers.QueryAnalysis;
import org.makumba.providers.QueryAnalysisProvider;

import antlr.collections.AST;

public class HQLQueryAnalysisProvider extends QueryAnalysisProvider {

    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return "hql";
    }

    @Override
    public QueryAnalysis getRawQueryAnalysis(String query) {
        return getHqlAnalyzer(query);
    }

    static public HqlAnalyzer getHqlAnalyzer(String hqlQuery) {
        return (HqlAnalyzer) NamedResources.getStaticCache(parsedHqlQueries).getResource(hqlQuery);
    }

    public static int parsedHqlQueries = NamedResources.makeStaticCache("Hibernate HQL parsed queries",
        new NamedResourceFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object makeResource(Object nm, Object hashName) throws Exception {
                return new HqlAnalyzer((String) nm);
            }
        }, true);

    @Override
    public FieldDefinition getAlternativeField(DataDefinition dd, String fn) {
        if (fn.equals("id")) {
            return dd.getFieldDefinition(dd.getIndexPointerFieldName());
        }
        return null;

    }

    @Override
    public String getPrimaryKeyNotation(String label) {
        // this is specific to Hibernate: we add '.id' in order to get the id as in makumba
        if (label.indexOf('.') == -1) {
            label += ".id";
        }
        return label;
    }

    @Override
    public String getParameterSyntax() {
        return ":";
    }

    @Override
    public QueryAnalysis getQueryAnalysis(AST pass1, DataDefinition knownLabels) {
        return new HqlAnalyzer(pass1, knownLabels);
    }

}
