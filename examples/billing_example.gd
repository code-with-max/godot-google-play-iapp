# AndroidIAPP is a plugin for the Godot game engine. 
# It provides an interface to work with Google Play Billing Library version 7. 
# The plugin supports all public functions of the library, passes all error codes, and can work with different subscription plans.
# https://developer.android.com/google/play/billing
#
# You can use this plugin with any node in Godot.
# But, I recommend adding this script as a singleton (autoload).
# This makes it easier to access and use its functions from anywhere in your project.
#
# An example of working with a plugin:


extends Node


signal already_buyed_updated
signal product_showcase_updated


# https://developer.android.com/reference/com/android/billingclient/api/Purchase.PurchaseState
enum purchaseState {
	UNSPECIFIED_STATE = 0,
	PURCHASED = 1,
	PENDING = 2,
	}


# https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode
enum billingResponseCode {
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
	NETWORK_ERROR = 12
	}


const PLUGIN_NAME: String = "AndroidIAPP"

const ITEM_CONSUMATED: Array = [
				"additional_life_v1",
				]

const ITEM_ACKNOWLEDGED: Array = [
				"red_skin_v1",
				"blue_skin_v1",
				"yellow_skin_v1",
				]

const SUBSCRIPTIONS: Array = [
	"remove_ads_sub_01",
	"test_iapp_v7",
	]


var billing = null


# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	# await get_tree().create_timer(0.4).timeout
	run_iapp_billing()


func run_iapp_billing():
	if Engine.has_singleton(PLUGIN_NAME):
		# Get the singleton instance of AndroidIAPP
		billing = Engine.get_singleton(PLUGIN_NAME)
		
		# Connection information
		# Handle the response from the helloResponse signal
		billing.helloResponse.connect(_on_hello_response)
		# Handle the startConnection signal
		billing.startConnection.connect(_on_start_connection)
		# Handle the connected signal
		billing.connected.connect(_on_connected)
		# Handle the disconnected signal
		billing.disconnected.connect(_on_disconnected)
		
		# Querying purchases
		# Handle the response from the query_purchases signal
		billing.query_purchases.connect(_on_query_purchases)
		# Handle the query_purchases_error signal
		billing.query_purchases_error.connect(_on_query_purchases_error)
		
		# Querying products details
		# Handle the response from the query_product_details signal
		billing.query_product_details.connect(_on_query_product_details)
		# Handle the query_product_details_error signal
		billing.query_product_details_error.connect(_on_query_product_details_error)
		
		# Purchase processing
		# Handle the purchase signal
		billing.purchase.connect(_on_purchase)
		# Handle the purchase_error signal
		billing.purchase_error.connect(_on_purchase_error)
		
		# Purchase updating
		# Handle the purchase_updated signal
		billing.purchase_updated.connect(_on_purchase_updated)
		# Handle the purchase_cancelled signal
		billing.purchase_cancelled.connect(_on_purchase_cancelled)
		# Handle the purchase_update_error signal
		billing.purchase_update_error.connect(_on_purchase_update_error)
		
		# Purchase consuming
		# Handle the purchase_consumed signal
		billing.purchase_consumed.connect(_on_purchase_consumed)
		# Handle the purchase_consumed_error signal
		billing.purchase_consumed_error.connect(_on_purchase_consumed_error)
		
		# Purchase acknowledging
		# Handle the purchase_acknowledged signal
		billing.purchase_acknowledged.connect(_on_purchase_acknowledged)
		# Handle the purchase_acknowledged_error signal
		billing.purchase_acknowledged_error.connect(_on_purchase_acknowledged_error)

		# Billing Info signal
		billing.billing_info.connect(_on_billing_info_received)

		# Not implemented in plugin yet
		billing.price_change_acknowledged.connect(_on_price_change_acknowledged)
		billing.price_change_error.connect(_on_price_change_error)
		billing.in_app_message_result.connect(_on_in_app_message_result)
		billing.alternative_billing_only_transaction_reported.connect(_on_alternative_billing_only_transaction_reported)

		await get_tree().create_timer(1).timeout
		connect_to_billing()
		
	else:
		printerr("%s singleton not found" % PLUGIN_NAME)


func connect_to_billing():
	if billing and not billing.isReady():
		print("%s: Starting connection to Google Play Billing..." % PLUGIN_NAME)
		billing.startConnection()
	else:
		print("%s: Billing is already connected" % PLUGIN_NAME)


func disconnect_billing():
	if billing and billing.isReady():
		print("%s: Disconnecting from Google Play Billing..." % PLUGIN_NAME)
		billing.endConnection()
	else:
		print("%s: Billing is already disconnected" % PLUGIN_NAME)


func _on_start_connection() -> void:
	print("%s: start connection" % PLUGIN_NAME)


func _on_connected() -> void:
	print("%s: Billing successfully connected" % PLUGIN_NAME)
	G.inapp_already_connected = true
	await get_tree().create_timer(0.2).timeout
	billing.sayHello("Hello from Godot Google IAPP plugin :)")
	# Show products available to buy
	# https://developer.android.com/google/play/billing/integrate#show-products
	billing.queryProductDetails(ITEM_ACKNOWLEDGED, "inapp")
	billing.queryProductDetails(ITEM_CONSUMATED, "inapp")
	billing.queryProductDetails(SUBSCRIPTIONS, "subs")
	# Handling purchases made outside your app
	# https://developer.android.com/google/play/billing/integrate#ooap
	billing.queryPurchases("subs")
	billing.queryPurchases("inapp")
	# Test not implemented plugin functions responses
	billing.showInAppMessages()
	billing.createAlternativeBillingOnlyReportingDetails()



func is_ready() -> bool:
	return billing.isReady()


func _on_disconnected() -> void:
	print("%s: Billing disconnected" % PLUGIN_NAME)


func _on_hello_response(response) -> void:
	print("%s: Hello signal response: %s" % [PLUGIN_NAME, response])


func _on_query_product_details(response) -> void:
	for product in response["product_details_list"]:
		G.product_showcase.append(product)
	product_showcase_updated.emit()


func _on_query_purchases(response) -> void:
	print("%s: on_query_Purchases_response: " % PLUGIN_NAME)
	for purchase in response["purchases_list"]:
		process_purchase(purchase)


func _on_purchase_updated(response):
	for purchase in response["purchases_list"]:
		process_purchase(purchase)
	

# Processing incoming purchase
func process_purchase(purchase):
	for product in purchase["products"]:
		if (product in ITEM_ACKNOWLEDGED) or (product in SUBSCRIPTIONS):
			# Acknowledge the purchase
			process_acknowledged(purchase)
		elif product in ITEM_CONSUMATED:
			# Consume the purchase
			process_consumed(purchase)
		else:
			# Product not found in app showcase
			print("%s: Product not found in app showcase: %s" % [PLUGIN_NAME, str(purchase["products"])])


func process_acknowledged(purchase):
	for product in purchase["products"]:
		if purchase["is_acknowledged"]:
			# Already acknowledged, proccesing it in game
			print("%s: Already acknowledged: %s" % [PLUGIN_NAME, str(purchase["products"])])
			update_already_buyed(product)
		else:
			if purchase["purchase_state"] == 1:
				# Money already received
				print("%s: Acknowledging: %s" % [PLUGIN_NAME, str(purchase["products"])])
				billing.acknowledgePurchase(purchase["purchase_token"])
				update_already_buyed(product)
			else:
				# Just waiting for money  and do nothing
				print("%s: Product pending: %s" % [PLUGIN_NAME, str(purchase["products"])])


func process_consumed(purchase):
	for product in purchase["products"]:
		if not purchase["purchase_state"] == 1:
			# Just waiting for money  and do nothing
			print("%s: Product pending: %s" % [PLUGIN_NAME, str(purchase["products"])])
		else:
			# Money already received, proccesing it in game
			match product:
				"additional_life_v1":
					# Add 1 life
					billing.consumePurchase(purchase["purchase_token"])
					G.increase_lives()


# Use "products": ["blue_skin_v1"] from purchased product
func update_already_buyed(p):
	G.already_buyed.append(p)
	already_buyed_updated.emit()
	print("%s: %s : added to already_buyed list" % [PLUGIN_NAME, p])


# Purchase
func do_purchase(id: String, is_personalized: bool = false):
	billing.purchase([id], is_personalized)

# Subscriptions
func do_subsciption(subscription_id: String, base_plan_id: String , is_personalized: bool = false):
	billing.subscribe([subscription_id], [base_plan_id], is_personalized)


func print_purchases(purchases: Dictionary):
	for purchase in purchases:
		_print_json_with_prefix(PLUGIN_NAME, purchase)


func _on_purchase(response: Dictionary) -> void:
	print("%s: Purchase started:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, response)


func _on_purchase_cancelled(response: Dictionary) -> void:
	print("%s: Purchase_cancelled:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, response)


func _on_purchase_consumed(response: Dictionary) -> void:
	print("%s: Purchase_consumed:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, response)


func _on_purchase_acknowledged(response: Dictionary) -> void:
	print("%s: Purchase_acknowledged:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, response)


func _on_purchase_update_error(error: Dictionary) -> void:
	print("%s: Purchase_update_error:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, error)


func _on_purchase_error(error: Dictionary) -> void:
	print("%s: Purchase_error:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, error)


func _on_purchase_consumed_error(error: Dictionary) -> void:
	print("%s: Purchase_consumed_error:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, error)


func _on_purchase_acknowledged_error(error: Dictionary) -> void:
	print("%s: Purchase_acknowledged_error:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, error)


func _on_query_purchases_error(error: Dictionary) -> void:
	print("%s: Query_purchases_error:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, error)


func _on_query_product_details_error(error: Dictionary) -> void:
	print("%s: Query_product_details_error:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, error)


func _on_billing_info_received(info: Dictionary) -> void:
	print("%s: Billing_info_received:" % PLUGIN_NAME)
	_print_json_with_prefix(PLUGIN_NAME, info)



# Helpers
func _print_json_with_prefix(prefix: String, data: Dictionary) -> void:
	var json_string = JSON.stringify(data, "  ")
	for line in json_string.split("\n"):
		print("%s: %s" % [prefix, line])


# Not implemented in plugin yet
# Return mock response with error

func _on_price_change_acknowledged(response: Dictionary) -> void:
	# Newer will be received, because plugin does not support it yet
	print("%s: Price_change_acknowledged:" % PLUGIN_NAME)
	print(JSON.stringify(response, "  "))


func _on_price_change_error(error: Dictionary) -> void:
	print("%s: Price_change_error:" % PLUGIN_NAME)
	print(JSON.stringify(error, "  "))


func _on_in_app_message_result(response: Dictionary) -> void:
	print("%s: In_app_message_result:" % PLUGIN_NAME)
	print(JSON.stringify(response, "  "))


func _on_alternative_billing_only_transaction_reported(response: Dictionary) -> void:
	print("%s: Alternative_billing_only_transaction_reported:" % PLUGIN_NAME)
	print(JSON.stringify(response, "  "))