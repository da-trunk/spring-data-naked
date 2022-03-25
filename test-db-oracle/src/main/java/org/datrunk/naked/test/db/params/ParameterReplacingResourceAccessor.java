package org.datrunk.naked.test.db.params;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.testcontainers.shaded.org.apache.commons.lang.StringEscapeUtils;

import liquibase.Scope;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.InputStreamList;

/**
 * A {@link liquibase.resource.ClassLoaderResourceAccessor} which can replace parameters in the change logs.
 * 
 * TODO: right now, this is hard coded to replace only ${liquibase.username}. We should change this to replace all parameters with their
 * values from {@link liquibase.Liquibase#getChangeLogParameters}.
 * 
 * @author ansonator
 *
 */
public class ParameterReplacingResourceAccessor extends ClassLoaderResourceAccessor {
    private final ClassLoader classLoader;
    private final String user;

    public ParameterReplacingResourceAccessor(String user) {
        super(Thread.currentThread()
            .getContextClassLoader());
        classLoader = Thread.currentThread()
            .getContextClassLoader();
        this.user = user;
    }

    @Override
    public InputStreamList openStreams(String relativeTo, String streamPath) throws IOException {
        init();

        InputStreamList returnList = new InputStreamList();

        streamPath = getFinalPath(relativeTo, streamPath);

        Set<String> seenUrls = new HashSet<>();

        Enumeration<URL> resources = classLoader.getResources(streamPath);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();

            if (seenUrls.add(url.toExternalForm())) {
                try {
                    InputStream is =
                        new ReplacingInputStream3(url.openStream(), StringEscapeUtils.escapeJava("${}"), user);
                    returnList.add(url.toURI(), is);
                } catch (URISyntaxException e) {
                    Scope.getCurrentScope()
                        .getLog(getClass())
                        .severe(e.getMessage(), e);
                }
            }
        }

        return returnList;
    }
}
