package hexlet.code.controllers;

import hexlet.code.dto.UrlCheckPage;
import hexlet.code.dto.UrlPage;
import hexlet.code.dto.UrlsPage;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.List;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.jsoup.Jsoup;


public class UrlController {

    public static void index(Context ctx) throws SQLException {
        var urls = UrlRepository.getUrls();
        var pageNumber = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        var per = 10;
        int skipCount = (pageNumber - 1) * per;

//        List<Url> pagedUrls = urls.stream()
//                .skip(skipCount)
//                .limit(per)
//                .collect(Collectors.toList());

        List<Url> pagedUrls = UrlRepository.getPagedUrls(per, skipCount);

        for (Url url: pagedUrls) {
            if (!UrlCheckRepository.getEntities(url.getId()).isEmpty()) {
                List<UrlCheck> checks = UrlCheckRepository.getEntities(url.getId());
                url.setUrlCheckList(checks);
            }
        }

        var page = new UrlsPage(pagedUrls, pageNumber);
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        ctx.render("urls/index.jte", Collections.singletonMap("page", page));
    }

    public static void create(Context ctx) throws SQLException {
        var name = ctx.formParam("url");
        String normalizedUrl = "";
        try {
            URL initialUrl = new URL(name);
            // The Authority part of the URL is the host name and the port of the URL.
            normalizedUrl = initialUrl.getProtocol() + "://" + initialUrl.getAuthority();
        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.redirect(NamedRoutes.ROOT_PATH);
            return;
        }

        Date currentDate = new Date();
        Timestamp createdAt = new Timestamp(currentDate.getTime());

        var checkedUrl = UrlRepository.find(normalizedUrl);
        if (checkedUrl.isPresent()) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.redirect(NamedRoutes.URLS_PATH);
        } else {
            var url = new Url(normalizedUrl, createdAt);

            UrlRepository.save(url);
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.redirect(NamedRoutes.URLS_PATH);
        }
    }

    public static void show(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();
        var displayedUrl = UrlRepository.find(id)
                .orElseThrow(() -> new NotFoundResponse("Page not found"));

//        if (url.isEmpty()) {
//            throw new NotFoundResponse("Page not found");
//        }

//        Url displayedUrl = url.get();
        var page = new UrlPage(displayedUrl);

        List<UrlCheck> checks = UrlCheckRepository.getEntities(id);

        var urlChecks = new UrlCheckPage(checks);
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        ctx.render("urls/show.jte", Map.of("page", page, "urlChecks", urlChecks));
    }

    public static void checkUrl(Context ctx) throws SQLException {

        var id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        var url = UrlRepository.find(id);

        if (url.isEmpty()) {
            throw new NotFoundResponse("Page not found");
        }

        HttpResponse<String> response = Unirest
                .get(url.get().getName())
                .asString();

        int status = response.getStatus();

        var document = Jsoup.parse(response.getBody());
        var title = document.title();
        String h1 = "";
        if (document.selectFirst("h1") != null) {
            h1 = document.selectFirst("h1").text();
        }

        String description = "";
        if (document.selectFirst("meta[name=description][content]") != null) {
            description = document.selectFirst("meta[name=description][content]").attr("content");
        }

        Date currentDate = new Date();
        Timestamp createdAt = new Timestamp(currentDate.getTime());

        UrlCheck urlCheck = new UrlCheck(status, h1, title, description, id, createdAt);
        UrlCheckRepository.save(urlCheck);

        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.redirect(NamedRoutes.showUrlPath(id));
    };

}
