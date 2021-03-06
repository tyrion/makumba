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

package org.makumba.devel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.makumba.analyser.engine.JavaParseData;
import org.makumba.commons.http.MakumbaServlet;
import org.makumba.commons.tags.MakumbaJspConfiguration;
import org.makumba.devel.relations.FileRelations;
import org.makumba.devel.relations.FileRelations.RelationOrigin;
import org.makumba.devel.relations.RelationCrawler;
import org.makumba.providers.Configuration;
import org.makumba.providers.DataDefinitionProvider;

/**
 * a viewer that shows everything per line
 * 
 * @version $Id$
 * @author Stefan Baebler
 * @author Rudolf Mayer
 */
public abstract class LineViewer implements SourceViewer {
    private static final String TYPE_JAVA = "Java";

    private static final String TYPE_JSP = "JSPs";

    private static final String TYPE_MDD = "MDD";

    private static final Pattern patternUrl = Pattern.compile("[http:|/|\\w]+\\.\\w+[\\.\\w]*[/|\\w]*");

    protected static final String PARAM_HIDE_LINES = "hideLines";

    protected String realPath;

    /** Store other info to be displayed in the header */
    protected String additionalHeaderInfo;

    protected String virtualPath;

    protected String contextPath;

    protected Reader reader;

    protected boolean printLineNumbers;

    protected File dir;

    protected String title;

    protected String viewerName;

    protected boolean searchJSPPages = true;

    protected boolean searchCompiledJSPClasses = true;

    protected boolean searchJavaClasses = true;

    protected boolean searchMDD = true;

    // default for old .jspx - change this to "s" when .jsps gets adopted (bug 677)
    protected String jspSourceViewExtension = "x";

    protected String jspClasspath;

    protected HttpServletRequest request;

    protected String servletPath;

    protected String logicPath;

    protected String additionalCodeStyleClasses = "";

    protected boolean hideLineNumbers = false;

    /** Default packages to be known. Use {@link JavaParseData#getImportedPackages()} to add more */
    protected String[] importedPackages = new String[] { "java.lang." };

    protected Hashtable<String, String> importedClasses = new Hashtable<String, String>();

    protected Error caughtError;

    protected boolean printHeaderFooter = true;

    private String versionControlRepositoryURL;

    private String versionControlRepositoryLinkText = "CVS";

    protected Throwable parseError = null;

    protected void addImportedPackages(HashSet<String> newPackages) {
        newPackages.addAll(Arrays.asList(importedPackages));
        importedPackages = newPackages.toArray(new String[newPackages.size()]);
    }

    /** if this resource is actually a directory, returns not null */
    @Override
    public File getDirectory() {
        if (dir != null && dir.isDirectory()) {
            return dir;
        }
        return null;
    }

    public Reader getReader() {
        return reader;
    }

    void readFromURL(java.net.URL u) throws IOException {
        if (u == null) {
            throw new FileNotFoundException(virtualPath);
        }
        realPath = u.getFile();
        try {
            dir = new File(realPath);
            if (!dir.isDirectory()) {
                reader = new InputStreamReader(new FileInputStream(dir));
            }
        } catch (FileNotFoundException fnfe) {
            realPath = null;
            reader = new InputStreamReader((InputStream) u.getContent());
        }
    }

    public LineViewer(boolean printLineNumbers, HttpServletRequest request) {
        this.request = request;
        this.printLineNumbers = printLineNumbers;
        versionControlRepositoryURL = MakumbaJspConfiguration.getRepositoryURL();
        versionControlRepositoryLinkText = MakumbaJspConfiguration.getRepositoryLinkText();

        contextPath = request.getContextPath();
        hideLineNumbers = StringUtils.equals(request.getParameter(PARAM_HIDE_LINES), "true");
    }

    public LineViewer(boolean printLineNumbers, HttpServletRequest request, ServletContext servletContext) {
        this.request = request;
        this.printLineNumbers = printLineNumbers;
        contextPath = request.getContextPath();
        hideLineNumbers = request.getParameter(PARAM_HIDE_LINES) != null
                && request.getParameter(PARAM_HIDE_LINES).equals("true");
    }

    /**
     * parse the text and write the output
     */
    @Override
    public void parseText(PrintWriter writer) throws IOException {
        long begin = System.currentTimeMillis();
        printPageBegin(writer);

        // display error messages if we have caught an exception

        if (caughtError != null) {
            setSearchLevels(true, true, true, true);
            writer.println(parseLine("There were errors analyzing the page - the page analysis reports an <i>"
                    + caughtError.getClass().getName() + "</i>")
                    + "\n");
            writer.println("<span style=\"color: red    ; \">" + caughtError.getMessage() + "</span>");
            writer.print("<hr color:\"red\" style=\"background-color: red; border-width: 0px;\">");
        }

        // we go line by line as an MDD references cannot span over newlines
        // as a bonus, we print the line numbers as well.
        LineNumberReader lr = new LineNumberReader(reader);
        String s = null;
        while ((s = lr.readLine()) != null) {
            if (printLineNumbers) {
                writeLineNumber(writer, lr.getLineNumber(), !hideLineNumbers);
            }
            if (this instanceof mddViewer) {
                printLine(writer, s, parseLine(s));
            } else {
                printLine(writer, s, parseLine(htmlEscape(s)));
            }
        }
        printPageEnd(writer);
        reader.close();
        double timeTaken = System.currentTimeMillis() - begin;
        java.util.logging.Logger.getLogger("org.makumba.org.makumba.devel.sourceViewer").fine(
            "Sourcecode viewer took :" + timeTaken / 1000.0 + " seconds");
    }

    protected void writeLineNumber(PrintWriter writer, int n, boolean initialVisibility) {
        writer.print("<a style=\"" + (initialVisibility ? "" : "display: none;") + " \" name=\"" + n + "\" href=\"#"
                + n + "\" class=\"lineNo\">" + n + ":\t</a>");
    }

    /**
     * @param writer
     * @throws IOException
     */
    public void printPageEnd(PrintWriter writer) throws IOException {
        printPageEnd(writer, null);
    }

    public void printPageEnd(PrintWriter writer, StringBuffer extra) throws IOException {
        writer.println("</code></pre>");
        if (extra != null) {
            writer.println(extra);
        }
        footer(writer);
        if (printHeaderFooter) {
            writer.println("\n</body></html>");
        }
    }

    /**
     * Write the beginning of the page to the given writer.
     */
    public void printPageBegin(PrintWriter writer) throws IOException {
        if (realPath != null && virtualPath != null) {
            title = virtualPath + "";
        } else if (StringUtils.isBlank(title)) {
            title = "";
        }
        if (printHeaderFooter) {
            DevelUtils.writePageBegin(writer);
            DevelUtils.writeStylesAndScripts(writer, contextPath);
            DevelUtils.writeTitleAndHeaderEnd(writer, title + " | Makumba " + viewerName);
            DevelUtils.printNavigationBegin(writer, viewerName);
            navigation(writer);
            DevelUtils.printNavigationEnd(writer);

            if (!StringUtils.isBlank(title) && !title.equals(virtualPath)) {
                writer.print("<h2><a class=\"brand\" href=\"#\">" + title + "</a></h2>");
            } else if (virtualPath != null) {
                writer.print("<h2><a class=\"brand\" href=\"" + virtualPath + "\">" + virtualPath + "</a></h2>");
            }

            if (additionalHeaderInfo != null) {
                writer.println(additionalHeaderInfo);
            }

            if (realPath != null) {
                writer.println("<small>" + new File(realPath).getCanonicalPath() + "</small>");
            }

            if (StringUtils.isNotBlank(printVersionControlLink())) {
                writer.println(printVersionControlLink());
            }

            printPageBeginAdditional(writer);

            writer.print("  <div class=\"viewer-options\">");

            if (printLineNumbers) {
                String urlParams = "";
                Enumeration<?> e = request.getParameterNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    Object value = request.getParameter(key);
                    if (!key.equals(PARAM_HIDE_LINES)) {
                        if (!urlParams.equals("")) {
                            urlParams += "&";
                        }
                        urlParams += key + "=" + value;
                    }
                }

                if (!urlParams.equals("")) {
                    urlParams += "&";
                }
                urlParams += PARAM_HIDE_LINES + "=" + !hideLineNumbers;
                if (!urlParams.equals("")) {
                    urlParams = "?" + urlParams;
                }
                String linkText = (hideLineNumbers ? "Show" : "Hide") + " line numbers";
                writer.println("<a href=\"" + request.getRequestURI() + urlParams
                        + "\" onclick=\"toggleLineNumbers(); return false;\">" + linkText + "</a>");
                writeAdditionalLinks(writer);
            }

            printAdditionalViewerOptions(writer);

            writer.println(" </div>");

        }
        writer.print("<pre class=\"" + additionalCodeStyleClasses + "\"><code>");
    }

    protected void printFileRelations(PrintWriter writer) {
        writer.println("<li>");
        DevelUtils.printPopoverLink(writer, "Relations", "", "fileRelations");
        writer.println("</li>");
        writer.println("<div id=\"fileRelations\" style=\"display: none;\">");
        String webAppRoot = request.getSession().getServletContext().getRealPath("/");
        int maxDisplay = 10;
        if (webAppRoot.endsWith("/")) {
            webAppRoot = webAppRoot.substring(0, webAppRoot.length() - 1);
        }
        RelationCrawler relationCrawler = RelationCrawler.getRelationCrawler(webAppRoot,
            RelationCrawler.getDefaultTargetDatabase(), false, "file://", "", false);

        if (realPath != null && realPath.startsWith(webAppRoot)) {
            String filePath = realPath.substring(webAppRoot.length());
            try {
                FileRelations fileDependents = relationCrawler.getFileDependents(filePath);

                printRelations(writer, fileDependents.getMddRelations(), TYPE_MDD, maxDisplay);
                printRelations(writer, fileDependents.getJspRelations(), TYPE_JSP, maxDisplay);
                printRelations(writer, fileDependents.getJavaRelations(), TYPE_JAVA, maxDisplay);
                if (fileDependents.isEmpty()) {
                    if (!relationCrawler.wasCrawled()) {
                        // TODO make nicer, i.e. display something else while it crawls using some JS
                        // TODO: simply deactivating calling the crawler again would be a first step..
                        writer.println("No relations have been computed for this webapp.");
                        if (MakumbaJspConfiguration.getServletLocation(MakumbaServlet.RELATION_CRAWLER).equals(
                            Configuration.PROPERTY_NOT_SET)) {
                            DevelUtils.printErrorMessage(writer, "", "Manually triggering crawling is disabled");
                        } else {
                            writer.println("<br><a href=\"" + request.getContextPath()
                                    + MakumbaJspConfiguration.getServletLocation(MakumbaServlet.RELATION_CRAWLER)
                                    + "\">Crawl now</a> (this will take some time)");
                        }
                    } else {
                        writer.println("No relations found for this file!");
                    }
                }
            } catch (Throwable e) {
                writer.println("Could not crawl relations, error: " + e.getMessage());
                Logger.getLogger("").log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            writer.println("Could not crawl relations, file not in webapp root!");
        }
        writer.println("</div>");
    }

    private void printRelations(PrintWriter writer, Map<String, Vector<RelationOrigin>> map, String fileType,
            int maxDisplay) {
        if (map.size() > 0) {
            writer.println("<b>" + fileType + "</b><br>");
            int count = 0;
            for (String key : map.keySet()) {
                count++;
                if (maxDisplay != -1 && count > maxDisplay) {
                    writer.print("<a href=\"\" style=\"color:grey; font-size:smaller\">" + (map.size() - maxDisplay)
                            + " more ...</a><br>");
                    break;
                }
                Vector<RelationOrigin> occurrences = map.get(key);
                String path = getPath(key, fileType);
                String display = getDisplay(key, fileType);

                if (occurrences.size() > 0) {
                    RelationOrigin firstElement = occurrences.firstElement();
                    if (fileType.equals(TYPE_JSP)) { // only JSPs have line info
                        writer.println("<a href=\"" + path + "#" + firstElement.getStartLine()
                                + "\" style=\"font-size:smaller\">" + display + " #" + firstElement.getStartLine()
                                + "</a>");
                        for (int i = 1; i < occurrences.size(); i++) {
                            RelationOrigin origin = occurrences.elementAt(i);
                            writer.println("<a href=\"" + path + "#" + origin.getStartLine()
                                    + "\" style=\"font-size:smaller\">#" + origin.getStartLine() + "</a>");
                        }
                    } else {
                        writer.println("<a href=\"" + path + "\" style=\"font-size:smaller\">" + display + "</a> ("
                                + occurrences.size() + "x)");
                    }
                } else {
                    writer.println("<a href=\"" + path + "\" style=\"font-size:smaller\">" + display + "</a> ("
                            + occurrences.size() + "x)");
                }
                writer.println("<br>");
            }
        }
    }

    private String getPath(String fileName, String fileType) {
        if (fileType.equals(TYPE_JSP)) {
            return contextPath + "/" + fileName + jspSourceViewExtension;
        } else if (fileType.equals(TYPE_MDD)) {
            fileName = removeFilenamePrefixes(fileName);
            return contextPath + MakumbaJspConfiguration.getToolLocation(DeveloperTool.MDD_VIEWER) + "/"
                    + fileName.replaceAll(".mdd", "").replaceAll("/", ".");
        } else if (fileType.equals(TYPE_JAVA)) {
            fileName = removeFilenamePrefixes(fileName);
            return contextPath + MakumbaJspConfiguration.getToolLocation(DeveloperTool.JAVA_VIEWER) + "/" + fileName;
        }
        return fileName;
    }

    private String getDisplay(String fileName, String fileType) {
        if (fileType.equals("JSP")) {
            return fileName;
        } else if (fileType.equals(TYPE_MDD)) {
            fileName = removeFilenamePrefixes(fileName);
            return fileName.replaceAll(".mdd", "").replaceAll("/", ".");
        } else if (fileType.equals(TYPE_JAVA)) {
            fileName = removeFilenamePrefixes(fileName);
            return fileName.replaceAll(".java", "").replaceAll("/", ".");
        }
        return fileName;
    }

    private String removeFilenamePrefixes(String fileName) {
        if (fileName.startsWith("WEB-INF/")) {
            fileName = fileName.substring("WEB-INF/".length());
        }
        if (fileName.startsWith("classes/")) {
            fileName = fileName.substring("classes/".length());
        }
        if (fileName.startsWith("dataDefinitions/")) {
            fileName = fileName.substring("dataDefinitions/".length());
        }
        return fileName;
    }

    /**
     * Write the page header to the given writer.
     */
    protected void navigation(PrintWriter printWriter) throws IOException {
    }

    protected void printAdditionalViewerOptions(PrintWriter printWriter) throws IOException {
    }

    protected void printPageBeginAdditional(PrintWriter writer) throws IOException {
    }

    /** Prints a link to the page CVS/SVN for the file currently viewed. */
    protected String printVersionControlLink() {
        if (versionControlRepositoryURL != null) {
            String path = virtualPath;
            if (this instanceof javaViewer) {
                path = "WEB-INF/classes/" + path.replace('.', '/') + ".java";
            } else if (this instanceof mddViewer) {
                String additionalPath = "WEB-INF/classes/";
                if (realPath != null && realPath.contains("classes/dataDefinitions")) {
                    additionalPath += "dataDefinitions/";
                }
                path = additionalPath + path.replace('.', '/') + ".mdd";
            }
            return " (<a title=\"See this file in the version control repository\" href=\""
                    + versionControlRepositoryURL + path + "\">" + versionControlRepositoryLinkText + "</a>)";
        } else {
            return "";
        }
    }

    protected void writeAdditionalLinks(PrintWriter writer) {
    }

    /** Write the page footer to the given writer. */
    public void footer(PrintWriter printWriter) throws IOException {
        if (parseError != null) {
            printWriter.println("<hr><div class=\"parseError\"><a name=\"errors\"></a><pre>"
                    + StringEscapeUtils.escapeHtml(parseError.getMessage()) + "</pre></div>");
        }
        DevelUtils.printDeveloperSupportFooter(printWriter);
        printWriter.println("</div>");
    }

    public void printLine(PrintWriter printWriter, String s, String toPrint) throws IOException {
        String t = getLineTag(s);
        if (t != null) {
            printWriter.print("<a name=\"" + t + "\"></a>");
        }
        printWriter.print(toPrint);

        // not sure of this fix...was "<br>"
        printWriter.print("\n");
    }

    public String getLineTag(String s) {
        return null;
    }

    /**
     * Sets the amount of links to other files the viewer is trying to find. changing some of these parameters can
     * significantely speed up the viewing process.
     * 
     * @param searchJSPPages
     *            whether to search for .jsp files.
     * @param searchCompiledJSPClasses
     *            wheter to search for compiled jsp files, i.e. files with the extension _jsp.java
     * @param searchJavaClasses
     *            wheter to search for java source files.
     * @param searchMDD
     *            whether to search for Makumba Data Definitions, .mdd files (and Inlcuded Data Defitions, .idd).
     */
    public void setSearchLevels(boolean searchJSPPages, boolean searchCompiledJSPClasses, boolean searchJavaClasses,
            boolean searchMDD) {
        this.searchJSPPages = searchJSPPages;
        this.searchCompiledJSPClasses = searchCompiledJSPClasses;
        this.searchJavaClasses = searchJavaClasses;
        this.searchMDD = searchMDD;
    }

    /**
     * Processes one line of code, and adds links for
     * <ul>
     * <li>MDDs</li>
     * <li>JSP pages</li>
     * <li>Java Classes</li>
     * <li>from JSP pages generated Java classes</li>
     * </ul>
     * Subclasses that want to provide any additional formatting (syntax highlighting, etc) should extend this method,
     * apply their formatting and before/afterwards call this method. This method is rather time-consuming, and
     * subclasses interested in providing links just to a part of the above should use the <code>setSearchLevels</code>
     * method to specify for what types of files are searched for.
     * 
     * @param s
     *            the unformatted code line.
     * @return The formatted code line.
     */
    public String parseLine(String s) {
        Class<?> javaClass;
        String jspPage;
        String jspClass;

        StringBuffer source = new StringBuffer(s);
        StringBuffer result = new StringBuffer();

        Matcher matcher = patternUrl.matcher(s);
        while (matcher.find()) {
            String token = matcher.group();

            int indexOf = source.indexOf(token);
            int indexAfter = indexOf + token.length();

            result.append(source.substring(0, indexOf));

            if (token.indexOf("www.makumba.org") != -1) {
                result.append(formatMakumbaLink(token));
            } else if (token.indexOf("java.sun.com") != -1) {
                result.append(formatSunTaglibLink(token));
                // FIXME should not depend directly on MDDProvider but instead be provided by DataDefinitionProvider
            } else if (searchMDD && DataDefinitionProvider.findDataDefinition(token, "mdd") != null
                    || DataDefinitionProvider.findDataDefinition(token, "idd") != null) {
                result.append(formatMDDLink(token));
            } else if (searchJavaClasses && (javaClass = findClassSimple(token)) != null) {
                result.append(formatClassLink(javaClass.getName(), token, null));
            } else if (searchJavaClasses && (javaClass = findClass(token)) != null) {
                result.append(formatClassLink(javaClass, null, token));
            } else if (searchJSPPages && (jspPage = findPage(token)) != null) {
                result.append(formatJSPLink(jspPage, token, null));
            } else if (searchCompiledJSPClasses && (jspClass = findCompiledJSP(token)) != null) {
                result.append(formatClassLink(jspClass, token, null));
            } else {
                result.append(token);
            }
            source.delete(0, indexAfter);
        }
        return result.append(source).toString();
    }

    /**
     * @param jspPage
     * @param result
     * @param token
     */
    public String formatJSPLink(String jspPage, String token, Integer lineNumber) {
        StringBuffer result = new StringBuffer();
        result.append("<a href=\"" + jspPage);
        if (jspPage.endsWith("jsp")) {
            result.append(jspSourceViewExtension);
        }
        if (lineNumber != null) {
            result.append("#" + lineNumber);
        }
        result.append("\">").append(token).append("</a>");
        return result.toString();
    }

    /**
     * @param className
     * @param token
     * @return
     */
    public String formatClassLink(String qualifiedClassName, String className, Integer lineNumber) {
        if (lineNumber != null) {
            return "<a href=\"" + contextPath + MakumbaJspConfiguration.getToolLocation(DeveloperTool.JAVA_VIEWER)
                    + "/" + qualifiedClassName + "#" + lineNumber + "\">" + className + "</a>";
        } else {
            return "<a href=\"" + contextPath + MakumbaJspConfiguration.getToolLocation(DeveloperTool.JAVA_VIEWER)
                    + "/" + qualifiedClassName + "\">" + className + "</a>";
        }
    }

    /**
     * @param token
     * @return
     */
    public String formatMDDLink(String mddName) {
        return "<a class=\"classlink\" title=\"DataDefinition '" + mddName + "'\" href=\"" + contextPath
                + MakumbaJspConfiguration.getToolLocation(DeveloperTool.MDD_VIEWER) + "/" + mddName + "\">" + mddName
                + "</a>";
    }

    /**
     * @param result
     * @param token
     */
    public String formatMakumbaLink(String token) {
        return "<a href=\"http://www.makumba.org\" target=\"_blank\">" + token + "</a>";
    }

    /**
     * @param token
     * @return
     */
    public String formatSunTaglibLink(String token) {
        if (token.indexOf("java.sun.com/jstl/") != -1 || token.indexOf("http://java.sun.com/jsp/jstl/") != -1) {
            return "<a href=\"http://java.sun.com/products/jsp/jstl/1.1/docs/tlddocs/\" target=\"_blank\">" + token
                    + "</a>";
        } else {
            return "<a href=\"http://java.sun.com\" target=\"_blank\">" + token + "</a>";
        }
    }

    /**
     * @param s
     * @return
     */
    public StringTokenizer getLineTokenizer(String s) {
        return new StringTokenizer(s, "\"\' (){}[]<>,;-?#:", true);
    }

    /**
     * Finds a JSP page with the given name.
     * 
     * @param s
     *            The page to search for
     * @return The page found, <code>null</code> otherwise
     */
    public String findPage(String s) {
        if (s.startsWith("/")) { // absolute reference to file
            File file = new File(request.getSession().getServletContext().getRealPath(s));
            if (file.exists()) {
                return contextPath + s;
            } else {
                file = new File(s);
                if (file.exists()) {// full path name?
                    return s;
                }
            }
        }
        if (s.startsWith("/")) { // absolute reference to file, take two. rather a dirty hack
            // needed e.g. for files like /usr/local/cvsroot/karamba/public_html/general/survey/user/viewStatistics.jsp
            s = s.substring(s.lastIndexOf("/") + 1);
        }
        if (!s.startsWith("/") && realPath != null && !s.equals(".")) { // relative reference, except simple dot
            File file = new File(realPath.substring(0, realPath.lastIndexOf(File.separatorChar)) + File.separatorChar
                    + s.replace('/', File.separatorChar));
            if (file.exists()) {
                return s;
            }
        }
        return null;
    }

    /**
     * Searches for Java Classes with the given name FIXME: still needed?
     * 
     * @param s
     *            The class name to search for
     * @return The class, if found, <code>null</code> otherwise.
     */
    public Class<?> findClassSimple(String s) {
        Class<?> c = null;
        try {
            c = Class.forName(s);
        } catch (Throwable t) {
            return null;
        }
        if (org.makumba.commons.ClassResource.get(c.getName().replace('.', '/') + ".java") != null) {
            return c;
        }
        return null;
    }

    /** Find the path to the compiled JSP file from the name of a compiled JSP class */
    public String findCompiledJSP(String s) {
        if (jspClasspath != null && s.indexOf("_jsp") != -1) {
            try {
                String newClassName = s.substring(0, s.indexOf("_jsp") + 4);
                String filePath = jspClasspath + "/" + newClassName.replace('.', '/') + ".java";
                File file = new File(filePath);
                if (file.exists()) {
                    return newClassName;
                } else {
                    return null;
                }
            } catch (Throwable t) {
                return null;
            }
        } else {
            return null;
        }
    }

    /** Find the path to the compiled JSP file from the name of the original JSP page */
    public String findCompiledJSPClassName(String compiledJSPDirectory, String jspPageName) {
        if (jspPageName.indexOf(".jsp") == -1) {
            return null;
        }
        try {
            jspPageName = jspPageName.substring(0, jspPageName.indexOf(".jsp"));
            String completeClassFileName = compiledJSPDirectory + jspPageName + "_jsp.java";
            File file = new File(completeClassFileName);
            if (file.exists()) {
                // return only the part relative to the context working directory
                completeClassFileName = completeClassFileName.replace(jspClasspath, "").replace(".java", "");
                if (completeClassFileName.startsWith("/")) {
                    completeClassFileName = completeClassFileName.substring(1);
                }
                completeClassFileName.replace("/", ".");
                return completeClassFileName;
            } else {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Escapes a string to HTML-conform format.
     * 
     * @param s
     *            The string to escape
     * @return The given, with &amp;, &lt; and &gt; escaped.
     */
    public String htmlEscape(String s) {
        // TODO: use internal java class?!

        // we NEED to have this replacement first, otherwise we will replace the '&' from the &lt; and &gt;
        s = s.replaceAll("&", "&amp;");

        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        return s;
    }

    /**
     * @param token
     * @return
     */
    public Class<?> findClass(String className) {
        Class<?> c = null;
        String classNameTrial = importedClasses.get(className);
        if (classNameTrial != null) {
            try {
                c = Class.forName(classNameTrial);
                return c;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (c == null) {
            for (String importedPackage : importedPackages) {
                try {
                    classNameTrial = importedPackage + className;
                    c = Class.forName(classNameTrial);
                    return c;
                } catch (Throwable throwable) {
                    // we just continue with the next package
                }
            }
        }
        if (className.indexOf(".") != -1) {
            // try to split class name and method name
            classNameTrial = className.substring(0, className.lastIndexOf("."));
            // methodName = token.substring(methodBegin + 1);
            try {
                for (String importedPackage : importedPackages) {
                    classNameTrial = importedPackage + classNameTrial;
                    c = Class.forName(classNameTrial);
                    return c;
                }
            } catch (Throwable throwable) {
                // we just continue with the next package
            }
        }
        return null;
    }

    public String formatClassLink(Class<?> c, String methodName, String displayName) {
        if (c != null) {
            String s = "<a class=\"classLink\" href=\"";
            if (c.getName().startsWith("java")) {
                s += "http://java.sun.com/j2se/1.4.2/docs/api/" + c.getName().replaceAll("\\.", "/") + ".html";
                if (methodName != null) {
                    s += "#" + methodName + "()";
                }
            } else if (c.getName().startsWith("org.makumba")) {
                s += "http://www.makumba.org/api/" + c.getName().replaceAll("\\.", "/") + ".html";
                if (methodName != null) {
                    s += "#" + methodName + "()";
                }
            } else if (c.getName().startsWith("org.hibernate")) {
                s += "http://www.hibernate.org/hib_docs/v3/api/" + c.getName().replaceAll("\\.", "/") + ".html";
                if (methodName != null) {
                    s += "#" + methodName + "()";
                }
            } else {
                s += contextPath + MakumbaJspConfiguration.getToolLocation(DeveloperTool.JAVA_VIEWER) + "/"
                        + c.getName().replace('.', '/');
            }
            return s + "\" title=\"" + (methodName != null ? "Method in " : "") + c.getName() + "\">" + displayName
                    + "</a>";
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        testLinkDetection();
    }

    private static void testLinkDetection() {
        String p1 = "http://www.makumba.org/presentation";
        String p2 = "mak:object from=\"general.survey.Survey survey\" where=\"survey=$survey\">";
        String p3 = "/layout/header.jsp?title=Statistics for ";
        String p4 = "c:set var=\"viewStatsPage\" value=\"viewStatistics.jsp?survey=";

        String[] patterns = new String[] { p1, p2, p3, p4 };
        // patternLineNumbers=new String[] {w1,w2};
        System.out.println("pattern: " + patternUrl.pattern());
        for (String pattern : patterns) {
            System.out.println("\n!!!trying\n---" + pattern + " ---");
            Matcher m = patternUrl.matcher(pattern);

            while (m.find()) {
                System.out.print(m.group() + " - ");
            }
            System.out.println();
        }
    }

}
