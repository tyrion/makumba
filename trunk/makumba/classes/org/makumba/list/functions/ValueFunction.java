package org.makumba.list.functions;

import java.util.Stack;

import javax.servlet.jsp.PageContext;

import org.makumba.LogicException;
import org.makumba.MakumbaError;
import org.makumba.ProgrammerError;
import org.makumba.analyser.AnalysableElement;
import org.makumba.analyser.AnalysableExpression;
import org.makumba.analyser.AnalysableTag;
import org.makumba.analyser.PageCache;
import org.makumba.commons.MakumbaJspAnalyzer;
import org.makumba.commons.MultipleKey;
import org.makumba.commons.StringUtils;
import org.makumba.controller.http.MakumbaJspFactory;
import org.makumba.list.engine.ComposedQuery;
import org.makumba.list.engine.valuecomputer.ValueComputer;
import org.makumba.list.tags.QueryTag;
import org.makumba.list.tags.ValueTag;

/**
 * Implements a mak:value() function, similar to the mak:value tag ({@link ValueTag}). The advantage of the function is
 * that it can be used inside JSTL/EL statements, e.g. a c:if. The function provides just basic value computing, but not
 * the advanced formatting that the {@link ValueTag} provides.
 * 
 * @author Rudolf Mayer
 * @version $Id$
 */
public class ValueFunction extends AnalysableExpression {

    private static final long serialVersionUID = 1L;

    @Override
    public void analyze(PageCache pageCache) {
        checkNumberOfArguments(1);
        String expr = StringUtils.removeSingleQuote(elData.getArguments().get(0));

        QueryTag parentList = getEnclosingList();
        if (parentList == null) {
            throw new ProgrammerError("Function '" + expression + "' needs to be enclosed in a LIST or OBJECT tag");
        }

        // analogously to ValueTag, we register a value computer
        ValueComputer vc = ValueComputer.getValueComputerAtAnalysis(true, parentList.getTagKey(), expr, pageCache);
        pageCache.cache(MakumbaJspAnalyzer.VALUE_COMPUTERS, key, vc);

        // FIXME: the following code is similar to ValueTag.doStartAnalyze; unifying might make sense.
        // if we add a projection to a query, we also cache this so that we know where the projection comes from (for
        // the relation analysis)
        ComposedQuery query = null;
        try {
            query = QueryTag.getQuery(pageCache, parentList.getTagKey());
        } catch (MakumbaError me) {
            // this happens when there is no query for this mak:value
            // we ignore it, query will stay null anyway
        }

        if (query != null) {
            pageCache.cache(MakumbaJspAnalyzer.PROJECTION_ORIGIN_CACHE, new MultipleKey(parentList.getTagKey(), expr),
                key);
        }
    }

    /**
     * Tells the ValueComputer to finish analysis.
     * 
     * @param pageCache
     *            the page cache of the current page
     */
    @Override
    public void doEndAnalyze(PageCache pageCache) {
        // analogously to ValueTag, we tell the value computer t a value computer
        ValueComputer vc = (ValueComputer) pageCache.retrieve(MakumbaJspAnalyzer.VALUE_COMPUTERS, key);
        vc.doEndAnalyze(pageCache);
    }

    private QueryTag getEnclosingList() {
        return (QueryTag) findParentWithClass(QueryTag.class);
    }

    @Override
    public String getPrefix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object resolve(PageContext pc, PageCache pageCache) throws LogicException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setKey(PageCache pageCache) {
        checkNumberOfArguments(1);
        AnalysableTag parentList = getEnclosingList();
        if (parentList == null) {
            throw new ProgrammerError("Function '" + expression + "' needs to be enclosed in a LIST or OBJECT tag");
        }
        String expr = StringUtils.removeSingleQuote(elData.getArguments().get(0));
        key = computeKey(expr, parentList.getTagKey());
    }

    private static MultipleKey computeKey(String expr, MultipleKey parentListKey) {
        return new MultipleKey(parentListKey, expr.trim());
    }

    @Override
    public String treatExpressionAtAnalysis(String expression) {
        return expression;
    }

    public static Object value(String expr) {
        // resolving the value expression is relatively easy, and similar to ValueTag
        //
        // 1. we need to get the key from the parent mak:list/object
        // as the EL function is static, we rely on QueryTag setting the key of the current list to the pageContext
        //
        // 2. then get the pageCache, and simply retrieve the value computer for the key combined of parentList and expr
        //

        PageContext pageContext = MakumbaJspFactory.getPageContext();

        // retrieve the key of the current mak:list from the stack of lists
        Stack<MultipleKey> currentListKeyStack = QueryTag.getRunningQueryTagStack(pageContext);
        MultipleKey parentListKey = currentListKeyStack.peek();

        // retrieve the value computer for the expression from the pageCache
        PageCache pageCache = AnalysableElement.getPageCache(pageContext, MakumbaJspAnalyzer.getInstance());
        ValueComputer vc = (ValueComputer) pageCache.retrieve(MakumbaJspAnalyzer.VALUE_COMPUTERS, computeKey(expr,
            parentListKey));

        if (vc != null) {
            try {
                return vc.getValue(pageContext);
            } catch (LogicException e) {
                // TODO: some error handling...
                e.printStackTrace();
                return null;
            }
        } else {
            // TODO: some error handling...
            return null;
        }
    }

}