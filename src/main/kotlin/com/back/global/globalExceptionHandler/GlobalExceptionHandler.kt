package com.back.global.globalExceptionHandler

import com.back.global.exception.ServiceException
import com.back.global.rsData.ResultCode
import com.back.global.rsData.RsData
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException::class)
    fun handle(ex: NoSuchElementException): ResponseEntity<RsData<Nothing?>> {
        return ResponseEntity(
            RsData("404-1", "해당 데이터가 존재하지 않습니다."),
            HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handle(ex: BadCredentialsException): ResponseEntity<RsData<Nothing?>> {
        return ResponseEntity(
            RsData(
                ResultCode.INVALID_CREDENTIALS.code,
                ResultCode.INVALID_CREDENTIALS.defaultMessage
            ),
            HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(ex: ConstraintViolationException): ResponseEntity<RsData<Nothing?>> {
        val message = ex.constraintViolations
            .map { v ->
                val field = v.propertyPath.toString().substringAfterLast('.')
                val code = v.messageTemplate
                    .removePrefix("{").removeSuffix("}")
                    .split('.')
                    .let { parts -> if (parts.size >= 2) parts[parts.size - 2] else "Unknown" }
                "$field-$code-${v.message}"
            }
            .sorted()
            .joinToString("\n")

        return ResponseEntity(
            RsData("400-1", message),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(ex: MethodArgumentNotValidException): ResponseEntity<RsData<Nothing?>> {
        val message = ex.bindingResult.allErrors
            .filterIsInstance<FieldError>()
            .map { error -> "${error.field}-${error.code}-${error.defaultMessage}" }
            .sorted()
            .joinToString("\n")

        return ResponseEntity(
            RsData("400-1", message),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(ex: HttpMessageNotReadableException): ResponseEntity<RsData<Nothing?>> {
        return ResponseEntity(
            RsData("400-1", "요청 본문이 올바르지 않습니다."),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handle(ex: MissingRequestHeaderException): ResponseEntity<RsData<Nothing?>> {
        return ResponseEntity(
            RsData("400-1", "${ex.headerName}-NotBlank-${ex.localizedMessage}"),
            HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler(ServiceException::class)
    fun handle(ex: ServiceException): ResponseEntity<RsData<Nothing?>> {
        val rsData = ex.rsData
        val status = HttpStatus.resolve(rsData.statusCode) ?: HttpStatus.BAD_REQUEST
        return ResponseEntity(rsData, status)
    }
}
