# Описание сигналов GooglePlayBilling.gd (AndroidIAPP Plugin)

Ниже представлено подробное описание всех сигналов, объявленных и транслируемых в `GooglePlayBilling.gd`.

---

## 📡 Основные и технические сигналы

* **`connected`**
  * **Описание:** Вызывается при успешном подключении к сервису Google Play Billing Client. После этого сигнала можно отправлять запросы товаров и покупок.
* **`disconnected`**
  * **Описание:** Вызывается при отключении от Google Play Billing (например, сервис Google Play перезапустился или сбой сети).
* **`error_occurred(fun_name: String, response: Dictionary)`**
  * **Описание:** Вызывается при возникновении ошибки в любом из методов плагина.
  * **Параметры:**
    * `fun_name` — имя метода или операции, вызвавшей ошибку (например, `"query_product_details"`, `"purchase"`, `"consume"`, `"acknowledge"` и др.).
    * `response` — словарь с кодом ответа Google Play Billing (`response_code`) и отладочным сообщением (`debug_message`).
* **`billing_info_received(info: Dictionary)`**
  * **Описание:** Передаёт техническую диагностическую информацию и статусы операций от плагина.
* **`hello_response(message: String)`**
  * **Описание:** Ответ на вызов метода `say_hello()`. Используется для проверки связи между GDScript и Android-плагином.

---

## 🛍 Продукты и детали товаров

* **`product_details_received(products: Array, unfetched: Array, type: String)`**
  * **Описание:** Возвращается в ответ на метод `query_details()`.
  * **Параметры:**
    * `products` — массив словарей `ProductDetails` с информацией о найденных товарах (ID, наименование, цены, форматированная цена, офферы и т.д.).
    * `unfetched` — массив словарей с информацией о продуктах, детали которых не удалось получить.
    * `type` — тип запрошенных продуктов (`TYPE_INAPP` или `TYPE_SUBS`).

---

## 💳 Покупки и транзакции

* **`purchases_queried(purchases: Array)`**
  * **Описание:** Возвращает список текущих активных покупок пользователя в ответ на запрос `query_purchases()`.
  * **Параметры:**
    * `purchases` — массив словарей с деталями активных покупок (состояние покупки, токены, ID продуктов и т.д.).
* **`purchases_updated(purchases: Array)`**
  * **Описание:** Вызывается при успешном завершении новой покупки через форму оплаты Google Play или при обновлении списка покупок.
  * **Параметры:**
    * `purchases` — массив словарей обновленных или вновь совершенных покупок.
* **`purchase_cancelled`**
  * **Описание:** Вызывается, если пользователь отменил процесс покупки во всплывающем окне Google Play.
* **`consumed_success(token: String)`**
  * **Описание:** Вызывается после успешного потребления (погашения) расходного товара методом `consume(token)`.
  * **Параметры:**
    * `token` — purchase token (токен покупки) обработанного товара.
* **`acknowledged_success(token: String)`**
  * **Описание:** Вызывается после успешного подтверждения (acknowledge) постоянной покупки или подписки методом `acknowledge(token)`.
  * **Параметры:**
    * `token` — purchase token подтверждённой покупки.

---

## ⚙ Конфигурация, In-App сообщения и подписки

* **`in_app_message_result(result: Dictionary)`**
  * **Описание:** Возвращает результат отображения системного In-App сообщения Google Play (например, предупреждение об истекающем способе оплаты).
* **`price_change_acknowledged(result: Dictionary)`**
  * **Описание:** Возвращает результат подтверждения изменения цены подписки пользователем.
* **`billing_config_received(config: Dictionary)`**
  * **Описание:** Возвращает конфигурацию биллинга (например, код страны пользователя) в ответ на `get_billing_config()`.

---

## 🌐 Альтернативные способы оплаты и внешние предложения (Alternative Billing / External Offer / Billing Program)

* **`alternative_billing_only_availability_response(response: Dictionary)`**
  * **Описание:** Результат проверки доступности режима Alternative Billing Only (`is_alternative_billing_only_available()`).
* **`alternative_billing_only_reporting_details_response(response: Dictionary)`**
  * **Описание:** Детали отчетности для Alternative Billing Only (`create_alternative_billing_only_reporting_details()`).
* **`alternative_billing_only_information_dialog_response(response: Dictionary)`**
  * **Описание:** Результат показа информационного диалога Alternative Billing Only.
* **`external_offer_availability_response(response: Dictionary)`**
  * **Описание:** Результат проверки доступности внешних предложений External Offer (`is_external_offer_available()`).
* **`external_offer_reporting_details_response(response: Dictionary)`**
  * **Описание:** Детали отчетности для External Offer (`create_external_offer_reporting_details()`).
* **`external_offer_information_dialog_response(response: Dictionary)`**
  * **Описание:** Результат показа информационного диалога External Offer.
* **`billing_program_availability_response(response: Dictionary)`**
  * **Описание:** Результат проверки доступности Billing Program (`is_billing_program_available()`).
* **`billing_program_reporting_details_response(response: Dictionary)`**
  * **Описание:** Детали отчетности для Billing Program (`create_billing_program_reporting_details()`).
* **`billing_program_information_dialog_response(response: Dictionary)`**
  * **Описание:** Результат показа информационного диалога Billing Program.
* **`billing_choice_info_response(response: Dictionary)`**
  * **Описание:** Информация о выборе способов оплаты (Billing Choice Info) в ответ на `get_billing_choice_info()`.
* **`launch_external_link_response(response: Dictionary)`**
  * **Описание:** Результат вызова открытия внешней ссылки через `launch_external_link()`.
