package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.example.list.CreateTodoListHandler;
import org.example.model.TodoList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CreateTodoListHandlerTest {

    private DynamoDbTable<TodoList> mockTable;
    private Gson gson;
    private CreateTodoListHandler handler;
    private Context mockContext;
    private LambdaLogger mockLogger;

    @BeforeEach
    public void setup() {
        mockTable = mock(DynamoDbTable.class);
        gson = new Gson();
        handler = new CreateTodoListHandler(mockTable, gson);
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    public void testCriarListaComSucesso() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("{\"userId\":\"123\",\"name\":\"Estudar Java\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(200, response.getStatusCode());
        assertEquals("Tarefa criada", response.getBody());
        verify(mockTable).putItem(any(TodoList.class));
    }

    @Test
    public void testCriarListaSemUserId() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("{\"name\":\"Estudar Java\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(400, response.getStatusCode());
        assertEquals("Campo 'userId' é obrigatório", response.getBody());
    }

    @Test
    public void testCriarListaCorpoInvalido() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("corpo invalido");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(400, response.getStatusCode());
        assertEquals("Erro: corpo inválido", response.getBody());
    }
}
