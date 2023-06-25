package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthVerifyRecordDao
import com.tencent.devops.auth.pojo.dto.VerifyRecordDTO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthVerifyRecordService @Autowired constructor(
    val dslContext: DSLContext,
    val authVerifyRecordDao: AuthVerifyRecordDao
) {
    fun createOrUpdateVerifyRecord(verifyRecordDTO: VerifyRecordDTO) {
        authVerifyRecordDao.createOrUpdate(
            dslContext = dslContext,
            verifyRecordDTO = verifyRecordDTO
        )
    }

    fun list(
        projectCode: String,
        resourceType: String,
        offset: Int,
        limit: Int
    ): List<VerifyRecordDTO> {
        return authVerifyRecordDao.list(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            offset = offset,
            limit = limit
        ).map { authVerifyRecordDao.convert(it) }
    }

    fun deleteVerifyRecord(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        userId: String? = null,
        action: String? = null
    ) {
        logger.info("delete verify record!|$projectCode|$resourceType|$resourceCode")
        authVerifyRecordDao.delete(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            userId = userId,
            action = action
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthVerifyRecordService::class.java)
    }
}
