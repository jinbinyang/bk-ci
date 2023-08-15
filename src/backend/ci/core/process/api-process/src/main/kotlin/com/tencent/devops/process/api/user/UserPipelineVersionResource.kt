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
 */

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.PipelineModelAndYaml
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.process.engine.pojo.PipelineResVersion
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.PipelineOperationDetail
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.transfer.PreviewResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PIPELINE_VERSION"], description = "用户-流水线版本管理")
@Path("/user/version")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList")
interface UserPipelineVersionResource {

    @ApiOperation("通过指定模板创建流水线")
    @POST
    @Path("/projects/{projectId}/createPipelineWithTemplate")
    fun createPipelineFromPipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("是否使用模板配置", required = false)
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean? = false,
        @ApiParam(value = "流水线模型实例请求", required = true)
        pipeline: TemplateInstanceCreateRequest
    ): Result<PipelineId>


    @ApiOperation("获取流水线指定版本的两种编排")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/versions/{version}")
    fun getVersion(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("流水线编排版本", required = true)
        @PathParam("version")
        version: Int
    ): Result<PipelineModelAndYaml>

    @ApiOperation("新建流水线编排")
    @POST
    @Path("/projects/{projectId}/createPipeline")
    fun createPipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("是否使用模板配置", required = false)
        @QueryParam("useTemplateSettings")
        useTemplateSettings: Boolean? = false,
        @ApiParam(value = "流水线PAC模型", required = true)
        pipeline: PipelineModelAndYaml
    ): Result<PipelineId>

    @ApiOperation("触发前配置")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/previewCode")
    fun preview(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("流水线版本号", required = false)
        @QueryParam("version")
        version: Int?
    ): Result<PreviewResponse>

    @ApiOperation("保存流水线编排草稿")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/saveDraft")
    fun savePipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "流水线模型与设置", required = true)
        @Valid
        model: Model,
        @ApiParam("变更说明", required = false)
        @QueryParam("description")
        description: String? = null
    ): Result<Boolean>

    @ApiOperation("保存流水线设置")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/saveSetting")
    fun saveSetting(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "流水线设置", required = true)
        setting: PipelineSetting
    ): Result<Boolean>

    @ApiOperation("获取流水线编排创建人列表（分页）")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/creatorList")
    fun creatorList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<String>>

    @ApiOperation("流水线编排版本列表（搜索、分页）")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/versions")
    fun versionList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("过滤创建人", required = false)
        @QueryParam("creator")
        creator: String? = null,
        @ApiParam("模糊查询变更说明", required = false)
        @QueryParam("description")
        description: String? = null,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PipelineResVersion>>

    @ApiOperation("获取流水线操作日志列表（分页）")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/operationLog")
    fun getPipelineOperationLogs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PipelineOperationDetail>>

    @ApiOperation("获取流水线操作人列表（分页）")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/operatorList")
    fun operatorList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<List<String>>
}
