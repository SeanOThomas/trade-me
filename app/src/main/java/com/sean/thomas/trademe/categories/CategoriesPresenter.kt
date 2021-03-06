package com.sean.thomas.trademe.categories

import android.support.annotation.VisibleForTesting
import android.util.Log
import com.sean.thomas.trademe.Bus
import com.sean.thomas.trademe.network.Repository
import com.sean.thomas.trademe.network.models.Category
import com.sean.thomas.trademe.schedulers.SchedulersProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils

/**
 * Presenter for the categories portion of the screen.
 */
class CategoriesPresenter(
        private val view: CategoriesContract.View,
        private val repository: Repository,
        private val schedulersProvider: SchedulersProvider
): CategoriesContract.Presenter {

    companion object {
        val TAG = CategoriesPresenter::class.java.canonicalName!!
    }

    private val disposables: CompositeDisposable = CompositeDisposable()
    private lateinit var categoryTree: Category
    private var currentCategoryId: String = ""

    /**
     * Request the category tree then update the view.
     */
    override fun setUp() {
        view.showProgress()
        disposables.add(
                repository.getCategoryTree()
                        .subscribeOn(schedulersProvider.background())
                        .observeOn(schedulersProvider.mainThread())
                        .subscribe({ root ->
                            view.hideProgress()

                            categoryTree = root
                            view.setCategories(categoryTree.subCategories?: ArrayList())
                        }, {
                            error ->
                            view.hideProgress()

                            Log.e(CategoriesPresenter.TAG, "Failed to get categories", error)
                            view.showNetworkErrorMessage()
                        })
        )
    }

    override fun tearDown() {
        disposables.clear()
    }

    /**
     * Publish the event for exogenous listeners, then display the new subcategories if they exist.
     */
    override fun onCategoryClicked(category: Category) {
        Bus.publish(category)

        if (category.subCategories != null) {
            currentCategoryId = category.categoryId
            view.setCategories(category.subCategories)
        }
    }

    /**
     * Display the root (i.e., last set of) subcategories. If the root id is empty, then finish.
     */
    override fun onBackPressed() {
        if (currentCategoryId.isEmpty()) {
            return view.finish()
        }

        val rootCategory = getRootCategory(categoryTree, currentCategoryId)

        onCategoryClicked( rootCategory)
    }

    /**
     * If the current category id is "1234-5678-", gets the root category with id "1234-" from
     * [categoryTree].
     */
    @VisibleForTesting
    fun getRootCategory(categoryTree: Category,  categoryId: String) : Category {
        val depth = categoryId.count({ it == '-'})

        var tempRoot = categoryTree

        for(i in 1..(depth - 1)) {
            // the i'th '-' is the last char of tempId
            val lastIndex = StringUtils.ordinalIndexOf(categoryId, "-", i)

            // the cat id for the current depth
            val tempId = categoryId.subSequence(0, lastIndex + 1)

            // filter subcategories for tempId. note, every root should have subcategories, hence
            // the bang operator.
            tempRoot = tempRoot.subCategories!!.filter({
                it.categoryId == tempId
            })[0]
        }

        return tempRoot
    }
}