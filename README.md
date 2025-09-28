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

## SIMPLE DEBUG

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

## Examples

- [Example script](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/billing_example.gd) for working with the plugin.
- [Another example](https://gist.github.com/code-with-max/56881cbb3796a19a68d8eabd819d6ff7).
- [Sample AutoLoad script](https://gist.github.com/nitish800/60a1f3b6e746805b67a68395ca8f4ca6) to initialize the plugin and handle signals.

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

## Signals Descriptions

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

## Functions

`startConnection()`: Starts the connection to Google Play Billing.
- Emits: `startConnection`, `connected`, or `disconnected` (if activity is unavailable).
- **Warning**: Ensure the plugin is initialized after Godot's Android activity is available.

`isReady()`: Checks if the billing connection is ready.
- **Returns**: `bool`.

`sayHello(message: String = "Hello from AndroidIAPP plugin")`: Sends a test message.
- Emits: `helloResponse`.
- Displays a Toast and logs to the console.
- **Warning**: May fail with `"Error: Activity is null"` if called too early. Avoid in production.

`queryPurchases(productType: String)`: Queries purchases.
- `productType`: `"inapp"` or `"subs"`.
- Emits: `query_purchases` or `query_purchases_error`.
- **Note**: Call after `connected` signal to ensure billing is ready.
- **Warning**: Due to Godot JNI limitations, `productType` must be specified. Future updates may include `queryInAppPurchases()` and `querySubscriptions()`.

`queryProductDetails(productId: List<String>, productType: String)`: Queries product or subscription details.
- `productId`: List of product/subscription IDs (must not be empty).
- `productType`: `"inapp"` or `"subs"`.
- Emits: `query_product_details` or `query_product_details_error`.
- **Warning**: Passing an empty `productId` list or incorrect `productType` triggers `query_product_details_error`.

`purchase(product_id: List<String>, is_personalized: bool)`: Initiates a product purchase.
- `product_id`: List of product IDs (must not be empty).
- `is_personalized`: Set to `false` unless complying with [EU directive](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:02011L0083-20220528) (see [details](https://developer.android.com/google/play/billing/integrate#personalized-price)).
- Emits: `purchase_updated`, `purchase_error`, `purchase_cancelled`, `purchase_update_error`, or `query_product_details_error`.
- **Warning**: Empty or invalid `product_id` triggers `purchase_error`.
- **Important**: Call `consumePurchase` or `acknowledgePurchase` to complete the transaction.

`subscribe(subscription_id: List<String>, base_plan_id: List<String>, is_personalized: bool)`: Initiates a subscription.
- `subscription_id`, `base_plan_id`: Lists of IDs (must not be empty).
- `is_personalized`: Set to `false` unless complying with EU directive.
- Emits: `purchase_updated`, `purchase_error`, `purchase_cancelled`, `purchase_update_error`, or `query_product_details_error`.
- **Warning**: Empty or invalid `subscription_id`/`base_plan_id` triggers `purchase_error`.
- **Important**: Call `acknowledgePurchase` to complete the subscription.

`consumePurchase(purchase_token: String)`: Consumes a purchase.
- `purchase_token`: Token from `purchase_updated` response.
- Emits: `purchase_consumed` or `purchase_consumed_error`.
- **Warning**: Invalid or empty `purchase_token` triggers `purchase_consumed_error`.

`acknowledgePurchase(purchase_token: String)`: Acknowledges a purchase or subscription.
- `purchase_token`: Token from `purchase_updated` response.
- Emits: `purchase_acknowledged` or `purchase_acknowledged_error`.
- **Warning**: Invalid or empty `purchase_token` triggers `purchase_acknowledged_error`.

## Step-by-step Set Up Guide

### 1. Connecting to the Google Play Billing Library
1. Enable the plugin in `Project > Project Settings > Plugins`.
2. Add `com.android.vending.BILLING` permission in `Project > Export > Permissions > Custom Permissions`.
3. Add an AutoLoad script (e.g., `iap.gd`) to initialize the plugin:
   ```gdscript
   extends Node
   var billing = null
   func _ready():
       if Engine.has_singleton("AndroidIAPP"):
           billing = Engine.get_singleton("AndroidIAPP")
           billing.connected.connect(_on_connected)
           billing.disconnected.connect(_on_disconnected)
           billing.startConnection()
       else:
           printerr("AndroidIAPP singleton not found!")
   func _on_connected():
       print("Connected to Google Play Billing")
   func _on_disconnected():
       print("Billing disconnected")
   ```

### 2. Requesting a List of Products and Subscriptions
- Query product details:
  ```gdscript
  billing.queryProductDetails(["blue_skin_v1"], "inapp")
  billing.query_product_details.connect(_on_query_product_details)
  func _on_query_product_details(response):
      for product in response.product_details_list:
          print("Product: ", product.name, ", Price: ", product.one_time_purchase_offer_details.formatted_price)
  ```
- Query purchases:
  ```gdscript
  billing.queryPurchases("inapp")
  billing.query_purchases.connect(_on_query_purchases)
  func _on_query_purchases(response):
      for purchase in response.purchases_list:
          print("Purchase: ", purchase.products, ", State: ", purchase.purchase_state)
  ```

### 3. Handling Purchases and Subscriptions
- Make a purchase:
  ```gdscript
  billing.purchase(["blue_skin_v1"], false)
  billing.purchase_updated.connect(_on_purchase_updated)
  func _on_purchase_updated(response):
      for purchase in response.purchases_list:
          if purchase.purchase_state == 1:  # PURCHASED
              if purchase.products.has("blue_skin_v1"):
                  billing.acknowledgePurchase(purchase.purchase_token)
  ```
- Subscribe to a plan:
  ```gdscript
  billing.subscribe(["remove_ads_sub_01"], ["remove-ads-on-year"], false)
  billing.purchase_updated.connect(_on_subscription_updated)
  func _on_subscription_updated(response):
      for purchase in response.purchases_list:
          if purchase.purchase_state == 1:  # PURCHASED
              billing.acknowledgePurchase(purchase.purchase_token)
  ```

### 4. Confirming and Consuming Purchases
- Consume a purchase:
  ```gdscript
  billing.consumePurchase(purchase.purchases_list[0].purchase_token)
  billing.purchase_consumed.connect(_on_purchase_consumed)
  func _on_purchase_consumed(response):
      print("Consumed: ", response.purchase_token)
  ```
- Acknowledge a purchase:
  ```gdscript
  billing.acknowledgePurchase(purchase.purchases_list[0].purchase_token)
  billing.purchase_acknowledged.connect(_on_purchase_acknowledged)
  func _on_purchase_acknowledged(response):
      print("Acknowledged: ", response.purchase_token)
  ```

### 5. Handling Errors and Purchase States
- Handle errors:
  ```gdscript
  billing.purchase_error.connect(_on_purchase_error)
  func _on_purchase_error(error):
      print("Purchase error: ", error.debug_message, ", Product: ", error.product_id)
  billing.query_product_details_error.connect(_on_query_product_details_error)
  func _on_query_product_details_error(error):
      print("Product details error: ", error.debug_message)
  ```
- Check purchase state (from `iap.txt`):
  ```gdscript
  enum purchaseState { UNSPECIFIED_STATE = 0, PURCHASED = 1, PENDING = 2 }
  func process_purchase(purchase):
      if purchase.purchase_state == purchaseState.PURCHASED:
          print("Processing purchase: ", purchase.products)
      else:
          print("Purchase pending: ", purchase.products)
  ```

## Common Errors

- `"Activity is null"`: The Android activity is not available. Ensure the plugin is called after Godot initialization.
- `"Product ID list is empty"`: The `product_id` or `base_plan_id` list is empty. Always pass non-empty lists.
- `"Base Plan ID not found"`: The `base_plan_id` does not match any subscription plan in Google Play Console.
- `"No product details found"`: The product ID or type is invalid. Verify IDs in Google Play Console.
- `"Purchase token is blank"`: The `purchase_token` is empty or invalid. Check the `purchase_updated` response.
