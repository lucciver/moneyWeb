package com.wizaord.moneyweb.services

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.xml.bind.DatatypeConverter

@Service
class JwtService {

    companion object {
        const val ISSUER = "moneyweb.com"
    }

    private val logger = LoggerFactory.getLogger(javaClass.canonicalName)

    @Value("\${moneyweb.jwt.secretKeyBase64}")
    lateinit var secretKey: String

    fun generateToken(userId: String, username: String): String {
        return Jwts.builder()
                .setSubject(username)
                .setIssuer(ISSUER)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plus(3000, ChronoUnit.SECONDS)))
                .setId(UUID.randomUUID().toString())
                .addClaims(generateCustomClaims(userId))
                .signWith(SignatureAlgorithm.HS512, this.secretKey)
                .compact()
    }

    fun generateSecretKey() {
        val keyPair = Keys.secretKeyFor(SignatureAlgorithm.HS512)
        println(Encoders.BASE64.encode(keyPair.encoded))
    }

    private fun generateCustomClaims(userId: String): Map<String, String> {
        val customClaims = mutableMapOf<String, String>()
        customClaims["USERID"] = userId
        return customClaims
    }

    fun isTokenValid(jwtFromRequest: String): Boolean {
        return decodeJwt(jwtFromRequest) != null
    }

    private fun decodeJwt(jwtToken: String): Jws<Claims>? {
        try {
            return Jwts.parser()
                    .setSigningKey(DatatypeConverter.parseBase64Binary(this.secretKey))
                    .parseClaimsJws(jwtToken)
        } catch (e: Exception) {
            logger.warn("Unable to parse JWT token : {}", jwtToken)
        }
        return null
    }

    fun getUsernameFromToken(jwtToken: String): String {
        return decodeJwt(jwtToken)!!.body!!.subject
    }

    fun getUserIdFromToken(jwtToken: String): String {
        return decodeJwt(jwtToken)!!.body!!["USERID"].toString()
    }
}


