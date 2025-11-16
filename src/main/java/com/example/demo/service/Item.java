package com.example.demo.service;

public class Item {
    private int id;
    private String name;
    private double price;
    private String updatedAt;

    public Item(int id, String name, double price, String updatedAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}