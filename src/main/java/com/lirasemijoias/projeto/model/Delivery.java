package com.lirasemijoias.projeto.model;

import com.lirasemijoias.projeto.model.enums.DeliveryType;

public class Delivery {
    private DeliveryType type;
    private DeliveryAddress address;

    public DeliveryType getType() { return type; }
    public void setType(DeliveryType type) { this.type = type; }
    public DeliveryAddress getAddress() { return address; }
    public void setAddress(DeliveryAddress address) { this.address = address; }
}
