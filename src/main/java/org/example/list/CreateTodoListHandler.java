package org.example.list;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.example.model.TodoList;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class CreateTodoListHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbTable<TodoList> taskTable;
    private final Gson gson;

    // Construtor padrão usado na Lambda
    public CreateTodoListHandler() {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient).build();
        String TABLE_NAME = System.getenv("TABLE_NAME");
        this.taskTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(TodoList.class));
        this.gson = new Gson();
    }

    // Construtor adicional para testes (usa mocks)
    public CreateTodoListHandler(DynamoDbTable<TodoList> taskTable, Gson gson) {
        this.taskTable = taskTable;
        this.gson = gson;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

        var logger = context.getLogger();

        try {
            String body = event.getBody();
            if (body == null || body.isEmpty()) {
                logger.log("Corpo da requisição está vazio");
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Corpo da requisição está vazio");
            }

            Map<String, String> map = gson.fromJson(body, Map.class);
            String userId = map.get("userId");
            if (userId == null || userId.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Campo 'userId' é obrigatório");
            }

            TodoList todo = gson.fromJson(body, TodoList.class);
            todo.setPk("USER#" + userId);
            todo.setSk("LIST#" + UUID.randomUUID());

            taskTable.putItem(todo);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("Tarefa criada")
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));

        } catch (JsonSyntaxException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Erro: corpo inválido")
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Erro no servidor interno")
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
        }
    }
}
