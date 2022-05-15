package com.xiaxiayige.plugin

/**
 * @description: 黑名单权限
 * @author : xiaxiayige@163.com
 * @date: 2022/5/10
 * @version: 1.0.0
 */
open class VerifyManifestPermission {

    companion object {
        const val EXTENSIONS_NAME = "verifyManifestPermission"
    }

    /**
     * 黑名单权限列表
     */
    var blackPermissionList = ArrayList<String>()

    /**
     * 构建类型
     */
    var productFlavor: String? = null
}