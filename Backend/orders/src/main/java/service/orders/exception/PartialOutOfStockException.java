package service.orders.exception;

import service.orders.dto.StockUpdateResult;

public class PartialOutOfStockException extends RuntimeException {

    private final StockUpdateResult result;

    public PartialOutOfStockException(String message, StockUpdateResult result) {
        super(message);
        this.result = result;
    }

    public StockUpdateResult getResult() {
        return result;
    }
}