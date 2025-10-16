package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.example.list.UpdateTodoListHandler;
import org.example.model.TodoList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UpdateTodoListHandlerTest {

    private DynamoDbTable<TodoList> mockTable;
    private Gson gson;
    private UpdateTodoListHandler handler;
    private Context mockContext;
    private LambdaLogger mockLogger;

    @BeforeEach
    public void setup() {
        mockTable = mock(DynamoDbTable.class);
        gson = new Gson();
        handler = new UpdateTodoListHandler(mockTable, gson);
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    public void testAtualizarListaComSucesso() {
        TodoList existing = new TodoList();
        existing.setPk("USER#123");
        existing.setSk("LIST#001");
        existing.setName("Antigo nome");

        when(mockTable.getItem(any(Key.class))).thenReturn(existing);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("{\"userId\":\"123\",\"sk\":\"LIST#001\",\"name\":\"Novo nome\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(200, response.getStatusCode());
        assertEquals("Lista atualizada", response.getBody());
        verify(mockTable).putItem(existing);
    }

    @Test
    public void testApagarLista() {
        TodoList existing = new TodoList();
        existing.setPk("USER#123");
        existing.setSk("LIST#001");

        when(mockTable.getItem(any(Key.class))).thenReturn(existing);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withBody("{\"userId\":\"123\",\"sk\":\"LIST#001\",\"name\":\"\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(200, response.getStatusCode());
        assertEquals("Lista apagada", response.getBody());
        verify(mockTable).deleteItem(any(Key.class));
    }
}
