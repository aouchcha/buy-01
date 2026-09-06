package Product.Service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {
    // Bird Categories
    LIVE_POULTRY("Live Poultry", "Live farm birds including chickens, ducks, and turkeys"),
    CHICKS("Chicks & Ducklings", "Young birds and newly hatched chicks"),
    EXOTIC_BIRDS("Exotic & Ornamental Birds", "Specialty and ornamental avian species"),
    
    // Egg Categories
    CONSUMPTION_EGGS("Fresh Eggs", "Table eggs intended for daily consumption"),
    HATCHING_EGGS("Hatching Eggs", "Fertilized eggs intended for incubation"),
    SPECIALTY_EGGS("Specialty Eggs", "Duck, quail, or organic/free-range specialty eggs"),
    
    // Equipment & Supplies (Optional add-on for farm stores)
    FEED_AND_SUPPLIES("Feed & Equipment", "Bird feed, incubators, and coop accessories");

    private final String displayName;
    private final String description;

}