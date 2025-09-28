# AndroidIAPP Godot Plugin

AndroidIAPP is a [plugin](<https://docs.godotengine.org/en/stable/tutorials/plugins/editor/installing_plugins.html#installing-a-plugin>) for the Godot game engine. It provides an interface to work with Google Play Billing Library version 7.1.1. The plugin supports all public functions of the library, passes all error codes, and can work with different subscription plans.

A simple game to demonstrate the work of purchases and subscriptions with different tariff plans: [Circle Catcher 2](https://play.google.com/store/apps/details?id=org.godotengine.circlecatcher)

## Features

- Connect to Google Play Billing.
- Query purchases and product details.
- Make purchases and subscriptions.
- Update, consume, and acknowledge purchases.

## Installation

- Install the plugin using Godot [Asset Library](https://godotengine.org/asset-library/asset/3068).

  or

- Download the plugin from [GitHub](https://github.com/code-with-max/godot-google-play-iapp/releases).
- Place the unpacked plugin folder in the `res://addons/` directory of your project.

> [!NOTE]
> Don't forget to enable the plugin in `Project > Project Settings > Plugins`.

## Before Start

- **Purchase Types**:
  - Consumable products (`"inapp"`): Must be consumed using `consumePurchase`.
  - Non-consumable products (`"inapp"`): Must be acknowledged using `acknowledgePurchase`.
  - Subscriptions (`"subs"`): Must be acknowledged using `acknowledgePurchase`.
  - Product and subscription IDs must be passed as a list of strings, even for a single ID (e.g., `["product_id"]`).

- **Return Format**:
  - All methods return a Godot `Dictionary` with keys in snake_case (e.g., `getProductId` → `product_id`).
  - See example responses: [in-app product](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_inapp.json), [subscription](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_subscription.json), [purchase](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/purchase_updated_inapp.json).
  - Error responses include `response_code` (from [BillingResponseCode](https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode)) and `debug_message`.

- **Godot Compatibility**: Tested with Godot 4.5. Kotlin coroutines are not supported in Godot 4.2 or earlier.

## Debugging

- Ensure the plugin is activated in `Project > Project Settings > Plugins`.
- Check `AndroidIAPP.gd` to confirm the correct path to the AAR file:
  ```gdscript
  if debug:
      return PackedStringArray(["AndroidIAPP-debug.aar"])
  else:
      return PackedStringArray(["AndroidIAPP-release.aar"])
  ```
- Use logcat to check the logs:
  ```shell
  ./adb logcat | grep IAPP
  ```

## Usage Example

A full example of how to work with the plugin can be found in [`billing_example.gd`](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/billing_example.gd).

Here is how you can initialize the plugin and connect to its signals:

```gdscript
extends Node

const PLUGIN_NAME: String = "AndroidIAPP"
var billing = null

func _ready() -> void:
	if Engine.has_singleton(PLUGIN_NAME):
		billing = Engine.get_singleton(PLUGIN_NAME)

		# Connect to signals
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

		# Start the connection
		if not billing.isReady:
			billing.startConnection()
	else:
		printerr("%s singleton not found" % PLUGIN_NAME)

func _on_connected() -> void:
	print("%s: Billing successfully connected" % PLUGIN_NAME)
	# Now you can query for products and purchases
	billing.queryProductDetails(ITEM_ACKNOWLEDGED, "inapp")
	billing.queryPurchases("inapp")

# ... other signal handlers
```

## Signals

### Test Signal
- `helloResponse`: Emitted when a response to a hello message is received.
  - **Returns**: `String` (the message passed to `sayHello` or an error like `"Error: Activity is null"`).

### Information Signals
- `startConnection`: Emitted when the connection to Google Play Billing starts.
  - **Returns**: None.
- `connected`: Emitted when successfully connected.
  - **Returns**: None.
- `disconnected`: Emitted when disconnected or if the Android activity is unavailable.
  - **Returns**: `Dictionary` (e.g., `{"debug_message": "Activity is null"}`).

### Billing Signals
- `query_purchases`: Emitted when a purchase query is successful.
  - **Returns**: `Dictionary`
    - `response_code`: Integer (e.g., `BillingClient.BillingResponseCode.OK`).
    - `purchases_list`: Array of Dictionaries (see [purchase example](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/purchase_updated_inapp.json)).
- `query_purchases_error`: Emitted on purchase query errors.
  - **Returns**: `Dictionary`
    - `response_code`: Integer (e.g., `BillingClient.BillingResponseCode.ERROR`).
    - `debug_message`: String (e.g., `"No purchase found"`).
    - `purchases_list`: null.
- `query_product_details`: Emitted when a product details query is successful.
  - **Returns**: `Dictionary`
    - `response_code`: Integer.
    - `product_details_list`: Array of Dictionaries (see [in-app example](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_inapp.json), [subscription example](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_subscription.json)).
- `query_product_details_error`: Emitted on product details query errors.
  - **Returns**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String (e.g., `"No product details found"`).
- `purchase_updated`: Emitted when purchase information is updated.
  - **Returns**: `Dictionary`
    - `response_code`: Integer.
    - `purchases_list`: Array of Dictionaries (e.g., `{"product_id": "blue_skin_v1", "purchase_token": "...", "is_acknowledged": false}`).
- `purchase`: Emitted when a purchase flow is successfully initiated.
  - **Returns**: `Dictionary`
- `purchase_error`: Emitted on purchase errors.
  - **Returns**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String (e.g., `"Activity is null"`, `"Product ID list is empty"`).
    - `product_id`: String (if applicable).
    - `base_plan_id`: String (for subscriptions).
- `purchase_cancelled`: Emitted when a purchase is cancelled by the user.
  - **Returns**: `Dictionary`
    - `response_code`: Integer (e.g., `BillingClient.BillingResponseCode.USER_CANCELED`).
    - `debug_message`: String.
- `purchase_update_error`: Emitted on purchase update errors.
  - **Returns**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String.
- `purchase_consumed`: Emitted when a purchase is consumed.
  - **Returns**: `Dictionary`
    - `response_code`: Integer.
    - `purchase_token`: String.
- `purchase_consumed_error`: Emitted on consume errors.
  - **Returns**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String.
    - `purchase_token`: String.
- `purchase_acknowledged`: Emitted when a purchase is acknowledged.
  - **Returns**: `Dictionary`
    - `response_code`: Integer.
    - `purchase_token`: String.
- `purchase_acknowledged_error`: Emitted on acknowledge errors.
  - **Returns**: `Dictionary`
    - `response_code`: Integer.
    - `debug_message`: String.
    - `purchase_token`: String.
- `billing_info`: An informational signal for debugging.
  - **Returns**: `Dictionary`
    - `plugin_name`: String.
    - `fun_name`: String (e.g., `"sayHello"`, `"startConnection"`).
    - `debug_message`: String.
- `price_change_acknowledged`: **Not implemented.**
- `price_change_error`: **Not implemented.**
- `in_app_message_result`: **Not implemented.**
- `alternative_billing_only_transaction_reported`: **Not implemented.**

## Functions

`startConnection()`: Starts the connection to Google Play Billing.
- Emits: `startConnection`, `connected`, or `disconnected` (if activity is unavailable).
- **Warning**: Ensure the plugin is initialized after Godot's Android activity is available.

`endConnection()`: Ends the connection to the Google Play Billing service.

`isReady`: Checks if the billing connection is ready.
- **Returns**: `bool`.

`sayHello(says: String)`: Sends a test message.
- Emits: `helloResponse`.
- Displays a Toast and logs to the console.
- **Warning**: May fail with `"Error: Activity is null"` if called too early. Avoid in production.

`queryPurchases(productType: String)`: Queries purchases.
- `productType`: `"inapp"` or `"subs"`.
- Emits: `query_purchases` or `query_purchases_error`.

`queryProductDetails(listOfProductsIDs: Array<String>, productType: String)`: Queries product or subscription details.
- `listOfProductsIDs`: List of product/subscription IDs (must not be empty).
- `productType`: `"inapp"` or `"subs"`.
- Emits: `query_product_details` or `query_product_details_error`.

`purchase(listOfProductsIDs: Array<String>, isOfferPersonalized: bool)`: Initiates a product purchase.
- `listOfProductsIDs`: List of product IDs (must not be empty).
- `isOfferPersonalized`: Set to `false` unless complying with [EU directive](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:02011L0083-20220528) (see [details](https://developer.android.com/google/play/billing/integrate#personalized-price)).
- Emits: `purchase_updated`, `purchase_error`, `purchase_cancelled`, `purchase_update_error`, or `query_product_details_error`.
- **Important**: Call `consumePurchase` or `acknowledgePurchase` to complete the transaction.

`subscribe(listOfProductsIDs: Array<String>, basePlanIDs: Array<String>, isOfferPersonalized: bool)`: Initiates a subscription.
- `listOfProductsIDs`, `basePlanIDs`: Lists of IDs (must not be empty).
- `isOfferPersonalized`: Set to `false` unless complying with EU directive.
- Emits: `purchase_updated`, `purchase_error`, `purchase_cancelled`, `purchase_update_error`, or `query_product_details_error`.
- **Important**: Call `acknowledgePurchase` to complete the subscription.

`consumePurchase(purchaseToken: String)`: Consumes a purchase.
- `purchaseToken`: Token from `purchase_updated` response.
- Emits: `purchase_consumed` or `purchase_consumed_error`.

`acknowledgePurchase(purchaseToken: String)`: Acknowledges a purchase or subscription.
- `purchaseToken`: Token from `purchase_updated` response.
- Emits: `purchase_acknowledged` or `purchase_acknowledged_error`.

### Not Implemented Functions
The following functions are included in the plugin as stubs but are not yet implemented:
- `showInAppMessages()`
- `launchPriceChangeConfirmationFlow(productDetails: Dictionary)`
- `createAlternativeBillingOnlyReportingDetails()`
- `reportAlternativeBillingOnlyTransaction(reportingDetails: Dictionary)`

## Implementation Guide

### 1. Querying for Available Products
Once connected, you should query for the products you have set up in the Google Play Console.

```gdscript
const ITEM_CONSUMABLE: Array = ["additional_life_v1"]
const ITEM_NON_CONSUMABLE: Array = ["red_skin_v1", "blue_skin_v1"]
const SUBSCRIPTIONS: Array = ["remove_ads_sub_01"]

func _on_connected() -> void:
    print("%s: Billing successfully connected" % PLUGIN_NAME)
    # Query for different types of products
    billing.queryProductDetails(ITEM_CONSUMABLE, "inapp")
    billing.queryProductDetails(ITEM_NON_CONSUMABLE, "inapp")
    billing.queryProductDetails(SUBSCRIPTIONS, "subs")

func _on_query_product_details(response: Dictionary) -> void:
    for product in response["product_details_list"]:
        # Store product details to display in your shop UI
        G.product_showcase.append(product)
    product_showcase_updated.emit()
```

### 2. Initiating a Purchase
To start a purchase, call the `purchase` or `subscribe` function with the appropriate product and base plan IDs.

```gdscript
# For a one-time product (consumable or non-consumable)
func do_purchase(product_id: String):
    billing.purchase([product_id], false)

# For a subscription
func do_subscription(subscription_id: String, base_plan_id: String):
    billing.subscribe([subscription_id], [base_plan_id], false)
```

### 3. Processing Purchases
The `purchase_updated` signal is the central place to handle all new purchases. You need to determine whether to acknowledge or consume the item.

```gdscript
func _on_purchase_updated(response: Dictionary) -> void:
    for purchase in response["purchases_list"]:
        process_purchase(purchase)

func process_purchase(purchase: Dictionary) -> void:
    for product_id in purchase["products"]:
        if purchase["purchase_state"] != 1: # Not PURCHASED
            print("Purchase is pending for: %s" % product_id)
            return

        if product_id in ITEM_NON_CONSUMABLE or product_id in SUBSCRIPTIONS:
            # Acknowledge non-consumables and subscriptions
            if not purchase["is_acknowledged"]:
                billing.acknowledgePurchase(purchase["purchase_token"])
            else:
                # Grant entitlement
                print("Purchase already acknowledged: %s" % product_id)

        elif product_id in ITEM_CONSUMABLE:
            # Consume consumables
            billing.consumePurchase(purchase["purchase_token"])
```

### 4. Handling Consumed and Acknowledged Purchases
Listen to the corresponding signals to confirm the transaction is complete and update the user's entitlements.

```gdscript
func _on_purchase_consumed(response: Dictionary) -> void:
    print("Purchase consumed: %s" % response["purchase_token"])
    # Grant consumable item to the user (e.g., add a life)
    G.increase_lives()

func _on_purchase_acknowledged(response: Dictionary) -> void:
    print("Purchase acknowledged: %s" % response["purchase_token"])
    # Unlock feature or content
    G.unlock_skin()
```

### 5. Restoring Purchases
To restore purchases (e.g., when a user reinstalls the app), query for their active purchases.

```gdscript
func _on_connected() -> void:
    # ... query for product details ...

    # Query for existing purchases
    billing.queryPurchases("inapp")
    billing.queryPurchases("subs")

func _on_query_purchases(response: Dictionary) -> void:
    for purchase in response["purchases_list"]:
        process_purchase(purchase) # Use the same processing logic
```

## Common Errors

- `"Activity is null"`: The Android activity is not available. Ensure the plugin is called after Godot initialization.
- `"Product ID list is empty"`: The `product_id` or `base_plan_id` list is empty. Always pass non-empty lists.
- `"Base Plan ID not found"`: The `base_plan_id` does not match any subscription plan in Google Play Console.
- `"No product details found"`: The product ID or type is invalid. Verify IDs in Google Play Console.
- `"Purchase token is blank"`: The `purchase_token` is empty or invalid. Check the `purchase_updated` response.
