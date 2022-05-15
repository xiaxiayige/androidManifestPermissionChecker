package com.xiaxiayige.plugin

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        createExtensions(project)

        project.afterEvaluate {
            when (val androidExt = project.extensions.findByName("android")) {
//                is LibraryExtension -> {
//                    applyLibrary(project, androidExt)
//                }
                is BaseAppModuleExtension -> {
                    applyApp(project, androidExt)
                }
            }
        }
    }

    private fun applyApp(project: Project, androidExt: BaseAppModuleExtension) {
        androidExt.applicationVariants.all { createTask(project, it) }
    }

    private fun createExtensions(project: Project) {
        project.extensions.create(
            VerifyManifestPermissionExtensions.EXTENSIONS_NAME, VerifyManifestPermissionExtensions::class.java
        )
    }

    /**
     * 创建一个任务
     */
    private fun createTask(project: Project, variant: BaseVariant) {
        //任务名称定义
        val taskName = "verify${variant.name.capitalize()}ManifestPermission"

        if (project.tasks.findByName(taskName) != null) {
            return
        }
        //注册一个task
        val task = project.tasks.create(taskName, VerifyManifestPermissionTask::class.java) {
            it.variantName = variant.name
        }

        //依赖的task
        val targetTaskName = "process${variant.name.capitalize()}MainManifest"
        val targetTask = project.tasks.findByName(targetTaskName)

        //之前执行
        //targetTask?.dependsOn(task)
        //之后执行
        targetTask?.finalizedBy(task)

//        project.gradle.taskGraph.afterTask {
//            if (task.state.executed && task.state.failure != null) {
//                it.enabled = false
//            }
//        }
    }

    private fun applyLibrary(project: Project, androidExt: LibraryExtension) {
        androidExt.libraryVariants.all { createTask(project, it) }
    }

}