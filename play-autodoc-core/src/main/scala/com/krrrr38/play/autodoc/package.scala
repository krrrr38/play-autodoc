package com.krrrr38.play

package object autodoc {
  type HeaderKey = String
  type HeaderValue = String

  def normalizeText(content: String, existPrefix: String = "") =
    if (content.trim.isEmpty) "" else existPrefix + content.trim
}
