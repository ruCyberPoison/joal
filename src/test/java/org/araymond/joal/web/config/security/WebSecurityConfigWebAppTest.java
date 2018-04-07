package org.araymond.joal.web.config.security;

import org.araymond.joal.TestConstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                WebSecurityConfig.class,
                org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration.class,
                org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration.class,
                org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.HttpEncodingAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration.class,
                org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.websocket.WebSocketAutoConfiguration.class,
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.main.web-environment=true",
                "joal.ui.path.prefix=" + TestConstant.UI_PATH_PREFIX,
                "joal.ui.secret-token=" + TestConstant.UI_SECRET_TOKEN
        }
)
@Import({WebSecurityConfigWebAppTest.TestWebUiController.class})
public class WebSecurityConfigWebAppTest {

    @LocalServerPort
    private int port;

    @Inject
    private TestRestTemplate restTemplate;

    @RestController
    public static class TestWebUiController {
        @RequestMapping(path = TestConstant.UI_PATH_PREFIX + "/ui/", method = RequestMethod.GET)
        public String mockedCtrl() {
            return "";
        }
    }

    @TestConfiguration
    @EnableWebSocketMessageBroker
    public static class TestWebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {
        @Override
        public void registerStompEndpoints(final StompEndpointRegistry registry) {
            registry.addEndpoint(TestConstant.UI_PATH_PREFIX).setAllowedOrigins("*");
        }
    }

    @Test
    public void shouldForbidNonPrefixedUri() {
        assertThat(this.restTemplate.getForEntity("http://localhost:" + port + "", String.class).getStatusCodeValue()).isEqualTo(403);
        assertThat(this.restTemplate.getForEntity("http://localhost:" + port + "/ui/", String.class).getStatusCodeValue()).isEqualTo(403);
        assertThat(this.restTemplate.getForEntity("http://localhost:" + port + "/bla", String.class).getStatusCodeValue()).isEqualTo(403);
    }

    @Test
    public void shouldPermitOnPrefixedUriForWebsocketHandshakeEndpoint() throws InterruptedException, ExecutionException, TimeoutException {
        final WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        final StompSession stompSession = stompClient.connect("ws://localhost:" + port + "/" + TestConstant.UI_PATH_PREFIX, new StompSessionHandlerAdapter() {
        }).get(10, TimeUnit.SECONDS);

        assertThat(stompSession.isConnected()).isTrue();
    }

    @Test
    public void shouldPermitPrefixedUriOnWebUiEndpoint() {
        assertThat(this.restTemplate.getForEntity(
                "http://localhost:" + port + "/" + TestConstant.UI_PATH_PREFIX + "/ui/",
                String.class
        ).getStatusCodeValue()).isEqualTo(200);
    }

}

