package com.mtesazi.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.main.lazy-initialization=true",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "services.auth.base-url=lb://auth-service",
        "services.employee.base-url=lb://employee-service",
        "services.department.base-url=lb://department-service",
        "security.auth.enabled=false",
        "security.auth.jwt-secret=test-secret"
})
class GatewayServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
