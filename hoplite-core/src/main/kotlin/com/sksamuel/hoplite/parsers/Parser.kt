package com.sksamuel.hoplite.parsers

import arrow.core.toOption
import arrow.core.invalid
import arrow.core.valid
import com.sksamuel.hoplite.ConfigFailure
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.Node
import java.io.InputStream
import java.util.*

interface Parser {
  fun load(input: InputStream, source: String): Node
  fun defaultFileExtensions(): List<String>
}

interface ParserRegistry {

  fun locate(ext: String): ConfigResult<Parser>

  fun register(ext: String, parser: Parser): ParserRegistry

  /**
   * Returns the currently supported file mappings
   */
  fun registeredExtensions(): Set<String>

  companion object {
    val zero: ParserRegistry = DefaultParserRegistry(emptyMap())
  }
}

class DefaultParserRegistry(private val map: Map<String, Parser>) : ParserRegistry {

  override fun locate(ext: String): ConfigResult<Parser> {
    return map[ext].toOption().fold({ ConfigFailure.NoSuchParser(ext, map).invalid() }, { it.valid() })
  }

  override fun registeredExtensions(): Set<String> = map.keys

  override fun register(ext: String, parser: Parser): ParserRegistry = DefaultParserRegistry(
    map.plus(ext to parser))
}

fun defaultParserRegistry(): ParserRegistry {
  return ServiceLoader.load(Parser::class.java).toList()
      .fold(ParserRegistry.zero) { registry, parser ->
        parser.defaultFileExtensions().fold(registry) { r, ext -> r.register(ext, parser) }
      }
}