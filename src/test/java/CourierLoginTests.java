import io.qameta.allure.junit4.DisplayName;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.example.Courier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

public class CourierLoginTests {

    private Courier courier;
    private static final String BASE_URI = "https://qa-scooter.praktikum-services.ru";
    private static final String COURIER_PATH = "/api/v1/courier";
    private static final String LOGIN_PATH = "/api/v1/courier/login";

    @Before
    @Step("Подготовка тестовых данных - создание курьера")
    public void setup() {
        RestAssured.baseURI = BASE_URI;
        courier = createTestCourier();
    }

    @Step("Создание тестового курьера")
    private Courier createTestCourier() {
        Courier newCourier = new Courier(
                "ninja_" + System.currentTimeMillis(),
                "12345",
                "TestCourier"
        );

        Response createResponse = given()
                .header("Content-type", "application/json")
                .body(newCourier)
                .post(COURIER_PATH);

        assertEquals("Неверный статус-код при создании курьера",
                201, createResponse.statusCode());
        assertTrue("Поле 'ok' должно быть true",
                createResponse.jsonPath().getBoolean("ok"));

        return newCourier;
    }

    @After
    @Step("Очистка тестовых данных - удаление курьера")
    public void cleanup() {
        deleteTestCourier(courier);
    }

    @Step("Удаление тестового курьера")
    private void deleteTestCourier(Courier courier) {
        Response loginResponse = attemptCourierLogin(courier);

        if (loginResponse.statusCode() == 200) {
            int courierId = loginResponse.jsonPath().getInt("id");
            Response deleteResponse = given()
                    .header("Content-type", "application/json")
                    .delete(COURIER_PATH + "/" + courierId);

            assertEquals("Неверный статус-код при удалении курьера",
                    200, deleteResponse.statusCode());
        }
    }

    @Step("Попытка авторизации курьера")
    private Response attemptCourierLogin(Courier courier) {
        return given()
                .header("Content-type", "application/json")
                .body(new Courier(courier.getLogin(), courier.getPassword(), null))
                .post(LOGIN_PATH);
    }

    @Test
    @DisplayName("Позитивный тест: успешная авторизация")
    @Step("Проверка авторизации с валидными данными")
    public void successfulLoginWithValidCredentials() {
        Response response = attemptCourierLogin(courier);

        verifySuccessfulLoginResponse(response);
    }

    @Step("Проверка успешного ответа авторизации")
    private void verifySuccessfulLoginResponse(Response response) {
        assertEquals("Неверный статус-код при авторизации",
                200, response.statusCode());
        assertNotNull("ID курьера не должен быть null",
                response.jsonPath().get("id"));
    }

    @Test
    @DisplayName("Негативный тест: неверный пароль")
    @Step("Проверка авторизации с неверным паролем")
    public void loginWithInvalidPasswordShouldFail() {
        Courier invalidCourier = new Courier(
                courier.getLogin(),
                "wrong_password",
                null
        );

        Response response = attemptCourierLogin(invalidCourier);
        verifyFailedLoginResponse(response, 404, "Учетная запись не найдена");
    }

    @Test
    @DisplayName("Негативный тест: отсутствие логина")
    @Step("Проверка авторизации без логина")
    public void loginWithoutLoginShouldFail() {
        Response response = given()
                .header("Content-type", "application/json")
                .body(new Courier(null, courier.getPassword(), null))
                .post(LOGIN_PATH);

        verifyFailedLoginResponse(response, 400, "Недостаточно данных для входа");
    }

    @Test
    @DisplayName("Негативный тест: пустые данные")
    @Step("Проверка авторизации с пустыми данными")
    public void loginWithEmptyCredentialsShouldFail() {
        Response response = given()
                .header("Content-type", "application/json")
                .body(new Courier("", "", null))
                .post(LOGIN_PATH);

        verifyFailedLoginResponse(response, 400, "Недостаточно данных для входа");
    }

    @Test
    @DisplayName("Негативный тест: несуществующий пользователь")
    @Step("Проверка авторизации несуществующего пользователя")
    public void loginNonExistentCourierShouldFail() {
        Courier nonExistentCourier = new Courier(
                "nonexistent_" + System.currentTimeMillis(),
                "12345",
                null
        );

        Response response = attemptCourierLogin(nonExistentCourier);
        verifyFailedLoginResponse(response, 404, "Учетная запись не найдена");
    }

    @Step("Проверка неудачной авторизации (ожидаемый статус: {expectedStatus}, сообщение: {expectedMessage})")
    private void verifyFailedLoginResponse(Response response, int expectedStatus, String expectedMessage) {
        assertEquals("Неверный статус-код при авторизации",
                expectedStatus, response.statusCode());
        assertEquals("Неверное сообщение об ошибке",
                expectedMessage, response.jsonPath().getString("message"));
    }
}