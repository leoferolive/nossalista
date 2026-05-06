package br.com.leoferolive.nossalista;

import io.n1detector.core.Detector;
import io.n1detector.hibernate.HibernateInstaller;
import io.n1detector.hibernate.N1StatementInspector;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class N1DetectorTestConfig {

    // strict=false: silent when no scope is open (e.g. context startup queries)
    private static final Detector DETECTOR = new Detector(false);

    @Bean
    HibernatePropertiesCustomizer n1DetectorHibernateCustomizer() {
        return properties -> properties.put(
            HibernateInstaller.INSPECTOR_PROPERTY,
            new N1StatementInspector(DETECTOR));
    }
}
