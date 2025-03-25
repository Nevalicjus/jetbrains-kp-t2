import java.io.File
import java.io.IOException
import java.util.jar.JarFile

object ClassGetter {
    private val classLocationsForCurrentClasspath: MutableList<File>
        get() {
            val javaClassPath = System.getProperty("java.class.path") ?: return mutableListOf<File>()
            return javaClassPath.split(File.pathSeparator.toRegex()).filter { it.isNotEmpty() }
                .map { File(it) }.toMutableList()
        }

    fun allKnownClasses(): MutableList<Class<*>> {
        val classFiles = mutableListOf<Class<*>>()
        val classLocations = this.classLocationsForCurrentClasspath
        for (file in classLocations) {
            classFiles.addAll(this.classesFromPath(file))
        }
        return classFiles
    }

    private fun file2ClassName(fileName: String): String {
        return fileName.replace(".class", "").replace(Regex("[/\\\\]"), "\\.")
    }

    private fun classesFromPath(path: File): MutableList<Class<*>> {
        return if (path.isDirectory) {
            this.classesFromDir(path)
        } else {
            this.classesFromJar(path)
        }
    }

    private fun classesFromJar(path: File): MutableList<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        try {
            val entries = JarFile(path).entries()
            for (entry in entries.asSequence().filter { it.name.endsWith(".class") }) {
                this.name2Class(classes, entry.name)
            }
        } catch (_: IOException) {}
        return classes
    }

    private fun classesFromDir(path: File): MutableList<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        // handle .jar files
        for (file in this.listFiles(path, filter = { file -> file.name.endsWith(".jar") })) {
            classes.addAll(this.classesFromJar(file))
        }
        // handle .class files
        for (classFile in this.listFiles(path, filter = { file -> file.name.endsWith(".class") })) {
            this.name2Class(classes, classFile.name)
        }
        return classes
    }

    // Both Dir and Jar methods accumulate to a list Classes from doing the same operation:
    // name (String) -> className (String) -> Class(className)
    private fun name2Class(classList: MutableList<Class<*>>, className: String) {
        try { classList.add(Class.forName(this.file2ClassName(className))) } catch (_: NoClassDefFoundError) {}
    }

    private fun listFiles(directory: File, filter: ((File) -> (Boolean))?): MutableList<File> {
        val files = mutableListOf<File>()
        val entries = directory.listFiles() ?: return files
        for (entry in entries) {
            // if no filter provided or filter passes, add to the list of matching files
            if (filter != null && filter(entry)) {
                files.add(entry)
            }
            // if the file is a directory and recurse = true, do the same for it
            if (entry.isDirectory) {
                files.addAll(this.listFiles(entry, filter))
            }
        }
        return files
    }
}