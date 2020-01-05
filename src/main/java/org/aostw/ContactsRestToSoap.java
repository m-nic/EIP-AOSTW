package org.aostw;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactsRestToSoap {

    private final RouteBuilder builder;

    ContactsRestToSoap(RouteBuilder builder) {
        this.builder = builder;
    }

    public void handleRestMutations() {
        String soapServer = "http://service.aostw.localhost/server-soap.php";

        String[][] contactRestMutators = new String[][]{
                {"PUT", "users/(.*)", "editUser"},
                {"POST", "users", "addUser"},
                {"DELETE", "users/(.*)", "deleteUser"},
                {"POST", "users/reset", "resetDb"},
        };

        this.builder
                .from("direct:handle-mutation-contacts-rest")
                .process((Exchange exchange) -> {
                    Message in = exchange.getIn();
                    String method = in.getHeader(Exchange.HTTP_METHOD, String.class).toLowerCase();
                    String path = in.getHeader(Exchange.HTTP_PATH, String.class);

                    List<Object> mutation = mapRestMethod(method, path, contactRestMutators);

                    String mutationMethod = (String) mutation.get(0);

                    if (!mutationMethod.equals("")) {
                        in.setHeader("mutationMethod", mutationMethod);
                        in.setHeader("mutationArgs", JsonHelper.toJSON(mutation.get(1)));
                    }
                })
                .choice()
                .when(this.builder.header("mutationMethod").isNotNull())
                .log("Update SOAP M: ${header.mutationMethod} ARG: ${header.mutationArgs}")
                .process((Exchange exchange) -> {
                    Message in = exchange.getIn();

                    String method = in.getHeader("mutationMethod", String.class);
                    String serializedArgs = in.getHeader("mutationArgs", String.class);

                    in.removeHeaders("*");
                    in.setHeader(Exchange.HTTP_METHOD, "POST");
                    in.setHeader(Exchange.HTTP_HOST, "service.aostw.localhost");
                    in.setHeader(Exchange.HTTP_BASE_URI, soapServer);
                    in.setHeader(Exchange.CONTENT_TYPE, "application/soap+xml; charset=utf-8; action=\"" + soapServer + "#" + method + "\"");

                    Object urlParts = JsonHelper.fromString(serializedArgs);

                    Map<String, Object> values = new HashMap<String, Object>();
                    values.put("namespace", soapServer);
                    values.put("method", method);
                    values.put("args", urlParts);

                    try {
                        String jsonData = in.getBody(String.class);
                        values.put("json", JsonHelper.fromString(jsonData));
                    } catch (Exception e) {
                    }

                    String soapEnvelope = Helpers.getSoapEnvelope("web/contactsSoap/", method, values);

                    in.setBody(soapEnvelope);
                })
                .to(soapServer)
                .end();
    }

    public static List<Object> mapRestMethod(String method, String path, String[][] mutatorEndpoints) {
        for (String[] endpoint : mutatorEndpoints) {
            String mutatorMethod = endpoint[0].toLowerCase();
            Pattern pattern = Pattern.compile(endpoint[1]);
            Matcher matcher = pattern.matcher(path);

            if (method.toLowerCase().equals(mutatorMethod) && matcher.matches()) {
                List<String> allMatches = Helpers.getAllMatches(matcher);
                return Arrays.asList(endpoint[2], allMatches);
            }
        }

        return Arrays.asList("", new ArrayList<String>());
    }

}
