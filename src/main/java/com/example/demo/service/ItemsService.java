package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class ItemsService {

    public Item findOne(int id) {
        int minute = java.time.LocalDateTime.now().getMinute();

        Item[] items = new Item[]{
                new Item(1, "Keyboard", 49.99, minute + " mins past the hour"),
                new Item(2, "Mouse", 19.99, minute + " mins past the hour")
        };

        for (Item item : items) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    public String computeETag(Item item) {
        String payload = item.getId() + "-" +
                item.getName() + "-" +
                item.getPrice() + "-" +
                item.getUpdatedAt();

        String base64 = Base64.getEncoder()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        return "\"" + base64 + "\""; // matches your NestJS output format
    }
}