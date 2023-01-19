package io.github.dearrudam;


import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.io.xpp3.MavenXpp3WriterEx;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.model.io.xpp3.MavenToolchainsXpp3Writer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Goal which touches a timestamp file.
 */
@Mojo(name = "add-dependency", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class MyMojo
        extends AbstractMojo {

    @Component
    MavenProject project;

    @Parameter(property = "dependency", required = true)
    String dependency;

    @Parameter(property = "scope", required = false)
    String scope;

    @Parameter(property = "force", required = false)
    boolean force;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Objects.requireNonNull(this.dependency, "dependency property is required");

        Dependency dependencyToAdd =
                Optional.of(this.dependency)
                        .map(dependency -> dependency.split(":"))
                        .filter(params -> params.length == 3)
                        .map(params -> {
                            var dependency = new Dependency();
                            dependency.setGroupId(params[0]);
                            dependency.setArtifactId(params[1]);
                            dependency.setVersion(params[2]);
                            dependency.setScope(this.scope);
                            return dependency;
                        }).orElseThrow(() -> new IllegalArgumentException("invalid dependency property: " + this.dependency));

        getLog().info(
                String.format(
                        "adding dependency:%s scope:%s in default profile",
                        this.dependency, this.scope));

        var projectModel = this.project.getModel();

        getLog().info("looking for similar dependencies declaration...");
        List<Dependency> similarDependencies = projectModel.getDependencies()
                .stream()
                .filter(dependency -> Objects
                        .equals(dependencyToAdd.getGroupId(), dependency.getGroupId()))
                .filter(dependency -> Objects
                        .equals(dependencyToAdd.getArtifactId(), dependency.getArtifactId()))
                .filter(Predicate.not(dependency -> Objects
                        .equals(dependencyToAdd.getVersion(), dependency.getVersion())))
                .collect(Collectors.toList());

        if (this.force && !similarDependencies.isEmpty()) {
            getLog().info("updating dependencies declaration...");
            similarDependencies.stream()
                    .forEach(dependency -> {
                        dependency.setVersion(dependencyToAdd.getVersion());
                        dependency.setScope(dependencyToAdd.getScope());
                    });
        }

        if (similarDependencies.isEmpty())
            projectModel.getDependencies().add(dependencyToAdd);

        try (OutputStream stream = Files.newOutputStream(this.project.getFile().toPath(), StandardOpenOption.WRITE)) {
            new MavenXpp3WriterEx().write(stream, projectModel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
