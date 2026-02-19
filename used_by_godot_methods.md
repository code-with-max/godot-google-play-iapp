# AndroidIAPP Plugin: Godot API Reference

Этот файл содержит описание всех методов и сигналов, предоставляемых плагином `AndroidIAPP` для работы в Godot.

## 📝 TODO по переходу на Google Play Billing Library v8

Проект уже использует версию `8.3.0`, но вот основные моменты, на которые стоит обратить внимание при обновлении логики:

- [ ] **Проверить использование `ReplacementMode`**: В v8 `SubscriptionUpdateParams` использует `ReplacementMode` для уточнения того, как именно должна быть заменена подписка (например, `CHARGE_FULL_PRICE` или `WITHOUT_PRORATION`).
- [ ] **Поддержка предоплаченных планов**: Убедиться, что логика обработки покупок корректно обрабатывает `is_auto_renewing = false` для подписок.
- [ ] **Альтернативный биллинг**: Если планируется использование Alternative Billing, необходимо реализовать методы `createAlternativeBillingOnlyReportingDetails` и `reportAlternativeBillingOnlyTransaction`.
- [ ] **Обработка ошибок**: v8 может возвращать новые коды ответов. Проверьте, что ваш GDScript код готов к расширению `response_code`.
- [ ] **Тестирование**: Провести полное тестирование флоу покупки (in-app) и подписки (включая апгрейд/даунгрейд).

---

## 🛠 Методы (UsedByGodot)

Все методы вызываются через объект плагина, полученный с помощью `Engine.get_singleton("AndroidIAPP")`.

### `isReady` (Property)
Возвращает `true`, если `BillingClient` инициализирован и готов к работе.
- **Тип**: `bool` (через getter)

### `sayHello(says: String)`
Выводит Toast-сообщение в Android и отправляет сигнал `helloResponse`.
- **Аргументы**:
  - `says` (String): Текст сообщения. По умолчанию: "Hello from AndroidIAPP plugin".
- **Пример GDScript**:
  ```gdscript
  if _plugin:
      _plugin.sayHello("Привет из Godot!")
  ```

### `startConnection()`
Инициализирует `BillingClient` и устанавливает соединение с Google Play.
- **Пример GDScript**:
  ```gdscript
  _plugin.startConnection()
  ```

### `endConnection()`
Закрывает соединение с Google Play.

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

### `purchase(listOfProductsIDs: Array[String], isOfferPersonalized: bool)`
Запускает процесс покупки разового товара (INAPP).
- **Аргументы**:
  - `listOfProductsIDs` (Array[String]): Массив с одним ID товара (берется первый элемент).
  - `isOfferPersonalized` (bool): Указывает, является ли цена персонализированной (согласно требованиям EU).
- **Пример GDScript**:
  ```gdscript
  _plugin.purchase(["my_item_id"], false)
  ```

### `subscribe(listOfProductsIDs: Array[String], basePlanIDs: Array[String], offerIDs: Array[String], isOfferPersonalized: bool)`
Запускает процесс оформления подписки.
- **Аргументы**:
  - `listOfProductsIDs`: Массив с одним ID подписки.
  - `basePlanIDs`: Массив с одним ID базового плана.
  - `offerIDs`: Массив с одним ID оффера (можно пустой массив, если оффера нет).
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

---

## 📡 Сигналы (Signals)

| Имя сигнала | Параметры (Dictionary) | Описание |
| :--- | :--- | :--- |
| `connected` | - | Соединение с Google Play успешно установлено. |
| `disconnected` | - | Соединение разорвано. |
| `query_purchases` | `{"purchases_list": Array, "response_code": int, ...}` | Список текущих владений. |
| `query_product_details` | `{"product_details_list": Array, ...}` | Информация о доступных товарах. |
| `purchase_updated` | `{"purchases_list": Array, "response_code": int}` | Успешная покупка или обновление состояния. |
| `purchase_cancelled` | `{"response_code": int, ...}` | Пользователь отменил покупку. |
| `purchase_error` | `{"debug_message": String, ...}` | Ошибка при запуске флоу покупки. |
| `billing_info` | `{"fun_name": String, "debug_message": String, ...}` | Общая отладочная информация от плагина. |

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
    print("Google Play Billing Ready")
    _plugin.queryProductDetails(["gold_100"], "inapp")

func buy_gold():
    if _plugin and _plugin.isReady:
        _plugin.purchase(["gold_100"], false)

func _on_purchase_updated(result):
    var purchases = result.get("purchases_list", [])
    for p in purchases:
        if p.purchase_state == 1: # PURCHASED
            print("Товар куплен: ", p.products[0])
            _plugin.consumePurchase(p.purchase_token)
```
