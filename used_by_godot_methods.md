# AndroidIAPP Plugin: Godot API Reference

Этот файл содержит описание всех методов и сигналов, предоставляемых плагином `AndroidIAPP` для работы в Godot с Google Play Billing Library v9.

---

## 🛠 Методы (UsedByGodot)

Все методы вызываются через объект плагина, полученный с помощью `Engine.get_singleton("AndroidIAPP")`, либо через обертку `GooglePlayBilling.gd`.

### `isReady()` (Property/Getter)
Возвращает `true`, если `BillingClient` инициализирован и готов к работе.
- **Возвращает**: `bool`

### `getConnectionState()`
Возвращает текущее состояние подключения BillingClient (`0` = DISCONNECTED, `1` = CONNECTING, `2` = CONNECTED, `3` = CLOSED).
- **Возвращает**: `int`

### `isFeatureSupported(feature: String)`
Проверяет поддержку конкретной функции Google Play Billing (например, `"subscriptions"`, `"priceChangeConfirmation"`).
- **Аргументы**:
  - `feature` (String): Название функции.
- **Возвращает**: `Dictionary` с ключами `response_code` и `debug_message`.

### `sayHello(says: String)`
Выводит Toast-сообщение в Android и отправляет сигнал `helloResponse`.
- **Аргументы**:
  - `says` (String): Текст сообщения. По умолчанию: "Hello from AndroidIAPP plugin".

### `startConnection()`
Инициализирует `BillingClient` и устанавливает соединение с Google Play.

### `endConnection()`
Закрывает соединение с Google Play.

### `getBillingConfig()`
Запрашивает конфигурацию биллинга пользователя (например, код страны).
- **Сигнал**: `billing_config_response` (`billing_config_received` в `GooglePlayBilling.gd`).

### `queryPurchases(productType: String, includeSuspended: bool)`
Запрашивает список текущих покупок (активные подписки или непотребленные in-app товары).
- **Аргументы**:
  - `productType` (String): `"inapp"` или `"subs"`.
  - `includeSuspended` (bool): Включать ли приостановленные подписки.
- **Сигналы**: `query_purchases` (успех), `query_purchases_error` (ошибка).

### `queryProductDetails(listOfProductsIDs: Array[String], productType: String)`
Запрашивает информацию о товарах (название, описание, цена). **Обязательно вызвать перед покупкой**, чтобы закэшировать `ProductDetails`.
- **Аргументы**:
  - `listOfProductsIDs` (Array[String]): Список ID товаров.
  - `productType` (String): `"inapp"` или `"subs"`.
- **Сигналы**: `query_product_details`, `query_product_details_error`.

### `purchase(listOfProductsIDs: Array[String], isOfferPersonalized: bool, offerToken: String)`
Запускает процесс покупки разового товара (INAPP).
- **Аргументы**:
  - `listOfProductsIDs` (Array[String]): Массив с ID товара (берется первый элемент).
  - `isOfferPersonalized` (bool): Персонализированное предложение (требование EU).
  - `offerToken` (String): Токен конкретного оффера (необязательно).

### `subscribe(listOfProductsIDs: Array[String], basePlanIDs: Array[String], offerIDs: Array[String], isOfferPersonalized: bool)`
Запускает процесс оформления подписки.
- **Аргументы**:
  - `listOfProductsIDs`: Массив с ID подписки.
  - `basePlanIDs`: Массив с ID базового плана.
  - `offerIDs`: Массив с ID оффера (можно пустой).
  - `isOfferPersonalized`: Персонализированное предложение.

### `updateSubscription(...)`
Обновление существующей подписки (апгрейд/даунгрейд).
- **Аргументы**: `listOfProductsIDs`, `basePlanIDs`, `offerIDs`, `isOfferPersonalized`, `oldPurchaseToken`, `oldProductID`, `replacementMode` (Int).

### `consumePurchase(purchaseToken: String)`
Потребляет купленный разовый товар, позволяя купить его снова.
- **Аргументы**: `purchaseToken` (String).
- **Сигналы**: `purchase_consumed`, `purchase_consumed_error`.

### `acknowledgePurchase(purchaseToken: String)`
Подтверждает покупку. **Необходимо вызвать в течение 3 дней**, иначе Google Play вернет деньги пользователю.
- **Аргументы**: `purchaseToken` (String).
- **Сигналы**: `purchase_acknowledged`, `purchase_acknowledged_error`.

### `showInAppMessages()`
Отображает системные In-App сообщения Google Play (например, проблемы с оплатой/истёкшей картой).
- **Сигнал**: `in_app_message_result`.

### Альтернативный биллинг и сторонние предложения (v8/v9)
- `isAlternativeBillingOnlyAvailable()`: Проверка доступности Alternative Billing Only (`alternative_billing_only_availability_response`).
- `createAlternativeBillingOnlyReportingDetails()`: Создание деталей отчетности Alternative Billing Only (`alternative_billing_only_reporting_details_response`).
- `showAlternativeBillingOnlyInformationDialog()`: Отображение информационного диалога Alternative Billing Only (`alternative_billing_only_information_dialog_response`).
- `isExternalOfferAvailable()`: Проверка доступности External Offer (`external_offer_availability_response`).
- `createExternalOfferReportingDetails()`: Создание деталей отчетности External Offer (`external_offer_reporting_details_response`).
- `showExternalOfferInformationDialog()`: Отображение информационного диалога External Offer (`external_offer_information_dialog_response`).

### Billing Program (v8/v9)
- `isBillingProgramAvailable(programType: Int)`: Проверка доступности Billing Program (`billing_program_availability_response`).
- `createBillingProgramReportingDetails(programType: Int, developerBillingType: Int)`: Создание деталей отчетности Billing Program (`billing_program_reporting_details_response`).
- `showBillingProgramInformationDialog(programType: Int, externalTransactionToken: String)`: Показ диалога Billing Program (`billing_program_information_dialog_response`).
- `getBillingChoiceInfo(programType: Int)`: Получение информации о выборе способа оплаты (`billing_choice_info_response`).
- `launchExternalLink(linkUri: String, linkType: Int, launchMode: Int, programType: Int, externalTransactionToken: String)`: Открытие внешней ссылки (`launch_external_link_response`).

---

## 📡 Сигналы (Signals)

| Имя сигнала | Параметры (Dictionary / String) | Описание |
| :--- | :--- | :--- |
| `connected` | - | Соединение с Google Play успешно установлено. |
| `disconnected` | - | Соединение разорвано. |
| `query_purchases` | `{"purchases_list": Array, "response_code": int, ...}` | Список текущих владений. |
| `query_product_details` | `{"product_details_list": Array, ...}` | Информация о доступных товарах. |
| `purchase_updated` | `{"purchases_list": Array, "response_code": int}` | Успешная покупка или обновление состояния. |
| `purchase_cancelled` | `{"response_code": int, ...}` | Пользователь отменил покупку. |
| `purchase_error` | `{"debug_message": String, ...}` | Ошибка при запуске флоу покупки. |
| `billing_info` | `{"fun_name": String, "debug_message": String, ...}` | Общая отладочная информация от плагина. |
| `billing_config_response` | `{"country_code": String, "response_code": int, ...}` | Конфигурация биллинга. |
| `in_app_message_result` | `{"response_code": int, "purchase_token": String}` | Результат показа In-App сообщения. |
| `alternative_billing_only_availability_response` | `{"response_code": int, ...}` | Доступность Alternative Billing Only. |
| `alternative_billing_only_reporting_details_response` | `{"external_transaction_token": String, ...}` | Токен транзакции Alternative Billing. |
| `billing_program_availability_response` | `{"billing_program": int, "response_code": int}` | Доступность Billing Program. |
| `billing_choice_info_response` | `{"play_billing_choice_image_url": String, ...}` | Информация о выборе способа оплаты. |
| `launch_external_link_response` | `{"response_code": int, ...}` | Результат открытия внешней ссылки. |

---

## 💡 Пример интеграции (GDScript)

```gdscript
var _plugin_name = "AndroidIAPP"
var _plugin

func _ready():
    if Engine.has_singleton(_plugin_name):
        _plugin = Engine.get_singleton(_plugin_name)
        _plugin.connected.connect(_on_connected)
        _plugin.purchase_updated.connect(_on_purchase_updated)
        _plugin.startConnection()

func _on_connected():
    print("Google Play Billing Ready. State: ", _plugin.getConnectionState())
    _plugin.queryProductDetails(["gold_100"], "inapp")

func buy_gold():
    if _plugin and _plugin.isReady():
        _plugin.purchase(["gold_100"], false)

func _on_purchase_updated(result):
    var purchases = result.get("purchases_list", [])
    for p in purchases:
        if p.purchase_state == 1: # PURCHASED
            print("Товар куплен: ", p.products[0])
            _plugin.consumePurchase(p.purchase_token)
```
