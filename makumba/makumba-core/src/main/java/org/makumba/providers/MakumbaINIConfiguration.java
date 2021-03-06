package org.makumba.providers;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.reloading.InvariantReloadingStrategy;
import org.apache.commons.configuration.tree.DefaultExpressionEngine;
import org.apache.commons.lang.StringUtils;
import org.makumba.ConfigurationError;

/**
 * An extended version of the {@link HierarchicalINIConfiguration} suitable for makumba configuration
 * 
 * @author Manuel Bernhardt <manuel@makumba.org>
 * @version $Id$
 */
public class MakumbaINIConfiguration extends HierarchicalINIConfiguration {

    private static final long serialVersionUID = 1L;

    private MakumbaINIConfiguration defaultConfiguration;

    private URL u;

    public MakumbaINIConfiguration(URL u, MakumbaINIConfiguration defaultConfiguration) throws ConfigurationException {
        this(u);
        this.defaultConfiguration = defaultConfiguration;
    }

    public MakumbaINIConfiguration(URL u) throws ConfigurationException {
        super(u);
        this.u = u;

        // disable saving of the configuration on reloading
        setAutoSave(false);
        // disable automatic reloading of the property file
        setReloadingStrategy(new InvariantReloadingStrategy());

        // we need this or Apache CLI will thing that the spaces in section names means we address more than one node
        setListDelimiter('+');
        ((DefaultExpressionEngine) getExpressionEngine()).setPropertyDelimiter("+");

        load();
    }

    Map<String, Object> results = new HashMap<String, Object>();

    private void cachePut(String key, Runnable runnable) {
        if (!results.containsKey(key)) {
            runnable.run();
        }
    }

    public boolean getBooleanProperty(final String section, final String property) {
        final String key = "boolean " + section + " " + property;
        cachePut(key, new Runnable() {
            @Override
            public void run() {
                String k = getSection(section).getString(property);
                if (k == null) {
                    k = defaultConfiguration.getSection(section).getString(property);
                    if (k == null) {
                        results.put(key, false);
                        return;
                    }
                }
                results.put(key, Boolean.parseBoolean(k));
            }
        });
        return (Boolean) results.get(key);
    }

    /**
     * returns the properties of a section, if it exists
     * 
     * @throws ConfigurationError
     *             if the section does not exist
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getPropertiesAsMap(final String section) {
        final String key = "asMap " + section;
        cachePut(key, new Runnable() {
            @Override
            public void run() {

                Map<String, String> m = new HashMap<String, String>();
                SubnodeConfiguration s = getSection(section);
                if (s == null) {
                    throw new ConfigurationError("Section " + section + " does not exist in Makumba.conf");
                }
                java.util.Iterator<?> i = s.getKeys();
                while (i.hasNext()) {
                    String k = (String) i.next();
                    String originalK = k;
                    // the ini configuration escapes the property delimiters so we have to unescape them here
                    if (k.indexOf("..") > 0) {
                        k = StringUtils.replace(k, "..", ".");
                    }
                    m.put(k, getSection(section).getString(originalK));
                }
                results.put(key, m);
            }
        });
        return (Map<String, String>) results.get(key);
    }

    /**
     * Returns the properties of a section, enriched with the default properties
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getPropertiesAsMap(final String section, final MakumbaINIConfiguration defaultConfig) {
        final String key = "propertiesAsMapDefault" + section;
        cachePut(key, new Runnable() {
            @Override
            public void run() {

                Map<String, String> defaults = defaultConfig.getPropertiesAsMap(section);
                if (getSections().contains(section)) {
                    Map<String, String> application = getPropertiesAsMap(section);
                    final Set<String> keySet = application.keySet();
                    for (String string : keySet) {
                        if (application.get(string) != null) {
                            defaults.put(string, application.get(string));
                        }
                    }
                } else {
                    Configuration.logger.info("No application specific config found for '" + section
                            + "', using only internal defaults.");
                }
                results.put(key, defaults);
            }
        });
        return (Map<String, String>) results.get(key);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, String>> getPropertiesStartingWith(final String sectionPrefix) {

        final String key = "propertiesWithPrefix" + sectionPrefix;
        cachePut(key, new Runnable() {
            @Override
            public void run() {

                LinkedHashMap<String, Map<String, String>> res = new LinkedHashMap<String, Map<String, String>>();
                final Set<?> sectionNames = getSections();
                for (Object s : sectionNames) {
                    String section = (String) s;
                    if (section.startsWith(sectionPrefix)) {
                        res.put(section.substring(sectionPrefix.length()), getPropertiesAsMap(section));
                    }
                }
                results.put(key, res);
            }
        });

        return (Map<String, Map<String, String>>) results.get(key);

    }

    public String getProperty(final String section, final String property) {

        final String key = "propertiesWithPrefix" + section + " " + property;
        cachePut(key, new Runnable() {
            @Override
            public void run() {
                String s = getSection(section).getString(property);
                if (s == null) {
                    s = defaultConfiguration.getSection(section).getString(property);
                    if (s == null) {
                        results.put(key, Configuration.PROPERTY_NOT_SET);
                        return;
                    }
                }
                results.put(key, s);
            }
        });
        return (String) results.get(key);
    }

    public String getSource() {
        return u.getPath();
    }

}
