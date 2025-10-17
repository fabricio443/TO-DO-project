package org.example.list;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.example.model.TodoList;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import java.util.Collections;

public class UpdateTodoListHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbTable<TodoList> taskTable;
    private final Gson gson;

    public UpdateTodoListHandler() {
        DynamoDbClient ddb = DynamoDbClient.create();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
        this.taskTable = enhancedClient.table(System.getenv("TABLE_NAME"), TodoList.TABLE_SCHEMA);
        this.gson = new Gson();
    }

    public UpdateTodoListHandler(DynamoDbTable<TodoList> taskTable, Gson gson) {
        this.taskTable = taskTable;
        this.gson = gson;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        var logger = context.getLogger();
        logger.log("Requisição recebida para atualizar/apagar lista: " + event.getBody());

        try {
            if (event.getBody() == null || event.getBody().isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Corpo da requisição está vazio");
            }

            TodoList todo = gson.fromJson(event.getBody(), TodoList.class);

            if (todo.getUserId() == null || todo.getUserId().isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("Campo 'userId' é obrigatório");
            }

            if (todo.getSk() == null || todo.getSk().isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("É necessário informar o SK da lista");
            }

            Key key = Key.builder()
                    .partitionValue("USER#" + todo.getUserId())
                    .sortValue(todo.getSk())
                    .build();

            TodoList existing = taskTable.getItem(key);
            if (existing == null) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withBody("Lista não encontrada")
                        .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
            }

            if (todo.getName() == null || todo.getName().isEmpty()) {
                taskTable.deleteItem(key);
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(200)
                        .withBody("Lista apagada")
                        .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
            }

            existing.setName(todo.getName());
            if (todo.getDescription() != null) existing.setDescription(todo.getDescription());
            if (todo.getStatus() != null) existing.setStatus(todo.getStatus());

            taskTable.putItem(existing);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("Lista atualizada")
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));

        } catch (JsonSyntaxException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody("Erro: corpo inválido")
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
        } catch (Exception e) {
            logger.log("Erro ao atualizar/apagar lista: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("Erro no servidor interno")
                    .withHeaders(Collections.singletonMap("Content-Type", "application/json"));
        }
    }
}
