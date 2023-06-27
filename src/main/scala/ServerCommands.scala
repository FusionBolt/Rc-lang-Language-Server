import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j

import collection.JavaConverters.seqAsJavaListConverter
import collection.JavaConverters.asScalaBufferConverter
import scala.reflect.ClassTag
import javax.annotation.Nullable

import JsonParser.*

sealed trait BaseCommand {
  def cmd: String

  protected def isValidCommand(params: ExecuteCommandParams): Boolean =
    params.getCommand == cmd
}

case class Command(cmd: String, desc: String) extends BaseCommand {
  def unapply(params: ExecuteCommandParams): Boolean = {
    isValidCommand(params)
  }

  def toExecuteCommandParams(): ExecuteCommandParams = {
    new ExecuteCommandParams(
      cmd,
      List[Object]().asJava,
    )
  }
}

case class ParametrizedCommand[T: ClassTag](cmd: String, desc: String, srgs: String) extends BaseCommand {
  private val parser = new JsonParser.Of[T]

  def unapply(params: ExecuteCommandParams): Option[T] = {
    if (!isValidCommand(params))
      return None

    val args = Option(params.getArguments()).toList.flatMap(_.asScala)
    if (args.size != 1) None
    else {
      args(0) match {
        case parser.Jsonized(t1) =>
          Option(t1)
        case _ => None
      }
    }
  }

  def toExecuteCommandParams(argument: T): ExecuteCommandParams = {
    new ExecuteCommandParams(
      cmd,
      List[Object](
        argument.toJson
      ).asJava,
    )
  }
}

case class ListParametrizedCommand[T: ClassTag](
                                                 cmd: String,
                                                 title: String,
                                                 description: String,
                                                 arguments: String,
                                               ) extends BaseCommand {
  private val parser = new JsonParser.Of[T]

  def unapply(params: ExecuteCommandParams): Option[List[Option[T]]] = {
    if (!isValidCommand(params)) {
      return None
    }
    val args = Option(params.getArguments()).toList
      .flatMap(_.asScala)
      .map {
        case parser.Jsonized(t) => Option(t)
        case _ => None
      }
    Some(args)
  }

  def toExecuteCommandParams(argument: T*): ExecuteCommandParams = {
    new ExecuteCommandParams(
      cmd,
      argument.map(_.toJson.asInstanceOf[AnyRef]).asJava,
    )
  }
}


final case class DiscoverTestParams(@Nullable mid: Int = 0,
                                   @Nullable fsPath: String = null,
                                   @Nullable external: String = null,
                                   @Nullable path: String = null,
                                   @Nullable scheme: String = null)

object ServerCommands {
  val RCC = ListParametrizedCommand[DiscoverTestParams](
    "rclang.rcc",
    "Request Client Connection",
    "desc", "args")
  val Run = Command("rclang.run", "Request Client Connection")

//  val Debugger = ParametrizedCommand[DebugSessionParams]("debug-adapter-start", "Start Debugger")

  val all = List(RCC, Run)

  val allIds = all.map(_.cmd).toSet
}