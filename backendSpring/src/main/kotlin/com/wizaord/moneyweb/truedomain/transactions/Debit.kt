package com.wizaord.moneyweb.truedomain.transactions

import kotlin.math.absoluteValue

class Debit(
        libelle: String,
        libelleBanque: String,
        descriptionBanque: String?,
        amount: Double) : Transaction(amount.absoluteValue * -1, libelle, libelleBanque, descriptionBanque)
