package org.deephacks.confit.internal.jpa;

import org.deephacks.confit.internal.jpa.JpaUtils.Jpaprovider;
import org.deephacks.confit.test.FeatureTests;
import org.deephacks.confit.test.FeatureTestsBuilder.TestRound;
import org.deephacks.confit.test.cdi.CdiFeatureTestBuilder;
import org.deephacks.confit.test.cdi.CdiFeatureTestsRunner;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.util.List;

import static org.deephacks.confit.test.JUnitUtils.getMavenProjectChildFile;

@Singleton
@RunWith(CdiFeatureTestsRunner.class)
public class Jpa20BeanManagerCdiTests implements FeatureTests {

    @Override
    public List<TestRound> build() {
        return CdiFeatureTestBuilder.named(Jpa20BeanManagerCdiTests.class.getSimpleName()).build();
    }

    @Produces
    @Singleton
    public EntityManagerFactory produceEntityManagerFactory() {
        File scriptDir = getMavenProjectChildFile(Jpa20BeanManager.class,
                "src/main/resources/META-INF/");
        File targetDir = getMavenProjectChildFile(Jpa20BeanManager.class, "target");
        File jpaProperties = new File(targetDir, "jpa.properties");
        Database database = Database.create(Database.DERBY, scriptDir);
        Jpaprovider provider = Jpaprovider.create(JpaUtils.HIBERNATE, database);

        provider.write(jpaProperties);
        database.initalize();
        EntityManagerFactory emf = new EntityManagerProvider()
                .createEntityManagerFactory(jpaProperties);
        return emf;
    }
}
