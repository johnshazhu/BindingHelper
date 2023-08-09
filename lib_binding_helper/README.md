# BindingHelper

提供一种在Activity或Fragment中初始化ViewBinding、DataBinding的便捷方式

以反射方式实现。主要方法有

getBinding : 获取对应的ViewBinding或DataBinding，instance为调用处的实例。使用场景包括Activity、Fragment、  
自定义View等。
```
fun <T : ViewBinding> getBinding(
    instance: Any,
    inflater: LayoutInflater,
    parent: ViewGroup? = null,
    attachToParent: Boolean = false,
    isInstanceInterface: Boolean = false
): T
```

getBindingInflateMethod : 获取ViewBinding或DataBinding的inflate方法，主要使用场景是在ListView或RecyclerView  
的Adapter中获取view时使用。
```
fun getBindingInflateMethod(instance: Any, isInstanceInterface: Boolean = false): Method?
```
  
setUseMapMethod : 是否使用map缓存通过反射得到的方法，默认为true  

```
fun setUseMapMethod(enable: Boolean)
```

setShowTimeLog : 是否显示方法调用耗时日志，默认为false  

```
fun setShowTimeLog(enable: Boolean)
```

具体使用例子参考app