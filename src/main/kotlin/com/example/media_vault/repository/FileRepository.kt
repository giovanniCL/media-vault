package com.example.media_vault.repository
import File
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.web.multipart.MultipartFile

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono

public interface FileRepository: ReactiveCrudRepository<File, String> {

}