# GooglePlayBilling.gd
class_name GooglePlayBilling
extends Node

## Универсальный мост для Google Play Billing Library 9.1.0.
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

## Состояние подключения BillingClient
enum ConnectionState {
	DISCONNECTED = 0,
	CONNECTING = 1,
	CONNECTED = 2,
	CLOSED = 3
}

# --- Сигналы (Прямые сигналы плагина AndroidIAPP) ---

## Ответ на sayHello
signal helloResponse(message: String)

## Попытка подключения начата (startConnection)
signal startConnection

## Попытка подключения начата
signal connection_starting

## Плагин подключён к Google Play Billing
signal connected

## Плагин отключён от Google Play Billing
signal disconnected

## Результат запроса покупок
signal query_purchases_response(response: Dictionary)

## Ошибка при запросе покупок
signal query_purchases_error(response: Dictionary)

## Результат запроса деталей продуктов
signal query_product_details_response(response: Dictionary)

## Ошибка при запросе деталей продуктов
signal query_product_details_error(response: Dictionary)

## Сигнал покупки
signal purchase_started(response: Dictionary)

## Ошибка при запуске покупки
signal purchase_error(response: Dictionary)

## Покупки обновлены
signal purchase_updated(response: Dictionary)

## Пользователь отменил покупку
signal purchase_cancelled(response: Dictionary)

## Ошибка при обновлении покупки
signal purchase_update_error(response: Dictionary)

## Результат погашения расходника (consume)
signal purchase_consumed(response: Dictionary)

## Ошибка при погашении расходника
signal purchase_consumed_error(response: Dictionary)

## Результат подтверждения покупки (acknowledge)
signal purchase_acknowledged(response: Dictionary)

## Ошибка при подтверждении покупки
signal purchase_acknowledged_error(response: Dictionary)

## Техническая информация биллинга
signal billing_info(response: Dictionary)

## Результат подтверждения изменения цены
signal price_change_acknowledged(response: Dictionary)

## Ошибка при изменении цены
signal price_change_error(response: Dictionary)

## Результат показа In-App сообщения
signal in_app_message_result(result: Dictionary)

## Результат транзакции Alternative Billing Only
signal alternative_billing_only_transaction_reported(response: Dictionary)

## Конфигурация биллинга получена
signal billing_config_response(response: Dictionary)

## Результат проверки доступности Alternative Billing Only
signal alternative_billing_only_availability_response(response: Dictionary)

## Детали отчетности Alternative Billing Only
signal alternative_billing_only_reporting_details_response(response: Dictionary)

## Результат показа диалога информации Alternative Billing Only
signal alternative_billing_only_information_dialog_response(response: Dictionary)

## Результат проверки доступности External Offer
signal external_offer_availability_response(response: Dictionary)

## Детали отчетности External Offer
signal external_offer_reporting_details_response(response: Dictionary)

## Результат показа диалога информации External Offer
signal external_offer_information_dialog_response(response: Dictionary)

## Результат проверки доступности Billing Program
signal billing_program_availability_response(response: Dictionary)

## Детали отчетности Billing Program
signal billing_program_reporting_details_response(response: Dictionary)

## Результат показа диалога информации Billing Program
signal billing_program_information_dialog_response(response: Dictionary)

## Информация о выборе способов оплаты (Billing Choice Info)
signal billing_choice_info_response(response: Dictionary)

## Результат открытия внешней ссылки (Launch External Link)
signal launch_external_link_response(response: Dictionary)


# --- Высокоуровневые удобные сигналы (IAP) ---

## Начало попытки подключения к Google Play Billing
signal iap_connection_starting

## Успешное подключение к Google Play Billing
signal iap_connected

## Отключение от Google Play Billing
signal iap_disconnected

## Зафиксирована ошибка. fun_name — метод, response — словарь с кодом и сообщением
signal iap_error_occurred(fun_name: String, response: Dictionary)

## Технический инфо-сигнал от плагина (диагностика, статусы операций)
signal iap_billing_info_received(info: Dictionary)

## Ответ на sayHello (используется для проверки соединения)
signal iap_hello_response(message: String)

## Возвращает Array[Dictionary] (ProductDetails) и Array[Dictionary] (UnfetchedProduct)
signal iap_product_details_received(products: Array[Dictionary], unfetched: Array[Dictionary], type: String)

## Активные покупки пользователя (ответ на query_purchases)
signal iap_purchases_queried(purchases: Array[Dictionary])

## Покупки обновлены (новая покупка завершена успешно)
signal iap_purchases_updated(purchases: Array[Dictionary])

## Расходник успешно погашен (consume)
signal iap_consumed_success(token: String)

## Постоянная покупка успешно подтверждена (acknowledge)
signal iap_acknowledged_success(token: String)

## Конфигурация биллинга получена (страна и т.д.)
signal iap_billing_config_received(config: Dictionary)


# --- Приватные переменные ---

var _plugin: Object = null


# --- Инициализация ---

func _ready() -> void:
	if Engine.has_singleton(PLUGIN_NAME):
		_initialize_plugin()


## Публичный метод инициализации плагина
func initialize_plugin() -> void:
	_initialize_plugin()


# Ищем JNI-синглтон и запускаем соединение с Google Play
func _initialize_plugin() -> void:
	if _plugin != null:
		return
	if not Engine.has_singleton(PLUGIN_NAME):
		push_error("[GOOGLE_PLAY_BILLING]: Плагин '%s' не найден в системе!" % PLUGIN_NAME)
		return

	_plugin = Engine.get_singleton(PLUGIN_NAME)
	_connect_signals()

	print("[GOOGLE_PLAY_BILLING]: Плагин найден, запускаем соединение с Google Play...")
	_plugin.startConnection()


# Подключаем все сигналы Kotlin-части к нашим локальным сигналам
func _connect_signals() -> void:
	if not _plugin:
		return

	# Соединение
	if _plugin.has_signal("startConnection"):
		_plugin.startConnection.connect(func():
			startConnection.emit()
			connection_starting.emit()
			iap_connection_starting.emit()
		)
	_plugin.connected.connect(func():
		connected.emit()
		iap_connected.emit()
	)
	_plugin.disconnected.connect(func():
		disconnected.emit()
		iap_disconnected.emit()
	)

	# Диагностика и тестовый сигнал
	_plugin.billing_info.connect(func(info: Dictionary):
		billing_info.emit(info)
		iap_billing_info_received.emit(info)
	)
	_plugin.helloResponse.connect(func(msg: String):
		helloResponse.emit(msg)
		iap_hello_response.emit(msg)
	)

	# Запрос деталей продуктов
	_plugin.query_product_details.connect(func(res: Dictionary):
		query_product_details_response.emit(res)
		_on_product_details_received(res)
	)
	_plugin.query_product_details_error.connect(func(res: Dictionary):
		query_product_details_error.emit(res)
		iap_error_occurred.emit("query_product_details", res)
	)

	# Запрос активных покупок
	_plugin.query_purchases.connect(func(res: Dictionary):
		query_purchases_response.emit(res)
		var purchases_list: Array[Dictionary] = []
		purchases_list.assign(res.get("purchases_list", []))
		iap_purchases_queried.emit(purchases_list)
	)
	_plugin.query_purchases_error.connect(func(res: Dictionary):
		query_purchases_error.emit(res)
		iap_error_occurred.emit("query_purchases", res)
	)

	# Покупки
	if _plugin.has_signal("purchase"):
		_plugin.purchase.connect(func(res: Dictionary):
			purchase_started.emit(res)
		)
	_plugin.purchase_error.connect(func(res: Dictionary):
		purchase_error.emit(res)
		iap_error_occurred.emit("purchase", res)
	)
	_plugin.purchase_updated.connect(func(res: Dictionary):
		purchase_updated.emit(res)
		var purchases_list: Array[Dictionary] = []
		purchases_list.assign(res.get("purchases_list", []))
		iap_purchases_updated.emit(purchases_list)
	)
	_plugin.purchase_cancelled.connect(func(res: Dictionary):
		purchase_cancelled.emit(res)
	)
	_plugin.purchase_update_error.connect(func(res: Dictionary):
		purchase_update_error.emit(res)
		iap_error_occurred.emit("purchase_update", res)
	)

	# Потребление / Подтверждение
	_plugin.purchase_consumed.connect(func(res: Dictionary):
		purchase_consumed.emit(res)
		iap_consumed_success.emit(res.get("purchase_token", ""))
	)
	_plugin.purchase_consumed_error.connect(func(res: Dictionary):
		purchase_consumed_error.emit(res)
		iap_error_occurred.emit("consume", res)
	)
	_plugin.purchase_acknowledged.connect(func(res: Dictionary):
		purchase_acknowledged.emit(res)
		iap_acknowledged_success.emit(res.get("purchase_token", ""))
	)
	_plugin.purchase_acknowledged_error.connect(func(res: Dictionary):
		purchase_acknowledged_error.emit(res)
		iap_error_occurred.emit("acknowledge", res)
	)

	# In-App сообщения и изменение цены
	_plugin.in_app_message_result.connect(func(res: Dictionary): in_app_message_result.emit(res))
	_plugin.price_change_acknowledged.connect(func(res: Dictionary): price_change_acknowledged.emit(res))
	_plugin.price_change_error.connect(func(res: Dictionary):
		price_change_error.emit(res)
		iap_error_occurred.emit("price_change", res)
	)

	# Billing Config
	if _plugin.has_signal("billing_config_response"):
		_plugin.billing_config_response.connect(func(res: Dictionary):
			billing_config_response.emit(res)
			iap_billing_config_received.emit(res)
		)

	# Alternative Billing Only
	if _plugin.has_signal("alternative_billing_only_availability_response"):
		_plugin.alternative_billing_only_availability_response.connect(func(res: Dictionary): alternative_billing_only_availability_response.emit(res))
	if _plugin.has_signal("alternative_billing_only_reporting_details_response"):
		_plugin.alternative_billing_only_reporting_details_response.connect(func(res: Dictionary): alternative_billing_only_reporting_details_response.emit(res))
	if _plugin.has_signal("alternative_billing_only_information_dialog_response"):
		_plugin.alternative_billing_only_information_dialog_response.connect(func(res: Dictionary): alternative_billing_only_information_dialog_response.emit(res))
	if _plugin.has_signal("alternative_billing_only_transaction_reported"):
		_plugin.alternative_billing_only_transaction_reported.connect(func(res: Dictionary): alternative_billing_only_transaction_reported.emit(res))

	# External Offer
	if _plugin.has_signal("external_offer_availability_response"):
		_plugin.external_offer_availability_response.connect(func(res: Dictionary): external_offer_availability_response.emit(res))
	if _plugin.has_signal("external_offer_reporting_details_response"):
		_plugin.external_offer_reporting_details_response.connect(func(res: Dictionary): external_offer_reporting_details_response.emit(res))
	if _plugin.has_signal("external_offer_information_dialog_response"):
		_plugin.external_offer_information_dialog_response.connect(func(res: Dictionary): external_offer_information_dialog_response.emit(res))

	# Billing Program
	if _plugin.has_signal("billing_program_availability_response"):
		_plugin.billing_program_availability_response.connect(func(res: Dictionary): billing_program_availability_response.emit(res))
	if _plugin.has_signal("billing_program_reporting_details_response"):
		_plugin.billing_program_reporting_details_response.connect(func(res: Dictionary): billing_program_reporting_details_response.emit(res))
	if _plugin.has_signal("billing_program_information_dialog_response"):
		_plugin.billing_program_information_dialog_response.connect(func(res: Dictionary): billing_program_information_dialog_response.emit(res))

	# Billing Choice Info & Launch External Link
	if _plugin.has_signal("billing_choice_info_response"):
		_plugin.billing_choice_info_response.connect(func(res: Dictionary): billing_choice_info_response.emit(res))
	if _plugin.has_signal("launch_external_link_response"):
		_plugin.launch_external_link_response.connect(func(res: Dictionary): launch_external_link_response.emit(res))


# --- Публичные методы API ---

## Проверка готовности плагина к работе
func is_ready() -> bool:
	return _plugin.isReady() if _plugin else false


## Получить состояние подключения BillingClient (0=DISCONNECTED, 1=CONNECTING, 2=CONNECTED, 3=CLOSED)
func get_connection_state() -> int:
	return _plugin.getConnectionState() if _plugin else 0


## Проверить поддержку функции Google Play Billing
func is_feature_supported(feature: String) -> Dictionary:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return {"response_code": BillingResponseCode.ERROR, "debug_message": "Plugin not initialized"}
	return _plugin.isFeatureSupported(feature)


## Тестовый запрос к плагину. Ответ придёт в сигнал iap_hello_response / helloResponse
func say_hello(message: String = "Hello from GDScript") -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.sayHello(message)


## Ручной запуск подключения к Google Play Billing
func start_connection() -> void:
	if _plugin == null:
		_initialize_plugin()
	else:
		_plugin.startConnection()


## Принудительное завершение соединения с Google Play Billing
func end_connection() -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.endConnection()


## Запрос конфигурации биллинга (например, код страны пользователя)
func get_billing_config() -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, get_billing_config пропущен.")
		return
	_plugin.getBillingConfig()


## Запрос деталей продуктов (цены, офферы, теги).
## Результат приходит в iap_product_details_received и query_product_details
func query_details(product_ids: Array[String], type: String = TYPE_INAPP) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, query_details пропущен.")
		return
	_plugin.queryProductDetails(product_ids, type)


## Прямой метод запроса деталей продуктов (соответствует Kotlin queryProductDetails)
func query_product_details(product_ids: Array[String], type: String = TYPE_INAPP) -> void:
	query_details(product_ids, type)


## Запрос текущих активных покупок пользователя.
## Результат приходит в iap_purchases_queried и query_purchases. Вызывай при старте приложения!
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


## Прямой метод запуска покупки (соответствует Kotlin purchase)
func purchase(product_ids: Array[String], is_personalized: bool = false, offer_token: String = "") -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, purchase пропущен.")
		return
	_plugin.purchase(product_ids, is_personalized, offer_token)


## Покупка подписки. base_plan_id обязателен, offer_id — опционален
func buy_subs(id: String, base_plan_id: String, offer_id: String = "", is_personalized: bool = false) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, subscribe пропущен.")
		return

	# Формирование списка предложений (если offer_id пустой, передаем пустой массив)
	var offer_ids: Array[String] = []
	if not offer_id.is_empty():
		offer_ids.append(offer_id)

	# Запуск покупки подписки
	_plugin.subscribe([id], [base_plan_id], offer_ids, is_personalized)


## Обновление существующей подписки (upgrade / downgrade).
## old_token — purchase_token старой подписки, old_id — product_id старой подписки.
## offer_id — опциональный ID предложения/скидки для новой подписки.
func update_subs(
	id: String,
	base_plan_id: String,
	old_token: String,
	old_id: String,
	mode: ReplacementMode = ReplacementMode.WITH_TIME_PRORATION,
	offer_id: String = "",
	is_personalized: bool = false
) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, update_subscription пропущен.")
		return

	# Формирование списка предложений (если offer_id пустой, передаем пустой массив)
	var offer_ids: Array[String] = []
	if not offer_id.is_empty():
		offer_ids.append(offer_id)

	# Запуск обновления подписки
	_plugin.updateSubscription([id], [base_plan_id], offer_ids, is_personalized, old_token, old_id, int(mode))


## Потребление (consume) для расходников. Открывает возможность повторной покупки
func consume(token: String) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, consume пропущен.")
		return
	_plugin.consumePurchase(token)


## Прямой метод потребления покупки (соответствует Kotlin consumePurchase)
func consume_purchase(token: String) -> void:
	consume(token)


## Подтверждение (acknowledge) для постоянных покупок. Обязательно в течение 3 дней!
func acknowledge(token: String) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, acknowledge пропущен.")
		return
	_plugin.acknowledgePurchase(token)


## Прямой метод подтверждения покупки (соответствует Kotlin acknowledgePurchase)
func acknowledge_purchase(token: String) -> void:
	acknowledge(token)


## Показать системные In-App сообщения Google Play (например, истёкшая карта)
func show_in_app_messages() -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.showInAppMessages()


## Запустить flow подтверждения изменения цены подписки (устарело в Billing 7+)
func launch_price_change_flow(product_details: Dictionary) -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.launchPriceChangeConfirmationFlow(product_details)


## Проверить доступность Alternative Billing Only
func is_alternative_billing_only_available() -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, is_alternative_billing_only_available пропущен.")
		return
	_plugin.isAlternativeBillingOnlyAvailable()


## Создать детали отчетности Alternative Billing Only
func create_alternative_billing_only_reporting_details() -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, create_alternative_billing_only_reporting_details пропущен.")
		return
	_plugin.createAlternativeBillingOnlyReportingDetails()


## Показать диалог информации Alternative Billing Only
func show_alternative_billing_only_information_dialog() -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.showAlternativeBillingOnlyInformationDialog()


## Отправить отчет о транзакции Alternative Billing Only (устарело в Billing Library 7+)
func report_alternative_billing_only_transaction(reporting_details: Dictionary) -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.reportAlternativeBillingOnlyTransaction(reporting_details)


## Проверить доступность External Offer
func is_external_offer_available() -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, is_external_offer_available пропущен.")
		return
	_plugin.isExternalOfferAvailable()


## Создать детали отчетности External Offer
func create_external_offer_reporting_details() -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, create_external_offer_reporting_details пропущен.")
		return
	_plugin.createExternalOfferReportingDetails()


## Показать диалог информации External Offer
func show_external_offer_information_dialog() -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.showExternalOfferInformationDialog()


## Проверить доступность Billing Program
func is_billing_program_available(program_type: int) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, is_billing_program_available пропущен.")
		return
	_plugin.isBillingProgramAvailable(program_type)


## Создать детали отчетности Billing Program
func create_billing_program_reporting_details(program_type: int, developer_billing_type: int = 0) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, create_billing_program_reporting_details пропущен.")
		return
	_plugin.createBillingProgramReportingDetails(program_type, developer_billing_type)


## Показать диалог информации Billing Program
func show_billing_program_information_dialog(program_type: int, external_transaction_token: String = "") -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.showBillingProgramInformationDialog(program_type, external_transaction_token)


## Запрос информации о выборе способов оплаты (Billing Choice Info)
func get_billing_choice_info(program_type: int = 0) -> void:
	if not is_ready():
		push_warning("[GOOGLE_PLAY_BILLING]: Плагин не готов, get_billing_choice_info пропущен.")
		return
	_plugin.getBillingChoiceInfo(program_type)


## Открыть внешнюю ссылку (Launch External Link)
func launch_external_link(link_uri: String, link_type: int = 0, launch_mode: int = 0, program_type: int = 0, external_transaction_token: String = "") -> void:
	if not _plugin:
		push_error("[GOOGLE_PLAY_BILLING]: Плагин не инициализирован!")
		return
	_plugin.launchExternalLink(link_uri, link_type, launch_mode, program_type, external_transaction_token)


# --- Внутренние обработчики ---

# Обработка полученных деталей продуктов
func _on_product_details_received(response: Dictionary) -> void:
	var raw_list: Array = response.get("product_details_list", [])
	var raw_unfetched: Array = response.get("unfetched_product_list", [])

	var list: Array[Dictionary] = []
	list.assign(raw_list)

	var unfetched: Array[Dictionary] = []
	unfetched.assign(raw_unfetched)

	var type: String = TYPE_INAPP
	if list.size() > 0:
		type = list[0].get("product_type", TYPE_INAPP)

	iap_product_details_received.emit(list, unfetched, type)
