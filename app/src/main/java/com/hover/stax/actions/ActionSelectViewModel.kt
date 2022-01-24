package com.hover.stax.actions

import android.app.Application
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.hover.sdk.actions.HoverAction
import com.hover.stax.R
import com.hover.stax.transfers.NonStandardVariable
import com.hover.stax.views.AbstractStatefulInput
import timber.log.Timber

class ActionSelectViewModel(private val application: Application) : ViewModel() {

    private val filteredActions = MediatorLiveData<List<HoverAction>>()
    val activeAction = MediatorLiveData<HoverAction>()
    val nonStandardVariables   =  MediatorLiveData<ArrayList<NonStandardVariable>>()

    init {
        activeAction.addSource(filteredActions, this::setActiveActionIfOutOfDate)
        nonStandardVariables.addSource(activeAction,this::setupNonStandardVariables)
    }

    private fun setActiveActionIfOutOfDate(actions: List<HoverAction>) {
        if (!actions.isNullOrEmpty() && (activeAction.value == null || !actions.contains(activeAction.value!!))) {
            val action = actions.first()
            activeAction.postValue(action)
        }
    }

    fun setActions(actions: List<HoverAction>) = filteredActions.postValue(actions)

    fun setActiveAction(action: HoverAction?) = action?.let { activeAction.postValue(action) }

    fun errorCheck(): String? {
        return if (activeAction.value == null) application.getString(R.string.action_fielderror) else null
    }

    private fun setupNonStandardVariables(action: HoverAction?) {
        action?.let {
            //The commented out is for easy functional testing sake
            //val variableKeys = ArrayList<String>()
            //variableKeys.add("Country")
            //variableKeys.add("City")

            val variableKeys :  List<String> = getNonStandardParams(action)
            if(variableKeys.isEmpty()) nullifyNonStandardVariables()
            else initNonStandardVariables(variableKeys)
        }
    }

    private fun getNonStandardParams(action: HoverAction) : List<String> {
        val variableKeys = mutableListOf<String>()
        action.requiredParams.forEach {
            if(!isAStandardParam(it)) variableKeys.add(it)
        }
        return variableKeys
    }

    private fun isAStandardParam(param: String): Boolean {
        return param == HoverAction.PHONE_KEY || param == HoverAction.ACCOUNT_KEY
                || param == HoverAction.AMOUNT_KEY || param == HoverAction.NOTE_KEY
                || param == HoverAction.PIN_KEY
    }

    private fun initNonStandardVariables(variableKeys :  List<String>) {
        Timber.i("init standard variables")

        val itemList = ArrayList<NonStandardVariable>()
        variableKeys.map { itemList.add(NonStandardVariable(it, null)) }
        nonStandardVariables.postValue(itemList)
    }

    private fun nullifyNonStandardVariables() {
        nonStandardVariables.postValue(null)
    }

    fun updateNonStandardVariables(nonStandardVariable: NonStandardVariable) {
        var itemList = nonStandardVariables.value
        if(itemList == null) itemList = ArrayList()

        itemList.find { it.key == nonStandardVariable.key }
                ?.let { it.value = nonStandardVariable.value }
                ?: itemList.add(nonStandardVariable)

        nonStandardVariables.postValue(itemList);
    }

    fun nonStandardVariablesAnError(): Boolean {
        with(nonStandardVariables.value) {
            when {
                this == null -> return false
                this.isEmpty() -> return true
                else -> {
                    this.forEachIndexed{index, it->
                        if (it.value == null) it.editTextState = AbstractStatefulInput.ERROR
                        else {
                            if (it.value!!.replace(" ".toRegex(), "").isEmpty()) it.editTextState = AbstractStatefulInput.ERROR
                            else it.editTextState = AbstractStatefulInput.SUCCESS
                        }
                    }

                    nonStandardVariables.postValue(this)
                    return find { it.editTextState == AbstractStatefulInput.ERROR } != null
                }
            }
        }
    }
}