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
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GetTodoItemHandlerTest {

    private DynamoDbTable<TodoItem> mockTable;
    private GetTodoItemHandler handler;
    private Context mockContext;
    private LambdaLogger mockLogger;

    @BeforeEach
    public void setup() {
        mockTable = mock(DynamoDbTable.class);
        handler = new GetTodoItemHandler(mockTable, new Gson());
        mockContext = mock(Context.class);
        mockLogger = mock(LambdaLogger.class);
        when(mockContext.getLogger()).thenReturn(mockLogger);
    }

    @Test
    public void testHandleRequest_Success() {
        TodoItem item = new TodoItem();
        item.setPk("LIST#123");
        item.setSk("001");
        item.setTitle("Estudar Java");

        when(mockTable.getItem(any(GetItemEnhancedRequest.class))).thenReturn(item);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("listId","123","itemId","001"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Estudar Java"));
        verify(mockTable, times(1)).getItem(any(GetItemEnhancedRequest.class));
    }

    @Test
    public void testHandleRequest_NotFound() {
        when(mockTable.getItem(any(GetItemEnhancedRequest.class))).thenReturn(null);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("listId","123","itemId","999"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(404, response.getStatusCode());
        verify(mockTable, times(1)).getItem(any(GetItemEnhancedRequest.class));
    }

    @Test
    public void testHandleRequest_MissingParams() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withPathParameters(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);

        assertEquals(400, response.getStatusCode());
        verify(mockTable, never()).getItem(any(GetItemEnhancedRequest.class));
    }
}
