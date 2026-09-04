package com.elicitsoftware.common.health;

/*-
 * ***LICENSE_START***
 * Elicit FHHS
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseHealthCheckTest {

    private DatabaseHealthCheck newCheck(DataSource dataSource) {
        DatabaseHealthCheck check = new DatabaseHealthCheck();
        check.dataSource = dataSource;
        return check;
    }

    @Test
    void call_connectionAndStatementSucceed_reportsUp() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        HealthCheckResponse response = newCheck(dataSource).call();

        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        verify(statement).execute("SELECT 1");
        verify(connection).close();
        verify(statement).close();
    }

    @Test
    void call_getConnectionThrows_reportsDownWithMessage() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

        HealthCheckResponse response = newCheck(dataSource).call();

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertTrue(response.getName().contains("connection refused"),
                "the health check name should surface the underlying SQLException message: " + response.getName());
    }

    @Test
    void call_statementExecuteThrows_reportsDown() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute("SELECT 1")).thenThrow(new SQLException("query failed"));

        HealthCheckResponse response = newCheck(dataSource).call();

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
    }

    @Test
    void call_alwaysClosesConnectionEvenOnFailure() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute("SELECT 1")).thenThrow(new SQLException("query failed"));

        newCheck(dataSource).call();

        verify(connection).close();
    }
}
