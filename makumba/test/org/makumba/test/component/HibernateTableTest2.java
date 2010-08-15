package org.makumba.test.component;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.makumba.Text;
import org.makumba.db.hibernate.HibernateTransactionProvider;

public class HibernateTableTest2 extends TableTest {

    public HibernateTableTest2(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(HibernateTableTest2.class);
    }

    @Override
    public void setUp() {
        db = HibernateTransactionProvider.getInstance().getConnectionTo("testDatabaseHibernate");
    }

    @Override
    public void testQueryValidMdds() {
    }

    @Override
    protected void checkInternationalizedField(Text comment) {
    }

    @Override
    protected void checkFKSupport() {
    }

    @Override
    public void testForeignKeysWithLongMDDName() {
    }

    @Override
    protected void deleteAllEntries() {
    }

    @Override
    public void testRealAggregation() {
    }

    @Override
    public void testIntAggregation() {
    }

}
