package org.example.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.item.model.TodoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class CreateTodoItemHandlerTest {

    private DynamoDbTable<TodoItem> mockTable;
    private ObjectMapper objectMapper;
    private CreateTodoItemHandler handler;
    private Context mockContext;
    private LambdaLogger mockLogger;

    @BeforeEach
    public void setup() {
        mockTable = mock(DynamoDbTable.class);
        objectMapper = new ObjectMapper();
        handler = new CreateTodoItemHandler(mockTable, objectMapper);
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    public void testCriarItemComSucesso() throws Exception {
        // Body compatível com handler atual (aceita title ou name)
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("{\"listId\":\"123\",\"title\":\"Estudar Java\",\"description\":\"Descricao teste\",\"status\":\"PENDING\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        // Valida se status é 201 (criado)
        assertEquals(201, response.getStatusCode());

        // Verifica se putItem foi chamado no mock da tabela
        verify(mockTable).putItem(any(TodoItem.class));
    }

    @Test
    public void testCriarItemSemListId() throws Exception {
        // Body sem listId, deve retornar 400
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("{\"title\":\"Estudar Java\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void testCriarItemCorpoInvalido() throws Exception {
        // Corpo inválido, deve retornar 500
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("corpo invalido");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(500, response.getStatusCode());
    }
}
