package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.model.DeliveryAddress;
import com.lirasemijoias.projeto.model.Order;
import com.lirasemijoias.projeto.model.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class WhatsappService {
    private final String ownerPhone;
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public WhatsappService(@Value("${app.whatsapp-owner-phone}") String ownerPhone) {
        this.ownerPhone = ownerPhone.replaceAll("\\D", "");
    }

    public String message(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá! Gostaria de finalizar meu pedido.\\n\\n");
        sb.append("PEDIDO #").append(order.getOrderNumber()).append("\\n\\n");
        sb.append("Cliente: ").append(order.getCustomer().getName()).append("\\n");
        sb.append("Telefone: ").append(order.getCustomer().getPhone()).append("\\n\\n");
        sb.append("Produtos:\\n");

        for (OrderItem item : order.getItems()) {
            sb.append(item.getQuantity()).append("x ")
                    .append(item.getName()).append(" - ")
                    .append(currency.format(item.getUnitPrice()))
                    .append(" cada\\nSubtotal: ")
                    .append(currency.format(item.getSubtotal()))
                    .append("\\n\\n");
        }

        sb.append("TOTAL: ").append(currency.format(order.getTotal())).append("\\n\\n");
        sb.append("Entrega: ").append(order.getDelivery().getType()).append("\\n");

        DeliveryAddress a = order.getDelivery().getAddress();
        if (a != null) {
            sb.append("Endereço: ").append(a.getStreet()).append(", ").append(a.getNumber()).append("\\n")
                    .append(a.getDistrict()).append(" - ").append(a.getCity()).append("/").append(a.getState()).append("\\n")
                    .append("CEP: ").append(a.getZipCode()).append("\\n");
            if (a.getComplement() != null && !a.getComplement().isBlank()) {
                sb.append("Complemento: ").append(a.getComplement()).append("\\n");
            }
        }

        if (order.getNotes() != null && !order.getNotes().isBlank()) {
            sb.append("\\nObservações: ").append(order.getNotes());
        }
        return sb.toString();
    }

    public String url(String message) {
        return "https://wa.me/" + ownerPhone + "?text=" +
                URLEncoder.encode(message, StandardCharsets.UTF_8);
    }
}
