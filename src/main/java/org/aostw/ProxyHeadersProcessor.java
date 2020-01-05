package org.aostw;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

public class ProxyHeadersProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String[] url = in.getHeader("Host").toString().split(":");

        in.setHeader("X-Forwarded-Host", url[0]);
        in.setHeader("X-Forwarded-Port", url[1]);
    }
}
