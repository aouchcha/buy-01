package service.orders.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Analytics {
    public List<BestSellingProductDTO> bestSellingProducts;
    public Double Total;
}
