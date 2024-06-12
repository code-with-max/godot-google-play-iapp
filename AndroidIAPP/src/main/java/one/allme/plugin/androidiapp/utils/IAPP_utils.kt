package one.allme.plugin.androidiapp.utils

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import org.godotengine.godot.Dictionary


object IAPP_utils {

    fun convertPurchasesListToArray(purchasesList: List<Purchase>): Array<Any?> {
        val purchasesArray = arrayOfNulls<Any>(purchasesList.size)
        for (i in purchasesList.indices) {
            purchasesArray[i] = convertPurchaseToDictionary(purchasesList[i])
        }
        return purchasesArray
    }


    private fun convertPurchaseToDictionary(purchase: Purchase): Dictionary {
        val dictionary = Dictionary() // from Godot type Dictionary
        dictionary["account_identifiers"] = purchase.accountIdentifiers
        dictionary["developer_payload"] = purchase.developerPayload
        dictionary["order_id"] = purchase.orderId
        dictionary["original_json"] = purchase.originalJson
        dictionary["package_name"] = purchase.packageName
        dictionary["pending_purchase_update"] = purchase.pendingPurchaseUpdate
        dictionary["products"] = purchase.products
        dictionary["purchase_state"] = purchase.purchaseState
        dictionary["purchase_time"] = purchase.purchaseTime
        dictionary["purchase_token"] = purchase.purchaseToken
        dictionary["quantity"] = purchase.quantity
        dictionary["signature"] = purchase.signature
        dictionary["hash_code"] = purchase.hashCode()
        dictionary["is_acknowledged"] = purchase.isAcknowledged
        dictionary["is_auto_renewing"] = purchase.isAutoRenewing
        dictionary["to_string"] = purchase.toString()
        return dictionary
    }


    fun convertProductDetailsListToArray(productDetailsList: List<ProductDetails>): Array<Any?> {
        val productDetailsArray = arrayOfNulls<Any>(productDetailsList.size)
        for (i in productDetailsList.indices) {
            productDetailsArray[i] = convertProductDetailsToDictionary(productDetailsList[i])
        }
        return productDetailsArray
    }


    private fun convertProductDetailsToDictionary(productsDetails: ProductDetails): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        dictionary["description"] = productsDetails.description
        dictionary["name"] = productsDetails.name
        dictionary["product_id"] = productsDetails.productId
        dictionary["product_type"] = productsDetails.productType
        dictionary["title"] = productsDetails.title
        dictionary["hash_code"] = productsDetails.hashCode()
        dictionary["to_string"] = productsDetails.toString()
        if (productsDetails.productType == BillingClient.ProductType.INAPP) {
            dictionary["one_time_purchase_offer_details"] = convertPurchaseOfferToDict(productsDetails.oneTimePurchaseOfferDetails)
        } else if (productsDetails.productType == BillingClient.ProductType.SUBS) {
            dictionary["subscription_offer_details"] = convertSubscriptionsDetailsListToArray(productsDetails.subscriptionOfferDetails)
        }
        return dictionary
    }


    // (INAPP)
    // Convert One Time Purchase Offer Details to Dictionary
    // yeah, lot of not Null checks :(
    private fun convertPurchaseOfferToDict(offerDetails: ProductDetails.OneTimePurchaseOfferDetails?): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        if (offerDetails != null) {
            dictionary["formatted_price"] = offerDetails.formattedPrice
        }
        if (offerDetails != null) {
            dictionary["price_currency_code"] = offerDetails.priceCurrencyCode
        }
        if (offerDetails != null) {
            dictionary["price_amount_micros"] = offerDetails.priceAmountMicros
        }
            return dictionary
        }


    // (SUBS)
    // Convert list of subscriptions offers to array
    // yeah, lot of not Null checks :(
    private fun convertSubscriptionsDetailsListToArray(subscriptionsOffersList: List<ProductDetails.SubscriptionOfferDetails>?): Array<Any?>? {
        val subscriptionsOffersArray = subscriptionsOffersList?.let { arrayOfNulls<Any>(it.size) }
        if (subscriptionsOffersList != null) {
            for (i in subscriptionsOffersList.indices) {
                subscriptionsOffersArray?.set(i, convertSubscriptionDetailsToDictionary(subscriptionsOffersList[i]))
            }
        }
        return subscriptionsOffersArray
    }

    // Convert subscription offer details to dictionary
    private fun convertSubscriptionDetailsToDictionary(offerDetails: ProductDetails.SubscriptionOfferDetails): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        dictionary["base_plan_id"] = offerDetails.basePlanId
        dictionary["installment_plan_details"] = offerDetails.installmentPlanDetails
        dictionary["offer_id"] = offerDetails.offerId
        dictionary["offer_tags"] = offerDetails.offerTags
        dictionary["offer_token"] = offerDetails.offerToken
        dictionary["pricing_phases"] = offerDetails.pricingPhases
        return dictionary
    }

}


