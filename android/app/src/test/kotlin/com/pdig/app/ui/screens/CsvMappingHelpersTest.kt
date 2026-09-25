package com.pdig.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * CSV 字段对应步骤（goal §23）所用纯函数的 JVM 测试：
 * 表头切分 / 首行示例 / 按选中列构造 MappingProfile 都是纯逻辑，不依赖设备。
 */
class CsvMappingHelpersTest {

    private val head = "交易时间,交易金额,商户名称\n" +
        "2026-01-05 10:00:00,-128.00,示例商户\n" +
        "2026-01-06 09:00:00,-20.50,另一商户\n"

    @Test
    fun csvColumnsParsesHeaderIgnoringQuotesAndBlanks() {
        assertEquals(listOf("交易时间", "交易金额", "商户名称"), csvColumns(head))
    }

    @Test
    fun csvSampleRowTakesFirstDataRowOnly() {
        val sample = csvSampleRow(head)
        assertEquals("2026-01-05 10:00:00", sample["交易时间"])
        assertEquals("-128.00", sample["交易金额"])
        assertEquals("示例商户", sample["商户名称"])
        assertEquals(3, sample.size)
    }

    @Test
    fun csvMappingBuildsProfileForSelectedColumns() {
        val mapping = requireNotNull(csvMapping(head, "交易时间", "交易金额"))
        assertEquals("交易时间", mapping.columns.dateTime)
        assertEquals("交易金额", mapping.columns.amount)
    }

    @Test
    fun csvMappingRejectsColumnsNotInHeader() {
        assertNull(csvMapping(head, "交易时间", "不存在的列"))
        assertNull(csvMapping(head, "不存在的列", "交易金额"))
    }

    @Test
    fun defaultCsvMappingDetectsDateAndAmountColumns() {
        val mapping = requireNotNull(defaultCsvMapping(head))
        assertEquals("交易时间", mapping.columns.dateTime)
        assertEquals("交易金额", mapping.columns.amount)
    }

    @Test
    fun defaultCsvMappingReturnsNullWhenNoUsableColumns() {
        assertNull(defaultCsvMapping("编号,备注\n1,无"))
    }

    @Test
    fun csvColumnsHandlesSemicolonDelimitedHeader() {
        assertEquals(listOf("date", "amount"), csvColumns("date;amount\n2026-01-05;100"))
    }
}
