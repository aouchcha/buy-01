package Product.Service.dto;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class StockUpdateResult {
    private final boolean allSuccessful;
    private final List<ItemStockStatus> items;

    @JsonCreator
    public StockUpdateResult(
            @JsonProperty("allSuccessful") boolean allSuccessful,
            @JsonProperty("items") List<ItemStockStatus> items) {
        this.allSuccessful = allSuccessful;
        this.items = items;
    }

    public boolean allSuccessful() {
        return allSuccessful;
    }

    public List<ItemStockStatus> items() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockUpdateResult)) return false;
        StockUpdateResult that = (StockUpdateResult) o;
        return allSuccessful == that.allSuccessful && Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allSuccessful, items);
    }

    @Override
    public String toString() {
        return "StockUpdateResult[allSuccessful=" + allSuccessful + ", items=" + items + "]";
    }
}
