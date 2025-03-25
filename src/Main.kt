import kotlin.* // we want to have the entire kotlin namespace for lookup
import kotlin.reflect.KVisibility
import kotlin.system.exitProcess

fun main(arguments: Array<String>)  {
    val partialName = if (arguments.isEmpty()) {
        print("Error: Partial class name not provided\nProvide one to continue: ")
        readln()
    } else {
        arguments[0]
    }

    val regex = Regex("kotlin(\\.[A-Za-z0-9]*)+$partialName.*")

    val classes = ClassGetter.allKnownClasses()
    val results = mutableListOf<String>()
    for (fclass in classes) {
        val res = regex.find(fclass.name)
        if (res == null) {
            // only matches
            continue
        } else if (res.value.contains("$")) {
            // kotlin.random.Random$Default
            // don't import inner definitions of classes
            continue
        } else if (fclass.kotlin.visibility != KVisibility.PUBLIC) {
            // kotlin.collections.builders.ListBuilder
            // not public == can't import (* I'm 99% sure)
            continue
            // during tries to compile with kotlinc-native for the cleanest "./solution <>",
            // I've removed dependency on kotlin.reflect.KVisibility through java's accessFlags
            // but this depends on jdk 20+, and allows kotlin.collections.builders.ListBuilder
        } else if (java.lang.reflect.AccessFlag.PUBLIC !in fclass.accessFlags()) {
            // not public == cant import
            continue
        }
        else if (fclass.name.endsWith("Kt")) {
            // kotlin.collections.builders.ListBuilderKt
            // ListBuilder gets removed as internal,
            // but the "for Java" - ListBuilderKt - name isn't removed and does not show up as internal
            // (actually, shows up as nothing because it's not there)
            continue
        }
        else {
            results.add(res.value)
        }
    }
    if (results.isEmpty()) {
        println("Error: No classes found matching $partialName")
        exitProcess(1)
    }
    //println(results.joinToString("\n"))

    // I've assumed the shortest name would probably be correct
    // we probably want kotlin.collections.AbstractMap,
    // not kotlin.coroutines.AbstractCoroutineContextElement
    // but we could add number of steps as heuristic,
    // or add some usage based statistics and let them influence our decision
    results.sortBy() { it.length }
    val ans = results[0]
    println("Answer: $ans")
}