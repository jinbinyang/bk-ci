/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.auth.service.migrate

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.request.FilterDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.bk.sdk.iam.exception.IamException
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.pojo.dto.ResourceMigrationCountDTO
import com.tencent.devops.auth.service.AuthResourceService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.RbacCacheService
import com.tencent.devops.auth.service.RbacPermissionResourceService
import com.tencent.devops.auth.service.ResourceService
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.service.trace.TraceTag
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired

/**
 * 将资源迁移到权限中心
 */
@Suppress("LongParameterList", "MagicNumber")
class MigrateResourceService @Autowired constructor(
    private val resourceService: ResourceService,
    private val rbacCacheService: RbacCacheService,
    private val rbacPermissionResourceService: RbacPermissionResourceService,
    private val authResourceService: AuthResourceService,
    private val migrateResourceCodeConverter: MigrateResourceCodeConverter,
    private val tokenApi: AuthTokenApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val dslContext: DSLContext,
    private val authMigrationDao: AuthMigrationDao,
    private val deptService: DeptService,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao
) {

    @Suppress("SpreadOperator")
    fun migrateResource(
        projectCode: String,
        iamApprover: String
    ) {
        val startEpoch = System.currentTimeMillis()
        logger.info("start to migrate resource:$projectCode")
        try {
            val resourceTypes = rbacCacheService.listResourceTypes()
                .map { it.resourceType }
                .filterNot { noNeedToMigrateResourceType.contains(it) }

            logger.info("MigrateResourceService|resourceTypes:$resourceTypes")
            // 迁移各个资源类型下的资源
            val traceId = MDC.get(TraceTag.BIZID)
            val resourceTypeFuture = resourceTypes.map { resourceType ->
                CompletableFuture.supplyAsync(
                    {
                        MDC.put(TraceTag.BIZID, traceId)
                        migrateResource(
                            projectCode = projectCode,
                            resourceType = resourceType,
                            iamApprover = iamApprover
                        )
                    },
                    executorService
                )
            }
            CompletableFuture.allOf(*resourceTypeFuture.toTypedArray()).join()

            // 统计资源数
            val resourceCountDTOs = resourceTypes.map { resourceType ->
                calculateResourceCount(projectCode = projectCode, resourceType = resourceType)
            }
            authMigrationDao.updateResourceCount(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceCountInfo = JsonUtil.toJson(resourceCountDTOs)
            )
        } finally {
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to migrate resource $projectCode")
        }
    }

    private fun migrateResource(
        projectCode: String,
        resourceType: String,
        iamApprover: String
    ) {
        val startEpoch = System.currentTimeMillis()
        logger.info("start to migrate resource|$projectCode|$resourceType")
        try {
            createRbacResource(
                resourceType = resourceType,
                projectCode = projectCode,
                iamApprover = iamApprover
            )
        } catch (ignore: Exception) {
            logger.error("Failed to migrate resource|$projectCode|$resourceType", ignore)
            throw ignore
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to migrate resource|$projectCode|$resourceType"
            )
        }
    }

    @Suppress("ALL")
    private fun createRbacResource(
        resourceType: String,
        projectCode: String,
        iamApprover: String
    ) {
        var offset = 0L
        val limit = 100L
        do {
            val resourceData = listInstance(
                offset = offset,
                limit = limit,
                resourceType = resourceType,
                projectCode = projectCode
            )
            logger.info("MigrateResourceService|resourceData:$resourceData")
            if (resourceData == null || resourceData.data.result.isNullOrEmpty()) {
                return
            }
            val ids = resourceData.data.result.map { it.id }
            val instanceInfoList = fetchInstanceInfo(
                resourceType = resourceType,
                projectCode = projectCode,
                ids = ids
            ) ?: return
            instanceInfoList.data.map {
                JsonUtil.to(JsonUtil.toJson(it), InstanceInfoDTO::class.java)
            }.forEach {
                val resourceCode =
                    migrateResourceCodeConverter.getRbacResourceCode(
                        resourceType = resourceType,
                        migrateResourceCode = it.id
                    ) ?: return@forEach
                logger.info("MigrateResourceService|resourceCode:$resourceCode")
                authResourceService.getOrNull(
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode
                ) ?: run {
                    // 版本体验名称会重复,需添加上时间戳
                    val resourceName = if (resourceType == AuthResourceType.EXPERIENCE_TASK_NEW.value) {
                        "${it.displayName}-${DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyyMMddHHmmss")}"
                    } else {
                        it.displayName
                    }
                    for (suffix in 0..MAX_RETRY_TIMES) {
                        try {
                            rbacPermissionResourceService.resourceCreateRelation(
                                userId = buildIamApprover(
                                    resourceCreator = it.iamApprover.firstOrNull(),
                                    iamApprover = iamApprover
                                ),
                                projectCode = projectCode,
                                resourceType = resourceType,
                                resourceCode = resourceCode,
                                resourceName = RbacAuthUtils.addSuffixIfNeed(resourceName, suffix),
                                async = false
                            )
                            break
                        } catch (iamException: IamException) {
                            if (iamException.errorCode != IAM_RESOURCE_NAME_CONFLICT_ERROR) throw iamException
                            if (suffix == MAX_RETRY_TIMES) throw iamException
                        }
                    }
                }
            }
            offset += limit
        } while (resourceData!!.data.result.size.toLong() == limit)
    }

    private fun listInstance(
        offset: Long,
        limit: Long,
        resourceType: String,
        projectCode: String
    ): ListInstanceResponseDTO? {
        val pathInfoDTO = PathInfoDTO().apply {
            type = AuthResourceType.PROJECT.value
            id = projectCode
        }
        val filterDTO = FilterDTO().apply {
            parent = pathInfoDTO
        }
        return resourceService.getInstanceByResource(
            callBackInfo = CallbackRequestDTO().apply {
                type = resourceType
                method = CallbackMethodEnum.LIST_INSTANCE
                filter = filterDTO
                page = PageInfoDTO().apply {
                    this.offset = offset
                    this.limit = limit
                }
            },
            token = tokenApi.getAccessToken(projectAuthServiceCode)
        ) as ListInstanceResponseDTO?
    }

    fun fetchInstanceInfo(
        resourceType: String,
        projectCode: String,
        ids: List<String>
    ): FetchInstanceInfoResponseDTO? {
        val pathInfoDTO = PathInfoDTO().apply {
            type = AuthResourceType.PROJECT.value
            id = projectCode
        }
        val filterDTO = FilterDTO().apply {
            parent = pathInfoDTO
            idList = ids
        }
        return resourceService.getInstanceByResource(
            callBackInfo = CallbackRequestDTO().apply {
                type = resourceType
                method = CallbackMethodEnum.FETCH_INSTANCE_INFO
                filter = filterDTO
            },
            token = tokenApi.getAccessToken(projectAuthServiceCode)
        ) as FetchInstanceInfoResponseDTO?
    }

    private fun calculateResourceCount(projectCode: String, resourceType: String): ResourceMigrationCountDTO {
        val count = authResourceService.countByProjectAndType(
            projectCode = projectCode,
            resourceType = resourceType
        )
        val groupConfigCount = authResourceGroupConfigDao.countByResourceType(
            dslContext = dslContext,
            resourceType = resourceType
        )
        return ResourceMigrationCountDTO(
            resourceType = resourceType,
            count = count,
            groupCount = count * groupConfigCount
        )
    }

    private fun buildIamApprover(
        resourceCreator: String?,
        iamApprover: String
    ): String = if (resourceCreator != null && deptService.getUserInfo("admin", resourceCreator) != null)
        resourceCreator else iamApprover

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateResourceService::class.java)
        private val noNeedToMigrateResourceType = listOf(
            AuthResourceType.PROJECT.value
        )
        private val executorService = Executors.newFixedThreadPool(10)
        private const val IAM_RESOURCE_NAME_CONFLICT_ERROR = 1902409L
        private const val MAX_RETRY_TIMES = 3
    }
}