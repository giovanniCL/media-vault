package com.example.worker.consumer

import io.minio.BucketExistsArgs
import io.minio.MinioClient
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.PutObjectArgs
import net.coobird.thumbnailator.Thumbnails
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

fun createThumbnail(inputStream: InputStream): ByteArray {
    val outputStream = ByteArrayOutputStream()
    Thumbnails.of(inputStream)
        .size(150, 150)
        .outputFormat("jpg")
        .toOutputStream(outputStream)
    return outputStream.toByteArray()
}
@Component
class MessageConsumer(private val minioClient: MinioClient) {
    @Value("\${minio.bucket.name}")
    lateinit var bucketName: String
    @Value("\${minio.thumbnails.bucket.name}")
    lateinit var thumbnailBucketName: String
    @RabbitListener(queues = ["imageQueue"])
    suspend fun receiveMessage(message: String) {
        println("Received message: $message")
        val stream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(message)
                .build()
        )
        val thumbnailStream = ByteArrayInputStream(createThumbnail(stream))
        val found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(thumbnailBucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(thumbnailBucketName).build());
        }
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(thumbnailBucketName)
                .`object`(message)
                .stream(thumbnailStream, -1,5242880
                )
                .build()
        )
        println("Created thumbnail for $message")
    }
}