package com.mtesazi.discoveryserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.main.lazy-initialization=true"
})
class DiscoveryServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
