package io.vertx.example.web.proxy.dashboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.example.util.Runner;
import io.vertx.example.web.proxy.events.EventBus;
import io.vertx.example.web.proxy.events.RedisEventBus;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import javax.swing.text.html.Option;
import java.util.Optional;

public class Dashboard extends AbstractVerticle {

    public static final int PORT = 8181;

    public static final VertxOptions DROPWIZARD_OPTIONS = new VertxOptions().
            setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true));

    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        System.out.println("Dashboard accepting requests: "+ Dashboard.PORT);
        Runner.runExample(Dashboard.class, new VertxOptions(DROPWIZARD_OPTIONS).setClustered(false));
    }

    @Override
    public void start() {
        EventBus bus = new RedisEventBus();
        Router router = Router.router(vertx);

        // Allow outbound traffic to the news-feed address

        BridgeOptions options = new BridgeOptions().
                addOutboundPermitted(
                        new PermittedOptions().
                                setAddress("metrics")
                );

        router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options));

        // Serve the static resources
        router.route().handler(StaticHandler.create());

        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(router::accept).listen(PORT);

        // Send a metrics events every 5 second
        vertx.setPeriodic(5000, t -> {
            Optional metrics = bus.subscribe("metrics");
            if(metrics.isPresent()) {
                vertx.eventBus().publish("metrics", metrics.get());
            }
        });


/*
    // Send some messages
    Random random = new Random();
    vertx.eventBus().consumer("whatever", msg -> {
      vertx.setTimer(10 + random.nextInt(50), id -> {
        vertx.eventBus().send("whatever", "hello");
      });
    });
    vertx.eventBus().send("whatever", "hello");
*/
    }

}
