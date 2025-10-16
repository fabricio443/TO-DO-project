package org.example.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.example.item.model.TodoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UpdateTodoItemHandlerTest {

    private DynamoDbTable<TodoItem> mockTable;
    private UpdateTodoItemHandler handler;
    private Context mockContext;
    private LambdaLogger mockLogger;

    @BeforeEach
    public void setup() {
        mockTable = mock(DynamoDbTable.class);
        handler = new UpdateTodoItemHandler(mockTable, new Gson());
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    public void testAtualizarItemComSucesso() {
        TodoItem existing = new TodoItem();
        existing.setPk("LIST#123");
        existing.setSk("001");

        when(mockTable.getItem(any(Key.class))).thenReturn(existing);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("listId","123","itemId","001"))
                .withBody("{\"title\":\"Novo título\",\"description\":\"Nova descrição\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(200, response.getStatusCode());
        verify(mockTable, times(1)).updateItem(existing);
    }

    @Test
    public void testItemNaoEncontrado() {
        when(mockTable.getItem(any(Key.class))).thenReturn(null);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("listId","123","itemId","999"))
                .withBody("{\"title\":\"Novo título\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(404, response.getStatusCode());
        verify(mockTable, never()).updateItem((UpdateItemEnhancedRequest<TodoItem>) any());

    }

    @Test
    public void testExcecaoNoDynamoDB() {
        when(mockTable.getItem(any(Key.class))).thenThrow(new RuntimeException("Erro Dynamo"));

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("listId","123","itemId","001"))
                .withBody("{\"title\":\"Novo título\"}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(500, response.getStatusCode());
    }
}
