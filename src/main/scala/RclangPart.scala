import rclang.lexer.Lexer
import rclang.parser.RcParser
import rclang.ast.*

import java.io.File
import java.net.URI
import rclang.tools.{DumpManager, GlobalTable, unwrap}
import org.eclipse.lsp4j.*

import scala.collection.immutable

def driver(uri: String) = {
  // 1. read from uri
  val f = new File(URI.create(uri))
  val source = scala.io.Source.fromFile(f)
  val src = source.getLines().mkString("\n") + "\n"
  // 2. parse and return ast
  val ast = RcParser(Lexer(src).unwrap).unwrap
  ast
}

def symbolTable(uri: String): GlobalTable = {
  val ast = driver(uri)
  symbolTable(ast)
}

def symbolTable(ast: RcModule): GlobalTable = {
  rclang.compiler.Driver.typeProc(ast)._2
}

def initRclang() = {
  DumpManager.mkDumpRootDir
}

def keywords = rclang.lexer.Keyword.values

extension (p: ASTNode) {
  def getPosition = new Position(p.pos.line, p.pos.column)

  def getPositionRange = p match
    case method: Method => PositionRange.getMethodPositionRange(method)
    case fieldDef: FieldDef => PositionRange.getFieldPositionRange(fieldDef)
    case klass: rclang.ast.Class => PositionRange.getClassPositionRange(klass)
    case _ => new Range(p.getPosition, p.getPosition)
}

object PositionRange {
  def getMethodPositionRange(method: Method) = {
    val begin = method.name.getPosition
    if (method.body.stmts.isEmpty) {
      new Range(begin, begin)
    } else {
      val end = method.body.stmts.last.getPosition
      new Range(begin, end)
    }
  }

  def getFieldPositionRange(fieldDef: FieldDef) =
    new Range(fieldDef.name.getPosition, fieldDef.getPosition)

  def getClassPositionRange(klass: rclang.ast.Class) = {
    val symbols = klass.methods.concat(klass.vars)
    val begin = klass.name.getPosition
    if (symbols.isEmpty) {
      new Range(begin, begin)
    }
    else {
      val end = getMethodPositionRange(klass.methods.maxBy(_.name.pos.line)).getEnd
      new Range(begin, end)
    }
  }
}