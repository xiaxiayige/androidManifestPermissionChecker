package com.xiaxiayige.plugin

import kotlinx.coroutines.*
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
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


    private val handler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is VerifyPermissionException) {
            throw Exception(throwable)
        }
    }

    private val job = CoroutineScope(Job() + handler)

    @TaskAction
    fun doWork() {
        val extensions = project.extensions.create(
            VerifyManifestPermission.EXTENSIONS_NAME, VerifyManifestPermission::class.java
        )
    }

    override fun doLast(action: Action<in Task>): Task {
        println("VerifyManifestPermissionTask ====>  doLast")
        return this
    }

    private suspend fun getManifestLogging(fileName: String): List<String> {
        return withContext(Dispatchers.IO) {
            val file = File(fileName)
            if (file.exists()) {
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

    private fun processTask(
        verifyManifestPermission: VerifyManifestPermission?, task: Task, fileName: String
    ) {
        job.launch {
            verifyManifestPermission?.blackPermissionList?.let {
                if (it.isNotEmpty()) {
                    checkBlacklistPermissions(task.name, fileName, it)
                }
            }
        }
    }

    /**
     * 检查黑名单权限
     */
    private suspend fun checkBlacklistPermissions(
        taskName: String, fileName: String, blackPermissions: ArrayList<String>
    ) {
        val result = waitLogFileCreate(fileName)

        if (result) {
            val manifestLogging = getManifestLogging(fileName)

            if (manifestLogging.isEmpty()) {
                throw VerifyPermissionException("$fileName file not found")
            } else {
                //查找所有权限
                val allPermissionList = findAllPermission(manifestLogging)
                //过滤出存在的黑名单权限
                val findPermissionResultList = filterBlackPermission(blackPermissions, allPermissionList)
                //
                if (findPermissionResultList.isNotEmpty()) {
                    val logTextList = getErrorSource(findPermissionResultList, manifestLogging)
                    throw VerifyPermissionException("Manifest has blackPermission,please check  $logTextList")
                }
            }
        }
    }

    /**
     * 监测日志文件
     */
    private suspend fun waitLogFileCreate(logFileName: String): Boolean {
        val result = withContext(Dispatchers.IO) {
            val file = File(logFileName)
            while (!file.exists()) {
                //nothing
            }
            true
        }
        return result
    }

    /**
     * 过滤查找黑名单权限那你
     */
    private fun filterBlackPermission(
        blackPermissions: ArrayList<String>, allPermissionList: List<String>
    ): ArrayList<String> {
        val findPermissionResultList = arrayListOf<String>()
        blackPermissions.forEach { blackPermission ->
            allPermissionList.forEach { permission ->
                val item = permission.replace(PREFIX_TAG, "")
                if (blackPermission == item) {
                    findPermissionResultList.add(permission)
                }
            }
        }
        return findPermissionResultList
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