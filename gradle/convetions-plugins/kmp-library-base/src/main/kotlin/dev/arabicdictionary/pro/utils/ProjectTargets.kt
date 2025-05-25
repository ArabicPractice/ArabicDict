@file:Suppress("unused")

package dev.arabicdictionary.pro.utils

/**
 * Targets for the project
 */
sealed interface ProjectTargets {
    sealed interface JvmTarget

    object Desktop : ProjectTargets, JvmTarget

    object Android : ProjectTargets, JvmTarget

    sealed interface IOS : ProjectTargets {
        sealed interface Simulator

        object SimulatorX64 : IOS, Simulator

        object SimulatorArm64 : IOS, Simulator

        object ARM64 : IOS
    }
}
