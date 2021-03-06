package com.wizaord.moneyweb.services

import com.nhaarman.mockitokotlin2.anyOrNull
import com.wizaord.moneyweb.domain.FamilyMember
import com.wizaord.moneyweb.infrastructure.FamilyBankAccountPersistence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class FamilyBankAccountsCreateServiceTest {

    @Mock
    lateinit var familyBankAccountPersistence: FamilyBankAccountPersistence

    @InjectMocks
    lateinit var familyBankAccountsCreateService: FamilyBankAccountsCreateService

    @Test
    internal fun `initFamily - when function is called, then family is created with nothing but with one owner`() {
        // given
        given(familyBankAccountPersistence.initFamily(anyOrNull())).willAnswer { i -> i.arguments[0] }
        // when
        val familyCreated = familyBankAccountsCreateService.initFamily("family")

        // then
        assertThat(familyCreated).isNotNull
        assertThat(familyCreated.getFamily()).hasSize(1)
        assertThat(familyCreated.getFamily()[0]).isEqualTo(FamilyMember("family"))
        assertThat(familyCreated.familyName).isEqualTo("family")
        assertThat(familyCreated.accessToAccounts()).hasSize(0)
    }
}