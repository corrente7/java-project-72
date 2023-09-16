@import hexlet.code.dto.UrlsPage

@param UrlsPage page

@template.layout.page(
page = page,
header = @`
    <section>
        <h2>Сайты</h2>
    </section>
`,
content = @`
    <section>
        <a class="btn btn-primary" href="/">Проверить сайт</a>
        <table class="table table-striped">
        <thead>
        <tr>
            <th class="col-1">ID</th>
            <th class="col-3">Адрес</th>
            <th class="col-2">Последняя проверка</th>
            <th class="col-2">Код ответа</th>
        </tr>
        </thead>
            @for(var url : page.getUrls())
                <tbody>
                <tr>
                    <td class="col-1">${url.getId()}</td>
                    <td class="col-3">
                        <a href="/urls/${url.getId()}">${url.getName()}</a>
                    </td>
                    @if (url.getUrlCheckList() == null)
                    <td class="col-2"></td>
                        <td class="col-2"></td>
                        @else
                        <td class="col-2">${url.getUrlCheckList().stream().reduce((a, a1) -> a1).get().getCreatedAt().toString()}</td>
                        <td class="col-2">${url.getUrlCheckList().stream().reduce((a, a1) -> a1).get().getStatusCode()}</td>
                    @endif
                        </tr>
                </tbody>
            @endfor
            </table>
    </section>
            <nav aria-label="Page navigation">
                <ul class="pagination justify-content-center mt-5">
                    <li class="page-item"><a class="page-link" href="/urls?page=${page.getPageNumber() - 1}">Previous</a></li>
                    <li class="page-item"><a class="page-link" href="/urls?page=${page.getPageNumber()}">${page.getPageNumber()}</a></li>
                    <li class="page-item"><a class="page-link" href="/urls?page=${page.getPageNumber() + 1}">Next</a></li>
                </ul>
            </nav>
`
)