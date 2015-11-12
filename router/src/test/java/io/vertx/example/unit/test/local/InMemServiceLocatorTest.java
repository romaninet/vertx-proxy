package io.vertx.example.unit.test.local;

import io.vertx.example.web.proxy.locator.InMemServiceLocator;
import io.vertx.example.web.proxy.locator.ServiceDescriptor;
import io.vertx.example.web.proxy.locator.ServiceVersion;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import static junit.framework.Assert.*;

@RunWith(VertxUnitRunner.class)
public class InMemServiceLocatorTest {

    public static final String DOMAIN = "test";
    public static final String SERVICE = "service";
    private InMemServiceLocator locator;

    @Test
    public void serviceLocatorSingleHostTest() {
        HashSet<ServiceDescriptor> descriptors = new HashSet<>();
        descriptors.add(ServiceDescriptor.create(new ServiceVersion(SERVICE,"1"),"localhost",8080 ));
        locator = new InMemServiceLocator(DOMAIN,descriptors);

        Optional<ServiceDescriptor> service = locator.getService("/service/prod1", "1");
        assertTrue(service.isPresent());
        assertEquals(service.get().getHost(), "localhost");
        assertEquals(service.get().getPort(), 8080);
        assertEquals(service.get().getServiceVersion().getName(), SERVICE);
        assertEquals(service.get().getServiceVersion().getVersion(), "1");

        //no appropriate version for service was found
        service = locator.getService("/service/prod1","4" );
        assertFalse(service.isPresent());

        service = locator.getService("/service/","1" );
        assertTrue(service.isPresent());
        assertEquals(service.get().getHost(), "localhost");
        assertEquals(service.get().getPort(), 8080);
        assertEquals(service.get().getServiceVersion().getName(), SERVICE);
        assertEquals(service.get().getServiceVersion().getVersion(), "1");
    }

    @Test
    public void serviceLocatorMultiHostsTest() {
        Map<ServiceDescriptor,Integer> map = new HashMap<>();

        HashSet<ServiceDescriptor> descriptors = new HashSet<>();

        ServiceDescriptor descriptor1 = ServiceDescriptor.create(new ServiceVersion(SERVICE, "1"), "localhost", 8080);
        ServiceDescriptor descriptor2 = ServiceDescriptor.create(new ServiceVersion(SERVICE, "1"), "localhost1", 8080);
        ServiceDescriptor descriptor3 = ServiceDescriptor.create(new ServiceVersion(SERVICE, "1"), "localhost2", 8080);

        descriptors.add(descriptor1);
        descriptors.add(descriptor2);
        descriptors.add(descriptor3);

        locator = new InMemServiceLocator(DOMAIN,descriptors);

        Optional<ServiceDescriptor> service = Optional.empty();
        for (int i = 0; i < 90; i++) {
            service = locator.getService("/service/prod1","1" );
            if(!map.containsKey(service.get())) {
                map.put(service.get(),0);
            }
            map.put(service.get(),map.get(service.get())+1);

            service = locator.getService("/service/prod2","1" );
            if(!map.containsKey(service.get())) {
                map.put(service.get(),0);
            }
            map.put(service.get(),map.get(service.get())+1);

            service = locator.getService("/service/","1" );
            if(!map.containsKey(service.get())) {
                map.put(service.get(),0);
            }
            map.put(service.get(),map.get(service.get())+1);
        }


        assertTrue(service.isPresent());
        assertEquals(map.get(descriptor1).intValue(),90 );
        assertEquals(map.get(descriptor2).intValue(),90 );
        assertEquals(map.get(descriptor3).intValue(),90 );
    }
}