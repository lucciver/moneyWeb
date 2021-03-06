package com.wizaord.moneyweb.init

import com.wizaord.moneyweb.domain.categories.Category
import com.wizaord.moneyweb.domain.categories.CategoryFamily
import com.wizaord.moneyweb.domain.transactions.Credit
import com.wizaord.moneyweb.domain.transactions.Debit
import com.wizaord.moneyweb.domain.transactions.Transaction
import com.wizaord.moneyweb.domain.transactions.ventilations.CreditVentilation
import com.wizaord.moneyweb.domain.transactions.ventilations.DebitVentilation
import com.wizaord.moneyweb.domain.transactions.ventilations.Ventilation
import com.wizaord.moneyweb.services.CategoryService
import com.wizaord.moneyweb.services.FamilyBankAccountServiceFactory
import com.wizaord.moneyweb.services.FamilyBankAccountsService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDate
import kotlin.math.absoluteValue

@Component
class DebitCreditLoader(
        @Autowired val familyBankAccountServiceFactory: FamilyBankAccountServiceFactory,
        @Autowired val categoryService: CategoryService
) {

    private val logger = LoggerFactory.getLogger(DebitCreditLoader::class.java)

    @Value("\${moneyweb.init.initdatabase.fileLocation.transactions}")
    lateinit var transactionsFilePath: String

    @Value("\${moneyweb.init.initdatabase.fileLocation.transactionsDetails}")
    lateinit var transactionDetailsFilePath: String

    private lateinit var serviceBeanForFamily: FamilyBankAccountsService

    private val transactionVentilations: MutableMap<String, MutableList<Ventilation>> = mutableMapOf()
    private lateinit var categories : List<CategoryFamily>
    private var accountsMap: Map<String, String>? = null;

    fun loadFamilyBankAccount(familyName: String) {
        serviceBeanForFamily = familyBankAccountServiceFactory.getFamilyServiceWithoutTransactions(familyName)
        this.categories = categoryService.getAll()
    }

    fun loadDebitCredit(accountsMap: Map<String, String>) {
        this.accountsMap = accountsMap

        loadDetailMontant()
        File(transactionsFilePath)
                .forEachLine {
                    val splitStr = it.split(";").map { splitedChar -> splitedChar.replace("\"", "") }
                    transformCsvDebitCreditLineInTransactionAndAddToAccount(splitStr)
                }
    }


    private fun loadDetailMontant() {
        File(transactionDetailsFilePath)
                .forEachLine {
                    val splitStr = it.split(";")
                            .map { splitedChar -> splitedChar.replace("\"", "") }
                    val transactionId = splitStr[2]
                    val ventilation = transformVentilationLine(splitStr)

                    val ventilationsForTransaction = transactionVentilations.get(transactionId)
                    if (ventilationsForTransaction != null) {
                        ventilationsForTransaction.add(transformVentilationLine(splitStr))
                        transactionVentilations[transactionId] = ventilationsForTransaction
                    } else {
                        transactionVentilations[transactionId] = mutableListOf(transformVentilationLine(splitStr))
                    }
                }
        logger.info("All transactions have been loaded")
    }

    fun transformVentilationLine(splitStr: List<String>): Ventilation {
        var categorieId = splitStr[3]
        val compteVirementInterne = splitStr[4]
        val amount = splitStr[1].toDouble()
        val ventilation = when (amount > 0) {
            true -> CreditVentilation(amount)
            false -> DebitVentilation(amount.absoluteValue)
        }
        if (categorieId == "NULL") {
            if (compteVirementInterne == "NULL") {
                categorieId = CategoryFamily.VIREMENT_INTERNE_ID
            } else {
                // on recherche le virement du compte
                val accountName = this.accountsMap?.get(compteVirementInterne)
                if (accountName == null) {
                    logger.error("Unable to find account for ID - $compteVirementInterne")
                }
                val categoryVirement = findCategoryByName("VIREMENT - $accountName")
                categorieId = categoryVirement.id
            }
        }
        ventilation.categoryId = findCategoryById(categorieId).id
        return ventilation
    }

    private fun findCategoryById(categoryId: String): Category {
        val categoryFamily = categories.firstOrNull { categoryFamily ->
            categoryFamily.findById(categoryId) != null
        } ?: error("Impossible de trouver la categorie avec l'ID $categoryId")
        return categoryFamily.findById(categoryId)!!
    }

    private fun findCategoryByName(categoryName: String): Category {
        val categoryFamily = categories.firstOrNull { categoryFamily ->
            categoryFamily.findByName(categoryName) != null
        } ?: error("Impossible de trouver la categorie avec le nom $categoryName")
        return categoryFamily.findByName(categoryName)!!
    }

    //"149645";"BP LEP";"2010-01-08 00:00:00";"1";"2010-01-08 00:00:00";"7700.00";"33";"SOUSCRIPTION LIVRET EPARG"
    fun transformCsvDebitCreditLineInTransactionAndAddToAccount(csvLine: List<String>) {
        val montantTransfere = csvLine[5].replace("\"", "").toDouble()
        val accountDestination = csvLine[6]
        val detailLibellebanque = csvLine.getOrElse(7) { "" }
        val isCredit = (montantTransfere >= 0)

        val isPointe = csvLine[3] == "1"

        val userLibelle = csvLine[1]
        val transactionId = csvLine[0]
        val ventilationsExtracted = this.transactionVentilations[transactionId]
        val amountTotal = ventilationsExtracted!!.stream().mapToDouble { it.amount }.sum()

        var transaction: Transaction?
        val dateCreate = createDateFromString(csvLine[2])!!
        if (isCredit) {
            transaction = Credit(userLibelle, userLibelle, detailLibellebanque, amountTotal, isPointe, transactionId, dateCreate)
        } else {
            transaction = Debit(userLibelle, userLibelle, detailLibellebanque, amountTotal, isPointe, transactionId, dateCreate)
        }

        // search account by Id
        ventilationsExtracted.forEach { transaction.addVentilation(it) }

        serviceBeanForFamily.transactionRegister(this.accountsMap?.get(accountDestination)!!, transaction, activateDupDetection = false)

    }

    fun createDateFromString(dateSrt: String): LocalDate? {
        if (dateSrt == "NULL") return null
        return LocalDate.parse(dateSrt.split(" ")[0])
    }

}