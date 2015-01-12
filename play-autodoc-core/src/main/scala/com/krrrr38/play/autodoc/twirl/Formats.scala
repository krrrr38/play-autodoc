package com.krrrr38.play.autodoc.twirl

import play.twirl.api.{ BufferedContent, Format }

import scala.collection.immutable
import play.twirl.api.Formats

/**
 * Type used in default Makrodown templates.
 */
class Markdown private (elements: immutable.Seq[Markdown], text: String) extends BufferedContent[Markdown](elements, text) {
  def this(text: String) = this(Nil, Formats.safe(text))
  def this(elements: immutable.Seq[Markdown]) = this(elements, "")

  /**
   * Content type of Markdown
   */
  val contentType = "text/x-markdown"
}

/**
 * Helper for Markdown utility methods.
 */
object Markdown {

  /**
   * Creates an Markdown fragment with initial content specified.
   */
  def apply(text: String): Markdown = {
    new Markdown(text)
  }
}

/**
 * Formatter for Markdown content.
 * which is same as TxtFormat
 */
object MarkdownFormat extends Format[Markdown] {

  /**
   * Create a Markdown fragment.
   */
  def raw(text: String) = Markdown(text)

  /**
   * No need for a safe (escaped) text fragment.
   */
  def escape(text: String) = Markdown(text)

  /**
   * Generate an empty Markdown fragment
   */
  val empty: Markdown = new Markdown("")

  /**
   * Create an Markdown Fragment that holds other fragments.
   */
  def fill(elements: immutable.Seq[Markdown]): Markdown = new Markdown(elements)

}
