package com.lirasemijoias.projeto.dto;

import com.lirasemijoias.projeto.dto.auth.LoginRequest;
import com.lirasemijoias.projeto.dto.category.CategoryRequest;
import com.lirasemijoias.projeto.dto.order.AddressRequest;
import com.lirasemijoias.projeto.dto.order.CheckoutRequest;
import com.lirasemijoias.projeto.dto.order.CustomerRequest;
import com.lirasemijoias.projeto.dto.order.DeliveryRequest;
import com.lirasemijoias.projeto.dto.order.OrderItemRequest;
import com.lirasemijoias.projeto.dto.order.UpdateOrderStatusRequest;
import com.lirasemijoias.projeto.dto.product.CreateProductRequest;
import com.lirasemijoias.projeto.dto.product.ProductImageRequest;
import com.lirasemijoias.projeto.dto.product.UpdateProductRequest;
import com.lirasemijoias.projeto.dto.stock.StockRequest;
import com.lirasemijoias.projeto.dto.user.CreateUserRequest;
import com.lirasemijoias.projeto.model.enums.DeliveryType;
import com.lirasemijoias.projeto.model.enums.OrderStatus;
import com.lirasemijoias.projeto.model.enums.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void loginRequiresValidEmailAndPassword() {
        assertInvalidFields(new LoginRequest("email-invalido", " "), "email", "password");
        assertValid(new LoginRequest("cliente@exemplo.com", "senha-segura"));
    }

    @Test
    void categoryRequiresAName() {
        assertInvalidFields(new CategoryRequest(" ", "descricao opcional"), "name");
        assertValid(new CategoryRequest("Brincos", null));
    }

    @Test
    void addressRequiresEveryShippingFieldExceptComplement() {
        AddressRequest invalid = new AddressRequest("", " ", "", " ", "", " ", null);

        assertInvalidFields(invalid, "street", "number", "district", "city", "state", "zipCode");
        assertValid(validAddress());
    }

    @Test
    void customerRequiresNameAndPhone() {
        assertInvalidFields(new CustomerRequest("", " "), "name", "phone");
        assertValid(new CustomerRequest("Maria Silva", "11999999999"));
    }

    @Test
    void orderItemRequiresProductAndPositiveQuantity() {
        assertInvalidFields(new OrderItemRequest(" ", 0), "productId", "quantity");
        assertValid(new OrderItemRequest("produto-1", 1));
    }

    @Test
    void deliveryRequiresTypeAndCascadesAddressValidation() {
        DeliveryRequest invalid = new DeliveryRequest(
                null,
                new AddressRequest("", "1", "Centro", "Sao Paulo", "SP", "01000-000", null));

        assertInvalidFields(invalid, "type", "address.street");
        assertValid(new DeliveryRequest(DeliveryType.PICKUP, null));
        assertValid(new DeliveryRequest(DeliveryType.DELIVERY, validAddress()));
    }

    @Test
    void checkoutRequiresItsMainPartsAndLimitsNotes() {
        CheckoutRequest invalid = new CheckoutRequest(null, null, List.of(), "x".repeat(501));

        assertInvalidFields(invalid, "customer", "delivery", "items", "notes");
        assertValid(validCheckout());
    }

    @Test
    void checkoutCascadesValidationToCustomerDeliveryAndItems() {
        CheckoutRequest invalid = new CheckoutRequest(
                new CustomerRequest("", ""),
                new DeliveryRequest(null,
                        new AddressRequest("", "1", "Centro", "Sao Paulo", "SP", "01000-000", null)),
                List.of(new OrderItemRequest("", -1)),
                null);

        assertInvalidFields(invalid,
                "customer.name",
                "customer.phone",
                "delivery.type",
                "delivery.address.street",
                "items[0].productId",
                "items[0].quantity");
    }

    @Test
    void orderStatusCannotBeNull() {
        assertInvalidFields(new UpdateOrderStatusRequest(null), "status");
        assertValid(new UpdateOrderStatusRequest(OrderStatus.CONFIRMED));
    }

    @Test
    void createProductEnforcesRequiredPricesAndNonNegativeStock() {
        CreateProductRequest invalid = new CreateProductRequest(
                "", " ", "", BigDecimal.ZERO, new BigDecimal("-0.01"), " ",
                null, null, -1, -2, false);

        assertInvalidFields(invalid,
                "name", "sku", "description", "price", "promotionalPrice", "categoryId",
                "stock", "minimumStock");
        assertValid(validCreateProduct());
    }

    @Test
    void updateProductEnforcesRequiredPricesAndNonNegativeMinimumStock() {
        UpdateProductRequest invalid = new UpdateProductRequest(
                "", " ", "", null, BigDecimal.ZERO, " ", null, null, -1, false, true);

        assertInvalidFields(invalid,
                "name", "sku", "description", "price", "promotionalPrice", "categoryId",
                "minimumStock");
        assertValid(validUpdateProduct());
    }

    @Test
    void productImageRequiresUrlAndCloudinaryPublicId() {
        assertInvalidFields(new ProductImageRequest(" ", "", false), "url", "publicId");
        assertValid(new ProductImageRequest("https://cdn.exemplo.com/anel.jpg", "produtos/anel", true));
    }

    @Test
    void stockMovementRequiresProductPositiveQuantityAndReason() {
        assertInvalidFields(new StockRequest("", 0, " "), "productId", "quantity", "reason");
        assertValid(new StockRequest("produto-1", 5, "Reposicao"));
    }

    @Test
    void userRequiresIdentityStrongEnoughPasswordAndRole() {
        CreateUserRequest invalid = new CreateUserRequest(" ", "email-invalido", "curta", null);

        assertInvalidFields(invalid, "name", "email", "password", "role");
        assertValid(new CreateUserRequest(
                "Administradora", "admin@exemplo.com", "senha123", UserRole.ADMIN));
    }

    private static CheckoutRequest validCheckout() {
        return new CheckoutRequest(
                new CustomerRequest("Maria Silva", "11999999999"),
                new DeliveryRequest(DeliveryType.DELIVERY, validAddress()),
                List.of(new OrderItemRequest("produto-1", 2)),
                "Entregar no periodo da tarde");
    }

    private static AddressRequest validAddress() {
        return new AddressRequest(
                "Rua das Flores", "123", "Centro", "Sao Paulo", "SP", "01000-000", "Apto 4");
    }

    private static CreateProductRequest validCreateProduct() {
        return new CreateProductRequest(
                "Anel", "AN-001", "Anel folheado", new BigDecimal("99.90"), null,
                "aneis", null, null, 0, 0, true);
    }

    private static UpdateProductRequest validUpdateProduct() {
        return new UpdateProductRequest(
                "Anel", "AN-001", "Anel folheado", new BigDecimal("99.90"), null,
                "aneis", null, null, 0, true, true);
    }

    private static void assertValid(Object value) {
        Set<ConstraintViolation<Object>> violations = validator.validate(value);
        assertTrue(violations.isEmpty(), () -> "Expected no violations, but got " + violations);
    }

    private static void assertInvalidFields(Object value, String... expectedFields) {
        Set<String> fields = validator.validate(value).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of(expectedFields), fields);
    }
}
