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

1. Download the plugin from [GitHub](https://github.com/code-with-max/godot-google-play-iapp/releases).
2. Place the plugin folder in the `res://addons/` directory.
3. Enable the plugin in the project settings.

## Examples

- [Example](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/billing_example.gd) of a script for working with a plugin

## Before start

- Google Play Billing uses these three types of purchases:
  - Products that will be consumed.
  - Products that will be purchased.
  - Subscriptions that will be purchased and consumed.

  And their IDs should be passed to the function as a list of String elements. Even if there is only one ID, it still needs to be wrapped in a list.

- All public methods and values returned by Google Play Billing are presented as a typed Godot dictionary. All dictionary keys represent the names of public methods written in snake_case style.
  - getProductId -> `product_id`
  - getSubscriptionOfferDetails -> `subscription_offer_details`

  See full variant of response [here](https://github.com/code-with-max/godot-google-play-iapp/blob/master/examples/details_inapp.json)

- The plugin also includes all standard [BillingResponseCode](https://developer.android.com/reference/com/android/billingclient/api/BillingClient.BillingResponseCode) messages as a key in the dictionary called `response_code`. Additionally, it adds a `debug_message` key if the code indicates an error.

## Step-by-step set up guide

### Connecting to the Google Play Billing Library

### Requesting a list of products and subscriptions

### Handling purchases and subscriptions

### Confirming and consuming purchases

### Handling errors and purchase states
