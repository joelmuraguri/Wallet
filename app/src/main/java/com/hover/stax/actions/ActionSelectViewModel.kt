package com.hover.stax.actions

import android.app.Application
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.hover.sdk.actions.HoverAction
import com.hover.stax.R
import com.hover.stax.utils.Constants

class ActionSelectViewModel(private val application: Application) : ViewModel() {

	private val filteredActions = MediatorLiveData<List<HoverAction>>()
	val activeAction = MediatorLiveData<HoverAction>()
	val nonStandardVariables = MediatorLiveData<LinkedHashMap<String, String>>()

	init {
		activeAction.addSource(filteredActions, this::setActiveActionIfOutOfDate)
		nonStandardVariables.addSource(activeAction, this::setupNonStandardVariables)
	}

	private fun setActiveActionIfOutOfDate(actions: List<HoverAction>) {
		if (!actions.isNullOrEmpty() && (activeAction.value == null || !actions.contains(
				activeAction.value!!))) {
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
		action?.let { //The commented out is for easy functional testing sake
			//val variableKeys = ArrayList<String>()
			//variableKeys.add("Country")
			// variableKeys.add("City")

			val variableKeys: List<String> = getNonStandardParams(action)
			if (variableKeys.isEmpty()) nullifyNonStandardVariables()
			else initNonStandardVariables(variableKeys)
		}
	}

	private fun getNonStandardParams(action: HoverAction): List<String> {
		val variableKeys = mutableListOf<String>()
		action.requiredParams.forEach {
			if (!isAStandardParam(it)) variableKeys.add(  it )
		}
		return variableKeys
	}

	private fun isAStandardParam(param: String): Boolean {
		return getListOfStandardParams().contains(param)
	}

	private fun getListOfStandardParams(): Set<String> {
		val listOfStrings = mutableSetOf<String>()
		listOfStrings.add(HoverAction.PHONE_KEY)
		listOfStrings.add(HoverAction.ACCOUNT_KEY)
		listOfStrings.add(HoverAction.AMOUNT_KEY)
		listOfStrings.add(HoverAction.NOTE_KEY)
		listOfStrings.add(HoverAction.PIN_KEY)
		listOfStrings.add(Constants.RECIPIENT_INSTITUTION)
		return listOfStrings;
	}


	private fun initNonStandardVariables(variableKeys: List<String>) {
		val map: LinkedHashMap<String, String> = LinkedHashMap()
		variableKeys.forEach {
			map[it] = ""
		}
		nonStandardVariables.postValue(map)
	}

	private fun nullifyNonStandardVariables() {
		nonStandardVariables.postValue(null)
	}

	fun updateNonStandardVariables(key: String, value: String) {
		val map: LinkedHashMap<String, String> = nonStandardVariables.value!!
		map[key] = value
		nonStandardVariables.postValue(map)
	}
}