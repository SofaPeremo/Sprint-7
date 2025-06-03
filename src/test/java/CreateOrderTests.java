import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.example.Order;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@RunWith(Parameterized.class)
public class CreateOrderTests {
    private static final String BASE_URI = "https://qa-scooter.praktikum-services.ru";
    private static final String CREATE_ORDER_PATH = "/api/v1/orders";

    private final List<String> color;

    public CreateOrderTests(List<String> color) {
        this.color = color;
    }

    @Parameterized.Parameters(name = "Цвет самоката: {0}")
    public static Collection<Object[]> getColors() {
        return Arrays.asList(new Object[][]{
                {Arrays.asList("BLACK")},
                {Arrays.asList("GREY")},
                {Arrays.asList("BLACK", "GREY")},
                {null}
        });
    }

    @Before
    public void setup() {
        RestAssured.baseURI = BASE_URI;
    }

    @Step("Создание нового заказа с цветом: {colors}")
    private void createNewOrder(List<String> colors) {
        Order order = new Order(
                "Naruto",
                "Uchiha",
                "Konoha, 142 apt.",
                4,
                "+7 800 355 35 35",
                5,
                "2020-06-06",
                "Saske, come back to Konoha",
                colors
        );

        Response response = sendCreateOrderRequest(order);

        validateOrderResponse(response);
    }

    @Step("Отправка запроса на создание заказа")
    private Response sendCreateOrderRequest(Order order) {
        return given()
                .contentType("application/json")
                .body(order)
                .when()
                .post(CREATE_ORDER_PATH)
                .then()
                .extract().response();
    }

    @Step("Проверка, что заказ создан успешно (код 201 и track не пустой)")
    private void validateOrderResponse(Response response) {
        response.then()
                .statusCode(201)
                .body("track", notNullValue());
    }

    @Test
    @DisplayName("Проверка создания заказа с различными вариантами цвета самоката")
    public void shouldCreateOrderWithDifferentColorOptions() {
        createNewOrder(color);
    }
}
