///////////////////////////////
//  Makumba, Makumba tag library
//  Copyright (C) 2000-2008  http://www.makumba.org
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

package org.makumba.providers;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.makumba.ConfigurationError;
import org.makumba.commons.ClassResource;

/**
 * This class knows how to read Makumba configuration and is used internally by different classes that need specific
 * services. It can be seen as a service dispatcher in a way.
 * 
 * @author Manuel Gay
 * @author Rudolf Mayer
 * @version $Id$
 */
public class Configuration implements Serializable {

    private static final long serialVersionUID = 1L;

    static Logger logger = Logger.getLogger("org.makumba.config");

    public static final String PROPERTY_NOT_SET = "PROPERTY_NOT_SET";

    public static final String MAKUMBA_CONF = "Makumba.conf";

    private static final String MAKUMBA_CONF_DEFAULT = MAKUMBA_CONF + ".default";

    public static final String PLACEHOLDER_CONTEXT_PATH = "_CONTEXT_PATH_";

    private static final String PATH = "path";

    private static final String HOST = "host";

    private static final String DATA_SOURCE = "dataSource";

    public static final String KEY_CLIENT_SIDE_VALIDATION = "clientSideValidation";

    public static final String KEY_RELOAD_FORM_ON_ERROR = "reloadFormOnError";

    public static final String KEY_FORM_ANNOTATION = "formAnnotation";

    public static final String KEY_DEFAULT_DATABASE_LAYER = "defaultDatabaseLayer";

    public static final String KEY_DATADEFINITIONPROVIDER = "dataDefinitionProvider";

    public static final String KEY_QUERYFUNCTIONINLINER = "queryFunctionInliner";

    public static final String MDD_DATADEFINITIONPROVIDER = "mdd";

    public static final String RECORDINFO_DATADEFINITIONPROVIDER = "recordinfo";

    public static final String GENERATE_ENTITY_CLASSES = "generateEntityClasses";

    // calendar editor
    public static final String KEY_CALENDAR_EDITOR = "calendarEditor";

    public static final String KEY_CALENDAR_EDITOR_LINK = "calendarEditorLink";

    public static final String KEY_TOOLS_LOCATION = PATH;

    // source code repository links
    public static final String KEY_REPOSITORY_URL = "repositoryURL";

    public static final String KEY_REPOSITORY_LINK_TEXT = "repositoryLinkText";

    // error logging to the database
    public static final String KEY_DB_ERROR_LOG = "logErrors";

    public static final String KEY_POINTER_UID_STRATEGY_CLASS = "pointerUIDStrategyClass";

    // default dataSource
    private static ConfiguredDataSource defaultDataSource = null;

    // all the dataSource-s described in the config file
    private static Map<String, ConfiguredDataSource> configuredDataSources = new HashMap<String, ConfiguredDataSource>();

    // a cache for the host & path based resolution of data sources
    private static Map<String, ConfiguredDataSource> resolvedConfiguredDataSources = new HashMap<String, ConfiguredDataSource>();

    // the key of the current dataSource, host and path the webapp runs on
    private static String remoteDataSourceConfigurationPath = "";

    private static MakumbaINIConfiguration defaultConfig;

    private static MakumbaINIConfiguration applicationConfig;

    private static Object loadLock = new Object();

    private static ConfigurationDTO d;

    private static void populateConfigurationDTO() {
        d = new ConfigurationDTO();
        d.dataDefinitionProvider = applicationConfig.getProperty("providers", KEY_DATADEFINITIONPROVIDER);
        d.queryInliner = applicationConfig.getProperty("providers", KEY_QUERYFUNCTIONINLINER);
        d.pointerUIDStrategyClass = applicationConfig.getProperty("providers", KEY_POINTER_UID_STRATEGY_CLASS);
        d.generateEntityClasses = applicationConfig.getBooleanProperty("providers", GENERATE_ENTITY_CLASSES);
        d.defaultDatabaseLayer = applicationConfig.getProperty("dataSourceConfig", KEY_DEFAULT_DATABASE_LAYER);
        d.defaultCalendarEditor = applicationConfig.getBooleanProperty("inputStyleConfig", KEY_CALENDAR_EDITOR);
        d.calendarEditorLink = applicationConfig.getProperty("inputStyleConfig", KEY_CALENDAR_EDITOR_LINK);
        d.repositoryURL = applicationConfig.getProperty("makumbaToolConfig", KEY_REPOSITORY_URL);
        d.repositoryLinkText = applicationConfig.getProperty("makumbaToolConfig", KEY_REPOSITORY_LINK_TEXT);
        d.errorLog = applicationConfig.getBooleanProperty("makumbaToolConfig", KEY_DB_ERROR_LOG);

        d.makumbaToolsLocation = applicationConfig.getProperty("makumbaToolPaths", KEY_TOOLS_LOCATION);

        for (DeveloperTool t : DeveloperTool.values()) {
            d.developerToolsLocations.put(t, applicationConfig.getProperty("makumbaToolPaths", t.getKey()));
        }

        for (MakumbaServlet s : MakumbaServlet.values()) {
            d.servletLocations.put(s, applicationConfig.getProperty("makumbaToolPaths", s.getKey()));
        }

        d.javaViewerSyntaxStyles = applicationConfig.getPropertiesAsMap("javaViewerSyntaxStyles", defaultConfig);
        d.jspViewerSyntaxStyles = applicationConfig.getPropertiesAsMap("jspViewerSyntaxStyles", defaultConfig);
        d.jspViewerSyntaxStylesTags = applicationConfig.getPropertiesAsMap("jspViewerSyntaxStylesTags", defaultConfig);
        d.internalCodeGeneratorTemplates = defaultConfig.getPropertiesStartingWith("codeGeneratorTemplate:");
        d.applicationSpecificCodeGeneratorTemplates = applicationConfig.getPropertiesStartingWith("codeGeneratorTemplate:");
        d.logicPackages = applicationConfig.getPropertiesAsMap("businessLogicPackages");
        d.authorizationDefinitions = applicationConfig.getPropertiesAsMap("authorization");

    }

    /**
     * Sets a given property, for a specific section
     * 
     * @param section
     *            the name of the configuration section
     * @param key
     *            the key of the property
     * @param value
     *            the value of the property
     */
    public static void setPropery(String section, String key, String value) {
        applicationConfig.getSection(section).setProperty(key, value);
    }

    public static String getApplicationConfigurationSource() {
        return applicationConfig != null ? applicationConfig.getSource() : null;
    }

    static {
        try {
            // the internal default configuration
            URL path = org.makumba.commons.ClassResource.get(MAKUMBA_CONF_DEFAULT);
            logger.info("Loading internal default configuration from " + path);
            defaultConfig = new MakumbaINIConfiguration(path);

            // application-specific configuration
            URL url = org.makumba.commons.ClassResource.get(MAKUMBA_CONF);
            if (url != null) {
                logger.info("Loading application configuration from " + url);
                synchronized (loadLock) {
                    applicationConfig = new MakumbaINIConfiguration(url, defaultConfig);
                }

            } else { // if we did not find any configuration, we shout. we need an application configuration for the
                // dataSource config.
                logger.severe("No application configuration found!");
                throw new ConfigurationError(
                        "Could not find application configuration file Makumba.conf in WEB-INF/classes!");
            }

            populateConfigurationDTO();

            d.defaultReloadFormOnError = applicationConfig.getBooleanProperty("controllerConfig",
                KEY_RELOAD_FORM_ON_ERROR);
            // FIXME: check if the value in the file is ok, throw an exception otherwise
            d.defaultClientSideValidation = applicationConfig.getProperty("controllerConfig",
                KEY_CLIENT_SIDE_VALIDATION);
            // FIXME: check if the value in the file is ok, throw an exception otherwise
            d.defaultFormAnnotation = applicationConfig.getProperty("controllerConfig", KEY_FORM_ANNOTATION);

            buildConfiguredDataSources();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gives the data definition provider implementation to use
     * 
     * @return a String containing the class name of the data definition provider implementation
     */
    public static String getDataDefinitionProvider() {
        return d.dataDefinitionProvider;
    }

    /**
     * Gives the query inliner implementation to use
     * 
     * @return a String containing the type of query inliner implementation to use
     */
    public static String getQueryInliner() {
        return d.queryInliner;
    }

    /**
     * Whether or not to generate JPA entity classes
     */
    public static boolean getGenerateEntityClasses() {
        return d.generateEntityClasses;
    }

    public static String getPointerUIDStrategyClass() {
        return d.pointerUIDStrategyClass;
    }

    /**
     * Gives the default database layer to use
     * 
     * @return "makumba" or "hibernate"
     */
    public static String getDefaultDatabaseLayer() {
        synchronized (loadLock) {
            return d.defaultDatabaseLayer;
        }
    }

    public static String getClientSideValidationDefault() {
        return d.defaultClientSideValidation;
    }

    public static boolean getReloadFormOnErrorDefault() {
        return d.defaultReloadFormOnError;
    }

    public static String getDefaultFormAnnotation() {
        return d.defaultFormAnnotation;
    }

    public static boolean getCalendarEditorDefault() {
        return d.defaultCalendarEditor;
    }

    public static String getDefaultCalendarEditorLink(String contextPath) {
        return d.calendarEditorLink.replaceAll(PLACEHOLDER_CONTEXT_PATH, contextPath);
    }

    public static Map<String, String> getLogicPackages() {
        return d.logicPackages;
    }

    public static Map<String, String> getAuthorizationDefinitions() {
        return d.authorizationDefinitions;
    }

    public static String getMakumbaToolsLocation() {
        final String property = d.makumbaToolsLocation;
        return property.endsWith("/") ? property.substring(0, property.length() - 1) : property;
    }

    public static String getToolLocation(DeveloperTool t) {
        return getCompletePath(d.developerToolsLocations.get(t));
    }

    public static String getServletLocation(MakumbaServlet s) {
        return getCompletePath(d.servletLocations.get(s));
    }

    public static String getRepositoryURL() {
        return d.repositoryURL;
    }

    public static String getRepositoryLinkText() {
        return d.repositoryLinkText;
    }

    public static boolean getErrorLog() {
        return d.errorLog;
    }

    public static Map<String, String> getJavaViewerSyntaxStyles() {
        return d.javaViewerSyntaxStyles;
    }

    public static Map<String, String> getJspViewerSyntaxStyles() {
        return d.jspViewerSyntaxStyles;
    }

    public static Map<String, String> getJspViewerSyntaxStylesTags() {
        return d.jspViewerSyntaxStylesTags;
    }

    public static Map<String, Map<String, String>> getInternalCodeGeneratorTemplates() {
        return d.internalCodeGeneratorTemplates;
    }

    public static Map<String, Map<String, String>> getApplicationSpecificCodeGeneratorTemplates() {
        return d.applicationSpecificCodeGeneratorTemplates;
    }

    private static String getCompletePath(String path) {
        return StringUtils.isBlank(path) || path.equals(PROPERTY_NOT_SET) ? PROPERTY_NOT_SET
                : getMakumbaToolsLocation() + path;
    }

    /**
     * Returns the configuration for a specific dataSource. If more than one dataSource with the same name are found,
     * performs lookup.
     */
    public static Map<String, String> getDataSourceConfiguration(String dataSourceName) {
        ConfiguredDataSource conf = lookupDataSource(dataSourceName);
        return conf.getProperties();
    }

    /**
     * Gives the type of the data source (makumba or hibernate)
     */
    public static DataSourceType getDataSourceType(String dataSourceName) {
        return lookupDataSource(dataSourceName).getType();
    }

    /**
     * Gives the name of the default data source, according to following determination method:
     * <ol>
     * <li>If only one dataSource is configured, this one is used</li>
     * <li>If several dataSources of the same name are configured and contain lookup parameters (host, working
     * directory, ...), the one that matches the machine on which it runs is used</li>
     * <li>The defaultDataSource named in the dataSourceConfig section is used</li>
     * </ol>
     * 
     * @return the name of the dataSource to use by default
     */
    public static String getDefaultDataSourceName() {
        return getDefaultDataSource().getName();

    }

    /** the configuration properties of the default data source **/
    public static Map<String, String> getDefaultDataSourceConfiguration() {
        return getDefaultDataSource().getProperties();
    }

    /** builds the data sources for the configuration. **/
    private static void buildConfiguredDataSources() {
        @SuppressWarnings("unchecked")
        java.util.Set<String> sections = applicationConfig.getSections();
        for (String section : sections) {

            // expect something like
            // dataSource:<name> host:<host> path:<path> web-app:<web-app>

            if (section.startsWith("dataSource:")) {
                ConfiguredDataSource c = buildConfiguredDataSource(section);
                configuredDataSources.put(c.toString(), c);
            }

        }
    }

    /** builds a {@link ConfiguredDataSource} based on a dataSource section **/
    private static ConfiguredDataSource buildConfiguredDataSource(String section) throws ConfigurationError {
        String name = null, host = null, path = null;
        StringTokenizer st = new StringTokenizer(section, " ");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int n = token.indexOf(":");
            if (n > -1) {
                String tokenName = token.substring(0, n);
                String tokenValue = token.substring(n + 1);

                if (org.makumba.commons.StringUtils.equalsAny(tokenName, DATA_SOURCE, HOST, PATH)) {

                    if (StringUtils.isBlank(tokenValue)) {
                        throw new ConfigurationError("Property " + tokenName + " has no value");
                    }

                    if (tokenName.equals(DATA_SOURCE)) {
                        name = tokenValue;
                    } else if (tokenName.equals(HOST)) {
                        host = tokenValue;
                    } else if (tokenName.equals(PATH)) {
                        path = tokenValue;
                    }
                } else {
                    throw new ConfigurationError("Invalid property '" + token + "' in dataSource section [" + section
                            + "]. Correct syntax is [dataSource:<name> host:<host> path:<path>}]");
                }

            } else {
                throw new ConfigurationError("Invalid property '" + token + "' in dataSource section [" + section
                        + "]. Correct syntax is [dataSource:<name> host:<host> path:<path>}]");
            }

        }

        // figure out type of data source. if none is provided, we shout
        String type = applicationConfig.getProperty(section, "databaseLayer");
        if (type.equals(PROPERTY_NOT_SET)) {
            throw new ConfigurationError("Data source section [" + section
                    + "] misses the required property 'databaseLayer'.");
        }

        // populate with properties

        String[] globalDatabaseConfigurationProperties = { "foreignKeys", "defaultDataSource" };

        Map<String, String> globalProperties = applicationConfig.getPropertiesAsMap("dataSourceConfig");

        Map<String, String> dataSourceConfiguration = new Hashtable<String, String>();
        for (String globalProperty : globalDatabaseConfigurationProperties) {
            if (globalProperties.get(globalProperty) != null) {
                dataSourceConfiguration.put(globalProperty, globalProperties.get(globalProperty));
            }
        }
        try {
            dataSourceConfiguration.putAll(applicationConfig.getPropertiesAsMap(section));
        } catch (ConfigurationError ce) {
            throw new ConfigurationError("DataSource [" + section + "] is not configured in Makumba.conf");
        }

        ConfiguredDataSource c = new ConfiguredDataSource(host, name, path, DataSourceType.valueOf(type));
        c.setProperties(dataSourceConfiguration);
        return c;
    }

    /** the configuration of the default data source **/
    private static ConfiguredDataSource getDefaultDataSource() {

        if (defaultDataSource == null) {
            Map<String, String> globalProperties = applicationConfig.getPropertiesAsMap("dataSourceConfig");
            String defaultDataSourceName = globalProperties.get("defaultDataSource");

            if (defaultDataSourceName != null) {
                // we fetch the default one
                for (String c : configuredDataSources.keySet()) {
                    if (c.equals("dataSource:" + defaultDataSourceName)
                            || c.startsWith("dataSource:" + defaultDataSourceName + " ")) {
                        defaultDataSource = configuredDataSources.get(c);
                        return defaultDataSource;
                    }
                }

                // nothing found?
                throw new ConfigurationError("Default dataSource " + defaultDataSourceName
                        + " not found in Makumba.conf");
            }

            // first we check if there is maybe only one dataSource, in that case we take it as default
            int count = 0;
            String lastSection = "";
            HashMap<String, Boolean> toLookUp = new HashMap<String, Boolean>();
            for (Object sectionObject : applicationConfig.getSections()) {
                String section = (String) sectionObject;
                if (section.startsWith("dataSource:")) {
                    count++;
                    lastSection = section;

                    // we collect the sections we went through. if two sections start the same, we put them in a map to
                    // do a lookup
                    if (section.indexOf(" ") > -1 && toLookUp.get(section.substring(0, section.indexOf(" "))) != null) {
                        toLookUp.put(section.substring(0, section.indexOf(" ")), true);
                    } else if (section.indexOf(" ") > -1) {
                        toLookUp.put(section.substring(0, section.indexOf(" ")), false);
                    }
                }
            }
            if (count == 1) {
                defaultDataSource = buildConfiguredDataSource(lastSection);
                return defaultDataSource;
            } else if (count == 0) {
                throw new ConfigurationError("You must configure at least one dataSource for Makumba to work properly");
            }

            // now we treat the case when there are two or more dataSources that have the same name, but different host,
            // path properties
            // i.e. run a lookup and decide accordingly
            // but do this only if there are only dataSources that have the same name

            for (Entry<String, Boolean> entry : toLookUp.entrySet()) {
                if (entry.getValue()) {
                    ConfiguredDataSource c = lookupDataSource(entry.getKey().substring("dataSource:".length()));
                    if (c != null) {
                        defaultDataSource = c;
                    }
                }
            }

            // now we can't really tell which one to use by ourselves so we see if there is a default one
            if (defaultDataSourceName == null) {
                throw new ConfigurationError(
                        "Since there is more than one configured dataSource, Makumba needs to know which one to use. Please specify a defaultDataSource in section dataSourceConfig.");
            }

        }

        return defaultDataSource;

    }

    public static String getRemoteDataSourceConfigurationPath() {
        return remoteDataSourceConfigurationPath;
    }

    /**
     * Looks up the right {@link ConfiguredDataSource} based on host and path.<br>
     * Tries to match all configured data sources against the local version of<br>
     * dataSource:<dataSourceName> host:<hostName> path:<workingDirPath> or of<br>
     * dataSource:<dataSourceName> host:<hostName> path:<webappPath><br>
     * If no match is found, tries to retrieve dataSource:<dataSourceName>
     * 
     * @throws ConfigurationError
     *             if no match is found
     */
    private static ConfiguredDataSource lookupDataSource(String dataSourceName) {

        ConfiguredDataSource result = resolvedConfiguredDataSources.get(dataSourceName);
        if (result == null) {
            try {

                String host = InetAddress.getLocalHost().getCanonicalHostName();
                String path = System.getProperty("user.dir");
                java.net.URL u = ClassResource.get("/");
                String alternatePath = u != null ? u.toString() : null;
                if (alternatePath != null && alternatePath.startsWith("file:")) {
                    alternatePath = alternatePath.substring("file:".length());
                }

                String thisConfiguration1 = "dataSource:" + dataSourceName + " host:" + host + " path:" + path;
                String thisConfiguration2 = "dataSource:" + dataSourceName + " host:" + host + " path:" + alternatePath;

                remoteDataSourceConfigurationPath = thisConfiguration1;

                // we go over all the data sources and compare them to those we have
                String maxKey = "";

                for (String k : configuredDataSources.keySet()) {
                    if (thisConfiguration1.startsWith(k) && k.length() > maxKey.length()
                            && k.startsWith("dataSource:" + dataSourceName + " ")) {
                        maxKey = k;
                        result = configuredDataSources.get(k);
                    }
                }

                if (result == null) {
                    for (String k : configuredDataSources.keySet()) {
                        if (thisConfiguration2.startsWith(k) && k.length() > maxKey.length()
                                && k.startsWith("dataSource:" + dataSourceName + " ")) {
                            maxKey = k;
                            result = configuredDataSources.get(k);
                        }
                    }

                    if (result == null) {
                        // there was no dataSource:<name> path: ... found
                        // we fall back to the simple one
                        result = configuredDataSources.get("dataSource:" + dataSourceName);

                        if (result == null) {
                            throw new ConfigurationError("No DataSource " + dataSourceName
                                    + " configured in Makumba.conf");
                        }
                    }
                }

            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            logger.info("Resolved dataSource " + dataSourceName + " to " + result.toString());
            resolvedConfiguredDataSources.put(result.getName(), result);
        }
        return result;

    }

    public enum DataSourceType {
        makumba,
        hibernate;
    }

}
