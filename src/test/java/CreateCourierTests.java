import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.example.Courier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.Random;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CreateCourierTests {
    private static final String BASE_URI = "https://qa-scooter.praktikum-services.ru";
    private static final String COURIER_PATH = "/api/v1/courier";
    private static final String LOGIN_PATH = "/api/v1/courier/login";

    private Courier testCourier;
    private String testLogin;

    @Step("Генерация случайного логина")
    private String randomLogin() {
        return "courier_" + new Random().nextInt(100000);
    }

    @Before
    @Step("Подготовка тестовых данных")
    public void setup() {
        RestAssured.baseURI = BASE_URI;
        testLogin = randomLogin();
        testCourier = new Courier(testLogin, "12345", "Ivan");
    }

    @After
    @Step("Очистка тестовых данных")
    public void cleanup() {
        if (testLogin != null) {
            authorizeAndDeleteCourier(testLogin);
        }
    }

    @Step("Авторизация и удаление курьера (логин: {login})")
    private void authorizeAndDeleteCourier(String login) {
        Response loginResponse = given()
                .contentType("application/json")
                .body(new Courier(login, "12345", null))
                .post(LOGIN_PATH);

        if (loginResponse.statusCode() == 200) {
            int courierId = loginResponse.jsonPath().getInt("id");
            deleteCourierById(courierId);
        }
    }

    @Step("Удаление курьера (ID: {courierId})")
    private void deleteCourierById(int courierId) {
        given()
                .contentType("application/json")
                .delete(COURIER_PATH + "/" + courierId)
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Проверка успешного создания курьера с валидными данными")
    @Step("Создание курьера с валидными данными")
    public void createCourierWithValidDataShouldSucceed() {
        given()
                .contentType("application/json")
                .body(testCourier)
                .when()
                .post(COURIER_PATH)
                .then()
                .statusCode(201)
                .body("ok", equalTo(true));
    }

    @Test
    @DisplayName("Проверка невозможности создания дубликата курьера")
    @Step("Попытка создания дубликата курьера")
    public void createDuplicateCourierShouldFail() {
        createTestCourier(testCourier);

        given()
                .contentType("application/json")
                .body(testCourier)
                .when()
                .post(COURIER_PATH)
                .then()
                .statusCode(409)
                .body("message", equalTo("Этот логин уже используется. Попробуйте другой."));
    }

    @Step("Создание тестового курьера")
    private void createTestCourier(Courier courier) {
        given()
                .contentType("application/json")
                .body(courier)
                .post(COURIER_PATH)
                .then()
                .statusCode(201);
    }

    @Test
    @DisplayName("Проверка валидации обязательных полей: отсутствие логина")
    @Step("Попытка создания курьера без логина")
    public void createCourierWithoutLoginShouldFail() {
        given()
                .contentType("application/json")
                .body(new Courier(null, "12345", "Ivan"))
                .when()
                .post(COURIER_PATH)
                .then()
                .statusCode(400)
                .body("message", equalTo("Недостаточно данных для создания учетной записи"));
    }

    @Test
    @DisplayName("Проверка валидации обязательных полей: отсутствие пароля")
    @Step("Попытка создания курьера без пароля")
    public void createCourierWithoutPasswordShouldFail() {
        given()
                .contentType("application/json")
                .body(new Courier(randomLogin(), null, "Ivan"))
                .when()
                .post(COURIER_PATH)
                .then()
                .statusCode(400)
                .body("message", equalTo("Недостаточно данных для создания учетной записи"));
    }
}