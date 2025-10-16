package org.example.list;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import org.example.model.TodoList;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.util.Map;

public class GetTodoListsHandler {

    private final DynamoDbTable<TodoList> table;
    private final Gson gson;

    /** Construtor padrão para a AWS Lambda */
    public GetTodoListsHandler() {
        this.gson = new Gson();

        String tableName = System.getenv("TABLE_NAME");
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalStateException("Variável de ambiente TABLE_NAME não definida");
        }

        var dynamoDbClient = software.amazon.awssdk.services.dynamodb.DynamoDbClient.builder().build();
        var enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        this.table = enhancedClient.table(tableName, TableSchema.fromBean(TodoList.class));
    }

    /** Construtor para testes unitários */
    public GetTodoListsHandler(DynamoDbTable<TodoList> table, Gson gson) {
        this.table = table;
        this.gson = gson;
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            Map<String, String> params = event.getQueryStringParameters();

            if (params == null || !params.containsKey("userId")) {
                return response.withStatusCode(400)
                        .withBody("Parâmetro 'userId' é obrigatório");
            }

            String userId = params.get("userId");
            String listId = params.get("listId");

            if (listId != null && !listId.isEmpty()) {
                // Buscar item específico
                Key key = Key.builder()
                        .partitionValue("USER#" + userId)
                        .sortValue("LIST#" + listId)
                        .build();

                TodoList todo = table.getItem(GetItemEnhancedRequest.builder().key(key).build());

                if (todo == null) {
                    return response.withStatusCode(404)
                            .withBody("Lista não encontrada");
                }

                return response.withStatusCode(200)
                        .withBody(gson.toJson(todo));
            } else {
                // Caso não tenha listId, apenas mensagem genérica (pode implementar listagem completa depois)
                return response.withStatusCode(200)
                        .withBody("Listagem geral não implementada neste teste");
            }

        } catch (Exception e) {
            logger.log("Erro interno: " + e.getMessage());
            return response.withStatusCode(500)
                    .withBody("Erro interno no servidor");
        }
    }
}
