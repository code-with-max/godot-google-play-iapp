package one.allme.plugin.androidiapp

import one.allme.plugin.androidiapp.utils.IAPP_utils
import one.allme.plugin.androidiapp.utils.IappUtils
import org.godotengine.godot.Dictionary
import org.junit.Assert.*
import org.junit.Test

class IappUtilsTest {

    @Test
    fun convertPurchasesListToDictionary_withNull_returnsDictionaryWithErrorAndEmptyArray() {
        val dict: Dictionary = IappUtils.convertPurchasesListToDictionary(null)

        assertNotNull(dict)
        assertEquals("Purchase list is null", dict["error"])
        assertTrue(dict.containsKey("purchases_list"))

        val purchasesList = dict["purchases_list"] as? Array<*>
        assertNotNull(purchasesList)
        assertEquals(0, purchasesList!!.size)
    }

    @Test
    fun convertPurchasesListToDictionary_withEmptyList_returnsDictionaryWithEmptyPurchasesList() {
        val dict: Dictionary = IappUtils.convertPurchasesListToDictionary(emptyList())

        assertNotNull(dict)
        assertFalse(dict.containsKey("error"))
        assertTrue(dict.containsKey("purchases_list"))

        val purchasesList = dict["purchases_list"] as? Array<*>
        assertNotNull(purchasesList)
        assertEquals(0, purchasesList!!.size)
    }

    @Test
    fun convertPurchasesListToArray_withNull_returnsEmptyArray() {
        val array = IappUtils.convertPurchasesListToArray(null)

        assertNotNull(array)
        assertEquals(0, array.size)
    }

    @Test
    fun iappUtilsLegacyDelegation_worksCorrectly() {
        val nullDict = IAPP_utils.convertPurchasesListToDictionary(null)
        assertNotNull(nullDict)
        assertEquals("Purchase list is null", nullDict["error"])

        val emptyArray = IAPP_utils.convertPurchasesListToArray(null)
        assertNotNull(emptyArray)
        assertEquals(0, emptyArray.size)
    }
}
