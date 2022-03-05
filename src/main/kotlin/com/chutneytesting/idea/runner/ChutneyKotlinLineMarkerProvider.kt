package com.chutneytesting.idea.runner

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.util.hasAnnotationWithShortName
import org.jetbrains.kotlin.psi.KtNamedFunction

class ChutneyKotlinLineMarkerProvider : ChutneyLineMarkerProvider() {

    override fun getLineMarkerInfo(psiElement: PsiElement): LineMarkerInfo<*>? {
        if ((psiElement is KtNamedFunction) && psiElement.hasAnnotationWithShortName("KChutney")) {
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
