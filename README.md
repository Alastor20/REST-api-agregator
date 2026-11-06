# REST API Aggregator (Этап 1)

Консольное однопоточное Java-приложение, агрегирующее данные из трёх
открытых REST API в единую модель с сохранением в CSV/JSON.

## Выбранные API

| Имя        | Endpoint                                | Параметры                                  | Особенности ответа                                                                   |
|------------|-----------------------------------------|--------------------------------------------|--------------------------------------------------------------------------------------|
| `weather`  | `api.open-meteo.com/v1/forecast`        | `latitude`, `longitude`, `current_weather` | вложенный объект `current_weather`                                                   |
| `currency` | `https://api.frankfurter.dev/v1/latest` | `from`, `to`                               | вложенный объект `rates`                                                             |
| `books`    | `openlibrary.org/search.json`           | `q`, `limit`, `page`                       | массив `docs[]`, вложенные массивы `author_name`, `language`, `subject`, `publisher` |

Ни один из них не требует API-ключа.

## Сборка

```bash
./gradlew clean build
```

Результат сборки:

- `build/libs/api-aggregator-1.0.0.jar` — обычный jar
- `build/libs/api-aggregator-1.0.0-all.jar` — fat-jar с зависимостями (используйте его для запуска)

## Запуск

### Опрос и запись

```bash
# создать файл
java -jar build/libs/api-aggregator-1.0.0-all.jar poll \
    --apis books \
    --format json --mode create --file data.json

# дозаписать в существующий
java -jar build/libs/api-aggregator-1.0.0-all.jar poll \
    --apis weather --format json --mode append --file data.json

# Переопределение query-параметров источника
java -jar build/libs/api-aggregator-1.0.0-all.jar poll \
    --apis weather --format csv --mode create --file paris.csv \
    --query weather.latitude=48.85 --query weather.longitude=2.35

# Пример с books: другой запрос и больше результатов
java -jar build/libs/api-aggregator-1.0.0-all.jar poll \
    --apis books --format json --mode create --file lotr.json \
    --query books.q=lord+of+the+rings --query books.limit=10
```

### Вывод содержимого файла

```bash
java -jar build/libs/api-aggregator-1.0.0-all.jar show --file data.json
java -jar build/libs/api-aggregator-1.0.0-all.jar show --file data.csv --source books
```

### Параметры CLI

Команда `poll`:

| Параметр   | Обязателен | Описание                                                                                        |
|------------|------------|-------------------------------------------------------------------------------------------------|
| `--apis`   | да         | Список API через запятую: `weather,currency,books`                                              |
| `--format` | да         | `csv` или `json`                                                                                |
| `--mode`   | да         | `create` (перезаписать) или `append` (дописать)                                                 |
| `--file`   | да         | Путь к выходному файлу                                                                          |
| `--query`  | нет        | Переопределение параметров источника, формат `source.key=value` (можно указывать несколько раз) |

Команда `show`:

| Параметр   | Обязателен | Описание                  |
|------------|------------|---------------------------|
| `--file`   | да         | Путь к файлу результатов  |
| `--format` | да         | `csv` или `json`          |
| `--source` | нет        | Фильтр по имени источника |

