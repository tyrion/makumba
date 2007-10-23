package org.makumba.providers;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.makumba.DBError;
import org.makumba.Transaction;
import org.makumba.commons.Configuration;

/**
 * This class is a facade for creating different kinds of TransactionProviders. Its constructor knows from a
 * Configuration (or in the future maybe through other means) which implementation to use, and provides this
 * implementation methods to its client, without revealing the implementation used.

 * @author Manuel Gay
 * @version $Id: TransactionProvider.java,v 1.1 28.09.2007 15:49:55 Manuel Exp $
 */
public class TransactionProvider implements TransactionProviderInterface {
    
    private TransactionProviderInterface transactionProviderImplementation;

    
    public TransactionProvider() {
        Configuration config = new Configuration();
        try {
            this.transactionProviderImplementation = (TransactionProviderInterface) Class.forName(config.getDefaultTransactionProviderClass()).newInstance();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public TransactionProvider(Configuration config) {
        try {
            this.transactionProviderImplementation = (TransactionProviderInterface) Class.forName(config.getTransactionProviderClass()).newInstance();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private Vector<Transaction> connections = new Vector<Transaction>();

    public Transaction getConnectionTo(String name) {
        Transaction t = transactionProviderImplementation.getConnectionTo(name);
        connections.add(t);
        return t;
    }

    public String getDefaultDataSourceName() {
        return transactionProviderImplementation.getDefaultDataSourceName();
    }


    public String getDatabaseProperty(String name, String propName) {
        return transactionProviderImplementation.getDatabaseProperty(name, propName);
    }

    public void _copy(String sourceDB, String destinationDB, String[] typeNames, boolean ignoreDbsv) {
        transactionProviderImplementation._copy(sourceDB, destinationDB, typeNames, ignoreDbsv);
    }

    public void _delete(String whereDB, String provenienceDB, String[] typeNames, boolean ignoreDbsv) {
        transactionProviderImplementation._delete(whereDB, provenienceDB, typeNames, ignoreDbsv);
    }

    public Object getHibernateSessionFactory(String name) {
        return transactionProviderImplementation.getHibernateSessionFactory(name);
    }

    public String getDataSourceName(String lookupFile) {
        return transactionProviderImplementation.getDataSourceName(lookupFile);
    }

    public boolean supportsUTF8() {
        return transactionProviderImplementation.supportsUTF8();
    }
    
    private void close() {
        Iterator<Transaction> i = connections.iterator();
        
        while(i.hasNext()) {
            Transaction t = i.next();
            try {
                t.commit();
                t.close();
                java.util.logging.Logger.getLogger("org.makumba." + "db").severe("Transaction not closed");
            } catch(DBError e) {
                // connection was already closed
            }
        }
    }
    
    protected synchronized void finalize() {
        close();
    }
}
