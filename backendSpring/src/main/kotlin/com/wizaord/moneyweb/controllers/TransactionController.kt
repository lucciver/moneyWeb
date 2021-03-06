package com.wizaord.moneyweb.controllers

import com.wizaord.moneyweb.configuration.toDate
import com.wizaord.moneyweb.configuration.toLocalDate
import com.wizaord.moneyweb.domain.transactions.Credit
import com.wizaord.moneyweb.domain.transactions.Debit
import com.wizaord.moneyweb.domain.transactions.Transaction
import com.wizaord.moneyweb.domain.transactions.ventilations.CreditVentilation
import com.wizaord.moneyweb.domain.transactions.ventilations.DebitVentilation
import com.wizaord.moneyweb.services.FamilyBankAccountServiceFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.websocket.server.PathParam

@RestController
@RequestMapping("/moneyapi/family/{familyName}/accounts/{accountName}/transactions")
class TransactionController(
        @Autowired private val familyBankAccountServiceFactory: FamilyBankAccountServiceFactory
) {

    @GetMapping("")
    @ResponseBody
    fun getTransactions(@PathVariable familyName: String,
                        @PathVariable accountName: String,
                        @PathParam("skipInternal") skipInternal: Boolean?): List<com.wizaord.moneyweb.controllers.Transaction> {
        val familyService = familyBankAccountServiceFactory.getFamilyServiceForAccountWithTransactions(familyName, accountName)
        val transactions = when (skipInternal) {
            true -> familyService.transactionsNotInternal(accountName)
            else -> familyService.transactions(accountName)
        }
        return transactions.map { com.wizaord.moneyweb.controllers.Transaction.fromDomain(it) }
    }

    @PatchMapping("/{transactionId}")
    fun updateTransaction(@PathVariable familyName: String,
                          @PathVariable accountName: String,
                          @PathVariable transactionId: String,
                          @RequestBody transaction: com.wizaord.moneyweb.controllers.Transaction) {
        val familyService = familyBankAccountServiceFactory.getFamilyServiceWithTransactions(familyName)
        familyService.transactionUpdate(accountName, transactionId, transaction.toDomain())
    }

    @DeleteMapping("/{transactionId}")
    fun deleteTransaction(@PathVariable familyName: String,
                          @PathVariable accountName: String,
                          @PathVariable transactionId: String) {
        val familyService = familyBankAccountServiceFactory.getFamilyServiceWithTransactions(familyName)
        familyService.transactionDelete(accountName, transactionId)
    }

    @PostMapping
    @ResponseBody
    fun createTransaction(@PathVariable familyName: String,
                          @PathVariable accountName: String,
                          @RequestBody transaction: com.wizaord.moneyweb.controllers.Transaction): Transaction? {
        val familyService = familyBankAccountServiceFactory.getFamilyServiceWithTransactions(familyName)
        return familyService.transactionRegister(accountName, transaction.toDomain())
    }
}


data class Transaction(var id: String?,
                       var amount: Double,
                       var userLibelle: String,
                       var bankLibelle: String,
                       var bankDetail: String?,
                       var isPointe: Boolean,
                       var dateCreation: Date,
                       var ventilations: List<Ventilation>
) {

    fun toDomain(): Transaction {
        val transactionId = id ?: UUID.randomUUID().toString()
        val transaction = when (amount > 0) {
            true -> Credit(userLibelle, bankLibelle, bankDetail, amount, isPointe, transactionId, dateCreation.toLocalDate())
            false -> Debit(userLibelle, bankLibelle, bankDetail, amount, isPointe, transactionId, dateCreation.toLocalDate())
        }
        this.ventilations.map { transaction.addVentilation(it.toDomain()) }.toMutableList()
        return transaction
    }

    companion object {
        fun fromDomain(t: Transaction): com.wizaord.moneyweb.controllers.Transaction {
            return com.wizaord.moneyweb.controllers.Transaction(t.id, t.amount, t.userLibelle,
                    t.bankLibelle, t.bankDetail, t.isPointe, t.dateCreation.toDate(), t.ventilations.map { Ventilation.fromDomain(it) })
        }
    }
}

data class Ventilation(var amount: Double,
                       var categoryId: String?) {
    fun toDomain(): com.wizaord.moneyweb.domain.transactions.ventilations.Ventilation {
        return when (amount >= 0) {
            true -> CreditVentilation(amount, categoryId)
            false -> DebitVentilation(amount, categoryId)
        }
    }

    companion object {
        fun fromDomain(v: com.wizaord.moneyweb.domain.transactions.ventilations.Ventilation): Ventilation {
            return Ventilation(v.amount, v.categoryId)
        }
    }

}