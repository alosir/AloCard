# AloCard 混淆规则

# Room：实体与 DAO 通过反射/生成代码使用，保留
-keep class com.alocard.app.data.local.entity.** { *; }
-keep class com.alocard.app.data.local.dao.** { *; }

# Hilt：保留生成的组件
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager

# Compose：默认规则已由 AGP 提供，无需额外配置

# 保留枚举 valueOf（Room 与状态徽标用到）
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Tink（androidx.security.crypto 的 EncryptedSharedPreferences 依赖）引用
# errorprone 注解但运行期不需要——只编译期存在，R8 放行
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
