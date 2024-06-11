package one.allme.plugin.androidiapp.utils

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
        val dictionary = Dictionary()
        dictionary["accountIdentifiers"] = purchase.accountIdentifiers
        dictionary["developerPayload"] = purchase.developerPayload
        dictionary["orderId"] = purchase.orderId
        dictionary["originalJson"] = purchase.originalJson
        dictionary["packageName"] = purchase.packageName
        dictionary["pendingPurchaseUpdate"] = purchase.pendingPurchaseUpdate
        dictionary["products"] = purchase.products
        dictionary["purchaseState"] = purchase.purchaseState
        dictionary["purchaseTime"] = purchase.purchaseTime
        dictionary["purchaseToken"] = purchase.purchaseToken
        dictionary["quantity"] = purchase.quantity
        dictionary["signature"] = purchase.signature
        dictionary["hashCode"] = purchase.hashCode()
        dictionary["isAcknowledged"] = purchase.isAcknowledged
        dictionary["isAutoRenewing"] = purchase.isAutoRenewing
        dictionary["toString"] = purchase.toString()
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
        dictionary["oneTimePurchaseOfferDetails"] = convertPurchaseOfferToDict(productsDetails.oneTimePurchaseOfferDetails)
        dictionary["productID"] = productsDetails.productId
        dictionary["productType"] = productsDetails.productType
        dictionary["subscriptionOfferDetails"] = productsDetails.subscriptionOfferDetails
        dictionary["title"] = productsDetails.title
        dictionary["hashCode"] = productsDetails.hashCode()
        dictionary["toString"] = productsDetails.toString()
        return dictionary
    }

//  Convert One Time Purchase Offer Details to Dictionary
    private fun convertPurchaseOfferToDict(offerDetails: ProductDetails.OneTimePurchaseOfferDetails?): Dictionary {
        val dictionary = Dictionary()  // from Godot type Dictionary
        if (offerDetails != null) {
            dictionary["formattedPrice"] = offerDetails.formattedPrice
        }
        if (offerDetails != null) {
            dictionary["priceCurrencyCode"] = offerDetails.priceCurrencyCode
        }
        if (offerDetails != null) {
            dictionary["priceAmountMicros"] = offerDetails.priceAmountMicros
        }
            return dictionary
        }

}


