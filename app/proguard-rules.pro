# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\firoz\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt

# Preserve JavaScript Interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepattributes JavascriptInterface
