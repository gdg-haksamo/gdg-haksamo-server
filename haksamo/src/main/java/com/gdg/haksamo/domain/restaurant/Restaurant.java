package com.gdg.haksamo.domain.restaurant;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public class Restaurant {
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
}
