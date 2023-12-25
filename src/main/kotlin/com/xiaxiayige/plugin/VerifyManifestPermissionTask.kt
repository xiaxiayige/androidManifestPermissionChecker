package com.xiaxiayige.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

/**
 * @description: 验证Manifest权限
 * @author : xiaxiayige@163.com
 * @date: 2022/5/14
 * @version: 1.0.0
 */
open class VerifyManifestPermissionTask : DefaultTask() {

    companion object {
        const val PREFIX_TAG = "uses-permission#"
    }

    @Internal
    var variantName: String = ""

    @TaskAction
    fun doWork() {
        executeTask()
    }

    private fun executeTask() {
        val verifyManifestPermissionExtensions =
            project.extensions.findByName(VerifyManifestPermissionExtensions.EXTENSIONS_NAME) as? VerifyManifestPermissionExtensions

        if (verifyManifestPermissionExtensions?.enable == true) {

            val fileName = Tool.getAnalyzeFileName(project, variantName)
            //原始文件文本内容
            val mergedManifestXmlText = Tool.getProjectdManifestXmlText(project)
            
            verifyManifestPermissionExtensions.blackPermissionList.let {
                if (it.isNotEmpty()) {
                    checkBlacklistPermissions(mergedManifestXmlText, fileName, it)
                }
            }
        }
    }

    /**
     * 检查黑名单权限
     */
    private fun checkBlacklistPermissions(
        mergedManifestXmlText: List<String>,
        fileName: String,
        blackPermissions: ArrayList<String>
    ) {
        val manifestLogging = getManifestLogging(fileName)

        if (manifestLogging.isEmpty()) {
            throw VerifyPermissionException("$fileName file not found")
        } else {
            //查找所有权限
            val allPermissionList = findAllPermission(manifestLogging)
            //过滤出存在的黑名单权限
            val findPermissionResultList =
                filterBlackPermission(mergedManifestXmlText, blackPermissions, allPermissionList)
            //
            if (findPermissionResultList.isNotEmpty()) {
                val logTextList = getErrorSource(findPermissionResultList, manifestLogging)
                throw VerifyPermissionException("Manifest has blackPermission,please check  $logTextList")
            }
        }
    }

    /**
     * 过滤查找黑名单权限那你
     */
    private fun filterBlackPermission(
        mergedManifestXmlText: List<String>,
        blackPermissions: ArrayList<String>, allPermissionList: List<String>
    ): ArrayList<String> {
        val findPermissionResultList = arrayListOf<String>()
        blackPermissions.forEach { blackPermission ->
            allPermissionList.forEach { needVerifyPermission ->
                val item = needVerifyPermission.replace(PREFIX_TAG, "")
                //如果mergedManifestXmlText 包含黑名单权限 则添加到结果中
                if (blackPermission == item) {
                    //检测是否声明了remove属性
                    val isExist = mergedManifestXmlText.any {
                        it.contains(blackPermission, true) && it.contains(
                            """node="remove"""".trimIndent(),
                            true
                        )
                    }
                    if (!isExist) {
                        findPermissionResultList.add(needVerifyPermission)
                    } else {
                        println("Skip permission Check ===> [${blackPermission}]")
                    }
                }
            }
        }
        return findPermissionResultList
    }

    private fun getManifestLogging(fileName: String): List<String> {
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

    /**
     * 获取所有权限
     */
    private fun findAllPermission(manifestLogging: List<String>): List<String> {
        return manifestLogging.filter { it.startsWith(PREFIX_TAG) }
    }

    /**
     * 获取错误的数据来源，方便定位问题
     */
    private fun getErrorSource(
        findPermissionResultList: ArrayList<String>, readLines: List<String>
    ): ArrayList<String> {
        val logTextList = arrayListOf<String>()
        findPermissionResultList.forEach {
            val index = readLines.indexOf(it)
            if (index > 0) {
                logTextList.add(it)
                if (index < readLines.size) {
                    logTextList.add("\n" + readLines[index + 1] + "\n")
                }
            }
        }
        return logTextList
    }
}