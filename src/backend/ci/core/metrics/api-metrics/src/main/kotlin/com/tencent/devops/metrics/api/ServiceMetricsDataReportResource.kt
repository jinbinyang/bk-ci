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

package com.tencent.devops.metrics.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.event.pojo.measure.DispatchJobMetricsData
import com.tencent.devops.metrics.pojo.dto.CodeccDataReportDTO
import com.tencent.devops.metrics.pojo.dto.QualityDataReportDTO
import com.tencent.devops.metrics.pojo.dto.TurboDataReportDTO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_METRICS_DATAS", description = "METRICS-数据上报")
@Path("/service/metrics/datas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMetricsDataReportResource {

    @Operation(summary = "质量红线数据上报")
    @Path("/quality/data/report")
    @POST
    fun metricsQualityDataReport(
        @Parameter(description = "质量红线数据上报传输对象", required = true)
        qualityDataReportDTO: QualityDataReportDTO
    ): Result<Boolean>

    @Operation(summary = "codecc数据上报")
    @Path("/codecc/data/report")
    @POST
    fun metricsCodeccDataReport(
        @Parameter(description = "codecc数据上报传输对象", required = true)
        codeccDataReportDTO: CodeccDataReportDTO
    ): Result<Boolean>

    @Operation(summary = "编译加速数据上报")
    @Path("/turbo/data/report")
    @POST
    fun metricsTurboDataReport(
        @Parameter(description = "编译加速数据上报传输对象", required = true)
        turboDataReportDTO: TurboDataReportDTO
    ): Result<Boolean>

    @Operation(summary = "Job并发数据上报")
    @Path("/jobdispatch/data/report")
    @POST
    fun metricsJobDispatchDataReport(
        @Parameter(description = "Job并发数据上报传输对象", required = true)
        dispatchJobMetricsData: DispatchJobMetricsData
    ): Result<Boolean>
}
