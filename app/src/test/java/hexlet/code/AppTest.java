package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.Javalin;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;


import static org.assertj.core.api.Assertions.assertThat;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;

public final class AppTest {


    private static Javalin app;
    private static String baseUrl;
    private static MockWebServer server;

    @BeforeAll
    public static void initApp() throws SQLException, IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
    }

    @BeforeEach
    public void initBase() throws SQLException {
        var url = new Url("https://abr.com", Timestamp.valueOf("2022-09-08 21:16:28.105"));
        UrlRepository.save(url);
        var urlCheck = new UrlCheck(200, "h1", "title", "description", 1, Timestamp.valueOf("2023-01-01 21:16:28.105"));
        UrlCheckRepository.save(urlCheck);
    }

    @BeforeEach
    public void initMockServer() throws IOException {
        server = new MockWebServer();
        String path = "src/test/resources/test.html";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        String page = Files.readString(Paths.get(absolutePath));

        MockResponse mockedResponse = new MockResponse()
                .setBody(page);
        server.enqueue(mockedResponse);
        server.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        server.shutdown();
        app.stop();
    }

    class RootTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Анализатор страниц");
        }
    }

    @Nested
    class UrlTest {

        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("https://abr.com");
            assertThat(body).contains("2023-01-01 21:16:28.105");
            assertThat(body).contains("Previous");
            assertThat(body).contains("Next");
        }

        @Test
        void testShow() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/1")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("https://abr.com");
            assertThat(body).contains("200");
            assertThat(body).contains("description");
            assertThat(body).contains("h1");
            assertThat(body).contains("title");
            assertThat(body).contains("2022-09-08 21:16:28.105");
            assertThat(body).contains("2023-01-01 21:16:28.105");
        }


        @Test
        void testCreateNew() throws MalformedURLException, SQLException {
            String inputName = "https://www.google.com/search?q";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputName)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            URL inputUrl = new URL(inputName);
            String normalizedName = inputUrl.getProtocol() + "://" + inputUrl.getAuthority();
            assertThat(UrlRepository.find(normalizedName).get().toString()).isEqualTo(normalizedName);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(normalizedName);
            //assertThat(body).contains("Страница успешно добавлена");

        }

        @Test
        void testCreateExisting() {
            String inputName = "https://abr.com";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputName)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            String body = responsePost.getBody();
            assertThat(body.contains("Страница уже существует"));
        }

        @Test
        void testCreateNonValid() {
            String inputName = "asdfg";
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputName)
                    .asEmpty();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> response = Unirest
                    .get(baseUrl)
                    .asString();
            String body = response.getBody();

            assertThat(body).contains("Некорректный URL");
        }

        @Test
        void testcheckUrl() throws IOException, SQLException {

            String testUrl = server.url("/").toString();

            HttpResponse response = Unirest
                    .post(baseUrl + "/urls/")
                    .field("url", testUrl)
                    .asEmpty();


            var url1 = UrlRepository.find(testUrl.substring(0, testUrl.length() - 1));

            assertThat(url1.isPresent());

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls/" + url1.get().getId() + "/checks")
                    .asString();

            String body = responsePost.getBody();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls/" + url1.get().getId());
            assertThat(body.contains("Страница успешно проверена"));

            String body1 = Unirest
                    .get(baseUrl + "/urls/" + url1.get().getId())
                    .asString()
                    .getBody();

            assertThat(body1).contains("200");
            assertThat(body1).contains("title text");
            assertThat(body1).contains("description text");
            assertThat(body1).contains("h1 text");
        }
    }
}

