import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
println "Let's sum!"
def pomFile = new File(basedir, "pom.xml");

def pom = new MavenXpp3Reader().read(new FileInputStream(pomFile));

assert pom != null

def dependencies = pom.dependencies.stream()
        .filter { it -> it.groupId == "someGroupId" }
        .filter { it -> it.artifactId == "someArtifactId" }
        .toList()


assert 1 == dependencies.size();