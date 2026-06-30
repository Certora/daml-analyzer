package com.certora.damlanalyzer.schema

sealed trait InteractionType

object InteractionType {
  case object Exercise            extends InteractionType
  case object ExerciseInterface   extends InteractionType
  case object ExerciseByKey       extends InteractionType
  case object Create              extends InteractionType
  case object CreateInterface     extends InteractionType
  case object Fetch               extends InteractionType
  case object FetchInterface      extends InteractionType
  case object FetchByKey          extends InteractionType
  case object LookupByKey         extends InteractionType
  case object QueryNByKey         extends InteractionType
  case object ImplementsInterface extends InteractionType
}
