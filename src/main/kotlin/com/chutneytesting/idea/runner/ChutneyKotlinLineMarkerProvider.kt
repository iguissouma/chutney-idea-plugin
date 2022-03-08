package com.chutneytesting.idea.runner

import com.chutneytesting.idea.ChutneyUtil
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction

class ChutneyKotlinLineMarkerProvider : ChutneyLineMarkerProvider() {

    override fun getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<*>? {
        if (ChutneyUtil.isChutneyDslMethod(psiElement) && psiElement is KtNamedFunction) {
            val displayName = "${psiElement.name}"
            return lineMarkerInfo(psiElement.funKeyword!!, displayName)
        }
        return null
    }


    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result:  MutableCollection<in LineMarkerInfo<*>>
    ) {}
}
