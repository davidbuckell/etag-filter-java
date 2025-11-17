package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ItemsService {

    public Item findOne(int id) {
        int minute = LocalDateTime.now().getMinute();

        Item[] items = {
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
}