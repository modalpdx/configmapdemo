package us.modal.configmapdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;


// The PropertySource annotation needs to make configmapdemo.properties optional for
// local development/testing (read: not running in a container in a Kubernetes cluster)
// and to cover situations where mounting the file to /etc/config failed during deployment.
//
// ALTERNATIVE: in application.properties, add this file to a spring.config.import property:
//
//    spring.config.import=optional:file:/etc/config/configmapdemo.properties	
// 
// Note the "optional:file:" prefix. This will allow the app to run even if the file isn't there.

@SpringBootApplication
@PropertySource(value="file:/etc/config/configmapdemo.properties", ignoreResourceNotFound = true)
public class ConfigmapdemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfigmapdemoApplication.class, args);
	}

}
