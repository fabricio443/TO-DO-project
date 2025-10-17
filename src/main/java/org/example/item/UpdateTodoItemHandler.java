package org.example.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.example.item.model.TodoItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;
import java.util.Map;

public class UpdateTodoItemHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbTable<TodoItem> table;
    private final Gson gson;

    // Construtor padrão Lambda
    public UpdateTodoItemHandler() {
        this.gson = new Gson();
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        this.table = enhancedClient.table(System.getenv("TABLE_NAME"), TableSchema.fromBean(TodoItem.class));
    }

    // Construtor para testes com mocks
    public UpdateTodoItemHandler(DynamoDbTable<TodoItem> table, Gson gson) {
        this.table = table;
        this.gson = gson;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        var logger = context.getLogger();
        try {
            if (event.getBody() == null || event.getBody().isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Corpo da requisição está vazio");
            }

            TodoItem item = gson.fromJson(event.getBody(), TodoItem.class);

            // Tenta pegar do path, se não vier tenta da query string
            Map<String, String> params = event.getPathParameters();
            if (params == null || !params.containsKey("listId") || !params.containsKey("itemId")) {
                params = event.getQueryStringParameters();
            }

            // Se ainda assim não tiver, retorna erro
            if (params == null || !params.containsKey("listId") || !params.containsKey("itemId")) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Parâmetros 'listId' e 'itemId' são obrigatórios");
            }

            String listId = params.get("listId");
            String itemId = params.get("itemId");

            Key key = Key.builder()
                    .partitionValue("LIST#" + listId)
                    .sortValue("ITEM#" + itemId)
                    .build();

            TodoItem existing = table.getItem(key);
            if (existing == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("Item não encontrado")
                        .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
            }

            if (item.getTitle() != null) existing.setTitle(item.getTitle());
            if (item.getDescription() != null) existing.setDescription(item.getDescription());
            if (item.getStatus() != null) existing.setStatus(item.getStatus());

            table.updateItem(existing);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("Item atualizado com sucesso")
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));

        } catch (JsonSyntaxException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Erro: corpo inválido")
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (Exception e) {
            logger.log("Erro ao atualizar item: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Erro interno no servidor")
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
        }
    }
}
