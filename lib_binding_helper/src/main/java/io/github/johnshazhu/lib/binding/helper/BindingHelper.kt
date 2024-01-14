package io.github.johnshazhu.lib.binding.helper

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.security.InvalidParameterException
import java.util.concurrent.ConcurrentHashMap

// 匹配类的名字，若是以Binding结尾的，认为该类是继承自ViewBinding或DataBinding
private const val CLASS_END_WITH_NAME = "Binding"

// ViewBinding或DataBinding中的inflate方法，是我们要查找的目标方法
private const val TARGET_METHOD_NAME = "inflate"

// 用map来保存之前获取过的目标方法（可能比再次通过反射查找会快一点）
private val reflectMethodMap = ConcurrentHashMap<String, Method>()

// 要查找的目标方法的参数类型列表，inflate方法有两个，我们使用有三个参数的那个
private val TARGET_METHOD_PARAMETER_TYPE_LIST = arrayListOf(
    "android.view.LayoutInflater", "android.view.ViewGroup", "boolean"
)

// 配置是否使用map缓存方法
private var useMapMethod = true

// 是否显示耗时日志
private var showTimeLog = false

// 设置是否使用缓存方法
fun setUseMapMethod(enable: Boolean) {
    useMapMethod = enable
}

// 设置是否显示耗时日志
fun setShowTimeLog(enable: Boolean) {
    showTimeLog = enable
}

// 清除缓存方法map，可能不需要调用，也可能在app退出时调用
fun clearMethodMap() {
    if (reflectMethodMap.isNotEmpty()) {
        if (showTimeLog) {
            reflectMethodMap.keys.forEach {
                Log.i("xdebug", "$it = ${reflectMethodMap[it]}")
            }
        }
        reflectMethodMap.clear()
    }
}

/**
 * getBinding 获取T类型的Binding实例
 *
 * @param instance : 要获取Binding类型的实例（可以说类或接口）
 * @param inflater
 * @param parent
 * @param attachToParent
 * @param isInstanceInterface
 * @return T 返回 Binding实例，反射方法获取失败时抛出InvalidParameterException异常
 */
@Throws(InvalidParameterException::class)
fun <T : ViewBinding> getBinding(
    instance: Any,
    inflater: LayoutInflater,
    parent: ViewGroup? = null,
    attachToParent: Boolean = false,
    isInstanceInterface: Boolean = false
): T {
    val start = System.currentTimeMillis()
    val method = getBindingInflateMethod(instance, isInstanceInterface)
    val mid = System.currentTimeMillis()
    runCatching {
        val binding = method?.invoke(null, inflater, parent, attachToParent)
        val end = System.currentTimeMillis()

        if (binding is ViewBinding) {
            if (showTimeLog) {
                Log.i("xdebug", "$binding ${method.name} found cost = ${mid - start}ms, execute cost = ${end - mid}ms")
            }
            return binding as T
        }
    }.onFailure {
        if (it is InvocationTargetException) {
            throw it.targetException
        }
    }

    throw InvalidParameterException("invalid parameter")
}

/**
 * getBindingInflateMethod 通过反射获取到T(<T: ViewBinding>)的inflate方法
 *
 * @param instance : 要获取Binding类型的实例（可以说类或接口）
 * @param isInstanceInterface : 实例是否是接口
 *
 * @return 返回inflate方法
 * public static inflate(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup parent, boolean attachToParent)
 */
fun getBindingInflateMethod(instance: Any, isInstanceInterface: Boolean = false): Method? {
    val type = getType(instance, isInstanceInterface)
    type?.let {
        if (useMapMethod && reflectMethodMap.contains(it.toString())) {
            return reflectMethodMap[it.toString()]
        }
        if (it is ParameterizedType) {
            for (actualType in it.actualTypeArguments) {
                val typeName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    actualType.typeName
                } else {
                    val splitArray = actualType.toString().split(" ")
                    splitArray[splitArray.size - 1]
                }
                if (typeName.endsWith(CLASS_END_WITH_NAME)) {
                    val clazz = Class.forName(typeName)
                    if (!ViewBinding::class.java.isAssignableFrom(clazz)) {
                        continue
                    }
                    for (method in clazz.declaredMethods) {
                        if (method.name == TARGET_METHOD_NAME && method.parameterTypes.size == TARGET_METHOD_PARAMETER_TYPE_LIST.size) {
                            var isMatched = true
                            method.parameterTypes.forEachIndexed { index, parameterType ->
                                if (parameterType.name != TARGET_METHOD_PARAMETER_TYPE_LIST[index]) {
                                    isMatched = false
                                }
                            }
                            if (isMatched) {
                                method.isAccessible = true
                                if (useMapMethod) {
                                    reflectMethodMap[type.toString()] = method
                                }
                                return method
                            }
                        }
                    }
                }
            }
        }
    }

    return null
}

/**
 * getType 获取泛型类型
 *
 * @param instance : 要获取Binding类型的实例（可以说类或接口）
 * @param isInstanceInterface : 实例是否是接口
 * @return Binding相关的泛型类型
 */
private fun getType(instance: Any, isInstanceInterface: Boolean): Type? {
    var clazz = instance::class.java
    if (isInstanceInterface) {
        return clazz.genericInterfaces[0]
    }

    while (clazz.genericSuperclass !is ParameterizedType && clazz.superclass != null) {
        clazz = clazz.superclass
    }
    return clazz.genericSuperclass
}