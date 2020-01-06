package com.wizaord.moneyweb.services

import com.wizaord.moneyweb.domain.Account
import com.wizaord.moneyweb.domain.AccountRepository
import com.wizaord.moneyweb.domain.User
import com.wizaord.moneyweb.domain.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class AccountServiceTest {

    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var accountRepository: AccountRepository
    @Mock
    lateinit var userService: UserService

    @InjectMocks
    lateinit var accountService: AccountService

    @Test
    fun `when create an account, then account is created and relies to all users`() {

        // given
        val user = User("id", "username", "pass", "email")
        user.addOwner("owner1")
        user.addOwner("owner2")

        val accountInput = Account(null, "accountName", Date(), user.owners)
        val accountOutput = Account("id", "accountName", Date(), user.owners)

        given(userService.getCurrentUser()).willReturn(user)
        given(accountRepository.save(ArgumentMatchers.any(Account::class.java))).willReturn(accountOutput)

        // when
        val accountCreated = accountService.create(accountInput)

        // then
        assertThat(accountCreated).isNotNull
        assertThat(accountCreated.id).isNotNull()
        assertThat(accountCreated.name).isEqualTo("accountName")
        assertThat(accountCreated.openDate).isNotNull()
        verify(userService).getCurrentUser()

        val userArgumentCaptor = ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).save(userArgumentCaptor.capture())
        val userUpdated = userArgumentCaptor.value
        assertThat(userUpdated).isNotNull
        val nbOwnerUpdated = userUpdated.owners.filter { it.accounts.isNotEmpty() }.count()
        assertThat(nbOwnerUpdated).isEqualTo(2)
    }
}