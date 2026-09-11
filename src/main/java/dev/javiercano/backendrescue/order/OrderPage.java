package dev.javiercano.backendrescue.order;

import java.util.List;

public record OrderPage(List<OrderResponse> items, int page, int size, long totalElements, int totalPages) {}
