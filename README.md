# AndroidIAPP Godot Plugin

AndroidIAPP is a [plugin](<https://docs.godotengine.org/en/stable/tutorials/plugins/editor/installing_plugins.html#installing-a-plugin>) for the Godot game engine. It provides an interface to work with Google Play Billing Library version 7. The plugin supports all public functions of the library, passes all error codes, and can work with different subscription plans.

## Features

- Connect to Google Play Billing.
- Query purchases.
- Query product details.
- Make purchases and subscriptions.
- Update purchases.
- Consume and acknowledge purchases.

## Installation

- Install the plugin using Godot [Asset Library](https://godotengine.org/asset-library/asset/3068).

  or

- Download the plugin from [GitHub](https://github.com/code-with-max/godot-google-play-iapp/releases).
- And place the unpacked plugin folder in the `res://addons/` directory of the project.

> [!NOTE]
> Dont forget to enable the plugin in the project settings.

## Examples

- [Example](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/billing_example.gd) of a script for working with a plugin

## Before start

- Google Play Billing uses these three types of purchases:
  - Products that will be consumed ("inapp").
  - Products that will be acknowledged ("inapp").
  - Subscriptions that will be purchased and consumed ("subs").

  And their IDs should be passed to the function as a list of String elements. Even if there is only one ID, it still needs to be wrapped in a list.

- All public methods and values returned by Google Play Billing are presented as a typed Godot dictionary. All dictionary keys represent the names of public methods written in snake_case style.
  - getProductId -> `product_id`
  - getSubscriptionOfferDetails -> `subscription_offer_details`

  See the variant of response [here](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_inapp.json)

- The plugin also includes all standard [BillingResponseCode](https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode) messages as a key in the dictionary called `response_code`. Additionally, it adds a `debug_message` key if the code indicates an error.

## Signals Descriptions (Event listeners)

### Test signal

*Returns a String value.*

`helloResponse`: Emitted when a response to a hello message is received.

### Information signals

*Does not return anything.*

`startConnection`: Emitted when the connection to Google Play Billing starts.

`connected`: Emitted when successfully connected to Google Play Billing.

`disconnected`: Emitted when disconnected from Google Play Billing.

### Billing signals  

*Returns a Dictionary of Godot type.*

`query_purchases`: Emitted when a query for purchases is successful.  
Returns a dictionary with purchases or subscriptions.

`query_purchases_error`: Emitted when there is an error querying purchases.  
Returns a dictionary with error codes and debug message.

`query_product_details`: Emitted when a query for product details is successful.  
Returns a dictionary with product or subscription details.

`query_product_details_error`: Emitted when there is an error querying product details.  
Returns a dictionary with error codes and debug message.

`purchase_error`: Emitted when there is an error during the purchase process.  
Returns a dictionary with error codes and debug message.

`purchase_updated`: Emitted when the purchase information is updated.  
Returns a dictionary with purchases or subscriptions.

`purchase_cancelled`: Emitted when a purchase is cancelled.  
Returns a dictionary with error codes and debug message.

`purchase_update_error`: Emitted when there is an error updating the purchase information.  
Returns a dictionary with error codes and debug message.

`purchase_consumed`: Emitted when a purchase is successfully consumed.  
Returns a dictionary with confirmation message.

`purchase_consumed_error`: Emitted when there is an error consuming the purchase.  
Returns a dictionary with error codes and debug message.

`purchase_acknowledged`: Emitted when a purchase is successfully acknowledged.  
Returns a dictionary with confirmation message.

`purchase_acknowledged_error`: Emitted when there is an error acknowledging the purchase.  
Returns a dictionary with error codes and debug message.

## Functions

`startConnection()`: Starts the connection to Google Play Billing, emit signals:

- `startConnection` signal when connection is started.
- `connected` signal if connection is successful.  

---
`isReady()`: Checks if the connection to Google Play Billing is ready and returns a boolean value.

---
`sayHello()` : Sends a hello message from the plugin.  
*For testing purposes, not recommended in production.*  

- Emit `helloResponse` signal
- Sending Log.v message to the console
- Display a system toast.

---
`queryPurchases(productType: String)`  
productType: **"inapp"** for products or **"subs"** for subscriptions.  
Handling purchases made [outside your app](https://developer.android.com/google/play/billing/integrate#ooap).  
> [!NOTE]
> I recommend calling it every time you establish a connection with the billing service.

Emit signals:

- `query_purchases`: if a query for purchases is successful.  
- `query_purchases_error`: if there is an error querying purchases.

---
`queryProductDetails(productId: List<String>, productType: String)`: This function queries product of subscriptions details from Google Play Billing.  
`productId`: ID of the product or subscription wrapped in a list.  
`productType`: **"inapp"** for products or **"subs"** for subscriptions.

> [!NOTE]
> You must pass the product type as a parameter. If you pass the wrong product type with the product IDs, like using subscription IDs with "inapp", it won't work and the function will return an error.

Emit signals:

- `query_product_details`: If a query for product details is successful.  
- `query_product_details_error`: If error :)

See an example of [product](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_inapp.json) details answer or [subscription](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_subscription.json).

---
> [!IMPORTANT]
> This is where the biggest difference from the official plugin begins.
> If you have never used the old plugin before, you don't need to worry.
> But if you are planning to switch to this version, you should know that I have implemented two separate functions for buying products and subscribing to plans.

---
`purchase(id: List<String>, is_personalized: bool)`: purchase a product from Google Play Billing.  
`id`: ID of the product or subscription wrapped in a list.  
`is_personalized`: This is to ensure compliance with the [EU directive](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:02011L0083-20220528), you can clarify this [here](https://developer.android.com/google/play/billing/integrate#personalized-price), but if you don't understand why, just set it to `false`.

Emit signals:

- `purchase_updated`: Emitted when the purchase information is updated. The purchase process was successful. [Example of response](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/purchase_updated_inapp.json)
`query_product_details_error`: If an error occurred while receiving information about the product being purchased.
- `purchase_error`: If there is an error during the purchase process.  
- `purchase_cancelled`: If a purchase is cancelled by the user.
- `purchase_update_error`: If there is an error updating the purchase information.  

> [!IMPORTANT] Do not forget consume or acknowledge the purchase.

---

## Step-by-step set up guide

### Connecting to the Google Play Billing Library

### Requesting a list of products and subscriptions

### Handling purchases and subscriptions

### Confirming and consuming purchases

### Handling errors and purchase states
