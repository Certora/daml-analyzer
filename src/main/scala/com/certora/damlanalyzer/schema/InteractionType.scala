package com.certora.damlanalyzer.schema

sealed trait InteractionType

object InteractionType {
  // todo: add others as needed later
  case object Exercise extends InteractionType
}
