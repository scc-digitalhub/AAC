/*******************************************************************************
 * Copyright 2015-2019 Smart Community Lab, FBK
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.repository;

import it.smartcommunitylab.aac.SystemKeys;
import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import org.springframework.jdbc.datasource.ConnectionHandle;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;

/**
 * @author raman
 *
 */
public class IsolationSupportHibernateJpaDialect extends HibernateJpaDialect {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    @Override
    public Object beginTransaction(EntityManager entityManager, TransactionDefinition definition)
        throws PersistenceException, SQLException, TransactionException {
        boolean readOnly = definition.isReadOnly();
        ConnectionHandle handle = this.getJdbcConnection(entityManager, readOnly);
        if (handle == null) {
            throw new TransactionSystemException("invalid connection handle");
        }
        Connection connection = handle.getConnection();
        Integer originalIsolation = DataSourceUtils.prepareConnectionForTransaction(connection, definition);

        entityManager.getTransaction().begin();

        // keep connection state per transaction, not per thread: nested REQUIRES_NEW must not clobber it
        Object transactionData = prepareTransaction(entityManager, readOnly, definition.getName());
        return new IsolationTransactionData(transactionData, connection, originalIsolation);
    }

    @Override
    public void cleanupTransaction(Object transactionData) {
        IsolationTransactionData data = (IsolationTransactionData) transactionData;
        try {
            super.cleanupTransaction(data.delegate);
        } finally {
            DataSourceUtils.resetConnectionAfterTransaction(data.connection, data.originalIsolation, true);
        }
    }

    private static class IsolationTransactionData {

        private final Object delegate;
        private final Connection connection;
        private final Integer originalIsolation;

        IsolationTransactionData(Object delegate, Connection connection, Integer originalIsolation) {
            this.delegate = delegate;
            this.connection = connection;
            this.originalIsolation = originalIsolation;
        }
    }
    
}
