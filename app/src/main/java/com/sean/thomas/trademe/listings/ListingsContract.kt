package com.sean.thomas.trademe.listings

import com.sean.thomas.trademe.BasePresenter
import com.sean.thomas.trademe.BaseView
import com.sean.thomas.trademe.network.models.Listing

interface ListingsContract {

    interface View : BaseView {
        fun hide()
        fun show()
        fun setListings(listings: List<Listing>)
    }

    interface Presenter: BasePresenter {
        fun onListingClicked(listingId: String)
    }
}