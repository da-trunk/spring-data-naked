package org.datrunk.naked.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DbUtils {
  private static Logger log = LogManager.getLogger();

  @FunctionalInterface
  public interface SqlThrowingMapper<T, R> {
    R apply(T t) throws SQLException;

    default <V> SqlThrowingMapper<V, R> compose(SqlThrowingMapper<? super V, ? extends T> before) {
      Objects.requireNonNull(before);
      return (V v) -> apply(before.apply(v));
    }

    default <V> SqlThrowingMapper<T, V> andThen(SqlThrowingMapper<? super R, ? extends V> after) {
      Objects.requireNonNull(after);
      return (T t) -> after.apply(apply(t));
    }

    static <T> SqlThrowingMapper<T, T> identity() {
      return t -> t;
    }
  }

  @FunctionalInterface
  public interface SqlThrowingConsumer<T> {
    void accept(T t) throws SQLException;

    default SqlThrowingConsumer<T> andThen(SqlThrowingConsumer<? super T> after) {
      Objects.requireNonNull(after);
      return (T t) -> {
        accept(t);
        after.accept(t);
      };
    }
  }

  public static <T> List<T> select(
      DataSource dataSource, String sql, SqlThrowingMapper<ResultSet, T> newT) throws SQLException {
    List<T> result = new ArrayList<>();
    log.info(sql);
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(sql)) {
        while (rs.next()) {
          result.add(newT.apply(rs));
        }
      }
    }
    return result;
  }

  public static <T> void consume(
      DataSource dataSource, String sql, SqlThrowingConsumer<ResultSet> consumer)
      throws SQLException {
    log.info(sql);
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      try (ResultSet rs = statement.executeQuery(sql)) {
        while (rs.next()) {
          consumer.accept(rs);
        }
      }
    }
  }

  public static <T> void consumeQuery(
      DataSource dataSource,
      String sql,
      SqlThrowingConsumer<ResultSet> consumer,
      SqlThrowingConsumer<PreparedStatement> initPreparedStatement)
      throws SQLException {
    log.info(sql);
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      initPreparedStatement.accept(statement);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          consumer.accept(rs);
        }
      }
    }
  }

  /**
   * Maintains a stack of AutoCloseable instances. Each time an instance is pushed to the stack, a
   * new implementation of this interface is created. Calling {@link #close()} on this
   * implementation ensures all the pushed instances are closed.
   */
  interface CloseableStack extends Runnable, AutoCloseable {
    static CloseableStack create(AutoCloseable c) {
      return c::close;
    }

    @Override
    default void run() {
      try {
        close();
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    /**
     * Pushes the provided {@link AutoCloseable} to the top of the stack. It will be closed first.
     *
     * @param c
     * @return a new concrete implementation of CloseableStack
     */
    default CloseableStack push(AutoCloseable c) {
      return () -> {
        try (CloseableStack c1 = this) {
          c.close();
        }
      };
    }
  }

  /**
   * Adapted from <a
   * href="https://stackoverflow.com/questions/32209248/java-util-stream-with-resultset">java-util-stream-with-resultset</a>
   *
   * @param <T>
   * @param dataSource
   * @param sql
   * @param newT
   * @return Stream<T>
   * @throws SQLException
   */
  public static <T> Stream<T> select(
      DataSource dataSource,
      String sql,
      SqlThrowingMapper<ResultSet, T> newT,
      SqlThrowingConsumer<PreparedStatement> initPreparedStatement)
      throws SQLException {
    log.info(sql);
    CloseableStack closer = null;
    try {
      Connection connection = dataSource.getConnection();
      closer = CloseableStack.create(connection);
      PreparedStatement statement = connection.prepareStatement(sql);
      closer = closer.push(statement);
      initPreparedStatement.accept(statement);
      connection.setAutoCommit(false);
      statement.setFetchSize(5000);
      ResultSet resultSet = statement.executeQuery();
      closer = closer.push(resultSet);
      return StreamSupport.stream(
              new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
                @Override
                public boolean tryAdvance(Consumer<? super T> action) {
                  try {
                    if (!resultSet.next()) return false;
                    action.accept(newT.apply(resultSet));
                    return true;
                  } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                  }
                }
              },
              false)
          .onClose(closer);
    } catch (SQLException sqlEx) {
      if (closer != null)
        try {
          closer.close();
        } catch (Exception ex) {
          sqlEx.addSuppressed(ex);
        }
      throw sqlEx;
    }
  }
}
