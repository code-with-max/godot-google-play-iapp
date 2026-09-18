# AndroidIAPP Godot Plugin

**AndroidIAPP** is a Godot plugin (Android) that makes working with Google Play Billing (Billing Library 8.0) as simple as possible. It supports one-time purchases, subscriptions with different plans, and—most importantly—**offers (discounts/deals) for one-time products**.

## 🚀 Quick Start

### 1. Installation
1. Download the plugin and place the `androidiapp` folder in `res://addons/`.
2. Enable the plugin in the menu: `Project -> Project Settings -> Plugins`.
3. **Most important:** Copy the file `examples/GooglePlayBilling.gd` to your project (e.g., to the `scripts/` folder).

### 2. Setup in Godot
Add the `GooglePlayBilling.gd` script to **Autoload** (Singleton) under the name `Billing`, or simply create a node with this script in your main scene.

### 3. Initialization
In your game script:

```gdscript
func _ready():
    # Connect main signals
    Billing.connected.connect(_on_connected)
    Billing.error_occurred.connect(_on_error)
    Billing.product_details_received.connect(_on_products_loaded)
    Billing.purchases_updated.connect(_on_purchases_updated)

    # Initialize the plugin
    Billing._initialize_plugin()

func _on_connected():
    print("Connected to Google Play!")
    # Request product information (prices, descriptions)
    Billing.query_details(["gold_100", "no_ads"], Billing.TYPE_INAPP)
```

---

## 💰 Working with Purchases

### Querying Products and Prices
Data will arrive in the `product_details_received` signal.
```gdscript
func _on_products_loaded(products: Array, unfetched: Array, type: String):
    for p in products:
        print("Product: ", p.title, " Price: ", p.formatted_price)
```

### Purchase (with Offers Support)
For a regular product, an ID is sufficient. If the product has an offer configured in the Google Play Console, you can pass its token.
```gdscript
# Simple purchase
Billing.buy_inapp("gold_100")

# Purchase with a specific offer (discount)
Billing.buy_inapp("gold_100", "your_offer_token_here")
```

### Subscriptions
For subscriptions, you need to specify the product ID and the Base Plan ID.
```gdscript
Billing.subscribe("premium_sub", "monthly-plan")
```

---

## ✅ Confirming Purchase (Required!)
Google requires you to confirm purchases; otherwise, the money will be refunded to the user after 3 days.

1. **Consumables (gold, lives)** — need to be "consumed" (`consume`).
2. **Non-consumables (ad removal)** — need to be "acknowledged" (`acknowledge`).

```gdscript
func _on_purchases_updated(purchases: Array):
    for p in purchases:
        if p.purchase_state == Billing.PurchaseState.PURCHASED:
            if "gold_100" in p.products:
                Billing.consume(p.purchase_token) # Give gold and consume
            else:
                if not p.is_acknowledged:
                    Billing.acknowledge(p.purchase_token) # Activate permanent item
```

---

## 🛠 All Signals in `GooglePlayBilling.gd`

### Core and Technical Signals
*   `connected` — successfully connected to Google Play Billing, ready to start.
*   `disconnected` — disconnected from Google Play Billing.
*   `error_occurred(fun_name, response)` — triggered on error. `fun_name` is the method name, `response` contains `response_code` and `debug_message`.
*   `billing_info_received(info)` — technical diagnostic information from the plugin.
*   `hello_response(message)` — response from test `say_hello` call.

### Products and Purchases
*   `product_details_received(products, unfetched, type)` — product details (prices, descriptions, offers) received. `products` is an array of details, `unfetched` contains unfetched items, `type` is `inapp` or `subs`.
*   `purchases_queried(purchases)` — user's active purchases retrieved in response to `query_purchases`.
*   `purchases_updated(purchases)` — triggered after a successful purchase or when purchase list is updated.
*   `purchase_cancelled` — user cancelled the purchase flow.
*   `consumed_success(token)` — consumable product successfully consumed, reward can be granted.
*   `acknowledged_success(token)` — non-consumable item or subscription successfully acknowledged.

### Configuration and Subscriptions
*   `in_app_message_result(result)` — result of showing Google Play In-App message.
*   `price_change_acknowledged(result)` — result of subscription price change acknowledgement.
*   `billing_config_received(config)` — billing configuration (e.g., user country code) received.

### Alternative Billing and External Offers
*   `alternative_billing_only_availability_response(response)` — result of checking Alternative Billing Only availability.
*   `alternative_billing_only_reporting_details_response(response)` — Alternative Billing Only reporting details.
*   `alternative_billing_only_information_dialog_response(response)` — result of showing Alternative Billing Only information dialog.
*   `external_offer_availability_response(response)` — result of checking External Offer availability.
*   `external_offer_reporting_details_response(response)` — External Offer reporting details.
*   `external_offer_information_dialog_response(response)` — result of showing External Offer information dialog.
*   `billing_program_availability_response(response)` — result of checking Billing Program availability.
*   `billing_program_reporting_details_response(response)` — Billing Program reporting details.
*   `billing_program_information_dialog_response(response)` — result of showing Billing Program information dialog.
*   `billing_choice_info_response(response)` — billing choice info received.
*   `launch_external_link_response(response)` — result of launching external link.

---

## 🔍 Debugging
If something isn't working, connect your phone and check the logs via terminal:
```bash
adb logcat | grep IAPP
```

**Compatibility:** Godot 4.3+ (Android Export). Requires the `Billing` permission to be enabled in export settings.
