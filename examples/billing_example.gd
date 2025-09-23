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

#region Signals
# Emitted when product details are received from the store.
signal product_details_received(product_id: String, price: String)
# Emitted when a purchase is successfully completed, consumed, or acknowledged.
signal purchase_successful(product_id: String)
# Emitted when a purchase fails or any other billing error occurs.
signal purchase_failed(product_id: String, error: Dictionary)
#endregion


#region Enums
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
#endregion


#region Constants
# Consumable items are designed to be consumed and repurchased.
const ITEM_CONSUMATED: Array = [
	"additional_life_v1",
]

# Acknowledged items are purchased once and permanently owned.
const ITEM_ACKNOWLEDGED: Array = [
	"red_skin_v1",
	"blue_skin_v1",
	"yellow_skin_v1",
]

# Subscription items provide access to content for a period of time.
const SUBSCRIPTIONS: Array = [
	"remove_ads_sub_01",
	"test_iapp_v7",
]
#endregion


#region Variables
var billing = null
#endregion


#region Godot Lifecycle
# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	# A short delay to ensure the singleton is fully initialized, especially in autoload scenarios.
	await get_tree().create_timer(1).timeout
	run_iapp_billing()
#endregion


#region Public Interface
# Call these methods from other parts of your game to initiate purchases.

# Initiates a purchase for a one-time product.
func do_purchase(id: String, is_personalized: bool = false):
	_call_billing("purchase", [[id], is_personalized])


# Initiates a purchase for a subscription.
func do_subsciption(subscription_id: String, base_plan_id: String, is_personalized: bool = false):
	_call_billing("subscribe", [[subscription_id], [base_plan_id], is_personalized])


# A helper function to print purchase details for debugging.
func print_purchases(purchases: Array):
	for purchase in purchases:
		print(JSON.stringify(purchase, "  "))
#endregion


#region Initialization
func run_iapp_billing():
	if Engine.has_singleton("AndroidIAPP"):
		# Get the singleton instance of AndroidIAPP
		billing = Engine.get_singleton("AndroidIAPP")
		print("AndroidIAPP singleton loaded")
		
		_connect_billing_signals()
		
		# Start the connection process
		_call_billing("startConnection")
	else:
		printerr("AndroidIAPP singleton not found")


# Connects all signals from the AndroidIAPP singleton to their handlers.
func _connect_billing_signals():
	# Connection information
	billing.helloResponse.connect(_on_hello_response)
	billing.startConnection.connect(_on_start_connection)
	billing.connected.connect(_on_connected)
	billing.disconnected.connect(_on_disconnected)

	# Querying purchases
	billing.query_purchases.connect(_on_query_purchases)
	billing.query_purchases_error.connect(_on_billing_error.bind("query_purchases"))

	# Querying products details
	billing.query_product_details.connect(query_product_details)
	billing.query_product_details_error.connect(_on_billing_error.bind("query_product_details"))

	# Purchase processing
	billing.purchase.connect(_on_purchase)
	billing.purchase_error.connect(_on_billing_error.bind("purchase"))

	# Purchase updating
	billing.purchase_updated.connect(_on_purchase_updated)
	billing.purchase_cancelled.connect(_on_purchase_cancelled)
	billing.purchase_update_error.connect(_on_billing_error.bind("purchase_update"))

	# Purchase consuming
	billing.purchase_consumed.connect(_on_purchase_consumed)
	billing.purchase_consumed_error.connect(_on_billing_error.bind("purchase_consumed"))

	# Purchase acknowledging
	billing.purchase_acknowledged.connect(_on_purchase_acknowledged)
	billing.purchase_acknowledged_error.connect(_on_billing_error.bind("purchase_acknowledged"))
#endregion


#region Signal Handlers
# These functions are callbacks for signals emitted by the AndroidIAPP plugin.

# ---- Connection Handlers ----

func _on_start_connection() -> void:
	print("Billing: start connection")


func _on_connected() -> void:
	print("Billing successfully connected")
	await get_tree().create_timer(0.4).timeout

	var is_ready = _call_billing("isReady")
	if is_ready:
		# Optional: Test call to the library
		# _call_billing("sayHello", ["Hello from Godot Google IAPP plugin :)"])

		# Query for products available to buy
		_call_billing("queryProductDetails", [ITEM_ACKNOWLEDGED, "inapp"])
		_call_billing("queryProductDetails", [ITEM_CONSUMATED, "inapp"])
		_call_billing("queryProductDetails", [SUBSCRIPTIONS, "subs"])

		# Query for any purchases made outside the app
		_call_billing("queryPurchases", ["subs"])
		_call_billing("queryPurchases", ["inapp"])


func _on_disconnected() -> void:
	print("Billing disconnected")


func _on_hello_response(response: String) -> void:
	print("Hello signal response: " + response)


# ---- Product & Purchase Query Handlers ----

func query_product_details(response: Dictionary) -> void:
	for product in response["product_details_list"]:
		var product_id: String = product["product_id"]
		var price: String = product["one_time_purchase_offer_details"]["formatted_price"]
		product_details_received.emit(product_id, price)


func _on_query_purchases(response: Dictionary) -> void:
	print("Query Purchases Response: ")
	for purchase in response["purchases_list"]:
		process_purchase(purchase)


# ---- Purchase Flow Handlers ----

func _on_purchase_updated(response: Dictionary):
	for purchase in response["purchases_list"]:
		process_purchase(purchase)


# Processes an incoming purchase, deciding whether to acknowledge or consume it.
func process_purchase(purchase: Dictionary):
	for product in purchase["products"]:
		var is_acknowledged = purchase.get("is_acknowledged", false)

		if (product in ITEM_ACKNOWLEDGED) or (product in SUBSCRIPTIONS):
			if not is_acknowledged:
				print("Granting entitlement for: " + product)
				purchase_successful.emit(product)

				print("Acknowledging: " + purchase["purchase_token"])
				_call_billing("acknowledgePurchase", [purchase["purchase_token"]])
			else:
				print("Granting entitlement for already acknowledged product: " + product)
				purchase_successful.emit(product)

		elif product in ITEM_CONSUMATED:
			print("Granting consumable item: " + product)
			purchase_successful.emit(product)

			print("Consuming: " + purchase["purchase_token"])
			_call_billing("consumePurchase", [purchase["purchase_token"]])
		else:
			printerr("Product '%s' not found in local product lists." % str(product))


func _on_purchase(response: Dictionary) -> void:
	print("Purchase flow started:")
	print(JSON.stringify(response, "  "))


func _on_purchase_cancelled(response: Dictionary) -> void:
	print("Purchase cancelled by user:")
	print(JSON.stringify(response, "  "))


func _on_purchase_consumed(response: Dictionary) -> void:
	print("Purchase consumed successfully: %s" % JSON.stringify(response, "  "))


func _on_purchase_acknowledged(response: Dictionary) -> void:
	print("Purchase acknowledged successfully: %s" % JSON.stringify(response, "  "))


# ---- Error Handler ----

func _on_billing_error(error: Dictionary, context: String) -> void:
	printerr("Billing Error during '%s': %s" % [context, JSON.stringify(error, "  ")])

	var error_with_context = error.duplicate()
	error_with_context["context"] = context
	purchase_failed.emit("", error_with_context)
#endregion


#region Private Helpers
# Safely calls a method on the billing singleton.
func _call_billing(method: String, args: Array = []) -> Variant:
	if billing and billing.has_method(method):
		return billing.callv(method, args)
	else:
		var error_msg = "Billing method not found: %s. The AndroidIAPP plugin may be missing, outdated, or not initialized." % method
		printerr(error_msg)
		# Also emit a signal so the UI can react
		var error_dict = {
			"response_code": -100, # Custom code for missing method
			"debug_message": error_msg,
			"context": "method_call"
		}
		purchase_failed.emit("", error_dict)
		return null
#endregion
