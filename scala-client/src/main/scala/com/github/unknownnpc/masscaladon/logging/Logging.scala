package com.github.unknownnpc.masscaladon.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

private[masscaladon] trait Logging:
  protected val logger: Logger = LoggerFactory.getLogger(getClass)
