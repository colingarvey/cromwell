package centaur

import java.io.FileNotFoundException
import java.nio.file.Path

import scala.util.{Failure, Success, Try}

object WorkflowRequest {
  /**
    * Assumes a path like /foo/bar/blah where there'll be blah.wdl, blah.inputs, blah.options. Not
    * at all bulletproof at the moment and will barf all over your face if you do it wrong
    *
    * TODO: It'd be good to make inputs and options optional, I'm pretty sure the submission endpoint views them
    * as optional anyways
    */
  def apply(path: Path): WorkflowRequest = {
    val name = path.getFileName

    val base = path.resolve(name)
    val wdl = base.slurpExtension("wdl")
    val inputs = base.slurpExtensionIfExists("inputs")
    val options = base.slurpExtensionIfExists("options")

    WorkflowRequest(name.toString, wdl, inputs, options)
  }

  implicit class EnhancedPath(val path: Path) extends AnyVal {
    def addExtension(extension: String): Path = path.resolveSibling(s"${path.getFileName}.$extension")
    def slurpExtension(extension: String): String = path.addExtension(extension).slurp

    def slurpExtensionIfExists(extension: String): Option[String] = {
      val attempt = Try(slurpExtension(extension))
      attempt match {
        case Success(x) => Option(x)
        case Failure(t: FileNotFoundException) => None
        case Failure(t) => throw t
      }
    }

    /** Read an entire file into a string, closing the underlying stream. */
    def slurp: String = {
      val source = io.Source.fromFile(path.toFile)
      try source.mkString finally source.close()
    }
  }
}

case class WorkflowRequest(name: String, wdl: String, inputs: Option[String], options: Option[String])
