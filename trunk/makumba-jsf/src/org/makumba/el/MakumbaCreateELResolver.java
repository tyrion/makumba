package org.makumba.el;

import java.beans.FeatureDescriptor;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.makumba.DataDefinition;
import org.makumba.FieldDefinition;
import org.makumba.NoSuchFieldException;
import org.makumba.jsf.ObjectComponent;

/**
 * {@link ELResolver} managing the creation of new makumba objects. It does so by:
 * <ul>
 * <li>if the base is null, check if there is a mak:object that declared itself as creator for the label given by the
 * property</li>
 * <li>if a creation mak:object is found, retrieve its CreateDataHolder which contains the base object label and type</li>
 * <li>for each new path element (property), check if it is valid</li>
 * <li>disallow setting .id</li>
 * <li></li>
 * </ul>
 * 
 * @author manu
 */
public class MakumbaCreateELResolver extends ELResolver {
    static final Logger log = java.util.logging.Logger.getLogger("org.makumba.el");

    private static ThreadLocal<Boolean> guard = new ThreadLocal<Boolean>() {

        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        // as per reference
        if (context == null) {
            throw new NullPointerException();
        }

        Object val = basicGetValue(context, base, property);

        if (base == null && val != null && val instanceof CreateExpressionPathPlaceholder) {
            return val;
        } else if (base != null && val != null && val instanceof CreateExpressionPathPlaceholder) {
            CreateExpressionPathPlaceholder parent = (CreateExpressionPathPlaceholder) val;
            log.finest("Getting value of create expression " + parent.toString());
            DataDefinition dd = parent.getType();
            String field = (String) property;
            FieldDefinition fd = dd.getFieldDefinition(field);

            CreateExpressionPathPlaceholder p = new CreateExpressionPathPlaceholder(parent, field);

            // is this a pointer?
            if (fd.isPointer()) {
                log.fine("Returning value of expression " + parent.getExpressionPath() + "." + field
                        + " as new expression " + p.toString());
                p.setType(fd.getPointedType());
                p.setPointer(true);
            } else {
                log.fine("Returning value of expression " + parent.getExpressionPath() + "." + field + " as null");
                return null;
            }

            return p;
        }

        return null;
    }

    /**
     * Does the basic value resolution:
     * <ul>
     * <li>if it is null, compute the base and set it to a placeholder</li>
     * <li>if a base is set and a property is passed, checks if the property is valid</li>
     * <li>if resolution was successful, set the flag in the ELContext</li>
     * </ul>
     * 
     * @return either null or a placeholder for further computation
     */
    private Object basicGetValue(ELContext context, Object base, Object property) {
        if (base == null && property != null && !guard.get()) {
            guard.set(true);
            base = computeBase(context, property);
            if (base != null) {
                context.setPropertyResolved(true);
                log.fine("Resolved base of creation expression " + base.toString());
            }
            guard.set(false);
            return base;
        }

        if (base != null && base instanceof CreateExpressionPathPlaceholder) {

            if (property != null) {

                CreateExpressionPathPlaceholder parent = (CreateExpressionPathPlaceholder) base;
                DataDefinition dd = parent.getType();
                String field = (String) property;

                if (!parent.isPointer()) {
                    // can't go further here
                    throw new NoSuchFieldException(parent.getType(), parent.getLabel() + "." + property);
                }

                FieldDefinition fd = dd.getFieldDefinition(field);

                // check expression validity
                if (fd == null) {
                    throw new NoSuchFieldException(dd, field);
                }

                if (fd.isSetType()) {
                    // TODO better exception type?
                    throw new RuntimeException(
                            "Field "
                                    + fd.getName()
                                    + " of type "
                                    + dd.getName()
                                    + " is a field type, and cannot be used directly to create a new value. Use a new <mak:object> tag for this purpose.");
                }

                context.setPropertyResolved(true);
                return parent;

            } else {
                throw new RuntimeException("Unexpected: property is null");
            }
        }

        return null;
    }

    private Object computeBase(ELContext context, Object property) {
        UIComponent c = FacesContext.getCurrentInstance().getApplication().evaluateExpressionGet(
            FacesContext.getCurrentInstance(), "#{component}", UIComponent.class);

        ObjectComponent object = ObjectComponent.findParentObject(c);
        if (object != null && object.isCreateObject()) {

            // see if this object knows something about the label we're trying to create
            if (object.getLabelTypes().containsKey(property)) {
                // we're in
                return new CreateExpressionPathPlaceholder(object.getLabelTypes().get(property), (String) property);
            }
        }
        return null;
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {

        // as per reference
        if (context == null) {
            throw new NullPointerException();
        }

        Object val = basicGetValue(context, base, property);

        if (val != null && val instanceof CreateExpressionPathPlaceholder) {
            CreateExpressionPathPlaceholder parent = (CreateExpressionPathPlaceholder) val;
            DataDefinition dd = parent.getType();
            String field = (String) property;
            FieldDefinition fd = dd.getFieldDefinition(field);

            return fd.getJavaType();
        }

        return Object.class;

    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {

        // as per reference
        if (context == null) {
            throw new NullPointerException();
        }

        if (base != null && base instanceof CreateExpressionPathPlaceholder) {
            // TODO check if the property is fixed
            // and the path to it goes thru fixed not null pointers?
            CreateExpressionPathPlaceholder p = (CreateExpressionPathPlaceholder) base;

            System.out.println(p.getType().getName() + " " + p.getExpressionPath() + " <<<<<<<<< " + value);

        }

    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        // TODO Auto-generated method stub
        return Object.class;
    }

}
