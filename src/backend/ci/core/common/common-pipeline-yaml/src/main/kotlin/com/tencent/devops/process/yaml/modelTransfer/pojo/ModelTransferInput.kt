package com.tencent.devops.process.yaml.modelTransfer.pojo

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.yaml.modelTransfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.pojo.YamlVersion

data class ModelTransferInput(
    val userId: String,
    var model: Model,
    val setting: PipelineSetting,
    val version: YamlVersion.Version,
    val aspectWrapper: PipelineTransferAspectWrapper,
    val defaultScmType: ScmType = ScmType.CODE_GIT
)
