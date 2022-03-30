package org.datrunk.naked.test.db.oracle;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.utility.DockerImageName;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Extends {@link OracleContainer} with properties and methods specific to our Oracle database projects.
 * 
 * @author ansonator
 *
 */
public class OracleTestContainer extends OracleContainer implements AutoCloseable {
    protected static final Logger log = LogManager.getLogger();

    public OracleTestContainer(@Nonnull String imageName, @Nonnull String userName, @Nonnull String password) {
        this(imageName, userName, password, Network.newNetwork(), container -> {});
    }

    public OracleTestContainer(@Nonnull String imageName, @Nonnull String userName, @Nonnull String password, @Nonnull Network network,
                               Consumer<OracleContainer> extraConfig) {
        super(DockerImageName.parse(imageName)
            .asCompatibleSubstituteFor("gvenzl/oracle-xe"));
        DockerImageName.parse(imageName)
            .assertValid();
        this.withSharedMemorySize(FileUtils.ONE_GB)
            .withExposedPorts(1521)
            .withReuse(true)
            .withNetwork(network)
            .withNetworkAliases("db")
            .withEnv("ORACLE_SID", "oracle")
            .withEnv("ORACLE_PDB", "oracle")
            .withEnv("ORACLE_PWD", "password")
            .withEnv("ORACLE_ALLOW_REMOTE", "false")
            .withUsername(userName)
            .withPassword(password)
            .withDatabaseName("xe")
            .waitingFor(Wait.forLogMessage("Completed: ALTER DATABASE OPEN\\n", 1));
        extraConfig.accept(this);
        if (imageName.endsWith(":latest")) {
            this.withImagePullPolicy(PullPolicy.alwaysPull());
        }
        start();
    }

    @Override
    public void close() {
        stop();
    }

    public Connection getConnection() throws SQLException {
        return getConnection(getUsername(), getPassword());
    }

    public Connection getConnection(String user, String password) throws SQLException {
        Connection connection = DriverManager.getConnection(getJdbcUrl(), user, password);
        assert (connection != null);
        return connection;
    }

    public Connection getSysConnection() throws SQLException {
        return getConnection("SYS as SYSDBA", "password");
    }

    public void update() throws LiquibaseException, SQLException {
        update(getUsername(), getPassword());
    }

    public void update(String user, String password) throws LiquibaseException, SQLException {
        final Contexts contexts = new Contexts("xe", "production");
        execute(liquibase -> liquibase.update(contexts), "schema-update-versioned.xml", user, password);
    }

    @FunctionalInterface
    public interface LiquibaseCommand extends ThrowingConsumer<Liquibase> {
        @Override
        void accept(Liquibase liquibase) throws LiquibaseException;

        /**
         * Returns a composed {@code Consumer} that performs, in sequence, this operation followed by the {@code after} operation. If
         * performing either operation throws an exception, it is relayed to the caller of the composed operation. If performing this
         * operation throws an exception, the {@code after} operation will not be performed.
         *
         * @param after the operation to perform after this operation
         * @return a composed {@code Consumer} that performs in sequence this operation followed by the {@code after} operation
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

    public void execute(LiquibaseCommand command, String changeLogFile, String user, String password)
        throws SQLException, LiquibaseException {
        Database database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(getConnection(user, password)));
        try (Liquibase liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), database)) {
            command.accept(liquibase);
        } finally {}
    }

    public void rollback(String rollBackToTag) throws LiquibaseException, SQLException {
        rollback(getUsername(), getPassword(), rollBackToTag);
    }

    public void rollback(String user, String password, String rollBackToTag) throws LiquibaseException, SQLException {
        final Contexts contexts = new Contexts("xe", "production");
        execute(liquibase -> liquibase.rollback(rollBackToTag, contexts), "schema-update-versioned.xml", user, password);
    }

    /**
     * Drop and recreate the provided user. If the user is different from the one this container was originally created with, the root
     * classpath must include a change set with name "schema-drop-create-${user}.xml".
     * 
     * @param user
     * @throws SQLException
     * @throws LiquibaseException
     */
    public void create(String user) throws SQLException, LiquibaseException {
        try (Connection connection = getSysConnection()) {
            String changeSet = user.equals(getUsername()) ? "schema-drop-create.xml" : String.format("schema-drop-create-%s.xml", user);
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            if (user.equals("cm_disposable")) {
                changeSet = "schema-drop-create-cm_disposable.xml";
            }
            try (Liquibase liquibase = new Liquibase(changeSet, new ClassLoaderResourceAccessor(), database)) {
                Contexts contexts = new Contexts("xe");
                liquibase.update(contexts);
            }
        }
    }

    /**
     * Build a fresh database. After it is built, tag "0" is applied. This mimics the process we used when creating the docker
     * image that we use in tests.
     * 
     * @throws SQLException
     * @throws LiquibaseException
     */
    public void installVersioned() throws SQLException, LiquibaseException {
        create(getUsername()); // drop and recreate the user
        final Contexts contexts = new Contexts("xe", "dev");
        execute(liquibase -> liquibase.update(contexts), "base.xml", getUsername(), getPassword());
        update(getUsername(), getPassword());
        log.trace("applying tag");
        execute(liquibase -> liquibase.tag("0"), "update.xml", getUsername(), getPassword());
    }

    public void rollback() throws LiquibaseException, SQLException {
        rollback("0");
    }
}
