package org.datrunk.naked.db;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.AbstractPostInsertGenerator;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.SequenceIdentityGenerator.NoCommentsInsert;
import org.hibernate.id.insert.AbstractReturningDelegate;
import org.hibernate.id.insert.IdentifierGeneratingInsert;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;

/**
 * @href https://developer-should-know.com/post/82479486933/how-to-use-oracle-before-insert-trigger-for-id
 * 
 *       <p>
 *       Usage: add
 *       <tt>@org.hibernate.annotations.GenericGenerator(name = "triggerAssigned", strategy = "org.datrunk.naked.db.TriggerAssignedIdentityGenerator")</tt>
 *       above your <tt>@Id</tt> annotation.
 * 
 *       TODO: can this be done with Hibernate sequence type "select"?
 */
public class TriggerAssignedIdentityGenerator extends AbstractPostInsertGenerator {

    @Override
    public InsertGeneratedIdentifierDelegate getInsertGeneratedIdentifierDelegate(PostInsertIdentityPersister persister, Dialect dialect,
        boolean isGetGeneratedKeysEnabled) throws HibernateException {
        return new Delegate(persister, dialect);
    }

    public static class Delegate extends AbstractReturningDelegate {

        private Dialect dialect;
        private String[] keyColumns;

        public Delegate(PostInsertIdentityPersister persister, Dialect dialect) {
            super(persister);
            this.dialect = dialect;
            this.keyColumns = getPersister().getRootTableKeyColumnNames();
            if (keyColumns.length > 1) {
                throw new HibernateException("trigger assigned identity generator cannot be used with multi-column keys");
            }
        }

        @Override
        public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert() {
            return new NoCommentsInsert(dialect);
        }

        @Override
        protected PreparedStatement prepare(String insertSQL, SharedSessionContractImplementor session) throws SQLException {
            return session.connection()
                .prepareStatement(insertSQL, keyColumns);
        }

        @Override
        protected Serializable executeAndExtract(PreparedStatement insert, SharedSessionContractImplementor session) throws SQLException {
            insert.executeUpdate();
            try (ResultSet generatedKeys = insert.getGeneratedKeys()) {
                return IdentifierGeneratorHelper.getGeneratedIdentity(generatedKeys, keyColumns[0], getPersister().getIdentifierType(),
                    dialect);
            }
        }
    }
}
