# AndroidIAPP Godot Plugin

**AndroidIAPP** is a Godot plugin for Android that makes working with the Google Play Billing Library as simple as possible. It supports one-time purchases, subscriptions with different plans, and offers (discounts or deals) for products.

---

## 🚀 Quick Start

### 1. Installation and setup

1. Copy the `android_IAPP` folder into `res://addons/`.
2. Enable the plugin in the editor: `Project -> Project Settings -> Plugins`.
3. Add `GooglePlayBilling.gd` as a node in your scene, or register it as an autoload named `Billing`.

### 2. Basic purchase example

Attach a script to your UI or main scene node and connect the core signals:

```gdscript
extends Node

@onready var billing: GooglePlayBilling = $GooglePlayBilling


func _ready() -> void:
    # Connect core signals
    billing.connected.connect(_on_billing_connected)
    billing.purchase_updated.connect(_on_purchase_updated)
    billing.error_occurred.connect(_on_error_occurred)

    # Initialize plugin connection
    billing.start_connection()


func _on_billing_connected() -> void:
    print("Connected to Google Play Billing!")
    # Fetch product details before purchasing (optional but recommended)
    billing.query_details(["coins_100", "remove_ads"], GooglePlayBilling.TYPE_INAPP)


# Called when the player clicks the "Buy Coins" button
func buy_coins() -> void:
    billing.buy_inapp("coins_100")


func _on_purchase_updated(purchases: Array) -> void:
    for purchase in purchases:
        if purchase.get("purchase_state", 0) == GooglePlayBilling.PurchaseState.PURCHASED:
            var token: String = purchase.get("purchase_token", "")
            var products: Array = purchase.get("products", [])

            # Consumable item: coins
            if "coins_100" in products:
                _grant_coins_to_player()
                billing.consume(token)  # must consume to allow buying again

            # Non-consumable item: remove ads
            elif "remove_ads" in products:
                if not purchase.get("is_acknowledged", false):
                    _disable_ads_for_player()
                    billing.acknowledge(token)  # must acknowledge within 3 days


func _on_error_occurred(fun_name: String, response: Dictionary) -> void:
    push_error("Billing error in %s: %s" % [fun_name, response])


func _grant_coins_to_player() -> void:
    print("Granted 100 coins!")


func _disable_ads_for_player() -> void:
    print("Ads removed!")
```

---

## 💰 Subscriptions and offers

### Purchasing a subscription

To start a subscription purchase, specify the product ID and the base plan ID as defined in the Google Play Console:

```gdscript
billing.subscribe("premium_subscription", "monthly-plan")
```

### Updating a subscription

To upgrade or downgrade an active subscription:

```gdscript
billing.update_subscription(
    "premium_subscription_v2",
    "yearly-plan",
    old_purchase_token,
    "premium_subscription",
    GooglePlayBilling.ReplacementMode.WITH_TIME_PRORATION
)
```

### Purchase with an offer token

If the product has a configured offer or discount in Google Play, pass the offer token when buying:

```gdscript
billing.buy_inapp("coins_100", "your_offer_token_here")
```

---

## 📡 Signal reference and advanced usage

`GooglePlayBilling.gd` acts as a bridge between Godot and the Android Billing plugin. It exposes both high-level convenience signals and raw plugin signals.

### High-level convenience signals

Use these signals for normal game logic:

- `connected` — emitted when the connection to Google Play Billing is established.
- `disconnected` — emitted when the connection is lost.
- `product_details_received(products: Array, unfetched: Array, type: String)` — emitted when `query_details()` returns product information or missing items.
- `purchases_updated(purchases: Array)` — emitted after a purchase flow completes or when purchase data changes.
- `purchases_queried(purchases: Array)` — emitted in response to `query_purchases()` with the player's active ownership data.
- `consumed_success(token: String)` — emitted after a consumable item is successfully consumed.
- `acknowledged_success(token: String)` — emitted after a non-consumable or subscription item is acknowledged.
- `error_occurred(fun_name: String, response: Dictionary)` — unified error handler for failed operations.
- `billing_config_received(config: Dictionary)` — emitted when `get_billing_config()` returns system information such as the country code.

### Raw plugin signals

These are direct signals that forward raw dictionaries from the Android Kotlin plugin:

- Connection: `startConnection`, `start_connection`
- Product details: `query_product_details(response)`, `query_product_details_error(response)`
- Purchases: `purchase(response)`, `purchase_error(response)`, `purchase_cancelled(response)`, `purchase_update_error(response)`
- Consumables and acknowledgement: `purchase_consumed(response)`, `purchase_consumed_error(response)`, `purchase_acknowledged(response)`, `purchase_acknowledged_error(response)`
- Diagnostics: `billing_info(response)`, `helloResponse(message)`

### Alternative billing and external offers signals

Signals related to Play Store regulatory and alternative billing features:

- `alternative_billing_only_availability_response(response)`
- `alternative_billing_only_reporting_details_response(response)`
- `alternative_billing_only_information_dialog_response(response)`
- `external_offer_availability_response(response)`
- `external_offer_reporting_details_response(response)`
- `external_offer_information_dialog_response(response)`
- `billing_program_availability_response(response)`
- `billing_program_reporting_details_response(response)`
- `billing_program_information_dialog_response(response)`
- `billing_choice_info_response(response)`
- `launch_external_link_response(response)`

---

## ✅ Purchase confirmation rules

Google requires purchases to be confirmed after the transaction completes. Otherwise, the money may be refunded after a few days.

1. Consumables such as coins or lives must be consumed with `consume()`.
2. Non-consumables such as ad removal must be acknowledged with `acknowledge()`.
3. Subscriptions may also require acknowledgment depending on the billing flow and product configuration.

Example:

```gdscript
func _on_purchase_updated(purchases: Array) -> void:
    for purchase in purchases:
        if purchase.get("purchase_state", 0) == GooglePlayBilling.PurchaseState.PURCHASED:
            if "coins_100" in purchase.get("products", []):
                billing.consume(purchase.get("purchase_token", ""))
            elif "remove_ads" in purchase.get("products", []):
                if not purchase.get("is_acknowledged", false):
                    billing.acknowledge(purchase.get("purchase_token", ""))
```

---

## 🔍 Debugging

To inspect live plugin logs on an Android device, run:

```bash
adb logcat | grep IAPP
```

This helps diagnose connection issues, billing errors, and failed purchases.

---

## Compatibility

- Godot 4.3+
- Android export enabled
- Google Play Billing permission enabled in the Android export settings

Make sure the `Billing` permission is checked in your Android export presets before testing purchases.
