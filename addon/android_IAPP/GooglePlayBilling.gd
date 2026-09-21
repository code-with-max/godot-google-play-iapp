class_name GooglePlayBilling
extends Node

## Мост для Google Play Billing Library.
## Обеспечивает транспорт данных между Android-плагином и логикой игры.

const PLUGIN_NAME: String = "AndroidIAPP"

const TYPE_INAPP: String = "inapp"
const TYPE_SUBS: String = "subs"

enum ReplacementMode {
	UNKNOWN = 0,
	WITH_TIME_PRORATION = 1,
	CHARGE_EXPR_DATE = 2,
	WITHOUT_PRORATION = 3,
	CHARGE_FULL_PRICE = 4,
}

enum PurchaseState {
	UNSPECIFIED_STATE = 0,
	PURCHASED = 1,
	PENDING = 2,
}

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
	NO_ELIGIBLE_OFFER = 13,
}

enum ConnectionState {
	DISCONNECTED = 0,
	CONNECTING = 1,
	CONNECTED = 2,
	CLOSED = 3,
}

# --- Основные сигналы ---

signal connected
signal disconnected
signal error_occurred(fun_name: String, response: Dictionary)
signal billing_info_received(info: Dictionary)
signal hello_response(message: String)

signal product_details_received(products: Array, unfetched: Array, type: String)
signal purchases_queried(purchases: Array)
signal purchases_updated(purchases: Array)
signal purchase_cancelled(response: Dictionary)

signal consumed_success(token: String)
signal acknowledged_success(token: String)
signal billing_config_received(config: Dictionary)

# Дополнительные / Alt Billing сигналы
signal in_app_message_result(result: Dictionary)
signal price_change_acknowledged(response: Dictionary)
signal alternative_billing_only_availability_response(response: Dictionary)
signal external_offer_availability_response(response: Dictionary)
signal billing_program_availability_response(response: Dictionary)
signal billing_choice_info_response(response: Dictionary)

# Приватные переменные
var _plugin: Object = null


## Инициализация JNI-синглтона
func initialize_plugin() -> void:
	if not Engine.has_singleton(PLUGIN_NAME):
		push_error("[GOOGLE_PLAY_BILLING]: Плагин '%s' не найден!" % PLUGIN_NAME)
		return

	_plugin = Engine.get_singleton(PLUGIN_NAME)
	_connect_signals()

	print("[GOOGLE_PLAY_BILLING]: Запуск соединения с Google Play...")
	_call_plugin("startConnection")


## Проверка готовности плагина
func is_ready() -> bool:
	return bool(_call_plugin_ret("isReady", false))


## Получение статуса подключения
func get_connection_state() -> int:
	return int(_call_plugin_ret("getConnectionState", ConnectionState.DISCONNECTED))


# --- Публичные методы API ---

## Тестовый вызов
func say_hello(message: String = "Hello from GDScript") -> void:
	_call_plugin("sayHello", [message])


## Запрос деталей продуктов
func query_details(product_ids: Array[String], type: String = TYPE_INAPP) -> void:
	if _check_ready_or_warn("query_details"):
		_call_plugin("queryProductDetails", [product_ids, type])


## Запрос активных покупок
func query_purchases(type: String = TYPE_INAPP, include_suspended: bool = false) -> void:
	if _check_ready_or_warn("query_purchases"):
		_call_plugin("queryPurchases", [type, include_suspended])


## Покупка разового товара или подписки
func buy_inapp(id: String, offer_token: String = "", is_personalized: bool = false) -> void:
	if _check_ready_or_warn("buy_inapp"):
		_call_plugin("purchase", [[id], is_personalized, offer_token])


## Оформление подписки
func subscribe(id: String, base_plan_id: String, offer_id: String = "", is_personalized: bool = false) -> void:
	if _check_ready_or_warn("subscribe"):
		_call_plugin("subscribe", [[id], [base_plan_id], [offer_id], is_personalized])


## Погашение расходника
func consume(token: String) -> void:
	if _check_ready_or_warn("consume"):
		_call_plugin("consumePurchase", [token])


## Подтверждение постоянной покупки
func acknowledge(token: String) -> void:
	if _check_ready_or_warn("acknowledge"):
		_call_plugin("acknowledgePurchase", [token])


# --- Внутренняя логика и проброс сигналов ---

# Автоматическое связывание сигналов Kotlin и GDScript
func _connect_signals() -> void:
	if not _plugin:
		return

	_safe_connect("connected", connected.emit)
	_safe_connect("disconnected", disconnected.emit)
	
	# Обработка ответов с трансформацией данных
	_safe_connect("helloResponse", func(msg: String): hello_response.emit(msg))
	_safe_connect("billing_info", func(info: Dictionary): billing_info_received.emit(info))
	
	_safe_connect("query_product_details", _on_product_details_received)
	_safe_connect("query_product_details_error", func(res: Dictionary): error_occurred.emit("query_product_details", res))

	_safe_connect("query_purchases", func(res: Dictionary): purchases_queried.emit(res.get("purchases_list", [])))
	_safe_connect("query_purchases_error", func(res: Dictionary): error_occurred.emit("query_purchases", res))

	_safe_connect("purchase_updated", func(res: Dictionary): purchases_updated.emit(res.get("purchases_list", [])))
	_safe_connect("purchase_error", func(res: Dictionary): error_occurred.emit("purchase", res))
	_safe_connect("purchase_cancelled", purchase_cancelled.emit)

	_safe_connect("purchase_consumed", func(res: Dictionary): consumed_success.emit(res.get("purchase_token", "")))
	_safe_connect("purchase_consumed_error", func(res: Dictionary): error_occurred.emit("consume", res))

	_safe_connect("purchase_acknowledged", func(res: Dictionary): acknowledged_success.emit(res.get("purchase_token", "")))
	_safe_connect("purchase_acknowledged_error", func(res: Dictionary): error_occurred.emit("acknowledge", res))


func _on_product_details_received(response: Dictionary) -> void:
	var list: Array = response.get("product_details_list", [])
	var unfetched: Array = response.get("unfetched_product_list", [])
	var type: String = list[0].get("product_type", TYPE_INAPP) if not list.is_empty() else TYPE_INAPP

	product_details_received.emit(list, unfetched, type)


# Вспомогательный метод безопасности подключения сигналов
func _safe_connect(signal_name: String, callable: Callable) -> void:
	if _plugin.has_signal(signal_name):
		_plugin.connect(signal_name, callable)


# Вспомогательный метод вызова функции в Kotlin плагине
func _call_plugin(method_name: String, args: Array = []) -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован при вызове %s" % method_name)
		return
	_plugin.callv(method_name, args)


# Вспомогательный метод вызова с возвратом значения
func _call_plugin_ret(method_name: String, default_value: Variant) -> Variant:
	if not _plugin:
		return default_value
	return _plugin.call(method_name)


func _check_ready_or_warn(method_name: String) -> bool:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, %s пропущен." % method_name)
		return false
	return true
