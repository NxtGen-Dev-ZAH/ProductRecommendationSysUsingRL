package com.datasaz.ecommerce.repositories.entities;

public enum ProductSellType {
    DIRECT, // Standard purchase of a product
    OFFER, // Offer a price for the product
    AUCTION, // Product sold through a bidding process
    PREORDER, // Product available for order before official release
    SUBSCRIPTION, // Product or service sold on a recurring basis
    RENTAL, // Product available for temporary use with a rental fee
    BUNDLE, // Multiple products sold together as a single package
    DIGITAL // Digital product (e.g., software, e-books, downloadable content)
}
