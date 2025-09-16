package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloHandlerTest {

    @Test
    void testHandleRequest() {
        HelloHandler handler = new HelloHandler();
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        Context context = null; // contexto pode ser null para teste

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals("Hello World!", response.getBody(), "A resposta deve ser 'Hello World!'");
    }
}
