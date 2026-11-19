package com.marble098.marblebr.core.i18n

import com.marble098.marblebr.core.preferences.BrowserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * منبع واحدِ زبان برنامه.
 *
 * هم لایه‌ی رابط کاربری (Compose) و هم ViewModelها از اینجا متن‌ها را می‌گیرند تا هیچ متنِ
 * سخت‌کدشده‌ای در برنامه باقی نماند.
 */
@Singleton
class LocalizationManager @Inject constructor(
    private val preferencesRepository: BrowserPreferencesRepository,
) {
    val languageFlow: Flow<AppLanguage> = preferencesRepository.preferencesFlow
        .map { it.language }
        .distinctUntilChanged()

    fun strings(language: AppLanguage = AppLanguage.SYSTEM): MarbleStrings =
        MarbleStrings.forLanguage(language, Locale.getDefault().toLanguageTag())

    suspend fun current(): MarbleStrings =
        strings(preferencesRepository.preferencesFlow.first().language)
}
