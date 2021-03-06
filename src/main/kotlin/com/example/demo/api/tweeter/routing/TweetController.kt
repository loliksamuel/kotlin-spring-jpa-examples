package com.example.demo.api.tweeter.routing

import com.example.demo.api.tweeter.domain.entities.Author
import com.example.demo.api.tweeter.domain.entities.Tweet
import com.example.demo.api.tweeter.domain.services.EsTweetService
import com.example.demo.api.tweeter.domain.services.JpaAuthorService
import com.example.demo.api.tweeter.domain.services.JpaTweetService
import com.example.demo.util.fp.pipe
import io.swagger.annotations.ApiModel
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@CrossOrigin(origins = arrayOf("*"))
class TweetController(
        private val jpaTweetService: JpaTweetService,
        private val jpaAuthorService: JpaAuthorService,
        private val esTweetService: EsTweetService
) {

    @PostMapping("/tweets", consumes = arrayOf(MediaType.APPLICATION_JSON_VALUE), produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    fun create(@Validated @RequestBody request: CreateRequest): Any? {
        val author: Author = jpaAuthorService.getById(authorId = request.authorId)

        val tweet = Tweet(
                id = UUID.randomUUID(),
                createdAt = Instant.EPOCH,
                modifiedAt = Instant.EPOCH,
                author = author,
                message = request.message
        ) pipe {
            jpaTweetService.save(it)
        }

        esTweetService.put(tweet)

        return tweet
    }

    @GetMapping("/tweets/{tweetId}")
    fun getOne(@PathVariable tweetId: UUID): Any? {
        val tweet: Tweet = jpaTweetService.getById(tweetId)

        return tweet
    }

    @PostMapping("/tweets/{tweetId}")
    fun update(
            @PathVariable tweetId: UUID,
            @Validated @RequestBody request: UpdateRequest
    ): Any? {
        val sourceTweet: Tweet = jpaTweetService.getById(tweetId = tweetId)

        val sinkTweet: Tweet = sourceTweet.pipe {
            if (request.authorId != null) {
                val author: Author = jpaAuthorService.getById(authorId = request.authorId)
                it.copy(author = author)
            } else {
                it
            }
        }.pipe {
            if (request.message != null) {
                it.copy(message = request.message)
            } else {
                it
            }
        } pipe {
            val isModified = it != sourceTweet
            if (isModified) {
                val savedTweet = jpaTweetService.save(it)
                esTweetService.put(savedTweet)
                savedTweet
            } else {
                it
            }
        }

        return sinkTweet
    }


    @ApiModel(value = "Tweet.CreateRequest")
    data class CreateRequest(
            val authorId: UUID,
            val message: String
    )

    @ApiModel(value = "Tweet.UpdateRequest")
    data class UpdateRequest(
            val authorId: UUID?,
            val message: String?
    )
}

