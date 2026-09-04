package com.lirasemijoias.projeto.service;

import com.lirasemijoias.projeto.model.*;
import com.lirasemijoias.projeto.model.enums.DeliveryType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class WhatsappServiceTest {

    @Test
    void constructorSanitizesOwnerPhoneAndUrlEncodesMessage() {
        WhatsappService service = new WhatsappService("+55 (11) 99999-0000");
        String message = "Pedido #1 & entrega";

        assertEquals("https://wa.me/5511999990000?text="
                + URLEncoder.encode(message, StandardCharsets.UTF_8), service.url(message));
    }

    @Test
    void messageIncludesItemsAddressComplementAndNotes() {
        WhatsappService service = new WhatsappService("5511999999999");
        Order order = baseOrder();
        DeliveryAddress address = new DeliveryAddress();
        address.setStreet("Rua A");
        address.setNumber("10");
        address.setDistrict("Centro");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setZipCode("01000-000");
        address.setComplement("Apto 2");
        order.getDelivery().setAddress(address);
        order.setNotes("Entregar à tarde");

        String message = service.message(order);
        String expectedPrice = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                .format(new BigDecimal("10.50"));

        assertAll(
                () -> assertTrue(message.contains("PEDIDO #PED-123")),
                () -> assertTrue(message.contains("Cliente: Ana")),
                () -> assertTrue(message.contains("2x Brinco")),
                () -> assertTrue(message.contains(expectedPrice)),
                () -> assertTrue(message.contains("Rua A, 10")),
                () -> assertTrue(message.contains("CEP: 01000-000")),
                () -> assertTrue(message.contains("Complemento: Apto 2")),
                () -> assertTrue(message.contains("Entregar à tarde"))
        );
    }

    @Test
    void messageOmitsOptionalAddressComplementAndNotes() {
        WhatsappService service = new WhatsappService("55");
        Order order = baseOrder();

        String nullOptionals = service.message(order);
        assertFalse(nullOptionals.contains("CEP:"));
        assertFalse(nullOptionals.contains("Complemento:"));

        order.setNotes("   ");

        String withoutAddress = service.message(order);
        assertFalse(withoutAddress.contains("CEP:"));
        assertFalse(withoutAddress.contains("Complemento:"));

        DeliveryAddress address = new DeliveryAddress();
        address.setStreet("Rua");
        address.setNumber("1");
        address.setDistrict("Bairro");
        address.setCity("Cidade");
        address.setState("UF");
        address.setZipCode("00000");
        order.getDelivery().setAddress(address);

        String nullComplement = service.message(order);
        assertFalse(nullComplement.contains("Complemento:"));
        assertFalse(nullComplement.contains("Observa"));

        address.setComplement(" ");
        assertFalse(service.message(order).contains("Complemento:"));
    }

    private static Order baseOrder() {
        OrderItem item = new OrderItem();
        item.setName("Brinco");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("10.50"));
        item.setSubtotal(new BigDecimal("21.00"));

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.DELIVERY);

        Order order = new Order();
        order.setOrderNumber("PED-123");
        order.setCustomer(new Customer("Ana", "11999999999"));
        order.setItems(List.of(item));
        order.setTotal(new BigDecimal("21.00"));
        order.setDelivery(delivery);
        return order;
    }
}
