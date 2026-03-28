package org.weihua.repository;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.SneakyThrows;

import java.sql.SQLException;
import java.util.List;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

public class ChatMemoryRepository implements ChatMemoryStore {
    @Override
    @SneakyThrows
    public List<ChatMessage> getMessages(Object memoryId) {
        // 根据memoryId查询chat_msg表，会话内容
        Entity chatMsg = null;
        try {
            chatMsg = Db.use().get(Entity.create("chat_msg").set("uid", memoryId));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (chatMsg != null) {
            String message = chatMsg.getStr("message");
            return messagesFromJson(message);
        } else {
            return List.of();
        }
    }

    @Override
    @SneakyThrows
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Entity chatMsg = Db.use().get(Entity.create("chat_msg").set("uid", memoryId));
        if (chatMsg != null) {
            Db.use().update(
                    Entity.create().set("message", messagesToJson(messages)),// 修改的数据
                    Entity.create("chat_msg").set("uid", memoryId)// where条件
                    );
        } else {
            Db.use().insert(
                    Entity.create("chat_msg").set("uid", memoryId)
                            .set("message", messagesToJson(messages))
            );
        }
    }

    @Override
    @SneakyThrows
    public void deleteMessages(Object memoryId) {
        Db.use().del(
                Entity.create("chat_msg").set("uid", memoryId)// where条件
        );
    }
}
