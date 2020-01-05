package org.aostw;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;

public class AostwEIP {

    public static void main(String... args) throws Exception {
        Main main = new Main();
        RouteBuilder routeBuilder = new ContactsIntegrationBuilder();
        main.addRouteBuilder(routeBuilder);
        main.start();

//        ProducerTemplate producerTemplate = routeBuilder.getContext().createProducerTemplate();
//        producerTemplate.sendBody("direct:start", "run");

        main.run(args);
    }

}

