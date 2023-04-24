/*
 * Copyright © 2023 Tobias Gierke (tobias.gierke@code-sourcery.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.maven.buildprofiler.extension;

import de.codesourcery.maven.buildprofiler.shared.ArtifactCoords;
import de.codesourcery.maven.buildprofiler.shared.Constants;
import org.apache.commons.lang3.Validate;
import de.codesourcery.maven.buildprofiler.shared.Utils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Named("track-build-times")
@Singleton
@Component(role = EventSpy.class, hint = "profiler", description = "Measure times taken by Maven.")
public class MyExtension extends AbstractEventSpy implements LogEnabled
{
    private static final Set<String> REPORTED_SYSTEM_PROPS = Set.of( "java.specification.version", "java.runtime.version", "user.name",
        "java.vm.vendor", "os.version", "java.runtime.name", "java.vendor.url", "java.vm.specification.version", "os.name", "java.vm.name",
        "java.vendor.version", "user.country", "sun.java.command", "jdk.debug", "sun.cpu.endian", "java.version", "os.arch", "java.specification.vendor",
        "java.vm.specification.name", "java.version.date", "java.home", "java.vm.compressedOopsMode", "java.vm.specification.vendor", "java.specification.name",
        "java.vm.info", "java.vendor", "java.vm.version", "java.class.version");

    private static final Set<String> REPORTED_ENV_VARS = Set.of("JAVA_HOME", "HOME", "JAVA_OPTS", "MAVEN_OPTS","USER","CLASSPATH","MAVEN_HOME");

    protected static long startupTimestamp = System.currentTimeMillis();
    protected static final ThreadLocal<Long> mojoStartNanos = new ThreadLocal<>();

    public static final String CONFIG_PROP_ENABLED = "buildTimeTrackingEnabled";
    public static final String CONFIG_PROP_PROJECT = "buildTimeTrackingProject";
    public static final String CONFIG_PROP_BRANCH = "buildTimeTrackingBranch";
    public static final String CONFIG_PROP_SERVER_URL = "buildTimeTrackingServerUrl";

    private final List<ExecutionRecord> records = new ArrayList<>();

    private Logger log;

    protected record ExecutionRecord(ArtifactCoords artifactBeingBuild,
                                     ArtifactCoords plugin,
                                     String phase, long startEpochMillis, long endEpochMillis)
     {
        protected ExecutionRecord
        {
            Validate.notNull(artifactBeingBuild, "artifactBeingBuild must not be null");
            Validate.notNull(plugin, "plugin must not be null");
            Validate.notBlank( phase, "phase must not be null or blank");
            Validate.isTrue( startEpochMillis <= endEpochMillis, "execution time must be positive");
        }
    }

    private final AtomicBoolean initialized = new AtomicBoolean( false );

    // timestamp relative to which all execution times will be recorded
    private final AtomicLong initialNanos = new AtomicLong();

    // configuration properties from pom.xml
    private volatile boolean extEnabled = true;
    private volatile String buildTimeTrackingServerUrl;

    // transient
    protected volatile String projectName;
    protected volatile String branchName;
    protected volatile String gitHash;
    protected final AtomicInteger concurrency = new AtomicInteger();
    protected final AtomicInteger maxConcurrency = new AtomicInteger();

    private Optional<String> getCurrentGitHash(File baseDir) {

        do {
            if ( ! baseDir.canRead() ) {
                break;
            }
            final File gitFolder = new File(baseDir, ".git");
            if ( gitFolder.exists() && gitFolder.isDirectory() ) {
                final File head = new File(gitFolder, "HEAD");
                if ( ! ( head.exists() && head.canRead() ) )
                {
                    log.error(".git/HEAD does not exist or is not readable ? " + gitFolder.getAbsolutePath());
                    break;
                }
                try
                {
                    final String path = Files.readString(head.toPath());
                    if ( path.startsWith("ref") )
                    {
                        // ref: refs/heads/develop
                        final File ref = new File( gitFolder, path.split("\\s")[1].trim() );
                        if ( !(ref.exists() && ref.canRead()) )
                        {
                            log.error( "ref does not exist or is not readable ? " + ref.getAbsolutePath() );
                            break;
                        }
                        return Optional.of( Files.readString( ref.toPath() ) );
                    }
                    // detached HEAD
                    return Optional.of( path );
                }
                catch (Exception e)
                {
                    log.error("Failed to read "+head.getAbsolutePath(), e);
                    return Optional.empty();
                }
            }
            baseDir = baseDir.getParentFile();
        } while ( baseDir != null );
        return Optional.empty();
    }

    private void init(MavenProject currentProject, MavenProject topLevelProject)
    {
        if ( initialized.compareAndSet( false,true ) )
        {
            initialNanos.set( System.nanoTime() );

            log.debug("Trying to find out current GIT hash");
            gitHash = getCurrentGitHash(currentProject.getBasedir()).orElse(null);
            log.debug("Got GIT hash " + gitHash);
            final Function<String,Optional<String>> properties = key ->
            {
                String value = currentProject.getProperties().getProperty( key, null );
                if ( isBlank( value ) )
                {
                    value = topLevelProject.getProperties().getProperty( key, null );
                }
                return isBlank( value ) ? Optional.empty() : Optional.of( value );
            };
            extEnabled = properties.apply( CONFIG_PROP_ENABLED ).map(Boolean::parseBoolean).orElse( false );
            if ( extEnabled )
            {
                projectName = properties.apply(CONFIG_PROP_PROJECT).orElseThrow( missingPropertyException(CONFIG_PROP_PROJECT) );
                branchName = properties.apply(CONFIG_PROP_BRANCH).orElseThrow( missingPropertyException(CONFIG_PROP_BRANCH) );

                buildTimeTrackingServerUrl =
                    properties.apply( CONFIG_PROP_SERVER_URL ).orElseThrow(missingPropertyException( CONFIG_PROP_SERVER_URL ));

                log.debug( "Tracking build execution times and sending them to "+buildTimeTrackingServerUrl );
            } else {
                log.debug( "NOT tracking build execution times.");
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Supplier<Error> missingPropertyException(String propertyKey ) {
        // need to throw JVM error as Maven swallows all regular exceptions
        // generated by an EventSpy implementation
        return () -> new Error( "pom.xml lacks configuration for property '" + propertyKey + "'" );
    }

    @Override
    public void onEvent(Object event)
    {
        if ( !extEnabled )
        {
            return;
        }
        if ( event instanceof DefaultMavenExecutionResult executionResult) {
            buildFinished(executionResult);
        }
        else if (event instanceof ExecutionEvent r)
        {
            final MavenSession session = r.getSession();
            if (session != null &&
                session.getTopLevelProject() != null &&
                session.getCurrentProject() != null &&
                ! initialized.get())
            {
                init(session.getTopLevelProject(), session.getCurrentProject());
            }

            final MojoExecution exec = r.getMojoExecution();
            String phase = "n/a";
            if (exec != null)
            {
                phase = exec.getLifecyclePhase();
            }
            switch (r.getType())
            {
                case MojoStarted -> {
                    if ( log.isDebugEnabled() ) {
                        log.debug("Mojo started.");
                    }
                    mojoStartNanos.set( System.nanoTime() - initialNanos.get() );
                    final int current = concurrency.incrementAndGet();
                    maxConcurrency.getAndUpdate(actual -> Math.max(actual, current));
                }
                case MojoSucceeded ->
                {
                    if ( log.isDebugEnabled() ) {
                        log.debug("Mojo succeeed.");
                    }
                    final long endNanos = System.nanoTime() - initialNanos.get();
                    concurrency.decrementAndGet();
                    final Artifact a = r.getProject().getArtifact();
                    synchronized (records)
                    {
                        final ArtifactCoords buildArtifact = new ArtifactCoords(a.getGroupId(), a.getArtifactId(), a.getVersion());
                        final Plugin p = exec.getPlugin();
                        final ArtifactCoords pluginArtifact = new ArtifactCoords(p.getGroupId(), p.getArtifactId(), p.getVersion());

                        // convert to epoch millis relative to start time
                        final long startMillis = startupTimestamp + ( mojoStartNanos.get() / 1_000_000 );
                        final long endMillis = startupTimestamp + ( endNanos / 1_000_000 );
                        records.add( new ExecutionRecord( buildArtifact, pluginArtifact, phase, startMillis, endMillis ) );
                    }
                }
            }
        }
    }

    private void buildFinished(MavenExecutionResult result)
    {
        if ( result.hasExceptions() ) {
            log.info("Build failed, not recording execution times.");
            return;
        }

        if ( records.isEmpty() ) {
            log.info("No phases executed, not recording execution times.");
            return;
        }

        final String json = getJSONRequest( records, this, System.currentTimeMillis() );
        if ( log.isDebugEnabled() ) {
            log.debug("JSON: " + json);
        }

        log.debug("Sending response to " + buildTimeTrackingServerUrl);
        try
        {
            sendPOST(json);
        }
        catch (IOException | InterruptedException e)
        {
            log.error("Failed to send POST request to " + buildTimeTrackingServerUrl+" ("+e.getMessage()+")", e);
        }
    }

    private void sendPOST(String body) throws IOException, InterruptedException
    {
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(buildTimeTrackingServerUrl))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if ( response.statusCode() != 200 ) {
            log.error("HTTP request to "+buildTimeTrackingServerUrl+" returned "+response.statusCode());
        }
    }

    private static Optional<String> hostName() {
        try
        {
            return Optional.ofNullable(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e)
        {
            return Optional.empty();
        }
    }

    private static InetAddress hostIP()
    {
        final byte[] address;
        try
        {
            address = InetAddress.getLocalHost().getAddress();
            return InetAddress.getByAddress( address );
        }
        catch( UnknownHostException e )
        {
            throw new RuntimeException( e );
        }
    }

    /**
     *
     * @param records
     * @param instance
     * @param timestampNow timestamp in millis since epoch
     * @return
     */
    public static String getJSONRequest(List<ExecutionRecord> records, MyExtension instance, long timestampNow)
    {
        final StringBuilder json = new StringBuilder();

        json.append( "{" );
        json.append( "\"jsonSyntaxVersion\" : " ).append( Constants.JSON_SYNTAX_VERSION ).append( ", " );
        json.append( "\"buildStartTime\" : " ).append( startupTimestamp ).append( ", " );
        final long buildTimeMillis = timestampNow - startupTimestamp;
        json.append( "\"buildDurationMillis\" : " ).append( buildTimeMillis ).append( ", " );
        String input1 = hostIP().getHostAddress();
        json.append( "\"hostIP\" : " ).append( Utils.jsonString( input1 ) ).append( ", " );
        hostName().ifPresent(x -> json.append( "\"hostName\" : " ).append( Utils.jsonString( x ) ).append( ", " ) );
        json.append( "\"maxConcurrency\" : " ).append( instance.maxConcurrency ).append( ", " );
        String input = System.getProperty("java.version");
        json.append( "\"jvmVersion\" : " ).append( Utils.jsonString( input ) ).append( ", " );
        json.append( "\"availableProcessors\" : " ).append( Runtime.getRuntime().availableProcessors() ).append( ", " );
        json.append( "\"projectName\" : " ).append( Utils.jsonString( instance.projectName ) ).append( ", " );
        json.append( "\"branchName\" : " ).append( Utils.jsonString( instance.branchName ) ).append( ", " );

        if ( instance.gitHash != null )
        {
            json.append("\"gitHash\" : ").append( Utils.jsonString( instance.gitHash ) ).append(", ");
        }
        // system properties
        json.append("\"systemProperties\" : { ");

        appendMapToJSON( new MapLike()
        {
            private final Properties props = System.getProperties();
            private final Iterator<String> it = props.stringPropertyNames().iterator();

            @Override public boolean hasNext() {return it.hasNext();}

            @Override
            public Map.Entry<String, String> nextEntry()
            {
                final String key = it.next();
                final String value = props.getProperty( key );
                return new Map.Entry<>()
                {
                    @Override public String getKey() {return key;}
                    @Override public String getValue() {return value;}
                    @Override public String setValue(String value) {throw new UnsupportedOperationException( "Method setValue not implemented" );}
                };
            }
        },REPORTED_SYSTEM_PROPS::contains, json  );
        json.append("}, ");

        // environment settings
        json.append("\"environment\" : { ");

        appendMapToJSON( new MapLike()
        {
            private final Iterator<Map.Entry<String, String>> it = System.getenv().entrySet().iterator();
            @Override public boolean hasNext() {return it.hasNext();}
            @Override public Map.Entry<String, String> nextEntry() {return it.next();}
        }, REPORTED_ENV_VARS::contains, json );

        json.append("}, ");
        // gather unique plugin/build artifact coordinates
        // and store them as a JSON array. That way we can
        // reference the array index instead of repeating
        // the same strings over and over.
        final Map<String, Integer> artifactIndexByCoords = new HashMap<>();
        final List<ArtifactCoords> coords = new ArrayList<>();
        final Iterator<Integer> intIt = new Iterator<>()
        {
            private int value;
            @Override public boolean hasNext() {return true;}
            @Override public Integer next() {return value++;}
        };
        records.stream().map(x -> List.of(x.artifactBeingBuild(), x.plugin())).flatMap(Collection::stream)
            .distinct().forEach(artifact ->
            {
                coords.add(artifact);
                artifactIndexByCoords.put(artifact.getAsString(), intIt.next());
            });

        json.append( "\"coords\"" ).append( " : [ " );
        for (Iterator<ArtifactCoords> iterator = coords.iterator(); iterator.hasNext(); )
        {
            final ArtifactCoords c = iterator.next();
            json.append("{");
            json.append("\"groupId\" : ").append(Utils.jsonString(c.groupId())).append(",");
            json.append("\"artifactId\" : ").append(Utils.jsonString(c.artifactId())).append(",");
            json.append("\"version\" : ").append(Utils.jsonString(c.version()));
            json.append("}");
            if ( iterator.hasNext() ) {
                json.append(",");
            }
        }
        json.append( "],");

        // records
        json.append( "\"records\"" ).append( " : [ " );

        for ( Iterator<ExecutionRecord>  it = records.iterator() ; it.hasNext() ;  )
        {
            final ExecutionRecord record = it.next();
            json.append( "{ " );

            int idx = artifactIndexByCoords.get(record.artifactBeingBuild().getAsString());
            json.append("\"artifactIdx\" : ").append(idx).append(", ");

            // --
            idx = artifactIndexByCoords.get(record.plugin().getAsString());
            json.append( "\"pluginIdx\" : " ).append(idx).append( ", " );

            // --
            json.append( "\"phase\" : " ).append( Utils.jsonString( record.phase() ) ).append( ", " );
            json.append( "\"startMillis\" : " ).append( record.startEpochMillis ).append( ", " );
            json.append( "\"endMillis\" : " ).append( record.endEpochMillis  );
            json.append( "}" ); // end record

            if ( it.hasNext() ) {
                json.append( ", " );
            }
        }

        json.append( "]" ); // end records
        json.append( "}" );
        return json.toString();
    }

    private interface MapLike {
        boolean hasNext();
        Map.Entry<String,String> nextEntry();
    }

    private static void appendMapToJSON(MapLike map, Predicate<String> filter, StringBuilder json)
    {
        boolean first = true;
        while( map.hasNext() )
        {
            final Map.Entry<String, String> entry = map.nextEntry();
            final String key = entry.getKey();
            if ( filter.test( key ) )
            {
                if ( ! first ) {
                    json.append( ", " );
                }
                first = false;
                final String value = entry.getValue();
                final String quotedKey = Utils.jsonString( key );
                final String quotedValue;
                quotedValue = value == null ? "null" : Utils.jsonString( value );
                json.append( quotedKey ).append( " : " ).append( quotedValue );
            }
        }
    }

    @Override
    public void enableLogging(Logger logger)
    {
        this.log = logger;
    }
}