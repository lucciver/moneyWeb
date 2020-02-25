package com.wizaord.moneyweb.services

import com.wizaord.moneyweb.domain.*
import com.wizaord.moneyweb.domain.transactions.Transaction
import com.wizaord.moneyweb.infrastructure.FamilyBankAccountPersistence
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@Scope("prototype")
class FamilyBankAccountsService(val familyName: String,
                                val loadTransactions: Boolean,
                                @Autowired val familyBankAccountPersistence: FamilyBankAccountPersistence): InfrastructureBankAccountFamilyNotifications, InfrastructureBankAccountNotifications {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    val familyBankAccounts = FamilyBankAccountsImpl(this.familyName, this)

    init {
        logger.info("FamilyBankAccountService will be initialised with family : {}", this.familyName)
        val familyBankAccounts = familyBankAccountPersistence.loadFamilyBankAccountByFamilyName(familyName)
        if (familyBankAccounts != null) {
            updateFamilyBankAccount(familyBankAccounts)
        }
        logger.info("FamilyBankAccountService successfully initialised")

    }

    private fun updateFamilyBankAccount(familyBankAccountsImpl: FamilyBankAccountsImpl) {
        familyBankAccounts.deactivateNotifications()
        familyBankAccountsImpl.getFamily().forEach {
            familyBankAccounts.registerFamilyMember(it)
        }
        familyBankAccounts.bankAccountsOwners.addAll(familyBankAccountsImpl.bankAccountsOwners)

        if (loadTransactions) {
            // load transactions from accounts
            familyBankAccountsImpl.bankAccountsOwners.forEach { bankAccountOwner ->
                val transactionsLoaded = familyBankAccountPersistence.loadTransactionsFromAccount(bankAccountOwner.bankAccount.getInternalId())
                transactionsLoaded.forEach { transaction -> bankAccountOwner.bankAccount.addTransaction(transaction) }
            }
        }

        // activate notification
        familyBankAccountsImpl.bankAccountsOwners.forEach { bankAccountOwner ->
            val bankAccountImpl = bankAccountOwner.bankAccount as BankAccountImpl
            bankAccountImpl.registerInfrastructureBankAccountNotification(this)
        }

        familyBankAccounts.activateNotifications()
    }

    fun accountRegister(name: String,
                        bankName: String,
                        createDate: LocalDate = LocalDate.now(),
                        owners: List<String> = this.familyBankAccounts.getFamily().map { it.username }) {

        val newBankAccount = BankAccountImpl(name, bankName, this, createDate)
        familyBankAccounts.registerAccount(newBankAccount, owners.map { owner -> FamilyMember(owner) })
    }


    fun accountClose(bankAccountName: String) = this.familyBankAccounts.accessToAccountByAccountName(bankAccountName)?.bankAccount?.close()
    fun accountOpen(bankAccountName: String) = this.familyBankAccounts.accessToAccountByAccountName(bankAccountName)?.bankAccount?.open()

    fun accountDelete(accountName: String) = familyBankAccounts.removeAccount(accountName)

    fun bankAccounts(): List<BankAccountOwners> = this.familyBankAccounts.accessToAccounts()

    fun owners() = familyBankAccounts.getFamily()

    fun ownerCreate(newFamilyMember: FamilyMember): FamilyMember {
        familyBankAccounts.registerFamilyMember(newFamilyMember)
        return newFamilyMember
    }

    override fun notifyFamilyBankAccountUpdate(familyBankAccountsImpl: FamilyBankAccountsImpl) {
        logger.info("FamilyAccount has been updated")
        this.familyBankAccountPersistence.updateFamily(familyBankAccountsImpl)
    }

    override fun notifyNewTransaction(accountInternalId: String, transaction: Transaction) {
        this.familyBankAccountPersistence.transactionCreate(accountInternalId, transaction)
        logger.info("new Transaction has been created")
    }

    override fun notifyRemoveTransaction(transaction: Transaction) {
        this.familyBankAccountPersistence.transactionRemove(transaction)
        logger.info("Transaction has been deleted")
    }

    override fun notifyAccountUpdate(accountImpl: BankAccountImpl) {
        this.notifyFamilyBankAccountUpdate(this.familyBankAccounts)
    }

    fun transactionRegister(accountId: String, transaction: Transaction) {
        logger.info("Registering a new transaction for account : $accountId")
        this.familyBankAccounts.accessToAccountByAccountName(accountId)?.bankAccount?.addTransaction(transaction)
    }

    fun transactions(accountName: String): List<Transaction> {
        return this.familyBankAccounts.accessToAccountByAccountName(accountName)?.bankAccount?.getTransactions()?: emptyList()
    }

    fun transactionUpdate(accountName: String, transactionId: String, transaction: Transaction) {
        if (transactionId != transaction.id) return
        if (! transaction.isValid()) return
        this.familyBankAccounts.accessToAccountByAccountName(accountName)?.bankAccount?.updateTransaction(transaction)
    }
    fun accountUpdateName(accountName: String, newAccountName: String) = this.familyBankAccounts.accessToAccountByAccountName(accountName)?.bankAccount?.updateName(newAccountName)
    fun accountUpdateBankName(accountName: String, newBankName: String) = this.familyBankAccounts.accessToAccountByAccountName(accountName)?.bankAccount?.updateBankName(newBankName)
    fun accountUpdateDateCreation(accountName: String, newAccountDate: LocalDate)  = this.familyBankAccounts.accessToAccountByAccountName(accountName)?.bankAccount?.updateBankAccountDateCreate(newAccountDate)
    fun accountUpdateOwners(accountName: String, newOwners: List<FamilyMember>)= this.familyBankAccounts.changeBankAccountOwners(accountName, newOwners)
    fun bankAccount(accountName: String) = this.familyBankAccounts.accessToAccountByAccountName(accountName)



}