package ghpages.examples.util

import japgolly.scalajs.react._, vdom.all._
import org.scalajs.dom.document
import org.scalajs.dom.ext.PimpedNodeList

object SideBySide {

  case class Content(jsSource: String, scalaSource: String, el: ReactElement) {
    def apply() = sideBySideComponent(this)
  }

  val sideBySideComponent = ReactComponentB[Content]("sideBySideExample")
    .render_P(p =>
      div(
        section(cls := "demo",
          div(cls := "demo", p.el)),
        hr,
        div(`class` := "row",
          div(`class` := "col-md-6",
            h3("JS source"),
            pre(code(p.jsSource.trim))),
          div(`class` := "col-md-6",
            h3("Scala source"),
            pre(code(p.scalaSource.trim))))
      )
    )
    .configure(installSyntaxHighlighting)
    .build

  def installSyntaxHighlighting[P, S, B, N <: TopNode] =
    (_: ReactComponentB[P, S, B, N])
      .componentDidMountCB(applySyntaxHighlight)
      .componentDidUpdateCB(applySyntaxHighlight)

  def applySyntaxHighlight = Callback {
    import scala.scalajs.js.Dynamic.{global => g}
    val nodeList = document.querySelectorAll("pre code").toArray
    nodeList.foreach(g.hljs highlightBlock _)
  }
}