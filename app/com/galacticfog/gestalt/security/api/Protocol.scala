package com.galacticfog.gestalt.security.api

sealed abstract class Protocol
final case object HTTP  extends Protocol {override def toString() = "http"}
final case object HTTPS extends Protocol {override def toString() = "https"}

