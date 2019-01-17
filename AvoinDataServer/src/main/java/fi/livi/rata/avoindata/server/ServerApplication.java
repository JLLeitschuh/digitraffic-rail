package fi.livi.rata.avoindata.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.TimeZone;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import fi.livi.rata.avoindata.common.xray.ElasticUDPEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import fi.livi.rata.avoindata.common.dao.CustomGeneralRepositoryImpl;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;

@Configuration
@EnableAutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackages = {"fi.livi.rata.avoindata.server", "fi.livi.rata.avoindata.common"})
@EntityScan(basePackages = "fi.livi.rata.avoindata.common.domain")
@EnableJpaRepositories(basePackages = "fi.livi.rata.avoindata.common.dao", repositoryBaseClass = CustomGeneralRepositoryImpl.class)
@EnableScheduling
@EnableCaching
public class ServerApplication {
    private static Logger log = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH","true");

        TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));

        SpringApplication application = createApplication();

        application.run(args);
    }

    private static SpringApplication createApplication() {
        SpringApplication application = new SpringApplication(ServerApplication.class);
        Properties properties = new Properties();

        properties.put("myHostname", getHostname());

        application.setDefaultProperties(properties);
        return application;
    }

    private static String getHostname() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName();
        } catch (UnknownHostException ex) {
            return "unknown";
        }
    }

    @Bean
    public Filter TracingFilter() {
        return new AWSXRayServletFilter("avoindataserver");
    }

    @PostConstruct
    public void setEmitter() {
        AWSXRay.getGlobalRecorder().setEmitter(new ElasticUDPEmitter());
    }
}