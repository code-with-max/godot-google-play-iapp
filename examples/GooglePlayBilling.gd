# GooglePlayBilling.gd
class_name GooglePlayBilling
extends Node

# Иконка плагина (пока нет)
# @icon("icon_billing.svg")

## Универсальный мост для Google Play Billing Library 8.0.
## Обеспечивает транспорт данных между Android-плагином и BillingHandler.
## Транслирует ВСЕ сигналы плагина и предоставляет доступ ко всем его методам.

# --- Константы и Перечисления ---

const PLUGIN_NAME: String = "AndroidIAPP"

## Типы продуктов в Google Play
const TYPE_INAPP: String = "inapp"
const TYPE_SUBS: String = "subs"

## Режимы замены подписки (Replacement Mode)
enum ReplacementMode {
	UNKNOWN = 0,
	WITH_TIME_PRORATION = 1, # Пропорционально времени (стандарт)
	CHARGE_EXPR_DATE = 2, # Списать при продлении
	WITHOUT_PRORATION = 3, # Без перерасчета
	CHARGE_FULL_PRICE = 4 # Списать полную стоимость сразу
}

## Состояние покупки
## https://developer.android.com/reference/com/android/billingclient/api/Purchase.PurchaseState
enum PurchaseState {
	UNSPECIFIED_STATE = 0,
	PURCHASED = 1,
	PENDING = 2,
}

## Коды ответа Google Play Billing
## https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode
enum BillingResponseCode {
	SERVICE_TIMEOUT = -3,
	FEATURE_NOT_SUPPORTED = -2,
	SERVICE_DISCONNECTED = -1,
	OK = 0,
	USER_CANCELED = 1,
	SERVICE_UNAVAILABLE = 2,
	BILLING_UNAVAILABLE = 3,
	ITEM_UNAVAILABLE = 4,
	DEVELOPER_ERROR = 5,
	ERROR = 6,
	ITEM_ALREADY_OWNED = 7,
	ITEM_NOT_OWNED = 8,
	NETWORK_ERROR = 12,
	NO_ELIGIBLE_OFFER = 13
}

# --- Сигналы ---

## Плагин подключён к Google Play Billing
signal connected

## Плагин отключён от Google Play Billing
signal disconnected

## Зафиксирована ошибка. fun_name — метод, response — словарь с кодом и сообщением
signal error_occurred(fun_name: String, response: Dictionary)

## Технический инфо-сигнал от плагина (диагностика, статусы операций)
signal billing_info_received(info: Dictionary)

## Ответ на sayHello (используется для проверки соединения)
signal hello_response(message: String)

## Возвращает Array[Dictionary] (ProductDetails) и Array[Dictionary] (UnfetchedProduct)
signal product_details_received(products: Array, unfetched: Array, type: String)

## Активные покупки пользователя (ответ на query_purchases)
signal purchases_queried(purchases: Array)

## Покупки обновлены (новая покупка завершена успешно)
signal purchases_updated(purchases: Array)

## Пользователь отменил покупку
signal purchase_cancelled

## Расходник успешно погашен (consume)
signal consumed_success(token: String)

## Постоянная покупка успешно подтверждена (acknowledge)
signal acknowledged_success(token: String)

## Результат показа In-App сообщения Google Play
signal in_app_message_result(result: Dictionary)

## Результат подтверждения изменения цены подписки
signal price_change_acknowledged(result: Dictionary)


# --- Приватные переменные ---

var _plugin: Object = null


# --- Инициализация ---

# Запускаем инициализацию при входе в дерево сцены
func _ready() -> void:
	# _initialize_plugin() # moved to handler
	pass


# Ищем JNI-синглтон и запускаем соединение с Google Play
func _initialize_plugin() -> void:
	if not Engine.has_singleton(PLUGIN_NAME):
		push_error("[GOOGLE_PLAY_BILLING]: Плагин '%s' не найден в системе!" % PLUGIN_NAME)
		return

	_plugin = Engine.get_singleton(PLUGIN_NAME)
	_connect_signals()

	print("[GOOGLE_PLAY_BILLING]: Плагин найден, запускаем соединение с Google Play...") ## TODO: Убрать в релизе
	_plugin.startConnection()


# Подключаем все сигналы Kotlin-части к нашим локальным сигналам
func _connect_signals() -> void:
	# Соединение
	_plugin.connected.connect(func(): connected.emit())
	_plugin.disconnected.connect(func(): disconnected.emit())

	# Технические диагностические сигналы
	_plugin.billing_info.connect(func(info: Dictionary): billing_info_received.emit(info))
	_plugin.helloResponse.connect(func(msg: String): hello_response.emit(msg))

	# Продукты
	_plugin.query_product_details.connect(_on_product_details_received)
	_plugin.query_product_details_error.connect(func(res: Dictionary):
		error_occurred.emit("query_product_details", res)
	)

	# Запрос активных покупок
	_plugin.query_purchases.connect(func(res: Dictionary):
		purchases_queried.emit(res.get("purchases_list", []))
	)
	_plugin.query_purchases_error.connect(func(res: Dictionary):
		error_occurred.emit("query_purchases", res)
	)

	# Покупки
	_plugin.purchase_updated.connect(func(res: Dictionary):
		purchases_updated.emit(res.get("purchases_list", []))
	)
	_plugin.purchase_cancelled.connect(func(_res: Dictionary): purchase_cancelled.emit())
	_plugin.purchase_error.connect(func(res: Dictionary):
		error_occurred.emit("purchase", res)
	)
	# Ошибка в onPurchasesUpdated (не cancel и не OK) — отдельный сигнал
	_plugin.purchase_update_error.connect(func(res: Dictionary):
		error_occurred.emit("purchase_update", res)
	)

	# Потребление / Подтверждение
	_plugin.purchase_consumed.connect(func(res: Dictionary):
		consumed_success.emit(res.get("purchase_token", ""))
	)
	_plugin.purchase_consumed_error.connect(func(res: Dictionary):
		error_occurred.emit("consume", res)
	)
	_plugin.purchase_acknowledged.connect(func(res: Dictionary):
		acknowledged_success.emit(res.get("purchase_token", ""))
	)
	_plugin.purchase_acknowledged_error.connect(func(res: Dictionary):
		error_occurred.emit("acknowledge", res)
	)

	# In-App сообщения и изменение цены
	_plugin.in_app_message_result.connect(func(res: Dictionary): in_app_message_result.emit(res))
	_plugin.price_change_acknowledged.connect(func(res: Dictionary): price_change_acknowledged.emit(res))
	_plugin.price_change_error.connect(func(res: Dictionary):
		error_occurred.emit("price_change", res)
	)


# --- Публичные методы API ---

## Проверка готовности плагина к работе
func is_ready() -> bool:
	return _plugin.isReady() if _plugin else false


## Тестовый запрос к плагину. Ответ придёт в сигнал hello_response
func say_hello(message: String = "Hello from GDScript") -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.sayHello(message)


## Принудительное завершение соединения с Google Play Billing
func end_connection() -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.endConnection()


## Запрос деталей продуктов (цены, офферы, теги).
## Результат приходит в product_details_received
func query_details(product_ids: Array[String], type: String = TYPE_INAPP) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, query_details пропущен.")
		return
	_plugin.queryProductDetails(product_ids, type)


## Запрос текущих активных покупок пользователя.
## Результат приходит в purchases_queried. Вызывай при старте приложения!
## include_suspended — включать ли приостановленные подписки
func query_purchases(type: String = TYPE_INAPP, include_suspended: bool = false) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, query_purchases пропущен.")
		return
	_plugin.queryPurchases(type, include_suspended)


## Покупка разового товара (расходники или unlockables).
## offer_token — токен конкретного оффера (если пустой, плагин выберет первый доступный)
func buy_inapp(id: String, offer_token: String = "", is_personalized: bool = false) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, buy_inapp пропущен.")
		return
	_plugin.purchase([id], is_personalized, offer_token)


## Покупка подписки. base_plan_id обязателен, offer_id — опционален
func subscribe(id: String, base_plan_id: String, offer_id: String = "", is_personalized: bool = false) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, subscribe пропущен.")
		return
	# Kotlin-плагин ожидает массивы для синхронизации индексов списков
	_plugin.subscribe([id], [base_plan_id], [offer_id], is_personalized)


## Обновление существующей подписки (upgrade / downgrade).
## old_token — purchase_token старой подписки, old_id — product_id старой подписки
func update_subscription(
	id: String,
	base_plan_id: String,
	old_token: String,
	old_id: String,
	mode: ReplacementMode = ReplacementMode.WITH_TIME_PRORATION
) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, update_subscription пропущен.")
		return
	_plugin.updateSubscription([id], [base_plan_id], [], false, old_token, old_id, int(mode))


## Потребление (consume) для расходников. Открывает возможность повторной покупки
func consume(token: String) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, consume пропущен.")
		return
	_plugin.consumePurchase(token)


## Подтверждение (acknowledge) для постоянных покупок. Обязательно в течение 3 дней!
func acknowledge(token: String) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, acknowledge пропущен.")
		return
	_plugin.acknowledgePurchase(token)


## Показать системные In-App сообщения Google Play (например, истёкшая карта)
func show_in_app_messages() -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.showInAppMessages()


## Запустить flow подтверждения изменения цены подписки.
## product_details — словарь с данными продукта из product_details_received
func launch_price_change_flow(product_details: Dictionary) -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.launchPriceChangeConfirmationFlow(product_details)


# --- Внутренние обработчики ---

# Распаковывает ответ плагина и транслирует список продуктов и нераспознанных товаров
func _on_product_details_received(response: Dictionary) -> void:
	var list: Array = response.get("product_details_list", [])
	var unfetched: Array = response.get("unfetched_product_list", [])

	# Определяем тип по первому продукту в списке
	var type: String = TYPE_INAPP
	if list.size() > 0:
		type = list[0].get("product_type", TYPE_INAPP)

	product_details_received.emit(list, unfetched, type)