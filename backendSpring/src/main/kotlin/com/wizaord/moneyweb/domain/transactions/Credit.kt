package com.wizaord.moneyweb.domain.transactions

import kotlin.math.absoluteValue

class Credit(
        libelle: String,
        libelleBanque: String,
        descriptionBanque: String?,
        amount: Double) : Transaction(amount.absoluteValue, libelle, libelleBanque, descriptionBanque)