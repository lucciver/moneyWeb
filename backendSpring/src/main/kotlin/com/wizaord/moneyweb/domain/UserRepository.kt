package com.wizaord.moneyweb.domain

import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, String> {

    fun findByUsername(username: String): User?
    fun findByEmail(email: String): User?
}