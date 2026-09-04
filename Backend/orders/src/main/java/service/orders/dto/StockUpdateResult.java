package service.orders.dto;

import java.util.List;

public record StockUpdateResult(
    boolean allSuccessful,
    List<ItemStockStatus> items
) {}

