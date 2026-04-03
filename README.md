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

## 🛠 Useful Signals in `GooglePlayBilling.gd`

*   `connected` — ready to start.
*   `error_occurred(method, data)` — something went wrong (e.g., no internet).
*   `product_details_received(products, ...)` — prices and descriptions received.
*   `purchases_updated(purchases)` — triggered after a purchase or during active product checks.
*   `consumed_success(token)` — product successfully "consumed", reward can be granted.

---

## 🔍 Debugging
If something isn't working, connect your phone and check the logs via terminal:
```bash
adb logcat | grep IAPP
```

**Compatibility:** Godot 4.3+ (Android Export). Requires the `Billing` permission to be enabled in export settings.
