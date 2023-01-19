package io.github.dearrudam;


import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MyMojoTest {
    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    /**
     * @throws Exception if any
     */
    @Test
    public void testWithoutDependency()
            throws Exception {

        File pom = new File("target/test-classes/project-to-test/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        Path baseTest = Files.createTempDirectory("");
        Files.copy(pom.toPath(),Path.of(baseTest.toAbsolutePath().toString(),"pom.xml"), StandardCopyOption.REPLACE_EXISTING);

        Mojo mojo = rule.lookupMojo("add-dependency", new File(baseTest.toFile(),"pom.xml"));

        assertNotNull(mojo);

        String expectedDependencyGroupId = "someGroupId";
        String expectedDependencyArtifactId = "someArtifactId";
        String expectedDependencyVersion = "1.0";

        Dependency expectedDependency = new Dependency();
        expectedDependency.setGroupId(expectedDependencyGroupId);
        expectedDependency.setArtifactId(expectedDependencyArtifactId);
        expectedDependency.setVersion(expectedDependencyVersion);

        rule.setVariableValueToObject(mojo,"dependency",
                String.format("%s:%s:%s",
                        expectedDependencyGroupId,
                        expectedDependencyArtifactId,
                        expectedDependencyVersion));

        rule.setVariableValueToObject(mojo,"scope", "test");

        rule.setVariableValueToObject(mojo,"project", rule.readMavenProject(baseTest.toFile()));

        mojo.execute();

        Model modifiedModel = rule.readMavenProject(baseTest.toFile()).getModel();

        assertTrue(modifiedModel.getDependencies()
                .stream()
                .filter(dependency-> Objects.equals(expectedDependency.getGroupId(), dependency.getGroupId()))
                .filter(dependency-> Objects.equals(expectedDependency.getArtifactId(), dependency.getArtifactId()))
                .filter(dependency-> Objects.equals(expectedDependency.getVersion(), dependency.getVersion()))
                .findFirst().isPresent());

    }
}

