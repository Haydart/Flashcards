package com.rossomak.flashcards.domain.usecase.base

interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): R
}

interface NoParamUseCase<out R> {
    suspend operator fun invoke(): R
}
