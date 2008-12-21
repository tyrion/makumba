package org.makumba.providers;

import java.util.Vector;

import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;

public interface DataDefinitionProviderInterface {

    /**
     * Gets the data definition defined by the given type.
     */
    public DataDefinition getDataDefinition(String typeName);

    /**
     * Gets a virtual data definition
     * 
     * @param name the name of the virtual data definition
     * @return a virtual data definition, representing e.g. a query result
     */
    public DataDefinition getVirtualDataDefinition(String name);

    /**
     * makes a field definition from the indicated string
     * FIXME this is particular to mdds
     * 
     * @param name the name of the field
     * @param definition the definition string
     * @return a field definition built on a definition string
     */
    public FieldDefinition makeFieldDefinition(String name, String definition);

    /**
     * makes a field definition with the elementary type
     * 
     * @param name the name of the field
     * @param type the type of the field
     * @return a field definition generated by the name and the type of the field
     */
    public FieldDefinition makeFieldOfType(String name, String type);

    /**
     * makes a field definition identical with the given one, except for the name
     * 
     * @param name the name of the field
     * @param type the FieldDefinition used as model
     * @return a copy of the initial field definition with a different name
     */
    public FieldDefinition makeFieldOfType(String name, String type, String description);

    /**
     * makes a field definition with the elementary type
     * 
     * @param name the name of the field
     * @param type the elementary type of the field
     * @param description the description of the field
     * @return a field definition generated by the name, type and description of the field
     */
    public FieldDefinition makeFieldWithName(String name, FieldDefinition type);

    /**
     * makes a field definition identical with the given one, except for the name and the description
     * 
     * @param name the name of the field
     * @param type the FieldDefinition used as model
     * @param description the description of the field
     * @return a copy of the initial field definition with a different name and description
     */
    public FieldDefinition makeFieldWithName(String name, FieldDefinition type, String description);

    /**
     * gives a list of data definitions in a given location
     * 
     * @param location the location where the data definitions should be
     * @return a vector with references to the data definitions in the location
     */
    public Vector<String> getDataDefinitionsInLocation(String location);

    /**
     * gives a list of data definitions in the default locations of the data definition provider
     * 
     * @return a vector with references to the data definitions in the default locations of the data definition provider
     */
    public Vector<String> getDataDefinitionsInDefaultLocations();

    /**
     * gives a list of data definitions in the default locations of the data definition provider, ignoring those MDDs
     * that start with any of the strings in the ignoreList
     * 
     * @param ignoreList
     *            a list of prefixes for MDDs to be ignored
     * @return a vector with references to the data definitions in the default locations of the data definition provider
     */
    public Vector<String> getDataDefinitionsInDefaultLocations(String... ignoreList);

}