package io.vertx.example.unit.test;

import com.codahale.metrics.health.HealthCheck;
import io.vertx.core.Vertx;
import io.vertx.example.web.proxy.RedisStarted;
import io.vertx.example.web.proxy.healthcheck.RedisReporter;
import io.vertx.example.web.proxy.healthcheck.Reporter;
import io.vertx.example.web.proxy.locator.ServiceDescriptor;
import io.vertx.example.web.proxy.locator.RedisServiceLocator;
import io.vertx.example.web.proxy.locator.ServiceRegistry;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import redis.clients.jedis.Jedis;

import java.net.Inet4Address;
import java.util.Optional;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class RedisServiceLocatorTest {
    //SERVICES
    public static final String SERVICE_A_OPEN = "serviceA";
    public static final String SERVICE_B_BLOCKED = "serviceB";
    //PRODUCTS
    public static final String PROD3568_BLOCKED = "prod3568";
    public static final String PROD7340_OPED = "prod7340";
    public static final String PROD8643_OPEN = "prod8643";
    public static final int PORT = 8080;
    public static final String REST = "REST";

    private static Jedis client;
    private static Vertx vertx;
    private static RedisReporter reporter;
    private static ServiceRegistry serviceRegistry;

    @BeforeClass
    public static void setUp(TestContext context) throws Exception {
        vertx = Vertx.vertx();
        client = new Jedis();
        vertx.deployVerticle(new RedisStarted(client),context.asyncAssertSuccess());
        reporter = new RedisReporter(client, 100);
        serviceRegistry = new ServiceRegistry();

    }

    @Test
    public void testRepositoryGetServices() throws Exception {
        serviceRegistry.register(ServiceDescriptor.create(SERVICE_A_OPEN, PORT));
        serviceRegistry.register(ServiceDescriptor.create(SERVICE_B_BLOCKED, PORT));
        //set services health checks
        Reporter.setUpHealthCheck(vertx,REST,serviceRegistry,reporter);
        //service locator
        RedisServiceLocator locator = new RedisServiceLocator(client, REST);
        assertEquals(locator.getDomain(), REST);

        Optional<String> locatorService = locator.getService("/" + SERVICE_A_OPEN + "/" + PROD7340_OPED);
        assertTrue(locatorService.isPresent());
        String hostAddress = Inet4Address.getLocalHost().getHostAddress();
        assertEquals(locatorService.get(), hostAddress +":"+ PORT);

        locatorService = locator.getService("/" + SERVICE_B_BLOCKED + "/" + PROD8643_OPEN);
        assertTrue(locatorService.isPresent());
        assertEquals(locatorService.get(), hostAddress +":"+ PORT);

    }

    @AfterClass
    public static void tearDown(TestContext context) {
        client.close();
        vertx.close(context.asyncAssertSuccess());
    }


}
