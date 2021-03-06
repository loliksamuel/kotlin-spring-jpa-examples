package com.example.demo.api.realestate.domain.jpa.services

import com.example.demo.api.common.EntityAlreadyExistException
import com.example.demo.api.common.EntityNotFoundException
import com.example.demo.api.realestate.domain.jpa.entities.Property
import com.example.demo.api.realestate.domain.jpa.entities.QueryDslEntity.qProperty
import com.example.demo.api.realestate.domain.jpa.repositories.PropertyRepository
import com.example.demo.util.optionals.toNullable
import com.querydsl.jpa.impl.JPAQuery
import org.springframework.stereotype.Component
import java.util.*
import javax.persistence.EntityManager
import javax.validation.Valid

@Component
class JpaPropertyService(
        private val propertyRepository: PropertyRepository,
        private val entityManager: EntityManager
) {
    fun exists(propertyId: UUID): Boolean = propertyRepository.exists(propertyId)

    fun findById(propertyId: UUID): Property? =
            propertyRepository
                    .getById(propertyId)
                    .toNullable()

    fun getById(propertyId: UUID): Property =
            findById(propertyId) ?: throw EntityNotFoundException(
                    "ENTITY NOT FOUND! query: property.id=$propertyId"
            )

    fun requireExists(propertyId: UUID): UUID =
            if (exists(propertyId)) {
                propertyId
            } else throw EntityNotFoundException(
                    "ENTITY NOT FOUND! query: property.id=$propertyId"
            )

    fun requireDoesNotExist(propertyId: UUID): UUID =
            if (!exists(propertyId)) {
                propertyId
            } else throw EntityAlreadyExistException(
                    "ENTITY ALREADY EXIST! query: property.id=$propertyId"
            )

    fun insert(@Valid property: Property): Property {
        requireDoesNotExist(property.id)
        return propertyRepository.save(property)
    }

    fun update(@Valid property: Property): Property {
        requireExists(property.id)
        return propertyRepository.save(property)
    }

    fun findByIdList(propertyIdList: List<UUID>): List<Property> {
        val query = JPAQuery<Property>(entityManager)
        val resultSet = query.from(qProperty)
                .where(
                        qProperty.id.`in`(propertyIdList)
                )
                .fetchResults()

        return resultSet.results
    }

    fun findByClusterId(clusterId: UUID): List<Property> {
        val query = JPAQuery<Property>(entityManager)
        val resultSet = query.from(qProperty)
                .where(
                        qProperty.clusterId.eq(clusterId)
                )
                .fetchResults()

        return resultSet.results
    }
}