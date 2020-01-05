package org.aostw;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.text.StringSubstitutor;

import java.util.*;

public class ContactsSoapToRest {

    private final RouteBuilder builder;

    public ContactsSoapToRest(RouteBuilder builder) {
        this.builder = builder;
    }

    public void handleSoapMutations() {
        String restServer = "http://service.aostw.localhost/rest/";

        String[][] contactSoapMutators = new String[][]{
                {"editUser", "PUT", "users/${id}"},
                {"addUser", "POST", "users"},
                {"deleteUser", "DELETE", "users/${id}"},
                {"resetDb", "POST", "users/reset"},
        };

        this.builder
                .from("direct:handle-mutation-contacts-soap")
                .process((Exchange exchange) -> {
                    Message in = exchange.getIn();
                    String httpMethod = in.getHeader(Exchange.HTTP_METHOD, String.class);

                    if (httpMethod.toLowerCase().equals("post")) {
                        String soapMethod = this.getSoapMethod(in.getHeader(Exchange.CONTENT_TYPE, String.class));

                        List<Object> mutation = mapSoapMethod(soapMethod, contactSoapMutators);

                        String restHttpMethod = (String) mutation.get(0);

                        if (!restHttpMethod.equals("")) {
                            in.setHeader("restHttpMethod", restHttpMethod);
                            in.setHeader("restHttpPath", mutation.get(1));
                        }
                    }
                })
                .choice()
                .when(this.builder.header("restHttpMethod").isNotNull())
                .process((Exchange exchange) -> {
                    Message in = exchange.getIn();

                    HashMap body = this.extractSoapPayload(in.getBody(String.class), new String[]{"env:Envelope", "env:Body"});
                    String payload = this.prepareRestPayload(body);

                    String restHttpMethod = in.getHeader("restHttpMethod", String.class);
                    String restHttpPath = this.prepareRestPath(in.getHeader("restHttpPath", String.class), body);

                    in.removeHeaders("*");
                    in.setHeader("restHttpPath", restHttpPath);
                    in.setHeader(Exchange.HTTP_METHOD, restHttpMethod);
                    in.setHeader(Exchange.CONTENT_TYPE, "application/json");
                    in.setHeader("httpARGS", payload);

                    in.setBody(payload);
                })
                .log("Update REST M: ${header.CamelHttpMethod} P: ${header.restHttpPath} ARG: ${header.httpARGS}")
                .toD(restServer + "${header.restHttpPath}?throwExceptionOnFailure=false&bridgeEndpoint=true")
                .end();
    }

    private String prepareRestPayload(HashMap<String, Object> body) {
        try {

            HashMap soapNewData = (HashMap) body.get("newData");

            if (soapNewData != null) {
                ArrayList<HashMap> items = (ArrayList) soapNewData.get("item");

                HashMap<String, String> newData = new HashMap<String, String>();

                for (HashMap<String, String> entry : items) {
                    newData.put(entry.get("key"), entry.get("value"));
                }

                body.put("newData", newData);
            }

            return JsonHelper.toJSONString(body);
        } catch (Exception e) {
            return "";
        }
    }

    private String prepareRestPath(String restHttpPath, Map<String, Object> valuesMap) {
        try {
            StringSubstitutor sub = new StringSubstitutor(valuesMap);
            return sub.replace(restHttpPath);
        } catch (Exception e) {
            return restHttpPath;
        }
    }

    private HashMap extractSoapPayload(String fromXML, String[] path) {
        try {
            HashMap body = (HashMap) JsonHelper.fromXML(fromXML);

            for (String part : path) {
                body = (HashMap) body.get(part);
            }

            return (HashMap) body.values().toArray()[0];
        } catch (Exception e) {
            return new HashMap<String, String>();
        }
    }

    private String getSoapMethod(String contentType) {
        try {
            return contentType.split("action=")[1]
                    .replace("\"", "")
                    .split("#")[1]
                    .trim();
        } catch (Exception ignored) {

        }

        return "";
    }

    public static List<Object> mapSoapMethod(String method, String[][] mutatorEndpoints) {
        for (String[] endpoint : mutatorEndpoints) {
            String mutatorMethod = endpoint[0].toLowerCase();

            if (method.toLowerCase().equals(mutatorMethod)) {
                return Arrays.asList(endpoint[1], endpoint[2]);
            }
        }

        return Arrays.asList("", new ArrayList<String>());
    }
}
