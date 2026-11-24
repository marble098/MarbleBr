package com.marble098.marblebr.feature.browser

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import org.mozilla.geckoview.BasicSelectionActionDelegate
import org.mozilla.geckoview.GeckoSession

/** Retain Gecko's select/copy/cut/paste actions and add explicit share and PDF actions. */
internal class PageSelectionDelegate(
    activity: Activity,
    private val shareLabel: String,
    private val pdfLabel: String,
    private val searchLabel: String,
    private val onSavePdf: () -> Unit,
    private val onSearchSelection: (String) -> Unit,
) : BasicSelectionActionDelegate(activity) {
    override fun getAllActions(): Array<String> = super.getAllActions() + arrayOf(SHARE, SEARCH, SAVE_PDF)

    override fun isActionAvailable(id: String): Boolean = when (id) {
        SHARE, SEARCH -> selection?.let {
            it.text.isNotBlank() && it.flags and GeckoSession.SelectionActionDelegate.FLAG_IS_PASSWORD == 0
        } == true
        SAVE_PDF -> selection != null
        else -> super.isActionAvailable(id)
    }

    override fun prepareAction(id: String, item: MenuItem) {
        when (id) {
            SHARE -> item.title = shareLabel
            SEARCH -> item.title = searchLabel
            SAVE_PDF -> item.title = pdfLabel
            else -> super.prepareAction(id, item)
        }
    }

    override fun performAction(id: String, item: MenuItem): Boolean = when (id) {
        SHARE -> {
            val text = selection?.text?.take(MAX_SHARE_LENGTH).orEmpty()
            if (text.isBlank()) {
                false
            } else {
                val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                mActivity.startActivity(Intent.createChooser(intent, shareLabel))
                clearSelection()
                true
            }
        }
        SEARCH -> {
            val text = selection?.text?.trim()?.take(MAX_SEARCH_LENGTH).orEmpty()
            if (text.isBlank()) {
                false
            } else {
                clearSelection()
                onSearchSelection(text)
                true
            }
        }
        SAVE_PDF -> {
            clearSelection()
            onSavePdf()
            true
        }
        else -> super.performAction(id, item)
    }

    private companion object {
        const val SHARE = "marblebr.share.selection"
        const val SEARCH = "marblebr.search.selection"
        const val SAVE_PDF = "marblebr.save.pdf"
        const val MAX_SHARE_LENGTH = 100_000
        const val MAX_SEARCH_LENGTH = 2_000
    }
}
