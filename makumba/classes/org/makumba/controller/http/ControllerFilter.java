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

package org.makumba.controller.http;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.makumba.commons.ControllerHandler;
import org.makumba.commons.RuntimeWrappedException;
import org.makumba.commons.ServletObjects;
import org.makumba.controller.MakumbaResponseWrapper;

/**
 * The filter that controls each makumba HTTP access. Performs login, form response, exception handling. This filter
 * uses a number of {@link ControllerHandler}-s which each serve a specific purpose.
 * 
 * @author Cristian Bogdan
 * @author Manuel Gay
 * @version $Id$ *
 */
public class ControllerFilter implements Filter {

    private FilterConfig conf;

    private String handlerClasses = "org.makumba.controller.AJAXDataControllerHandler," //
            + "org.makumba.devel.SourceViewControllerHandler," //
            + "org.makumba.devel.DataToolsControllerHandler," //
            + "org.makumba.commons.MakumbaToolsControllerHandler," //
            + "org.makumba.devel.ErrorControllerHandler," //
            + "org.makumba.analyser.AnalysisInitControllerHandler," //
            + "org.makumba.controller.FilterConditionControllerHandler," //
            + "org.makumba.controller.CharsetControllerHandler," //
            + "org.makumba.commons.attributes.DatabaseConnectionControllerHandler," //
            + "org.makumba.commons.attributes.AttributesControllerHandler," //
            + "org.makumba.forms.responder.ResponseControllerHandler";

    private ArrayList<ControllerHandler> handlers = new ArrayList<ControllerHandler>();

    public void init(FilterConfig c) {
        conf = c;
        String handlerParam = c.getInitParameter("handlerClasses");
        if (handlerParam == null) {
            handlerParam = handlerClasses;
        }
        StringTokenizer str = new StringTokenizer(handlerParam, ",");

        while (str.hasMoreTokens()) {
            try {
                handlers.add((ControllerHandler) Class.forName(str.nextToken().trim()).newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static final JspFactory fact = JspFactory.getDefaultFactory();

    static JspFactory makFact = new JspFactory() {

        @Override
        public JspEngineInfo getEngineInfo() {
            return fact.getEngineInfo();
        }

        @Override
        public PageContext getPageContext(Servlet servlet, ServletRequest request, ServletResponse response,
                String errorPageURL, boolean needsSession, int buffer, boolean autoflush) {
            // System.out.println(servlet);
            return fact.getPageContext(servlet, request, response, errorPageURL, needsSession, buffer, autoflush);
            // here we can hang the pageContext in a threadLocal stack
            // and also trigger page analysis
            // this also tells us when a page starts or is included

        }

        @Override
        public void releasePageContext(PageContext pc) {
            fact.releasePageContext(pc);
            // this tells us when a page finishes and if it was included, we will go back to the including page
            // here we can pop the pageContext from the thread local stack
            // and activate the runtime state of the makumba tags from the including page

        }
    };

    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException,
            java.io.IOException {

        JspFactory.setDefaultFactory(makFact);

        // replace the response by the MakumbaResponseWrapper, which modifies the output to inject links to CSS/JS files
        // TODO: consider using a ControllerHandler; then, however, we can only work with ServletObjects in all methods
        resp = new MakumbaResponseWrapper((HttpServletResponse) resp, (HttpServletRequest) req);

        int i = 0;
        int imax = -1;
        ServletObjects servletObjects = new ServletObjects(req, resp);
        try {
            for (; i < handlers.size(); i++) {
                imax = i;
                if (!handlers.get(i).beforeFilter(req, resp, conf, servletObjects)) {
                    break;
                }
            }

            for (i = imax; i >= 0; i--) {
                handlers.get(i).afterBeforeFilter(req, resp, conf);
            }

            if (imax == handlers.size() - 1) { // continue filtering if all handlers have returned true
                chain.doFilter(servletObjects.getRequest(), servletObjects.getResponse());
            }

            for (i = imax; i >= 0; i--) {
                handlers.get(i).afterFilter(req, resp, conf);
            }
        } catch (Throwable t) {
            for (i = imax; i >= 0; i--) {
                if (!handlers.get(i).onError(req, resp, t, conf)) {
                    return;
                }
            }
            throw new RuntimeWrappedException(t);
        }

        finally {
            for (i = handlers.size() - 1; i >= 0; i--) {
                try {
                    handlers.get(i).finalize(req, resp);
                } catch (Throwable t) {
                    throw new RuntimeWrappedException(t);
                }
            }
        }

    }

    public void destroy() {

    }

}
