package org.makumba.devel;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.makumba.analyser.MakumbaJspAnalyzer;
import org.makumba.analyser.PageCache;
import org.makumba.analyser.engine.JspParseData;
import org.makumba.commons.FileUtils;
import org.makumba.commons.ReadableFormatter;
import org.makumba.devel.relations.JavaMDDParser;
import org.makumba.devel.relations.JspRelationsAnalyzer;
import org.makumba.devel.relations.RelationParseStatus;
import org.makumba.list.engine.ComposedQuery;

/**
 * Analyses all JSP pages of a webapp and writes the queries found in the pages to a file. Inspired from
 * {@link WebappJSPAnalysisCrawler}.
 * 
 * @author Rudolf Mayer
 * @version $Id: WebappPageAnalyser.java,v 1.1 May 17, 2008 3:35:12 PM rudi Exp $
 */
public class WebappJSPQueryCrawler {
    /** A file filter that accepts JSP files and directories. */
    private static final class JavaFileFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(".java") || pathname.isDirectory();
        }
    }

    public static Logger logger = Logger.getLogger("org.makumba.pageAnalyser");

    public static Hashtable<String, Throwable> JSPAnalysisErrors = new Hashtable<String, Throwable>();

    public static final FileFilter jspFilter = new WebappJSPAnalysisCrawler.JSPFileFilter();

    public static final FileFilter javaFilter = new JavaFileFilter();

    public static void main(String[] args) throws FileNotFoundException {
        CommandLine line = WebappJSPAnalysisCrawler.parseCrawlParams(args, WebappJSPAnalysisCrawler.class.getName());

        String webappRoot = line.getOptionValue("w");
        String[] skipPaths = line.getOptionValues("s");

        // this seems to be a bug in commons CLI
        if (skipPaths == null) {
            skipPaths = new String[] {};
        }
        String analysisOutputFile = line.getOptionValue("q", "queries.txt");

        System.out.println("Starting query crawler, config:");
        System.out.println("\twebappRoot: " + webappRoot);
        System.out.println("\tqueryOutputFile: " + analysisOutputFile);
        System.out.println("\tSkip: " + Arrays.toString(skipPaths));
        System.out.println("\t(from : " + Arrays.toString(args) + ")");
        Date beginDate = new Date();
        System.out.println("\nCrawling starts at " + beginDate + "\n");

        File f = new File(analysisOutputFile);
        PrintWriter pw = new PrintWriter(new FileOutputStream(f));

        String[] jspFiles = getFiles(webappRoot, skipPaths, jspFilter);
        for (String jspFile : jspFiles) {
            try {
                Object analysisResult;
                JspParseData jpd = JspParseData.getParseData(webappRoot, jspFile, JspRelationsAnalyzer.getInstance());
                analysisResult = jpd.getAnalysisResult(new RelationParseStatus());
                if (analysisResult != null && analysisResult instanceof PageCache) {
                    PageCache pageCache = (PageCache) analysisResult;
                    Map<Object, Object> cache = pageCache.retrieveCache(MakumbaJspAnalyzer.QUERY);
                    if (cache != null) {
                        for (Object key : cache.keySet()) {
                            ComposedQuery query = (ComposedQuery) cache.get(key);
                            String s = query.getComputedQuery();
                            pw.println(s);
                            // FIXME this won't work until we implement a method to print back to MQL analyzed query
                            // trees
                            // ArgumentReplacer ar = new ArgumentReplacer(s, false);
                            // Map<String, Object> d = new HashMap<String, Object>();
                            // int j = 1;
                            // for (Iterator<String> e = ar.getArgumentNames(); e.hasNext();)
                            // d.put(e.next(), "$" + (j++));
                            // pw.println(ar.replaceValues(d));
                        }
                    }
                }
            } catch (Throwable t1) {
                t1.printStackTrace();
            }
        }

        pw.println();
        String[] javaFiles = getFiles(webappRoot, skipPaths, javaFilter);
        for (String javaFile : javaFiles) {
            JavaMDDParser jqp = new JavaMDDParser(webappRoot + File.separator + javaFile);
            Vector<String> queries = jqp.getQueries();
            for (String query : queries) {
                pw.println(query);
            }
        }

        pw.flush();
        pw.close();

        System.out.println("\n\nCrawling finished, took: "
                + ReadableFormatter.readableAge(System.currentTimeMillis() - beginDate.getTime()));
        System.out.println("Wrote queries to file: " + f.getAbsolutePath());
    }

    private static String[] getFiles(String webappRoot, String[] skipPaths, FileFilter fileFilter) {
        ArrayList<String> allFilesInDirectory = FileUtils.getAllFilesInDirectory(webappRoot, skipPaths, fileFilter);
        Collections.sort(allFilesInDirectory);
        String[] files = allFilesInDirectory.toArray(new String[allFilesInDirectory.size()]);
        return files;
    }

}
