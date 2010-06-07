package org.makumba.providers;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.makumba.ConfigurationError;
import org.makumba.DataDefinition;
import org.makumba.DataDefinitionParseError;
import org.makumba.FieldDefinition;
import org.makumba.commons.ClassResource;
import org.makumba.commons.NameResolver;
import org.makumba.commons.SingletonHolder;
import org.makumba.providers.bytecode.AbstractClassWriter;
import org.makumba.providers.bytecode.EntityClassGenerator;
import org.makumba.providers.bytecode.JavassistClassWriter;

/**
 * This class is a facade for creating different kinds of DataDefinitionProviders. Its constructor knows from a
 * Configuration (or in the future maybe through other means) which implementation to use, and provides this
 * implementation methods to its client, without revealing the implementation used.
 * 
 * @author Manuel Gay
 * @version $Id$
 */
public abstract class DataDefinitionProvider implements SingletonHolder {

    private static String[] dataDefinitionProviders = { Configuration.MDD_DATADEFINITIONPROVIDER,
            "org.makumba.providers.datadefinition.mdd.MDDProvider", Configuration.RECORDINFO_DATADEFINITIONPROVIDER,
            "org.makumba.providers.datadefinition.makumba.MakumbaDataDefinitionFactory" };

    static final Map<String, DataDefinitionProvider> providerInstances = new HashMap<String, DataDefinitionProvider>();

    /**
     * Puts the DataDefinition providers into a Map
     */
    static {
        for (int i = 0; i < dataDefinitionProviders.length; i += 2) {
            try {
                Method getInstance = Class.forName(dataDefinitionProviders[i + 1]).getDeclaredMethod("getInstance",
                    new Class<?>[] {});
                DataDefinitionProvider tp = (DataDefinitionProvider) getInstance.invoke(null, new Object[] {});
                providerInstances.put(dataDefinitionProviders[i], tp);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public DataDefinitionProvider() {

    }

    /**
     * Gives an instance of a {@link DataDefinitionProvider}.
     */
    public static DataDefinitionProvider getInstance() {
        if (providerInstances.get(Configuration.getDataDefinitionProvider()) == null) {
            throw new ConfigurationError("Unknown data definition provider '"
                    + Configuration.getDataDefinitionProvider() + "', eligible values are: "
                    + providerInstances.keySet());
        }
        return providerInstances.get(Configuration.getDataDefinitionProvider());
    }

    public void release() {
        providerInstances.clear();
    }

    public DataDefinition getDataDefinition(String typeName) {
        if (!classesGenerated && !isGenerating && Configuration.getGenerateEntityClasses()) {
            generateEntityClasses();
            classesGenerated = true;
        }

        return null;
    }

    public abstract DataDefinition getVirtualDataDefinition(String name);

    /**
     * makes a field definition from the indicated string
     * 
     * @param name
     *            the name of the field
     * @param definition
     *            the definition string, e.g. "ptr general.Person ;pointer to a person"
     * @return a field definition built on a definition string
     */
    public abstract FieldDefinition makeFieldDefinition(String name, String definition);

    /**
     * makes a field definition with the elementary type
     * 
     * @param name
     *            the name of the field
     * @param type
     *            the type of the field
     * @return a field definition generated by the name and the type of the field
     */
    public abstract FieldDefinition makeFieldOfType(String name, String type);

    /**
     * makes a field definition identical with the given one, except for the name
     * 
     * @param name
     *            the name of the field
     * @param type
     *            the FieldDefinition used as model
     * @return a copy of the initial field definition with a different name
     */
    public abstract FieldDefinition makeFieldOfType(String name, String type, String description);

    /**
     * makes a field definition with the elementary type
     * 
     * @param name
     *            the name of the field
     * @param type
     *            the elementary type of the field
     * @param description
     *            the description of the field
     * @return a field definition generated by the name, type and description of the field
     */
    public abstract FieldDefinition makeFieldWithName(String name, FieldDefinition type);

    /**
     * makes a field definition identical with the given one, except for the name and the description
     * 
     * @param name
     *            the name of the field
     * @param type
     *            the FieldDefinition used as model
     * @param description
     *            the description of the field
     * @return a copy of the initial field definition with a different name and description
     */
    public abstract FieldDefinition makeFieldWithName(String name, FieldDefinition type, String description);

    /**
     * gives a list of data definitions in a given location
     * 
     * @param location
     *            the location where the data definitions should be
     * @return a vector with references to the data definitions in the location
     */
    public Vector<String> getDataDefinitionsInLocation(String location) {
        return mddsInDirectory(location);
    }

    /**
     * gives a list of data definitions in the default locations of the data definition provider
     * 
     * @return a vector with references to the data definitions in the default locations of the data definition provider
     */
    public Vector<String> getDataDefinitionsInDefaultLocations() {
        return getDataDefinitionsInDefaultLocations((String[]) null);
    }

    /**
     * gives a list of data definitions in the default locations of the data definition provider, ignoring those MDDs
     * that start with any of the strings in the ignoreList
     * 
     * @param ignoreList
     *            a list of prefixes for MDDs to be ignored
     * @return a vector with references to the data definitions in the default locations of the data definition provider
     */
    public Vector<String> getDataDefinitionsInDefaultLocations(String... ignoreList) {
        Vector<String> mdds = mddsInDirectory("dataDefinitions");
        Vector<String> mddsInClasses = mddsInDirectory(""); // should direct to classes dir
        // take all MDDs that are new in classes, i.e. not already found in dataDefinitions
        for (String string : mddsInClasses) {
            if (!string.startsWith("dataDefinitions.")) {
                mdds.add(string);
            }
        }
        // check for MDDs in packages that should be removed
        if (ignoreList != null) {
            Vector<String> mddCopy = new Vector<String>(mdds);
            for (String s : ignoreList) {
                for (String mdd : mddCopy) {
                    if (mdd.startsWith(s)) {
                        mdds.remove(mdd);
                    }
                }
            }
        }
        return mdds;
    }

    /**
     * Discover mdds in a directory in classpath.
     * 
     * @return filenames as Vector of Strings.
     */
    private Vector<String> mddsInDirectory(String dirInClasspath) {
        Vector<String> mdds = new java.util.Vector<String>();
        try {
            java.net.URL u = org.makumba.commons.ClassResource.get(dirInClasspath);
            // we need to create the file path with this method. rather than u.getFile(), as that method would keep
            // e.g. %20 for spaces in the path, which fails on windows.
            if (u != null) {
                java.io.File dir = new File(u.toURI());
                fillMdds(dir.toString().length() + 1, dir, mdds);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return mdds;
    }

    private void fillMdds(int baselength, java.io.File dir, java.util.Vector<String> mdds) {
        if (dir.isDirectory()) {
            String[] list = dir.list();
            for (String element : list) {
                String s = element;
                if (s.endsWith(".mdd")) {
                    s = dir.toString() + java.io.File.separatorChar + s;
                    s = s.substring(baselength, s.length() - 4); // cut off the ".mdd"
                    s = s.replace(java.io.File.separatorChar, '.');
                    mdds.add(s);
                } else {
                    java.io.File f = new java.io.File(dir, s);
                    if (f.isDirectory()) {
                        fillMdds(baselength, f, mdds);
                    }
                }
            }
        }
    }

    /**
     * This method finds a field definition with the given name within the given data definition. The difference to a
     * simple {@link DataDefinition#getFieldDefinition(String)} is that the field name can be of the form
     * field.subfield.otherSubfield, over an arbitrary number of levels.
     */
    public static final FieldDefinition getFieldDefinition(DataDefinition dd, String fieldName,
            String lineWithDefinition) throws DataDefinitionParseError {
        DataDefinition checkedDataDef = dd;

        // treat sub-fields
        int indexOf = -1;
        while ((indexOf = fieldName.indexOf(".")) != -1) {
            // we have a sub-record-field
            String subFieldName = fieldName.substring(0, indexOf);
            fieldName = fieldName.substring(indexOf + 1);
            checkedDataDef = checkedDataDef.getFieldDefinition(subFieldName).getPointedType();
        }

        FieldDefinition fd = checkedDataDef.getFieldDefinition(fieldName);
        if (fd == null) {
            throw new DataDefinitionParseError(checkedDataDef.getName(), "Field '" + fieldName
                    + "' not defined in type " + dd.getName() + "!", lineWithDefinition);
        }
        return fd;
    }

    /***
     * Methods related to the refactoring from DataDefinition/FieldDefinition to Class/FieldMetadata.<br>
     * Will probably move somewhere else or stay here but under a different name.
     ***/

    /** have the entity classes already been generated? **/
    private static boolean classesGenerated = false;

    /** are we generating the classes at the moment ? **/
    private static boolean isGenerating = false;

    /**
     * Generates the classes for all Java entities based on the MDDs of the web-application
     */
    public void generateEntityClasses() {
        generateEntityClasses(getDataDefinitionsInDefaultLocations());
    }

    public void generateEntityClasses(Vector<String> dds) {
        // FIXME concurrency on this method

        isGenerating = true;

        Map<String, Vector<FieldDataDTO>> entities = new LinkedHashMap<String, Vector<FieldDataDTO>>();
        NameResolver nr = new NameResolver(); // FIXME find a way to pass the properties

        for (String type : dds) {
            Vector<FieldDataDTO> fields = getFieldDataDTOs(type);
            entities.put(getDataDefinition(type).getName(), fields);
            nr.initializeType(getDataDefinition(type));
        }

        try {
            AbstractClassWriter ac = new JavassistClassWriter();
            new EntityClassGenerator(entities, findClassesRootFolder("Makumba.conf"), ac, nr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        isGenerating = false;
    }

    // TODO this method is here because we do not want the EntityResolver to know about DataDefinition
    // it should move away once the NameResolver works with FieldGroups
    public void initializeNameResolver(NameResolver nr, String typeName) {
        nr.initializeType(getDataDefinition(typeName));
    }

    /**
     * Given a type, returns a vector of {@link FieldDataDTO} representing the fields of this type
     */
    public Vector<FieldDataDTO> getFieldDataDTOs(String type) {
        Vector<FieldDataDTO> fields = new Vector<FieldDataDTO>();

        for (String field : getDataDefinition(type).getFieldNames()) {
            FieldDefinition fd = getDataDefinition(type).getFieldDefinition(field);

            String relatedTypeName = fd.getIntegerType() == FieldDefinition._ptr
                    || fd.getIntegerType() == FieldDefinition._ptrOne || fd.getIntegerType() == FieldDefinition._ptrRel
                    || fd.isSetType() ? fd.getPointedType().getName() : null;
            String mappingTableName = fd.isSetType() ? fd.getSubtable().getName() : null;
            String setMappingColumnName = fd.getIntegerType() == FieldDefinition._set ? fd.getSubtable().getSetMemberFieldName()
                    : null;
            int charLength = fd.getIntegerType() == FieldDefinition._char ? fd.getWidth() : -1;

            FieldDataDTO f = new FieldDataDTO(field, fd.getIntegerType(), relatedTypeName, mappingTableName,
                    setMappingColumnName, charLength, fd.isFixed(), fd.isNotNull(), fd.isNotEmpty(), fd.isUnique());
            fields.add(f);
        }
        return fields;
    }

    /**
     * Finds the folder in which the classes should be generated based on a seed file
     */
    public static String findClassesRootFolder(String locatorSeed) {
        String rootFolder = "";
        try {
            rootFolder = new File(ClassResource.get(locatorSeed).getFile()).getParentFile().getCanonicalPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rootFolder;
    }

}