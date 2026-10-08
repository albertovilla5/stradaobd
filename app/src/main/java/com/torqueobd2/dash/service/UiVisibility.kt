package com.torqueobd2.dash.service

/** La pantalla anterior puede detenerse después de que empiece su sustituta. */
internal class UiVisibility {
    private val owners = mutableSetOf<Any>()
    fun update(owner: Any, visible: Boolean): Boolean {
        if (visible) owners.add(owner) else owners.remove(owner)
        return owners.isNotEmpty()
    }
}
