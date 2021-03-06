package org.makumba.forms.responder;

import java.util.Dictionary;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.makumba.CompositeValidationException;
import org.makumba.FieldValueDiff;
import org.makumba.LogicException;
import org.makumba.Pointer;
import org.makumba.commons.DbConnectionProvider;
import org.makumba.commons.attributes.RequestAttributes;
import org.makumba.controller.Logic;
import org.makumba.forms.tags.FormOperationType;

/**
 * this class helps to differentiate between the different types of forms
 * 
 * @author Cristian Bogdan
 * @author Manuel Bernhardt <manuel@makumba.org>
 */
public abstract class ResponderOperation implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public abstract FormOperationType getOperationType();

    /** respond to the given request, with the data from the given responder, read using the given multiple form suffix */
    public abstract Object respondTo(HttpServletRequest req, Responder resp, String suffix, String parentSuffix)
            throws LogicException;

    /** check the validity of the given responder data, return not-null if there is a problem */
    public abstract String verify(Responder resp);

    /** gets the transaction provider **/
    public DbConnectionProvider getConnectionProvider(HttpServletRequest req, Object controller) {
        return RequestAttributes.getConnectionProvider(req);
    }

    /** gets the http data, rolls back the whole transaction in case of validation errors */
    protected Dictionary<String, Object> getCheckedHttpData(HttpServletRequest req, Responder resp, String suffix) {
        Dictionary<String, Object> data = null;
        try {
            data = resp.getHttpData(req, suffix);
        } catch (CompositeValidationException cve) {
            // we had a validation exception while running the form
            // thus we need to roll back the operations of any parent forms
            getConnectionProvider(req, resp.getController()).getConnectionTo(resp.getDatabase()).rollback();
            throw cve;
        }
        return data;
    }

    public final static ResponderOperation newOp = new ResponderOperation() {
        private static final long serialVersionUID = 1L;

        @Override
        public FormOperationType getOperationType() {
            return FormOperationType.NEW;
        }

        @Override
        public Object respondTo(HttpServletRequest req, Responder resp, String suffix, String parentSuffix)
                throws LogicException {
            String handlerName;
            if (resp.getHandler() != null) {
                handlerName = resp.getHandler();
            } else {
                handlerName = "on_new" + Logic.upperCase(resp.getNewType());
            }
            String afterHandlerName;
            if (resp.getAfterHandler() != null) {
                afterHandlerName = resp.getAfterHandler();
            } else {
                afterHandlerName = "after_new" + Logic.upperCase(resp.getNewType());
            }
            return Logic.doNew(resp.getController(), handlerName, afterHandlerName, resp.getNewType(),
                getCheckedHttpData(req, resp, suffix), new RequestAttributes(resp.getController(), req,
                        resp.getDatabase()), resp.getDatabase(), getConnectionProvider(req, resp.getController()));
        }

        @Override
        public String verify(Responder resp) {
            return null;
        }

    };

    public static final ResponderOperation editOp = new ResponderOperation() {
        private static final long serialVersionUID = 1L;

        @Override
        public FormOperationType getOperationType() {
            return FormOperationType.EDIT;
        }

        @Override
        public Object respondTo(HttpServletRequest req, Responder resp, String suffix, String parentSuffix)
                throws LogicException {
            String handlerName;
            if (resp.getHandler() != null) {
                handlerName = resp.getHandler();
            } else {
                handlerName = "on_edit" + Logic.upperCase(resp.getBasePointerType());
            }
            String afterHandlerName;
            if (resp.getAfterHandler() != null) {
                afterHandlerName = resp.getAfterHandler();
            } else {
                afterHandlerName = "after_edit" + Logic.upperCase(resp.getBasePointerType());
            }

            String recordChangesIn = resp.getRecordChangesIn();
            Pointer basePointer = resp.getHttpBasePointer(req, suffix);
            Dictionary<String, Object> httpData = getCheckedHttpData(req, resp, suffix);
            List<FieldValueDiff> diff = Logic.doEdit(resp.getController(), handlerName, afterHandlerName,
                resp.getBasePointerType(), basePointer, httpData, new RequestAttributes(resp.getController(), req,
                        resp.getDatabase()), resp.getDatabase(), getConnectionProvider(req, resp.getController()),
                recordChangesIn);
            if (StringUtils.isNotBlank(recordChangesIn)) {
                req.setAttribute(recordChangesIn, diff);
            }
            return basePointer;
        }

        @Override
        public String verify(Responder resp) {
            return null;
        }
    };

    public final static ResponderOperation addToNewOp = new ResponderOperation() {
        private static final long serialVersionUID = 1L;

        @Override
        public FormOperationType getOperationType() {
            return FormOperationType.ADD;
        }

        @Override
        public Object respondTo(HttpServletRequest req, Responder resp, String suffix, String parentSuffix)
                throws LogicException {
            // get result we got from the new form
            Object resultFromNew = req.getAttribute(ResponderFactory.resultNamePrefix + parentSuffix);

            // if we got a null response from the new form (possibly from a logic exception thrown by the
            // programmer)
            if (resultFromNew == org.makumba.Pointer.Null) {
                return org.makumba.Pointer.Null; // we return null here too
            }

            String handlerName;
            if (resp.getHandler() != null) {
                handlerName = resp.getHandler();
            } else {
                handlerName = "on_add" + Logic.upperCase(resp.getNewType() + "->" + resp.getAddField());
            }
            String afterHandlerName;
            if (resp.getAfterHandler() != null) {
                afterHandlerName = resp.getAfterHandler();
            } else {
                afterHandlerName = "after_add" + Logic.upperCase(resp.getNewType() + "->" + resp.getAddField());
            }

            Dictionary<String, Object> data = getCheckedHttpData(req, resp, suffix);

            // otherwise, we add to the new object
            return Logic.doAdd(resp.getController(), handlerName, afterHandlerName, resp.getNewType() + "->"
                    + resp.getAddField(), (Pointer) resultFromNew, data, new RequestAttributes(resp.getController(),
                    req, resp.getDatabase()), resp.getDatabase(), getConnectionProvider(req, resp.getController()));
        }

        @Override
        public String verify(Responder resp) {
            return null;
        }
    };

    public final static ResponderOperation addOp = new ResponderOperation() {
        private static final long serialVersionUID = 1L;

        @Override
        public FormOperationType getOperationType() {
            return FormOperationType.ADD;
        }

        @Override
        public Object respondTo(HttpServletRequest req, Responder resp, String suffix, String parentSuffix)
                throws LogicException {
            String handlerName;
            if (resp.getHandler() != null) {
                handlerName = resp.getHandler();
            } else {
                handlerName = "on_add" + Logic.upperCase(resp.getBasePointerType() + "->" + resp.getAddField());
            }
            String afterHandlerName;
            if (resp.getAfterHandler() != null) {
                afterHandlerName = resp.getAfterHandler();
            } else {
                afterHandlerName = "after_add" + Logic.upperCase(resp.getBasePointerType() + "->" + resp.getAddField());
            }
            return Logic.doAdd(resp.getController(), handlerName, afterHandlerName, resp.getBasePointerType() + "->"
                    + resp.getAddField(), resp.getHttpBasePointer(req, suffix), getCheckedHttpData(req, resp, suffix),
                new RequestAttributes(resp.getController(), req, resp.getDatabase()), resp.getDatabase(),
                getConnectionProvider(req, resp.getController()));
        }

        @Override
        public String verify(Responder resp) {
            return null;
        }
    };

    public final static ResponderOperation simepleOp = new ResponderOperation() {

        private static final long serialVersionUID = 1L;

        @Override
        public FormOperationType getOperationType() {
            return FormOperationType.SIMPLE;
        }

        @Override
        public Object respondTo(HttpServletRequest req, Responder resp, String suffix, String parentSuffix)
                throws LogicException {
            return Logic.doOp(resp.getController(), resp.getHandler(), getCheckedHttpData(req, resp, suffix),
                new RequestAttributes(resp.getController(), req, resp.getDatabase()), resp.getDatabase(),
                getConnectionProvider(req, resp.getController()));
        }

        @Override
        public String verify(Responder resp) {
            return null;
        }
    };

    public final static ResponderOperation deleteLinkOp = new ResponderOperation() {
        private static final long serialVersionUID = 1L;

        @Override
        public FormOperationType getOperationType() {
            return FormOperationType.DELETE;
        }

        @Override
        public Object respondTo(HttpServletRequest req, Responder resp, String suffix, String parentSuffix)
                throws LogicException {
            return Logic.doDelete(resp.getController(), resp.getBasePointerType(),
                resp.getHttpBasePointer(req, suffix), new RequestAttributes(resp.getController(), req,
                        resp.getDatabase()), resp.getDatabase(), getConnectionProvider(req, resp.getController()));
        }

        @Override
        public String verify(Responder resp) {
            return null;
        }
    };

    public final static ResponderOperation deleteFormOp = new ResponderOperation() {

        private static final long serialVersionUID = 1L;

        @Override
        public FormOperationType getOperationType() {
            return FormOperationType.DELETE;
        }

        @Override
        public Object respondTo(HttpServletRequest req, Responder resp, String suffix, String parentSuffix)
                throws LogicException {
            return Logic.doDelete(resp.getController(), resp.getBasePointerType(),
                resp.getHttpBasePointer(req, suffix), new RequestAttributes(resp.getController(), req,
                        resp.getDatabase()), resp.getDatabase(), getConnectionProvider(req, resp.getController()));
        }

        @Override
        public String verify(Responder resp) {
            return null;
        }
    };
}
