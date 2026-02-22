# AndroidIAPP Godot Plugin (Русская версия)

AndroidIAPP — это [плагин](<https://docs.godotengine.org/en/stable/tutorials/plugins/editor/installing_plugins.html#installing-a-plugin>) для игрового движка Godot. Он предоставляет интерфейс для работы с Google Play Billing Library версии 8. Плагин поддерживает все публичные функции библиотеки, передает все коды ошибок и позволяет работать с различными тарифными планами подписок.

Простая игра для демонстрации работы покупок и подписок с различными тарифными планами: [Circle Catcher 2](https://play.google.com/store/apps/details?id=org.godotengine.circlecatcher)

## Возможности

- Подключение к Google Play Billing.
- Запрос покупок и данных о продуктах (Product Details).
- Совершение покупок и оформление подписок.
- Обновление, потребление (consume) и подтверждение (acknowledge) покупок.

## Установка

- Установите плагин через Godot [Asset Library](https://godotengine.org/asset-library/asset/3068).

  или

- Скачайте плагин с [GitHub](https://github.com/code-with-max/godot-google-play-iapp/releases).
- Поместите папку плагина в директорию `res://addons/` вашего проекта.

> [!NOTE]
> Не забудьте включить плагин в меню `Project > Project Settings > Plugins`.

## Перед началом

- **Типы покупок**:
  - Потребляемые продукты (`"inapp"`): Должны быть потреблены с помощью метода `consumePurchase`.
  - Непотребляемые продукты (`"inapp"`): Должны быть подтверждены с помощью метода `acknowledgePurchase`.
  - Подписки (`"subs"`): Должны быть подтверждены с помощью метода `acknowledgePurchase`.
  - ID продуктов и подписок должны передаваться в виде списка строк (Array of Strings), даже если это один ID (например, `["product_id"]`).

- **Формат возвращаемых данных**:
  - Все методы возвращают Godot `Dictionary` с ключами в snake_case (например, `getProductId` → `product_id`).
  - Примеры ответов: [in-app продукт](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_inapp.json), [подписка](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_subscription.json), [покупка](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/purchase_updated_inapp.json).
  - Ответы с ошибками содержат `response_code` (из [BillingResponseCode](https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode)) и `debug_message`.

- **Совместимость с Godot**: Протестировано на Godot 4.5. Kotlin coroutines не поддерживаются в Godot 4.2 и более ранних версиях.

## Отладка

- Убедитесь, что плагин активирован в `Project > Project Settings > Plugins`.
- Проверьте `AndroidIAPP.gd`, чтобы подтвердить правильный путь к AAR файлу:

  ```gdscript
  if debug:
      return PackedStringArray(["AndroidIAPP-debug.aar"])
  else:
      return PackedStringArray(["AndroidIAPP-release.aar"])
  ```

- Используйте logcat для проверки логов:

  ```shell
  ./adb logcat | grep IAPP
  ```

## Пример использования

Полный пример работы с плагином можно найти в файле [`billing_example.gd`](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/billing_example.gd).

Ниже приведено описание того, как инициализировать плагин и подключиться к его сигналам:

```gdscript
extends Node

const PLUGIN_NAME: String = "AndroidIAPP"
var billing = null

func _ready() -> void:
 if Engine.has_singleton(PLUGIN_NAME):
  billing = Engine.get_singleton(PLUGIN_NAME)

  # Подключение к сигналам
  billing.connected.connect(_on_connected)
  billing.disconnected.connect(_on_disconnected)
  billing.query_purchases.connect(_on_query_purchases)
  billing.query_purchases_error.connect(_on_query_purchases_error)
  billing.query_product_details.connect(_on_query_product_details)
  billing.query_product_details_error.connect(_on_query_product_details_error)
  billing.purchase_updated.connect(_on_purchase_updated)
  billing.purchase_cancelled.connect(_on_purchase_cancelled)
  billing.purchase_update_error.connect(_on_purchase_update_error)
  billing.purchase_consumed.connect(_on_purchase_consumed)
  billing.purchase_consumed_error.connect(_on_purchase_consumed_error)
  billing.purchase_acknowledged.connect(_on_purchase_acknowledged)
  billing.purchase_acknowledged_error.connect(_on_purchase_acknowledged_error)

  # Старт соединения
  if not billing.isReady():
   billing.startConnection()
 else:
  printerr("%s singleton not found" % PLUGIN_NAME)

func _on_connected() -> void:
 print("%s: Billing successfully connected" % PLUGIN_NAME)
 # Теперь можно запрашивать продукты и покупки
 billing.queryProductDetails(ITEM_ACKNOWLEDGED, "inapp")
 billing.queryPurchases("inapp")

# ... обработчики других сигналов
```

## Сигналы (Signals)

### Тестовый сигнал

- `helloResponse`: Вызывается при получении ответа на тестовое сообщение.
  - **Возвращает**: `String` (сообщение, переданное в `sayHello`, или ошибка, например `"Error: Activity is null"`).

### Информационные сигналы

- `startConnection`: Вызывается в момент начала процесса соединения с Google Play Billing.
  - **Возвращает**: Ничего.
- `connected`: Вызывается при успешном подключении.
  - **Возвращает**: Ничего.
- `disconnected`: Вызывается при разрыве соединения или если Android activity недоступна.
  - **Возвращает**: `Dictionary` (например, `{"debug_message": "Activity is null"}`).

### Сигналы биллинга

- `query_purchases`: Вызывается при успешном запросе списка покупок.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer (например, `BillingClient.BillingResponseCode.OK`).
    - `purchases_list`: Список (Array) словарей (см. [пример покупки](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/purchase_updated_inapp.json)).
- `query_purchases_error`: Вызывается при ошибке запроса покупок.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String (например, `"No purchase found"`).
    - `purchases_list`: null.
- `query_product_details`: Вызывается при успешном получении данных о продуктах.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `product_details_list`: Список словарей (см. [пример для in-app](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_inapp.json), [пример для подписок](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_subscription.json)).
- `query_product_details_error`: Вызывается при ошибке запроса данных о продуктах.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String.
- `purchase_updated`: Вызывается при обновлении информации о покупке.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `purchases_list`: Список словарей (например, `{"product_id": "blue_skin_v1", "purchase_token": "...", "is_acknowledged": false}`).
- `purchase`: Вызывается при успешном запуске флоу покупки.
  - **Возвращает**: `Dictionary`
- `purchase_error`: Вызывается при возникновении ошибки во время покупки.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String (например, `"Activity is null"`, `"Product ID list is empty"`).
    - `product_id`: String (если применимо).
    - `base_plan_id`: String (для подписок).
- `purchase_cancelled`: Вызывается, если покупка была отменена пользователем.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer (например, `BillingClient.BillingResponseCode.USER_CANCELED`).
    - `debug_message`: String.
- `purchase_update_error`: Вызывается при ошибке обновления покупки.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String.
- `purchase_consumed`: Вызывается после успешного потребления покупки.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `purchase_token`: String.
- `purchase_consumed_error`: Вызывается при ошибке потребления.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String.
    - `purchase_token`: String.
- `purchase_acknowledged`: Вызывается после успешного подтверждения покупки.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `purchase_token`: String.
- `purchase_acknowledged_error`: Вызывается при ошибке подтверждения.
  - **Возвращает**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String.
    - `purchase_token`: String.
- `billing_info`: Информационный сигнал для отладки.
  - **Возвращает**: `Dictionary`
    - `plugin_name`: String.
    - `fun_name`: String (например, `"sayHello"`, `"startConnection"`).
    - `debug_message`: String.
- `price_change_acknowledged`: **Не реализовано.**
- `price_change_error`: **Не реализовано.**
- `in_app_message_result`: **Не реализовано.**
- `alternative_billing_only_transaction_reported`: **Не реализовано.**

## Функции (Functions)

`startConnection()`: Инициирует подключение к Google Play Billing.

- Генерирует сигналы: `startConnection`, `connected` или `disconnected` (если activity недоступна).
- **Внимание**: Убедитесь, что плагин инициализируется после того, как Android activity в Godot станет доступна.

`endConnection()`: Завершает соединение с сервисом Google Play Billing.

`isReady()`: Проверяет, готово ли соединение с биллингом.

- **Возвращает**: `bool`.

`sayHello(says: String)`: Отправляет тестовое сообщение.

- Генерирует сигнал: `helloResponse`.
- Отображает Toast-сообщение в Android и пишет в консоль.
- **Внимание**: Может завершиться ошибкой `"Error: Activity is null"`, если метод вызван слишком рано. Не используйте в продакшене.

`queryPurchases(productType: String, includeSuspended: bool)`: Запрашивает текущие покупки.

- `productType`: `"inapp"` или `"subs"`.
- `includeSuspended`: Включать ли приостановленные подписки.
- Генерирует сигналы: `query_purchases` или `query_purchases_error`.

`queryProductDetails(listOfProductsIDs: Array<String>, productType: String)`: Запрашивает подробную информацию о продуктах или подписках.

- `listOfProductsIDs`: Список ID продуктов/подписок (не должен быть пустым).
- `productType`: `"inapp"` или `"subs"`.
- Генерирует сигналы: `query_product_details` или `query_product_details_error`.

`purchase(listOfProductsIDs: Array<String>, isOfferPersonalized: bool)`: Запускает процесс покупки продукта.

- `listOfProductsIDs`: Список ID продуктов (не должен быть пустым).
- `isOfferPersonalized`: Установите `false`, если только вы не следуете [директиве ЕС](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:02011L0083-20220528) (подробнее в [документации Android](https://developer.android.com/google/play/billing/integrate#personalized-price)).
- Генерирует сигналы: `purchase_updated`, `purchase_error`, `purchase_cancelled`, `purchase_update_error` или `query_product_details_error`.
- **Важно**: Вы должны вызвать `consumePurchase` или `acknowledgePurchase` для завершения транзакции.

`subscribe(listOfProductsIDs: Array<String>, basePlanIDs: Array<String>, offerIDs: Array<String>, isOfferPersonalized: bool)`: Запускает процесс оформления подписки.

- `listOfProductsIDs`, `basePlanIDs`: Списки ID (не должны быть пустыми).
- `offerIDs`: Список с одним ID оффера (может быть пустым массивом, если оффер не применяется).
- `isOfferPersonalized`: Установите `false`, если не требуется соблюдение директивы ЕС.
- Генерирует сигналы: `purchase_updated`, `purchase_error`, `purchase_cancelled`, `purchase_update_error` или `query_product_details_error`.
- **Важно**: Вы должны вызвать `acknowledgePurchase` для завершения оформления подписки.

`updateSubscription(listOfProductsIDs: Array<String>, basePlanIDs: Array<String>, offerIDs: Array<String>, isOfferPersonalized: bool, oldPurchaseToken: String, oldProductID: String, replacementMode: int)`: Обновляет существующую подписку (апгрейд или даунгрейд).

- `oldPurchaseToken`: Токен текущей активной подписки.
- `oldProductID`: ID продукта текущей активной подписки.
- `replacementMode`: Одно из значений `ReplacementMode` (например, `CHARGE_FULL_PRICE`, `WITHOUT_PRORATION`).
- Генерирует сигналы: `purchase_updated`, `purchase_error`, `purchase_cancelled` или `purchase_update_error`.

`consumePurchase(purchaseToken: String)`: Потребляет покупку (для многоразовых товаров).

- `purchaseToken`: Токен из ответа `purchase_updated`.
- Генерирует сигналы: `purchase_consumed` или `purchase_consumed_error`.

`acknowledgePurchase(purchaseToken: String)`: Подтверждает покупку или подписку.

- `purchaseToken`: Токен из ответа `purchase_updated`.
- Генерирует сигналы: `purchase_acknowledged` или `purchase_acknowledged_error`.

### Не реализованные функции

Следующие функции объявлены в плагине как заглушки и еще не реализованы:

- `showInAppMessages()`
- `launchPriceChangeConfirmationFlow(productDetails: Dictionary)`
- `createAlternativeBillingOnlyReportingDetails()`
- `reportAlternativeBillingOnlyTransaction(reportingDetails: Dictionary)`

## Руководство по реализации

### 1. Запрос доступных продуктов

После подключения следует запросить данные о продуктах, настроенных в Google Play Console.

```gdscript
const ITEM_CONSUMABLE: Array = ["additional_life_v1"]
const ITEM_NON_CONSUMABLE: Array = ["red_skin_v1", "blue_skin_v1"]
const SUBSCRIPTIONS: Array = ["remove_ads_sub_01"]

func _on_connected() -> void:
    print("%s: Billing успешно подключен" % PLUGIN_NAME)
    # Запрос данных для различных типов продуктов
    billing.queryProductDetails(ITEM_CONSUMABLE, "inapp")
    billing.queryProductDetails(ITEM_NON_CONSUMABLE, "inapp")
    billing.queryProductDetails(SUBSCRIPTIONS, "subs")

func _on_query_product_details(response: Dictionary) -> void:
    for product in response["product_details_list"]:
        # Сохранение данных о продукте для отображения в игровом магазине
        G.product_showcase.append(product)
    product_showcase_updated.emit()
```

### 2. Инициация покупки

Для начала покупки вызовите функцию `purchase` или `subscribe` с соответствующими ID продукта и базового плана.

```gdscript
# Для разового товара (потребляемого или непотребляемого)
func do_purchase(product_id: String):
    billing.purchase([product_id], false)

# Для подписки
func do_subscription(subscription_id: String, base_plan_id: String):
    billing.subscribe([subscription_id], [base_plan_id], [], false)
```

### 3. Обработка покупок

Сигнал `purchase_updated` является основным местом обработки всех новых покупок. Вам нужно решить, подтверждать (acknowledge) или потреблять (consume) товар.

```gdscript
func _on_purchase_updated(response: Dictionary) -> void:
    for purchase in response["purchases_list"]:
        process_purchase(purchase)

func process_purchase(purchase: Dictionary) -> void:
    for product_id in purchase["products"]:
        if purchase["purchase_state"] != 1: # Не PURCHASED
            print("Покупка в ожидании для: %s" % product_id)
            return

        if product_id in ITEM_NON_CONSUMABLE or product_id in SUBSCRIPTIONS:
            # Подтверждение непотребляемых товаров и подписок
            if not purchase["is_acknowledged"]:
                billing.acknowledgePurchase(purchase["purchase_token"])
            else:
                # Предоставление контента (уже подтверждено)
                print("Покупка уже подтверждена: %s" % product_id)

        elif product_id in ITEM_CONSUMABLE:
            # Потребление (для многоразовых товаров)
            billing.consumePurchase(purchase["purchase_token"])
```

### 4. Обработка подтвержденных и потребленных покупок

Используйте соответствующие сигналы, чтобы окончательно выдать пользователю купленные товары.

```gdscript
func _on_purchase_consumed(response: Dictionary) -> void:
    print("Покупка потреблена: %s" % response["purchase_token"])
    # Выдача потребляемого предмета (например, добавление жизни)
    G.increase_lives()

func _on_purchase_acknowledged(response: Dictionary) -> void:
    print("Покупка подтверждена: %s" % response["purchase_token"])
    # Разблокировка функции или контента
    G.unlock_skin()
```

### 5. Восстановление покупок

Чтобы восстановить покупки (например, после переустановки приложения), выполните запрос текущих владений.

```gdscript
func _on_connected() -> void:
    # ... запрос данных о продуктах ...

    # Запрос существующих покупок
    billing.queryPurchases("inapp")
    billing.queryPurchases("subs")

func _on_query_purchases(response: Dictionary) -> void:
    for purchase in response["purchases_list"]:
        process_purchase(purchase) # Используйте ту же логику обработки
```

## Распространенные ошибки

- `"Activity is null"`: Android activity недоступна. Убедитесь, что методы плагина вызываются после полной инициализации Godot.
- `"Product ID list is empty"`: Список `product_id` или `base_plan_id` пуст. Всегда передавайте непустые массивы.
- `"Base Plan ID not found"`: `base_plan_id` не совпадает ни с одним базовым планом в Google Play Console.
- `"No product details found"`: Неверный ID или тип продукта. Проверьте ID в Google Play Console.
- `"Purchase token is blank"`: Токен покупки пуст или недействитеен. Проверьте ответ `purchase_updated`.
