package com.zaba.notez

import java.io.File

/**
 * Utilitas guard struktural: membaca berkas proyek tanpa Android runtime.
 *
 * Semua guard yang memakai ini tetap leksikal. Ia bisa membuktikan sebuah
 * atribut atau pemanggilan ada/tidak ada di source, tidak bisa membuktikan
 * perilaku di perangkat.
 */
internal object SourceScan {

    /**
     * Hapus komentar dan literal string agar pencocokan hanya mengenai kode.
     * Keluaran dijaga sama panjang supaya posisi baris tidak bergeser.
     *
     * Memakai state machine, bukan regex: blok komentar Kotlin bisa bersarang
     * dan pemindai naif pernah menelan kode asli.
     */
    fun codeOnly(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            val c = source[i]
            when {
                c == '/' && i + 1 < source.length && source[i + 1] == '/' -> {
                    while (i < source.length && source[i] != '\n') {
                        out.append(' ')
                        i++
                    }
                }
                c == '/' && i + 1 < source.length && source[i + 1] == '*' -> {
                    var depth = 0
                    while (i < source.length) {
                        when {
                            source[i] == '/' && i + 1 < source.length && source[i + 1] == '*' -> {
                                depth++
                                out.append("  ")
                                i += 2
                            }
                            source[i] == '*' && i + 1 < source.length && source[i + 1] == '/' -> {
                                depth--
                                out.append("  ")
                                i += 2
                                if (depth == 0) break
                            }
                            else -> {
                                out.append(if (source[i] == '\n') '\n' else ' ')
                                i++
                            }
                        }
                    }
                }
                c == '"' && source.startsWith("\"\"\"", i) -> {
                    out.append("\"\"\"")
                    i += 3
                    while (i < source.length && !source.startsWith("\"\"\"", i)) {
                        out.append(if (source[i] == '\n') '\n' else ' ')
                        i++
                    }
                    if (i < source.length) {
                        out.append("\"\"\"")
                        i += 3
                    }
                }
                c == '"' -> {
                    out.append('"')
                    i++
                    while (i < source.length && source[i] != '"' && source[i] != '\n') {
                        if (source[i] == '\\' && i + 1 < source.length) {
                            out.append("  ")
                            i += 2
                        } else {
                            out.append(if (source[i] == '\n') '\n' else ' ')
                            i++
                        }
                    }
                    if (i < source.length && source[i] == '"') {
                        out.append('"')
                        i++
                    }
                }
                c == '\'' -> {
                    out.append('\'')
                    i++
                    while (i < source.length && source[i] != '\'' && source[i] != '\n') {
                        if (source[i] == '\\' && i + 1 < source.length) {
                            out.append("  ")
                            i += 2
                        } else {
                            out.append(if (source[i] == '\n') '\n' else ' ')
                            i++
                        }
                    }
                    if (i < source.length && source[i] == '\'') {
                        out.append('\'')
                        i++
                    }
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    /**
     * Berkas sumber modul app. Gradle menjalankan unit test dari direktori
     * modul; jalur alternatif disediakan supaya guard tidak lolos diam-diam
     * kalau working directory berubah.
     */
    fun readMainFile(relativePath: String): String =
        readFirst(listOf(File("src/main/$relativePath"), File("app/src/main/$relativePath")))

    fun readRes(relativePath: String): String =
        readFirst(listOf(File("src/main/res/$relativePath"), File("app/src/main/res/$relativePath")))

    private fun readFirst(candidates: List<File>): String {
        val file = candidates.firstOrNull { it.isFile }
            ?: error(
                "Berkas ${candidates.first().path} tidak ditemukan " +
                    "(workingDir=${File(".").absolutePath})."
            )
        return file.readText()
    }
}
