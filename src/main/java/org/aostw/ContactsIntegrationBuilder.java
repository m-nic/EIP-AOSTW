package org.aostw;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

public class ContactsIntegrationBuilder extends RouteBuilder {

    public void configure() {
//        onException(Exception.class).log("${body}");

        this.serveClient(
                "http://public.service.aostw.localhost:8080/",
                "http://service.aostw.localhost/",
                "client-rest.php"
        );

        this.exposeRestService(
                "http://public.service.aostw.localhost:8080/rest/",
                "http://service.aostw.localhost/rest/"
        );

        new ContactsRestToSoap(this).handleRestMutations();

        this.serveClient(
                "http://public.service.aostw.localhost:9090/",
                "http://service.aostw.localhost/",
                "client-soap.php"
        );

        this.exposeSoapService(
                "http://public.service.aostw.localhost:9090/server-soap.php",
                "http://service.aostw.localhost/server-soap.php"
        );

        new ContactsSoapToRest(this).handleSoapMutations();
    }

    private void serveClient(String publicUri, String realHost, String clientUri) {
        from("jetty:" + publicUri + "/ui?matchOnUriPrefix=true")
                .process((exchange) -> {
                    Message in = exchange.getIn();
                    exchange.getIn().setHeader("reqAsset", in.getHeader(Exchange.HTTP_URI, String.class));
                })
                .toD(realHost + "${header.reqAsset}?throwExceptionOnFailure=false&bridgeEndpoint=true");

        from("jetty:" + publicUri + "?matchOnUriPrefix=false")
                .process(new ProxyHeadersProcessor())
                .to(realHost + clientUri + "?bridgeEndpoint=true")
                .process((exchange) -> {
                    String body = exchange.getIn().getBody(String.class);
                    body = body.replace("</body>", "<style>" + Helpers.getClientStyle() + "</style></body>");
                    exchange.getOut().setBody(body);
                });
    }


    private void exposeRestService(String publicEndpoint, String realEndpoint) {
        from("jetty:" + publicEndpoint + "?matchOnUriPrefix=true")
                .process(new ProxyHeadersProcessor())
                .wireTap("direct:handle-mutation-contacts-rest")
                .to(realEndpoint + "?bridgeEndpoint=true");
    }

    private void exposeSoapService(String publicEndpoint, String realEndpoint) {
        from("jetty:" + publicEndpoint + "?matchOnUriPrefix=false")
                .process(new ProxyHeadersProcessor())
                .wireTap("direct:handle-mutation-contacts-soap")
                .to(realEndpoint + "?bridgeEndpoint=true");
    }

}
