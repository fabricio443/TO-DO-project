package org.example.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.example.item.model.TodoItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

public class CreateTodoItemHandler {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<TodoItem> table;
    private ObjectMapper objectMapper = new ObjectMapper();

    public CreateTodoItemHandler() {
        DynamoDbClient client = DynamoDbClient.create();
        this.enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
        this.table = enhancedClient.table(System.getenv("TABLE_NAME"), TodoItem.TABLE_SCHEMA);
    }

    public CreateTodoItemHandler(DynamoDbTable<TodoItem> mockTable, ObjectMapper objectMapper) {
        this.table = mockTable;
        this.objectMapper = objectMapper;
        this.enhancedClient = null;
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();

        try {
            logger.log("Body recebido: " + request.getBody());

            // Agora lemos 'title' em vez de 'name'
            Map<String, String> body = objectMapper.readValue(request.getBody(), Map.class);
            String listId = body.get("listId");
            String title = body.get("title"); // ✅ substituído
            String description = body.getOrDefault("description", "");
            String status = body.getOrDefault("status", "PENDING");
            String userId = body.get("userId");

            // Validações básicas
            if (listId == null || listId.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Parâmetro 'listId' é obrigatório.");
            }

            if (title == null || title.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Parâmetro 'title' é obrigatório.");
            }

            // Gera ID único para o item
            String itemId = UUID.randomUUID().toString();

            // Cria o objeto TodoItem
            TodoItem item = new TodoItem(
                    "LIST#" + listId, // PK
                    "ITEM#" + itemId, // SK
                    listId,
                    itemId,
                    title,
                    description,
                    status
            );

            // Seta userId se existir
            if (userId != null && !userId.isEmpty()) {
                item.setUserId(userId);
            }

            // Salva no DynamoDB
            table.putItem(item);

            logger.log("Item criado com sucesso: " + itemId);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(objectMapper.writeValueAsString(item));

        } catch (Exception e) {
            logger.log("Erro ao criar item: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Erro interno: " + e.getMessage());
        }
    }
}
