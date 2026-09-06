package Product.Service.dto;

import java.util.List;

public record StockUpdateResult(
    boolean allSuccessful,
    List<ItemStockStatus> items
) {}

