package org.example.item.model;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class TodoItem {
    private String pk; // LIST#<listId>
    private String sk; // ITEM#<itemId>
    private String listId;
    private String itemId;
    private String title;
    private String description;
    private String status;
    private String userId;

    public TodoItem() {}

    public TodoItem(String pk, String sk, String listId, String itemId, String title, String description, String status) {
        this.pk = pk;
        this.sk = sk;
        this.listId = listId;
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    @DynamoDbPartitionKey
    public String getPk() { return pk; }
    public void setPk(String pk) { this.pk = pk; }

    @DynamoDbSortKey
    public String getSk() { return sk; }
    public void setSk(String sk) { this.sk = sk; }

    public String getListId() { return listId; }
    public void setListId(String listId) { this.listId = listId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static final TableSchema<TodoItem> TABLE_SCHEMA = TableSchema.fromBean(TodoItem.class);

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
