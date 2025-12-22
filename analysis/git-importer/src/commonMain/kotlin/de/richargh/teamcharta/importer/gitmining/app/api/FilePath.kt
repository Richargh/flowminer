package de.richargh.teamcharta.importer.gitmining.app.api

@JvmInline
value class FilePath(val value: String) : Comparable<FilePath> {
    companion object {
        fun of(path: String): FilePath = FilePath(path)
    }

    override fun toString(): String = value

    override fun compareTo(other: FilePath): Int = value.compareTo(other.value)
}