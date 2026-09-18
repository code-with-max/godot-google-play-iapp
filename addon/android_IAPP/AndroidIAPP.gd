@tool
extends EditorPlugin

const BILLING_VERSION ="8.3.0"

var exportPlugin = AndroidExportPlugin


func _enter_tree() -> void:
	# Initialization of the plugin goes here.
	exportPlugin = AndroidExportPlugin.new()
	add_export_plugin(exportPlugin)


func _exit_tree() -> void:
	# Clean-up of the plugin goes here.
	remove_export_plugin(exportPlugin)
	exportPlugin = null


class AndroidExportPlugin extends EditorExportPlugin:
	var pluginName  = "AndroidIAPP"

	func _get_name() -> String:
		return pluginName

	func _supports_platform(platform: EditorExportPlatform) -> bool:
		if platform is EditorExportPlatformAndroid:
			return true
		return false


	func _get_android_libraries(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		if debug:
			return PackedStringArray(["android_IAPP/AndroidIAPP-debug.aar"])
		else:
			return PackedStringArray(["android_IAPP/AndroidIAPP-release.aar"])
	

	func _get_android_dependencies(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		if debug:
			return PackedStringArray([
				"com.android.billingclient:billing-ktx:%s" % BILLING_VERSION, # "7.1.1",
				# "com.android.billingclient:billing:7.0.0", # deprecated
				])
		else:
			return PackedStringArray([
				"com.android.billingclient:billing-ktx:%s" % BILLING_VERSION, # "7.1.1",
				# "com.android.billingclient:billing:7.0.0", # deprecated
				])
