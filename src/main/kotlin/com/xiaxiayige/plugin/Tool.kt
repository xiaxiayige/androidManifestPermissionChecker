package com.xiaxiayige.plugin

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

object Tool {

    fun getAnalyzeFileName(project: Project, variantName: String): String {
        val verifyManifestPermissionExtensions =
            project.extensions.findByName(VerifyManifestPermissionExtensions.EXTENSIONS_NAME) as? VerifyManifestPermissionExtensions
        //googleDebug or xiaomi
        val productFlavor = verifyManifestPermissionExtensions?.productFlavor?.toLowerCase() ?: ""
        //variant.name = googleDebug
        val flavor = variantName.toLowerCase().replace(productFlavor, "")
        //变体路径
        val variantPath = if (productFlavor.isEmpty()) flavor else "$productFlavor-$flavor"

        //查找权限的文件名
        //manifest-merger-google-debug-report.txt
        return "${project.buildDir.absoluteFile}/outputs/logs/manifest-merger-${variantPath}-report.txt"
    }

    fun getProjectdManifestXmlText(project: Project): List<String> {
        val filePath =
            "${project.projectDir.absolutePath}/src/main/AndroidManifest.xml"
        println("AndroidManifest project.filePath = [${filePath}]")
        return getManifestText(filePath)
    }

    private fun getManifestText(fileName: String): List<String> {
        val file = File(fileName)
        return if (file.exists()) {
            val fileInputStream = FileInputStream(file)
            val isr = InputStreamReader(fileInputStream, "UTF-8")
            val readLines = isr.readLines()
            fileInputStream.close()
            readLines
        } else {
            arrayListOf()
        }
    }
}