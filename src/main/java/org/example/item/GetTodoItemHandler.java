package org.example.item;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.example.item.model.TodoItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.util.Map;

public class GetTodoItemHandler {

    private final DynamoDbTable<TodoItem> table;
    private final Gson gson;

    // Construtor padrão Lambda
    public GetTodoItemHandler() {
        this.gson = new Gson();
        DynamoDbClient client = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(client)
                .build();
        String TABLE_NAME = System.getenv("TABLE_NAME");
        this.table = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(TodoItem.class));
    }

    // Construtor para testes com mocks
    public GetTodoItemHandler(DynamoDbTable<TodoItem> table, Gson gson) {
        this.table = table;
        this.gson = gson;
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        LambdaLogger logger = context.getLogger();

        try {
            // Primeiro tenta pegar os parâmetros do path
            Map<String, String> params = event.getPathParameters();

            // Se não vier do path, tenta pegar da query string
            if (params == null || !params.containsKey("listId") || !params.containsKey("itemId")) {
                params = event.getQueryStringParameters();
            }

            // Se ainda assim não encontrar, retorna erro
            if (params == null || !params.containsKey("listId") || !params.containsKey("itemId")) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Parâmetros 'listId' e 'itemId' são obrigatórios");
            }

            String listId = params.get("listId");
            String itemId = params.get("itemId");

            // Monta a chave no formato correto
            Key key = Key.builder()
                    .partitionValue("LIST#" + listId)
                    .sortValue("ITEM#" + itemId)
                    .build();

            // Busca o item no DynamoDB
            TodoItem item = table.getItem(GetItemEnhancedRequest.builder().key(key).build());

            if (item == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("Item não encontrado");
            }

            // Retorna o item encontrado
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(item));

        } catch (Exception e) {
            logger.log("Erro ao buscar item: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Erro interno no servidor");
        }
    }
}
