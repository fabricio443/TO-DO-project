package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.example.list.GetTodoListsHandler;
import org.example.model.TodoList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GetTodoListsHandlerTest {

    @Mock
    private Context context;

    @Mock
    private LambdaLogger logger;

    @Mock
    private DynamoDbTable<TodoList> mockTable;

    private GetTodoListsHandler handler;

    private Gson gson = new Gson();

    @BeforeEach
    public void setUp() {
        handler = new GetTodoListsHandler(mockTable, gson);
        when(context.getLogger()).thenReturn(logger);
    }

    @Test
    public void testHandleRequest_Success() {
        // dado um todo existente
        TodoList todo = new TodoList();
        todo.setPk("USER#123");
        todo.setSk("LIST#001");
        todo.setName("Estudar Java");

        when(mockTable.getItem(any(GetItemEnhancedRequest.class))).thenReturn(todo);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withQueryStringParameters(Map.of("userId", "123", "listId", "001"));

        // quando
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        // então
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("Estudar Java"));
        verify(mockTable, times(1)).getItem(any(GetItemEnhancedRequest.class));
    }

    @Test
    public void testHandleRequest_NotFound() {
        when(mockTable.getItem(any(GetItemEnhancedRequest.class))).thenReturn(null);

        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withQueryStringParameters(Map.of("userId", "123", "listId", "999"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(404, response.getStatusCode());
        verify(mockTable, times(1)).getItem(any(GetItemEnhancedRequest.class));
    }

    @Test
    public void testHandleRequest_MissingParams() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withQueryStringParameters(null);

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, context);

        assertEquals(400, response.getStatusCode());
        verify(mockTable, never()).getItem((GetItemEnhancedRequest) any());
    }
}
