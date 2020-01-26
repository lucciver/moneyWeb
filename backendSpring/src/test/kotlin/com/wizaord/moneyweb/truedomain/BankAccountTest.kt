package com.wizaord.moneyweb.truedomain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.time.LocalDate

internal class BankAccountTest {

    lateinit var bankAccount : BankAccount

    @BeforeEach
    internal fun beforeEach() {
        bankAccount = BankAccount("name", "bankName")
    }

    @Test
    internal fun `constructor - When I create a account with mandatory param, I can read theses param`() {
        // given

        // when

        // then
        assertThat(bankAccount.name).isEqualTo("name")
        assertThat(bankAccount.bankName).isEqualTo("bankName")
    }

    @Test
    internal fun `constructor - When I create a bankAccount without date, the current day date is setted`() {
        // given

        // when

        // then
        assertThat(bankAccount.dateCreation).isEqualTo(LocalDate.now())
    }

    @Test
    internal fun `constructor - When I create a bankAccount with a specific date, the dateCreation is this date`() {
        // given
        val oldDate = LocalDate.now().minusYears(3)

        // when
        val bankAccount = BankAccount("name", "bankName", oldDate)

        // then
        assertThat(bankAccount.dateCreation).isEqualTo(oldDate)
    }

}

