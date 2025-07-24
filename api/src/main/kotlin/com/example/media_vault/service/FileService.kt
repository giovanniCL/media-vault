package com.example.media_vault.service
import File
import com.example.media_vault.producer.MessageProducer
import com.example.media_vault.repository.FileRepository
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import kotlinx.coroutines.reactive.collect
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.data.repository.query.Param
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestHeader
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable


@Throws(IOException::class)
fun getInputStreamFromFluxDataBuffer(data: Flux<DataBuffer?>): InputStream {
    val osPipe = PipedOutputStream()
    val isPipe = PipedInputStream(osPipe)

    DataBufferUtils.write(data, osPipe)
        .subscribeOn(Schedulers.boundedElastic())
        .doOnComplete({
            try {
                osPipe.close()
            } catch (ignored: IOException) {
            }
        })
        .subscribe(DataBufferUtils.releaseConsumer())
    return isPipe
}


@Service
class FileService(private val minioClient: MinioClient, private val fileRepository: FileRepository, private val messageProducer: MessageProducer) {
    @Value("\${minio.bucket.name}")
    lateinit var bucketName: String
    fun uploadFile(@Param("file") filePart: FilePart, contentLength: Long): Mono<String>{
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val formattedNow = now.format(formatter)
        val fileId = filePart.filename() + "_" + formattedNow
        val objectName = Mono.fromCallable<String>(Callable {
            getInputStreamFromFluxDataBuffer(filePart.content())
                .use fromCallable@{ inputStream ->
                    val objectName: String = fileId
                    val found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                    if (!found) {
                        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    }
                    minioClient.putObject(
                        PutObjectArgs.builder()
                            .bucket(bucketName)
                            .`object`(objectName)
                            .stream(inputStream, filePart.headers().getContentLength(),5242880
                            )
                            .contentType(filePart.headers().getContentType().toString())
                            .build()
                    )
                    messageProducer.sendMessage(objectName, "fileExchange", "images")
                    return@fromCallable objectName
                }

        })
        val fileToUpload: File = File(fileId=fileId)
        val saved = fileRepository.save(fileToUpload).mapNotNull({it.id})
        return Mono.zip(objectName, saved){ _, p2 -> "File uploaded successfully: $p2" }
    }

    fun downloadFile(@Param("id") id: String): Mono<Pair<InputStreamResource, String>>{
        return fileRepository.findById(id)
            .map {
                println(it)
                Pair(InputStreamResource(
                    minioClient.getObject(
                        GetObjectArgs.builder()
                            .bucket(bucketName)
                            .`object`(it.fileId)
                            .build()
                    )
                ),it.fileId)
            }
    }
}