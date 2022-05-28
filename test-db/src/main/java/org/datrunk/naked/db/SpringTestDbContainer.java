package org.datrunk.naked.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.datrunk.naked.db.jdbc.DataSourceWrapper;
import org.datrunk.naked.db.jdbc.ThrowingConsumer;
import org.datrunk.naked.test.container.SpringTestContainer;
import org.springframework.boot.jdbc.DataSourceBuilder;

/** Adds methods to {@link SpringTestContainer} that are useful for RDBMS containers. */
public interface SpringTestDbContainer extends SpringTestContainer {
  String getJdbcUrl();

  String getUsername();

  String getPassword();

  String getDriverClassName();

  Connection getSysConnection() throws SQLException;

  default Connection getConnection() throws SQLException {
    return getConnection(getUsername(), getPassword());
  }

  default Connection getConnection(String user, String password) throws SQLException {
    return DriverManager.getConnection(getJdbcUrl(), user, password);
  }

  default DataSourceWrapper getDataSourceWrapper() {
    return new DataSourceWrapper(getUsername(), getPassword(), getJdbcUrl());
  }

  default DataSource getDataSource() {
    return DataSourceBuilder.create()
        .url(getJdbcUrl())
        .driverClassName(getDriverClassName())
        .username(getUsername())
        .password(getPassword())
        .build();
  }

  @FunctionalInterface
  public interface LiquibaseCommand extends ThrowingConsumer<Liquibase> {
    @Override
    void accept(Liquibase liquibase) throws LiquibaseException;

    /**
     * Returns a composed {@code Consumer} that performs, in sequence, this operation followed by
     * the {@code after} operation. If performing either operation throws an exception, it is
     * relayed to the caller of the composed operation. If performing this operation throws an
     * exception, the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this operation followed by the
     *     {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default LiquibaseCommand andThen(LiquibaseCommand after) {
      Objects.requireNonNull(after);
      return (Liquibase liquibase) -> {
        accept(liquibase);
        after.accept(liquibase);
      };
    }
  }

  default void execute(LiquibaseCommand command, String changeLogFile, String user, String password)
      throws SQLException, LiquibaseException {
    Database database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(getConnection(user, password)));
    try (Liquibase liquibase =
        new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), database)) {
      command.accept(liquibase);
    } finally {
    }
  }

  default void updateAsSys(String changeLog) throws LiquibaseException, SQLException {
    update(changeLog, getSysConnection());
  }

  default void update(String changeLog) throws LiquibaseException, SQLException {
    update(changeLog, getConnection());
  }

  default void update(String changeLog, String user, String password)
      throws LiquibaseException, SQLException {
    update(changeLog, getConnection(user, password));
  }

  default void update(String changeLog, Connection connection)
      throws LiquibaseException, SQLException {
    final Contexts contexts = new Contexts("xe", "production");
    execute(liquibase -> liquibase.update(contexts), changeLog, connection);
  }

  default void execute(LiquibaseCommand command, String changeLogFile, Connection connection)
      throws SQLException, LiquibaseException {
    Database database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
    try (Liquibase liquibase =
        new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), database)) {
      command.accept(liquibase);
    } finally {
    }
  }

  default void rollback(String rollBackToTag) throws LiquibaseException, SQLException {
    rollback(rollBackToTag, getConnection());
  }

  default void rollback(String rollBackToTag, Connection connection)
      throws LiquibaseException, SQLException {
    final Contexts contexts = new Contexts("xe", "production");
    execute(
        liquibase -> liquibase.rollback(rollBackToTag, contexts),
        "schema-update-versioned.xml",
        connection);
  }

  default void rollback() throws LiquibaseException, SQLException {
    rollback("0");
  }
}
