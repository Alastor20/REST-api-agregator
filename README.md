# REST API Aggregator

Многопоточный агрегатор данных из трёх открытых REST API. Ядро первого этапа
переиспользуется, управление выполняется через собственный REST API на Spring Boot.

## Выбранные API

| Имя        | Endpoint                                | Параметры                                  | Особенности ответа                                                                   |
|------------|-----------------------------------------|--------------------------------------------|--------------------------------------------------------------------------------------|
| `weather`  | `api.open-meteo.com/v1/forecast`        | `latitude`, `longitude`, `current_weather` | вложенный объект `current_weather`                                                   |
| `currency` | `https://api.frankfurter.dev/v1/latest` | `from`, `to`                               | вложенный объект `rates`                                                             |
| `books`    | `openlibrary.org/search.json`           | `q`, `limit`, `page`                       | массив `docs[]`, вложенные массивы `author_name`, `language`, `subject`, `publisher` |

Ни один из них не требует API-ключа.

## Сборка и запуск

```bash
./gradlew clean build
java -jar build/libs/api-aggregator.jar
```

Приложение слушает `http://localhost:8080`.

Альтернативный запуск в режиме разработки:

```bash
./gradlew bootRun
```

## Конфигурация

Настройки находятся в `src/main/resources/application.yml`.

```yaml
app:
  polling:
    n: 3            # максимальное число одновременно выполняемых задач
    t: 10           # интервал повторного опроса задачи, секунды
  storage:
    format: json    # json | csv
    file: data/results.json
    mode: create    # create | append
  sources:
    weather:
      params:
        - latitude: "52.52"
          longitude: "13.41"
          current_weather: "true"
        - latitude: "48.85"
          longitude: "2.35"
          current_weather: "true"
    currency:
      params:
        - from: USD
          to: EUR
        - from: USD
          to: GBP
    books:
      params:
        - q: "harry potter"
          limit: "5"
        - q: "lord of the rings"
          limit: "5"
```

Для каждого API задано минимум два набора параметров. Если в запросе
`POST /polling/start` для источника передан свой `params`, он полностью заменяет
набор из конфигурации.

### Параметры `n` и `t`

- `n` — размер пула потоков; одновременно выполняется не более `n` задач.
- `t` — пауза после завершения предыдущего опроса той же задачи (тот же API и те
  же параметры). Реализовано через `scheduleWithFixedDelay`, поэтому следующий
  запуск происходит не раньше чем через `t` секунд после завершения предыдущего.
- Если задач больше, чем `n`, фактический интервал может быть больше `t`.

## REST API

### POST /polling/start

Запуск опроса. Все поля тела необязательны, отсутствующие берутся из
`application.yml`.

```json
{
  "sources": [
    {
      "name": "weather",
      "params": [
        {"latitude": "52.52", "longitude": "13.41", "current_weather": "true"},
        {"latitude": "48.85", "longitude": "2.35", "current_weather": "true"}
      ]
    }
  ],
  "format": "json",
  "mode": "create",
  "file": "data/custom.json"
}
```

Примеры:

```bash
# запуск с настройками по умолчанию
curl -X POST http://localhost:8080/polling/start \
  -H "Content-Type: application/json" -d "{}"

# запуск с переопределением источников, формата и файла
curl -X POST http://localhost:8080/polling/start \
  -H "Content-Type: application/json" \
  -d '{"sources":[{"name":"currency","params":[{"from":"USD","to":"JPY"}]}],"format":"csv","mode":"create","file":"data/currency.csv"}'
```

Ответы:

- `200` — опрос запущен;
- `400` — неверные параметры (неизвестный источник, пустой набор параметров,
  неверный формат/режим и т.п.);
- `409` — опрос уже запущен.

### POST /polling/stop

Остановка опроса. Приложение отменяет запланированные задачи, дожидается
завершения уже выполняющихся и возвращает управление; данные сохраняются.

```bash
curl -X POST http://localhost:8080/polling/stop
```

- `200` — опрос остановлен;
- `409` — опрос не запущен.

### GET /polling/status

```bash
curl http://localhost:8080/polling/status
```

Пример ответа:

```json
{
  "running": true,
  "activeSources": {
    "weather": [
      {"latitude": "52.52", "longitude": "13.41", "current_weather": "true"}
    ]
  },
  "n": 3,
  "t": 10,
  "startedAt": "2026-11-06T10:00:00Z",
  "format": "json",
  "file": "data/results.json",
  "mode": "create"
}
```

### GET /data

Содержимое файла результатов. Необязательный параметр `source` фильтрует записи
по имени API.

```bash
curl http://localhost:8080/data
curl "http://localhost:8080/data?source=weather"
```