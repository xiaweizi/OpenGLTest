package com.example.openglstudy.model

/**
 * 学习天数数据模型
 *
 * @param day 天数（1-14）
 * @param title 标题
 * @param description 简短描述
 * @param activityClass 对应的 Activity 类名（用于跳转）
 */
data class DayItem(
    val day: Int,
    val title: String,
    val description: String,
    val activityClass: Class<*>
)
