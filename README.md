# AndroidIAPP for Godot

AndroidIAPP connects your Godot Android game to Google Play Billing. Use it for one-time in-app products, subscriptions, and subscription offers.

If you'd rather learn from a working example, take a look at the [iapp_demo app](https://github.com/code-with-max/iapp_demo).

## Quick Start

### 1. Install the plugin

1. Copy `addon/android_IAPP` into your project's `res://addons/` folder.
2. Enable **AndroidIAPP** under **Project > Project Settings > Plugins**.
3. Add a `GooglePlayBilling` node to your scene. The examples below assume it is named `GooglePlayBilling` and is a child of the node running your script.

### 2. Configure Android export

In **Project > Export > Android**:

1. Enable **Use Custom Build** so Gradle can include the Billing Library.
2. Enable the **Billing** permission.
3. Disable Godot's built-in **Google Play Billing** plugin to avoid bundling the legacy billing implementation.

### 3. Connect and query products

Replace the product IDs with IDs configured in your Google Play Console. `GooglePlayBilling` starts connecting when it enters the scene tree, so connect to its signals and wait for `iap_connected` before making billing requests.

```gdscript
extends Node

@onready var billing: GooglePlayBilling = $GooglePlayBilling

const COINS_ID: String = "coins_100"
const PREMIUM_ID: String = "premium_unlock"


func _ready() -> void:
    billing.iap_connected.connect(_on_billing_connected)
    billing.iap_product_details_received.connect(_on_product_details_received)
    billing.iap_purchases_updated.connect(_on_purchases_updated)
    billing.iap_purchases_queried.connect(_on_purchases_queried)
    billing.iap_error_occurred.connect(_on_billing_error)


func _on_billing_connected() -> void:
    billing.query_details([COINS_ID, PREMIUM_ID], GooglePlayBilling.TYPE_INAPP)
    # Query owned items on startup to restore non-consumables and find pending purchases.
    billing.query_purchases(GooglePlayBilling.TYPE_INAPP)


func _on_product_details_received(
    products: Array[Dictionary],
    unfetched: Array[Dictionary],
    type: String
) -> void:
    for product: Dictionary in products:
        print("Product: ", product.get("product_id"), " ", product)
    for product: Dictionary in unfetched:
        push_warning("Could not fetch product: %s" % product)


func buy_coins() -> void:
    billing.buy_inapp(COINS_ID)


func buy_premium() -> void:
    billing.buy_inapp(PREMIUM_ID)


func _on_purchases_updated(purchases: Array[Dictionary]) -> void:
    _process_purchases(purchases)


func _on_purchases_queried(purchases: Array[Dictionary]) -> void:
    _process_purchases(purchases)


func _process_purchases(purchases: Array[Dictionary]) -> void:
    for purchase: Dictionary in purchases:
        if int(purchase.get("purchase_state", 0)) != GooglePlayBilling.PurchaseState.PURCHASED:
            continue

        var token: String = str(purchase.get("purchase_token", ""))
        var products: Array = purchase.get("products", [])
        if token.is_empty() or products.is_empty():
            continue

        var product_id: String = str(products[0])
        if product_id == COINS_ID:
            _grant_coins_once(token)
            billing.consume(token)
        elif product_id == PREMIUM_ID:
            _enable_premium_once()
            if not purchase.get("is_acknowledged", false):
                billing.acknowledge(token)


func _on_billing_error(fun_name: String, response: Dictionary) -> void:
    push_error("Billing error in %s: %s" % [fun_name, response])


func _grant_coins_once(purchase_token: String) -> void:
    # TODO: Grant and persist this purchase idempotently before consuming it.
    pass


func _enable_premium_once() -> void:
    # TODO: Persist the entitlement so it can be restored on future launches.
    pass
```

The example handles both purchase updates and the ownership query. Make entitlement grants idempotent: Google Play can report a purchase more than once. Persist a consumable grant before calling `consume()`, and persist non-consumable or subscription ownership so it can be restored. For production games, verify purchases with your backend where possible.

## Subscriptions

Use the product ID and base plan ID from the Play Console. An offer ID is optional:

```gdscript
billing.buy_subs("premium_subscription", "monthly-plan")
billing.buy_subs("premium_subscription", "monthly-plan", "introductory-offer")
```

To replace an existing subscription, provide its purchase token and product ID:

```gdscript
billing.update_subs(
    "premium_subscription",
    "yearly-plan",
    old_purchase_token,
    "premium_subscription",
    GooglePlayBilling.ReplacementMode.WITH_TIME_PRORATION
)
```

For an in-app product with an offer token, pass the token as the second argument:

```gdscript
billing.buy_inapp("coins_100", "offer_token_from_product_details")
```

## Useful IAP Signals

Connect these high-level `iap_*` signals for typical purchase flows:

| Signal | What it reports |
| --- | --- |
| `iap_connection_starting` | Billing connection attempt started. |
| `iap_connected` / `iap_disconnected` | Connection status changed. |
| `iap_product_details_received(products, unfetched, type)` | Product details query completed. |
| `iap_purchases_queried(purchases)` | Owned purchases returned by `query_purchases()`. |
| `iap_purchases_updated(purchases)` | Purchase flow updated purchase data. Check `purchase_state` before granting anything. |
| `iap_consumed_success(token)` | Consumable purchase was consumed. |
| `iap_acknowledged_success(token)` | Purchase was acknowledged. |
| `iap_error_occurred(fun_name, response)` | Unified operation error, including the operation name and response dictionary. |
| `iap_billing_config_received(config)` | Billing configuration response, such as the user's country. |

Purchase dictionaries include fields such as `purchase_token`, `products`, `purchase_state`, and `is_acknowledged`. Check the raw response for the exact fields available to your flow. A `PENDING` purchase is not paid for yet: do not grant its entitlement until it becomes `PURCHASED`.

The wrapper also forwards the Android plugin's lower-level signals, such as `purchase_updated(response)`, `query_product_details(response)`, and their corresponding error signals. These expose raw response dictionaries; see `GooglePlayBilling.gd` for the full signal list and method API.

## Troubleshooting

- Test billing with a build installed through a Google Play testing track and a tester account. Billing may not work as expected when launched directly from the editor.
- Confirm the product IDs and subscription base plans match the Play Console exactly.
- Inspect Android logs with `adb logcat | grep AndroidIAPP`.

## Compatibility

- Godot 4.3 or newer
- Android export with **Use Custom Build** enabled
- Billing permission enabled in the Android export preset
- Godot's built-in Google Play Billing plugin disabled
