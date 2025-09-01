package com.back.util

import org.mockito.ArgumentMatchers

/**
 * Kotlin에서 Mockito any() null 문제 해결을 위한 유틸리티
 */
object MockitoKotlinUtils {
    fun <T> any(): T = ArgumentMatchers.any<T>()
    fun <T> any(type: Class<T>): T = ArgumentMatchers.any(type)
}
