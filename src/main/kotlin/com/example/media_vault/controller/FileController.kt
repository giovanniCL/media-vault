package com.example.media_vault.controller

import com.example.media_vault.service.FileService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@RestController
@RequestMapping("/files")
class FileController(private val fileService: FileService) {
    @PostMapping("/upload",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun uploadFile(@RequestPart("file") file: FilePart, @RequestHeader("Content-Length") contentLength: Long): Mono<String> {
        return fileService.uploadFile(file, contentLength)
    }
    @GetMapping("/{id}")
    fun downloadFile(@PathVariable("id") id: String): Mono<ResponseEntity<InputStreamResource>> {
        val stream = fileService.downloadFile(id)
        return stream.map {
            val (stream, name) = it
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${name}\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(stream)
        }
    }
}