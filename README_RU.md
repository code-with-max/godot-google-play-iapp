# AndroidIAPP Godot Plugin

**AndroidIAPP** — это плагин для Godot (Android), который делает работу с платежами Google Play (Billing Library 8.0) максимально простой. Он поддерживает разовые покупки, подписки с разными тарифами и — самое главное — **офферы (скидки/предложения) для разовых товаров**.

## 🚀 Быстрый старт

### 1. Установка
1. Скачайте плагин и поместите папку `androidiapp` в `res://addons/`.
2. Включите плагин в меню: `Project -> Project Settings -> Plugins`.
3. **Самое важное:** Скопируйте файл `examples/GooglePlayBilling.gd` в свой проект (например, в папку `scripts/`).

### 2. Настройка в Godot
Добавьте скрипт `GooglePlayBilling.gd` в **Autoload** (Singleton) под именем `Billing` или просто создайте ноду с этим скриптом в вашей главной сцене.

### 3. Инициализация
В скрипте вашей игры:

```gdscript
func _ready():
    # Подключаем основные сигналы
    Billing.connected.connect(_on_connected)
    Billing.error_occurred.connect(_on_error)
    Billing.product_details_received.connect(_on_products_loaded)
    Billing.purchases_updated.connect(_on_purchases_updated)

    # Запускаем плагин
    Billing._initialize_plugin()

func _on_connected():
    print("Мы подключились к Google Play!")
    # Запрашиваем информацию о товарах (цены, описания)
    Billing.query_details(["gold_100", "no_ads"], Billing.TYPE_INAPP)
```

---

## 💰 Работа с покупками

### Запрос товаров и цен
Данные придут в сигнал `product_details_received`.
```gdscript
func _on_products_loaded(products: Array, unfetched: Array, type: String):
    for p in products:
        print("Товар: ", p.title, " Цена: ", p.formatted_price)
```

### Покупка (с поддержкой офферов)
Для обычного товара достаточно ID. Если у товара в консоли Google Play настроен оффер, можно передать его токен.
```gdscript
# Простая покупка
Billing.buy_inapp("gold_100")

# Покупка со специфическим оффером (скидкой)
Billing.buy_inapp("gold_100", "your_offer_token_here")
```

### Подписки
Для подписок нужно указать ID товара и ID базового плана (Base Plan).
```gdscript
Billing.subscribe("premium_sub", "monthly-plan")
```

---

## ✅ Подтверждение покупки (Обязательно!)
Google требует подтверждать покупки, иначе деньги вернутся пользователю через 3 дня.

1. **Расходники (золото, жизни)** — нужно "потребить" (`consume`).
2. **Вечные товары (отключение рекламы)** — нужно "подтвердить" (`acknowledge`).

```gdscript
func _on_purchases_updated(purchases: Array):
    for p in purchases:
        if p.purchase_state == Billing.PurchaseState.PURCHASED:
            if "gold_100" in p.products:
                Billing.consume(p.purchase_token) # Даем золото и потребляем
            else:
                if not p.is_acknowledged:
                    Billing.acknowledge(p.purchase_token) # Активируем вечный товар
```

---

## 🛠 Все сигналы `GooglePlayBilling.gd`

### Основные и технические сигналы
*   `connected` — успешно подключено к Google Play Billing, можно начинать работу.
*   `disconnected` — произошел сброс подключения к Google Play Billing.
*   `error_occurred(fun_name, response)` — возникла ошибка при выполнении операции. `fun_name` — имя метода, `response` — словарь с `response_code` и `debug_message`.
*   `billing_info_received(info)` — техническая диагностическая информация от плагина.
*   `hello_response(message)` — ответ на тестовый вызов `say_hello`.

### Продукты и покупки
*   `product_details_received(products, unfetched, type)` — получены детали товаров (цены, описания, офферы). `products` — список доступных продуктов, `unfetched` — ненайденные продукты, `type` — тип (`inapp`/`subs`).
*   `purchases_queried(purchases)` — получен список активных покупок пользователя в ответ на `query_purchases`.
*   `purchases_updated(purchases)` — сработал после покупки или при обновлении списка покупок.
*   `purchase_cancelled` — пользователь отменил процесс покупки.
*   `consumed_success(token)` — товар успешно "съеден" (consumed), можно зачислить награду.
*   `acknowledged_success(token)` — покупка или подписка успешно подтверждена (acknowledged).

### Конфигурация и подписки
*   `in_app_message_result(result)` — результат показа In-App сообщения Google Play.
*   `price_change_acknowledged(result)` — результат подтверждения изменения цены подписки.
*   `billing_config_received(config)` — получена конфигурация биллинга (код страны пользователя и др.).

### Альтернативная оплата и внешние ссылки
*   `alternative_billing_only_availability_response(response)` — результат проверки доступности Alternative Billing Only.
*   `alternative_billing_only_reporting_details_response(response)` — детали отчетности Alternative Billing Only.
*   `alternative_billing_only_information_dialog_response(response)` — результат показа диалога Alternative Billing Only.
*   `external_offer_availability_response(response)` — результат проверки доступности External Offer.
*   `external_offer_reporting_details_response(response)` — детали отчетности External Offer.
*   `external_offer_information_dialog_response(response)` — результат показа диалога External Offer.
*   `billing_program_availability_response(response)` — результат проверки доступности Billing Program.
*   `billing_program_reporting_details_response(response)` — детали отчетности Billing Program.
*   `billing_program_information_dialog_response(response)` — результат показа диалога Billing Program.
*   `billing_choice_info_response(response)` — информация о выборе способов оплаты.
*   `launch_external_link_response(response)` — результат открытия внешней ссылки.

---

## 🔍 Отладка
Если что-то не работает, подключите телефон и смотрите логи через терминал:
```bash
adb logcat | grep IAPP
```

**Совместимость:** Godot 4.3+ (Android Export). Требуется включенное разрешение `Billing` в настройках экспорта.
