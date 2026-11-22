package com.marble098.marblebr.vault

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VaultModule {

    @Binds
    @Singleton
    abstract fun bindSecretKeyProvider(
        impl: AndroidKeystoreSecretKeyProvider,
    ): SecretKeyProvider

    /** اثر انگشتِ گذرواژه هم با کلیدِ غیرقابل‌استخراجِ Keystore ساخته می‌شود. */
    @Binds
    @Singleton
    abstract fun bindFingerprintKeyProvider(
        impl: KeystoreFingerprintKeyProvider,
    ): FingerprintKeyProvider

    /** تنظیماتِ ورود از DataStoreِ ماژول `:core` خوانده می‌شود. */
    @Binds
    @Singleton
    abstract fun bindLoginSettings(
        impl: PreferencesLoginSettings,
    ): LoginSettings
}
